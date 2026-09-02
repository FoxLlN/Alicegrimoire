package org.aliceGrimoire.alicegrimoire.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.aliceGrimoire.alicegrimoire.entity.doll.data.*;
import org.aliceGrimoire.alicegrimoire.entity.doll.equipment.DollEquipmentHandler;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollState;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollStateManager;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.DollCombatManager;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.DollTargetSelector;
import org.aliceGrimoire.alicegrimoire.entity.doll.movement.DollMoveControl;
import org.aliceGrimoire.alicegrimoire.entity.doll.movement.DollMovementHandler;
import org.aliceGrimoire.alicegrimoire.entity.doll.util.DollCollisionHelper;
import org.aliceGrimoire.alicegrimoire.event.PlayerMoveDetector;
import org.aliceGrimoire.alicegrimoire.item.string.StringHelper;
import org.aliceGrimoire.alicegrimoire.registry.ModDataComponents;
import org.aliceGrimoire.alicegrimoire.registry.ModItems;
import org.jetbrains.annotations.Nullable;

import com.mojang.logging.LogUtils;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DollEntity extends PathfinderMob implements GeoEntity, OwnableEntity, RangedAttackMob {
    
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    // ========== 数据管理器 ==========
    private final DollDataManager dataManager;

    private int shieldDisableTicks = 0;
    private boolean isPlayerMoving = false;
    private int assignedTargetId = -1;

    // ========== 状态管理器 ==========
    private boolean lastHasString;

    // 返回模式（人偶哨）
    private boolean isReturning = false;
    // 激怒时间（用于发光等）
    private long enrageTime = 0;
    // 不主动移动的剩余 tick 数（人偶哨使用）
    private int noMovementTicks = 0;

    // 平滑目标Y坐标（用于人偶跟随目标高度时的平滑滤波）
    private double smoothedTargetY = Double.NaN;

    // ========== 同步数据字段 ==========
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> EVOKE_TIME =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> IS_ENRAGED =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> IS_TETHERED =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> DATA_MAIN_HAND =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_OFF_HAND =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.ITEM_STACK);
    
    // ========== 核心管理器 ==========
    private final DollStateManager stateManager;
    private final DollMovementHandler movementHandler;
    private final DollCombatManager combatManager;
    private final DollEquipmentHandler equipmentHandler;

    // ========== GeckoLib 缓存 ==========
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    // ========== 构造函数 ==========
    public DollEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.noPhysics = true;
        
        // 初始化数据管理器（必须在最前面）
        this.dataManager = new DollDataManager(this);
        
        // 初始化其他管理器
        this.stateManager = new DollStateManager(this);
        this.movementHandler = new DollMovementHandler(this, stateManager);
        this.combatManager = new DollCombatManager(this);
        this.moveControl = new DollMoveControl(this);
        this.equipmentHandler = new DollEquipmentHandler(this);
    }

    // ========== 数据同步 ==========
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(EVOKE_TIME, 0L);
        builder.define(IS_ENRAGED, false);
        builder.define(IS_TETHERED, false);
        builder.define(DATA_MAIN_HAND, ItemStack.EMPTY);
        builder.define(DATA_OFF_HAND, ItemStack.EMPTY);
    }

    // ========== 主要 tick 逻辑 ==========
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 1. 更新拴住状态（检测护腿装备）
            LivingEntity owner = this.getOwner();
            if (owner instanceof Player player) {
                boolean moving = PlayerMoveDetector.getPlayerMoving(player.getUUID());
                this.isPlayerMoving = moving; // 直接赋值
            }

            if (owner != null) {
                boolean hasString = StringHelper.hasStringEquipped(owner);
                // 检测丝线状态变化（缓存上次状态）
                if (this.lastHasString != hasString) {
                    if (this.lastHasString && !hasString) {
                        // ===== 脱下丝线：解除所有拴住 =====
                        List<DollEntity> dolls = owner.level().getEntitiesOfClass(DollEntity.class,
                            owner.getBoundingBox().inflate(64.0),
                            d -> owner.getUUID().equals(d.getOwnerUUID()) && d.isTethered()
                        );
                        for (DollEntity d : dolls) {
                            d.setTethered(false);
                        }
                    } else if (!this.lastHasString && hasString) {
                        // ===== 穿上丝线：自动拴住周围最多数量的人偶 =====
                        if (owner instanceof Player playerOwner) {
                            // 先收集所有需要拴住的人偶（延迟执行）
                            List<DollEntity> untetheredDolls = owner.level().getEntitiesOfClass(DollEntity.class,
                                owner.getBoundingBox().inflate(64.0),
                                d -> owner.getUUID().equals(d.getOwnerUUID()) && !d.isTethered() && d.isAlive()
                            );
                            
                            int maxSlots = StringHelper.getMaxTethered(playerOwner);
                            int currentOccupied = StringHelper.countOccupiedSlots(playerOwner);
                            int available = maxSlots - currentOccupied;
                            int toTether = Math.min(available, untetheredDolls.size());
                            
                            // 避免在循环中修改列表导致 ConcurrentModificationException
                            if (toTether > 0) {
                                // 复制一份需要拴住的人偶列表，防止在 setTethered 过程中列表被修改
                                List<DollEntity> toTetherList = new ArrayList<>(untetheredDolls.subList(0, toTether));
                                for (DollEntity d : toTetherList) {
                                    d.setTethered(true);
                                }
                            }
                        }
                    }
                    this.lastHasString = hasString;
                }
            }

            // 2. 主人失明/黑暗时强制解除激怒并清空目标
            if (owner != null && (owner.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS) ||
                    owner.hasEffect(net.minecraft.world.effect.MobEffects.DARKNESS))) {
                if (this.isEnraged()) {
                    this.setEnraged(false);
                }
                this.setTarget(null);
            }

            // 如果激怒但指定目标无效，则解除激怒
            if (this.isEnraged() && assignedTargetId != -1) {
                Entity target = this.level().getEntity(assignedTargetId);
                if (!(target instanceof LivingEntity) || !target.isAlive()) {
                    this.setEnraged(false);
                }
            }

            // ===== 守御人偶自动索敌与激怒 =====
            if (this.getJobType() == DollJobType.DEFENDER) {
                if (owner != null) {
                    List<LivingEntity> enemies = owner.level().getEntitiesOfClass(
                        LivingEntity.class,
                        owner.getBoundingBox().inflate(8.0),
                        e -> e != owner && e != this && e.isAlive() && owner.canAttack(e)
                    );
                    LivingEntity nearest = null;
                    if (!enemies.isEmpty()) {
                        nearest = enemies.stream()
                            .min((a, b) -> Double.compare(a.distanceToSqr(owner), b.distanceToSqr(owner)))
                            .orElse(null);
                    }
                    
                    if (nearest != null) {
                        if (this.getTarget() != nearest) {
                            this.setTarget(nearest);
                        }
                        if (!this.isEnraged()) {
                            this.setEnraged(true);
                        }
                    } else {
                        if (this.isEnraged()) {
                            this.setEnraged(false);
                        }
                        this.setTarget(null);
                    }
                }
            }
            
            // ===== 距离检测：强制解除激怒 =====
            if (owner != null && this.isTethered()) {
                double distance = this.distanceTo(owner);
                double dragForce = this.getDollData().getDragForceRange(); // 24
                if (distance > dragForce) {
                    if (this.isEnraged()) {
                        this.setEnraged(false);
                        this.setTarget(null);
                        if (owner instanceof Player player) {
                            player.displayClientMessage(
                                Component.translatable("message.alicegrimoire.doll_too_far"),
                                true
                            );
                        }
                    }
                }
            }

            if (noMovementTicks > 0) {
                noMovementTicks--;
                // 如果还在不移动状态，清除移动目标
                if (noMovementTicks > 0) {
                    this.getMoveControl().setWantedPosition(
                        this.getX(), this.getY(), this.getZ(), 0
                    );
                    this.setDeltaMovement(Vec3.ZERO);
                } else {
                    // 不移动状态结束，自动退出返回模式
                    this.setReturning(false);
                }
            }

            // 3. 状态机更新
            stateManager.tick();
            
            // 4. 移动控制
            movementHandler.tick();

            // 5. 战斗逻辑（仅在 ENGAGING 状态下执行）
            if (stateManager.getCurrentState() == DollState.ENGAGING) {
                combatManager.tick();
            }

            // 6. 自动回血（未激怒时每2秒1点）
            if (!this.isEnraged() && this.tickCount % 40 == 0) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(1.0F);
                }
            }
            
            // 拾取掉落物（每5 tick检测一次）
            if (this.tickCount % 5 == 0) {
                double pickupRange = this.getDollData().getPickupRange();
                List<ItemEntity> items = this.level().getEntitiesOfClass(
                    ItemEntity.class,
                    this.getBoundingBox().inflate(pickupRange),
                    item -> !item.hasPickUpDelay() && item.isAlive() && !item.getItem().isEmpty()
                );
                // 按距离排序，优先拾取最近的
                items.sort(Comparator.comparingDouble(e -> e.distanceToSqr(this)));
                for (ItemEntity itemEntity : items) {
                    if (tryPickupItem(itemEntity)) {
                        break; // 一次只捡一个（与玩家行为一致，也可连续捡多个，但每5 tick足够）
                    }
                }
            }

        }

        // 8. 人偶互相排斥
        if (!this.level().isClientSide) {
            applyRepulsion();
        }
        if (shieldDisableTicks > 0) {
            shieldDisableTicks--;
        }
    }

    /**
     * 应用人偶之间的排斥力
     */
    private void applyRepulsion() {
        List<DollEntity> nearby = this.level().getEntitiesOfClass(
            DollEntity.class,
            this.getBoundingBox().inflate(1.5),
            other -> other != this
        );
        if (nearby.isEmpty()) return;

        double repelX = 0, repelY = 0, repelZ = 0;
        for (DollEntity other : nearby) {
            double dx = this.getX() - other.getX();
            double dy = this.getY() - other.getY();
            double dz = this.getZ() - other.getZ();
            double distSq = dx*dx + dy*dy + dz*dz;
            if (distSq < 1.0 && distSq > 0.0001) {
                double dist = Math.sqrt(distSq);
                // 强度根据是否开启物理而调整：开启物理时减小强度，避免干扰碰撞箱
                double strength = this.noPhysics ? 0.3 / (dist + 0.1) : 0.1 / (dist + 0.1);
                strength = Math.min(strength, 0.3);
                double invDist = 1.0 / dist;
                repelX += dx * invDist * strength;
                repelY += dy * invDist * strength;
                repelZ += dz * invDist * strength;
                // 给对面也施加力
                other.applyRepulsionFrom(this, new Vec3(-dx * invDist * strength, -dy * invDist * strength, -dz * invDist * strength));
            }
        }
        
        if (repelX != 0 || repelY != 0 || repelZ != 0) {
            Vec3 newPos = this.position().add(repelX, repelY, repelZ);
            this.moveTo(newPos.x, newPos.y, newPos.z);
            // 轻微速度影响，让移动更平滑
            this.setDeltaMovement(this.getDeltaMovement().add(repelX * 0.2, repelY * 0.2, repelZ * 0.2));
        }
    }

    /**
     * 从其他人偶接收排斥力
     */
    public void applyRepulsionFrom(DollEntity source, Vec3 repel) {
        if (!this.noPhysics) return; // 只在无碰撞箱时响应
        Vec3 newPos = this.position().add(repel.x, repel.y, repel.z);
        this.moveTo(newPos.x, newPos.y, newPos.z);
        this.setDeltaMovement(this.getDeltaMovement().add(repel.x * 0.2, repel.y * 0.2, repel.z * 0.2));
    }

    public void syncEquipmentToClient() {
        if (!this.level().isClientSide) { // 只在服务端设置同步数据
            this.entityData.set(DATA_MAIN_HAND, equipmentHandler.getMainHand());
            this.entityData.set(DATA_OFF_HAND, equipmentHandler.getOffHand());
        }
    }

    
    public ItemStack getSyncMainHand() {
        return this.entityData.get(DATA_MAIN_HAND);
    }

    public ItemStack getSyncOffHand() {
        return this.entityData.get(DATA_OFF_HAND);
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi();
    }

    public int getNoMovementTicks() {
        return noMovementTicks;
    }

    public void setNoMovementTicks(int ticks) {
        this.noMovementTicks = ticks;
        if (ticks > 0) {
            // 开始不移动状态时，清除所有速度
            this.setDeltaMovement(Vec3.ZERO);
        }
    }

    /**
     * 尝试拾取一个物品实体，放入背包，若背包满则尝试自动装备。
     * @return true 如果成功拾取（物品被消耗）
     */
    private boolean tryPickupItem(ItemEntity itemEntity) {
        ItemStack stack = itemEntity.getItem();
        if (stack.isEmpty()) return false;

        // ===== 1. 先尝试合并到已有的背包物品 =====
        int backpackStart = DollSlots.BACKPACK_START;
        int backpackEnd = backpackStart + this.getDollData().getBackpackSlots();
        for (int i = backpackStart; i < backpackEnd; i++) {
            ItemStack existing = this.getDollData().getItem(i);
            if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                int maxStack = existing.getMaxStackSize();
                int space = maxStack - existing.getCount();
                if (space > 0) {
                    int toAdd = Math.min(space, stack.getCount());
                    existing.grow(toAdd);
                    stack.shrink(toAdd);
                    if (stack.isEmpty()) {
                        itemEntity.discard();
                        this.getDataManager().applyDataToEntity(); // 刷新数据（可选）
                        return true;
                    }
                }
            }
        }

        // ===== 2. 寻找空背包槽位 =====
        for (int i = backpackStart; i < backpackEnd; i++) {
            if (this.getDollData().getItem(i).isEmpty()) {
                // 复制一份（取1个）
                ItemStack toPlace = stack.copy();
                toPlace.setCount(1);
                this.getDataManager().setItem(i, toPlace);
                stack.shrink(1);
                if (stack.isEmpty()) {
                    itemEntity.discard();
                    return true;
                }
                // 如果还有剩余，继续尝试合并或放其他槽
                // 但本次只拾取一个物品，所以我们break并返回true，剩余会留在原地
                // 但为了效率，我们可以继续循环，但这里先只处理一个物品
                // 我们可以选择继续，但为了简洁，我们返回true表示已消耗1个
                // 注意：如果stack还有剩余，itemEntity仍存在，下次tick再拾取
                return true;
            }
        }

        // ===== 3. 背包满，尝试自动装备（仅当物品可装备到主手/副手/盔甲） =====
        // 使用已有的 equipmentHandler.tryEquip
        boolean equipped = this.equipmentHandler.tryEquip(stack);
        if (equipped) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                itemEntity.discard();
            }
            return true;
        }
        // ===== 4. 无法拾取 =====
        return false;
    }

    // ========== 目标选择（注册Goal） ==========
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new DollStateGoal());
        this.goalSelector.addGoal(2, new DollMoveGoal());
        this.goalSelector.addGoal(3, new DollCombatGoal());

        this.targetSelector.addGoal(1, new DollTargetSelector(this));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        // 防止人偶攻击自己
        if (target == this) {
            this.setEnraged(false);
            return;
        }
        // 防止人偶攻击同主人的其他人偶
        if (target instanceof DollEntity otherDoll) {
            UUID thisOwner = this.getOwnerUUID();
            UUID otherOwner = otherDoll.getOwnerUUID();
            if (thisOwner != null && otherOwner != null && thisOwner.equals(otherOwner)) {
                this.setEnraged(false);
                return;
            }
        }
        // LOGGER.info("[进入激怒状态] 目标为：" + target);
        super.setTarget(target);
    }

    // ========== 内部 Goal 类 ==========
    private class DollStateGoal extends Goal {
        @Override public boolean canUse() { return true; }
        @Override public boolean canContinueToUse() { return true; }
        @Override public void tick() { stateManager.tick(); }
    }

    private class DollMoveGoal extends Goal {
        @Override public boolean canUse() { return true; }
        @Override public boolean canContinueToUse() { return true; }
        @Override public void tick() { movementHandler.tick(); }
    }

    private class DollCombatGoal extends Goal {
        @Override public boolean canUse() {
            return stateManager.getCurrentState() == DollState.ENGAGING && getTarget() != null;
        }
        @Override public boolean canContinueToUse() {
            return stateManager.getCurrentState() == DollState.ENGAGING && getTarget() != null && getTarget().isAlive();
        }
        @Override public void tick() { combatManager.tick(); }
    }

    // ========== 属性 ==========
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    // ========== OwnableEntity 接口 ==========
    @Nullable @Override public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }
    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }
    @Nullable @Override public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerUUID();
            return uuid == null ? null : this.level().getPlayerByUUID(uuid);
        } catch (Exception e) {
            return null;
        }
    }

    // ========== 激怒状态 ==========
    public boolean isEnraged() {
        return this.entityData.get(IS_ENRAGED);
    }
    public void setEnraged(boolean enraged) {
        // LOGGER.info("[进入激怒状态] 是否激怒：" + enraged);
        this.entityData.set(IS_ENRAGED, enraged);
        if (!enraged) {
            this.setTarget(null);
            this.enrageTime = 0;
            this.assignedTargetId = -1;
        }
        // 强制立即执行状态机，减少 tick 延迟
        if (!this.level().isClientSide) {
            this.stateManager.tick();
        }
    }

    // ========== 数据管理器访问 ==========
    public DollDataManager getDataManager() {
        return dataManager;
    }

    public DollData getDollData() {
        return dataManager.getData();
    }

    // ========== 职业相关 ==========
    public DollJobType getJobType() {
        return dataManager.getData().getJobType();
    }
    public void setJobType(DollJobType jobType) {
        dataManager.setJobType(jobType);
    }

    // ========== 破损状态 ==========
    public boolean isBroken() { return dataManager.getData().isBroken(); }
    public void setBroken(boolean broken) { dataManager.getData().setBroken(broken); }

    // ========== canBeEnraged ==========
    public boolean canBeEnraged() {
        if (this.isEnraged()) return false;
        if (!this.isAlive()) return false;
        if (this.isBroken()) return false;
        return true;
    }

    // ========== 唤起时间 ==========
    public long getEvokeTime() { return this.entityData.get(EVOKE_TIME); }
    public void setEvokeTime(long time) { this.entityData.set(EVOKE_TIME, time); }

    // ========== 拴住状态 ==========   
        public boolean isTethered() {
        return this.entityData.get(IS_TETHERED);
    }

    public void setTethered(boolean tethered) {
        getDollData().setTethered(tethered);       // 持久化
        this.entityData.set(IS_TETHERED, tethered); // 同步到客户端
        getDollData().setOccupiesSlot(tethered);
    }
    
    public boolean isTethered(LivingEntity owner) {
        if (owner == null) return false;
        ItemStack leggings = owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        return !leggings.isEmpty() && leggings.getItem() instanceof org.aliceGrimoire.alicegrimoire.item.DollStringItem;
    }

    // ========== 属性刷新 ==========
    public void refreshAttributes() {
        // 由 DollDataManager 负责
        dataManager.applyDataToEntity();
    }

    // ========== 伤害处理（近卫盾牌反击） ==========
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypeTags.IS_FALL)) return false;
        
        if (this.isBlocking() && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            float reducedAmount = amount * 0.5f;
            if (source.getDirectEntity() instanceof LivingEntity attacker && !isSameOwner(attacker)) {
                attacker.hurt(this.damageSources().mobAttack(this), 4.0F);
                Vec3 dir = attacker.position().subtract(this.position()).normalize();
                attacker.knockback(0.5, -dir.x, -dir.z);
            }
            if (source.getDirectEntity() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandItem();
                if (weapon.canDisableShield(ItemStack.EMPTY, this, attacker)) {
                    CombatParameters params = this.getDollData().getCombatParams();
                    this.shieldDisableTicks = params.getShieldDisableTime();
                }
            }
            if (reducedAmount > 0) {
                return super.hurt(source, reducedAmount);
            } else {
                return false;
            }
        }

        if (this.getJobType() == DollJobType.DEFENDER && !source.is(DamageTypeTags.IS_FALL)) {
            CombatParameters params = this.getDollData().getCombatParams();
            int disableTime = params.getShieldDisableTime();
            // 检测斧头破盾效果
            if (source.getDirectEntity() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandItem();
                if (weapon.canDisableShield(ItemStack.EMPTY, this, attacker)) {
                    disableTime = (int)(disableTime * 1.5);
                }
            }
            this.setShieldDisableTicks(disableTime);
        }  
            
        // ===== 执行伤害 =====
        boolean result = super.hurt(source, amount);
        
        // ===== 如果确实受到了伤害，消耗盔甲耐久度 =====
        if (result && !this.level().isClientSide && amount > 0) {
            // 盔甲耐久度消耗（参考原版 LivingEntity 的实现）
            damageArmor(source, amount);
        }
        
        return result;
    }

    @Override
    public boolean isBlocking() {
        DollJobType job = getJobType();
        if (!(job == DollJobType.GUARD || job == DollJobType.DEFENDER)) return false;
        if (shieldDisableTicks > 0) return false;
        
        ItemStack offHand = getOffhandItem();
        boolean hasShield = offHand.getItem() instanceof ShieldItem;
        if (!hasShield) return false;

        if (job == DollJobType.DEFENDER) {
            // 守御人偶：只要持有盾牌且未破盾就举盾，不依赖状态
            return true;
        }
        // 近卫人偶：需处于战斗状态才举盾
        return stateManager.getCurrentState() == DollState.ENGAGING;
    }

    public int getShieldDisableTicks() { return shieldDisableTicks; }
    public void setShieldDisableTicks(int ticks) { this.shieldDisableTicks = ticks; }

    /**
     * 消耗人偶装备的盔甲耐久度
     * 将伤害平均分配给所有已装备的盔甲
     */
    private void damageArmor(DamageSource source, float amount) {
        if (amount <= 0) return;

        // ===== 1. 统计已装备的盔甲数量 =====
        int armorCount = 0;
        for (int slot = DollSlots.HELMET; slot <= DollSlots.BOOTS; slot++) {
            ItemStack stack = this.getDollData().getItem(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                armorCount++;
            }
        }

        // 没有可损耗的盔甲，直接返回
        if (armorCount == 0) return;

        // ===== 2. 计算每件盔甲应承受的伤害 =====
        // 原版逻辑：总伤害 / 盔甲数量，最少为 1
        int damagePerArmor = Math.max(1, (int) (amount / armorCount));

        // ===== 3. 遍历所有盔甲槽位，消耗耐久 =====
        for (int slot = DollSlots.HELMET; slot <= DollSlots.BOOTS; slot++) {
            ItemStack armorStack = this.getDollData().getItem(slot);
            if (armorStack.isEmpty() || !armorStack.isDamageableItem()) continue;

            // 消耗耐久
            armorStack.hurtAndBreak(damagePerArmor, this, getEquipmentSlotForSlot(slot));

            // 如果盔甲损坏，清空该槽位
            if (armorStack.isEmpty()) {
                this.getDollData().setItem(slot, ItemStack.EMPTY);
            }
        }

        // ===== 4. 护甲变化后刷新属性（可能有的盔甲损坏了） =====
        this.getDataManager().applyDataToEntity();
        this.syncEquipmentToClient();
    }

    /**
     * 将 DollSlots 槽位映射到 EquipmentSlot
     */
    private EquipmentSlot getEquipmentSlotForSlot(int dollSlot) {
        return switch (dollSlot) {
            case DollSlots.HELMET -> EquipmentSlot.HEAD;
            case DollSlots.CHESTPLATE -> EquipmentSlot.CHEST;
            case DollSlots.LEGGINGS -> EquipmentSlot.LEGS;
            case DollSlots.BOOTS -> EquipmentSlot.FEET;
            default -> EquipmentSlot.MAINHAND;
        };
    }
    

    // ========== 远程攻击 ==========
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        DollJobType job = getJobType();
        WeaponType weapon = this.getDollData().getWeaponType();
        AbstractArrow arrow = null;
        if (job == DollJobType.SHARPSHOOTER) {
            if (weapon == WeaponType.TRIDENT) {
                // 投射三叉戟（使用三叉戟实体）
                // 暂时用箭代替，后续可完善
                arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
                arrow.setBaseDamage(4.0D);
            } else {
                arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
                arrow.setBaseDamage(3.0D);
            }
        } else if ((job == DollJobType.GUARD || job == DollJobType.VANGUARD) && (weapon == WeaponType.CROSSBOW || weapon == WeaponType.BOW)) {
            arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
            arrow.setBaseDamage(2.0D); // 近卫/游侠远程伤害略低
            LOGGER.info("[远程攻击] 职业类型：" + job + " 武器类型：" + weapon);
        }

        if (arrow != null) {
            double d0 = target.getX() - this.getX();
            double d1 = target.getY(0.33D) - arrow.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            arrow.shoot(d0, d1 + d3 * 0.2D, d2, velocity, (float)(14 - this.level().getDifficulty().getId() * 4));
            this.level().addFreshEntity(arrow);
        }
    }

    // ========== 平滑追踪 ==========
    /**
     * 获取平滑后的目标Y坐标（过滤击退跳跃等高频抖动）
     * @param target 当前攻击目标
     * @return 平滑后的Y坐标
     */
    public double getSmoothedTargetY(LivingEntity target) {
        if (target == null) return this.getY();
        
        double realY = target.getY();
        
        // 首次初始化
        if (Double.isNaN(smoothedTargetY)) {
            smoothedTargetY = realY;
            return smoothedTargetY;
        }
        
        // 如果目标Y变化过大（比如目标瞬间传送或掉入虚空），直接跳转
        double delta = realY - smoothedTargetY;
        if (Math.abs(delta) > 3.0) {
            smoothedTargetY = realY;
            return smoothedTargetY;
        }
        
        // 指数平滑：每帧向真实值靠近 20%（即 0.2 的平滑系数）
        // 系数越小过滤越强，击退跳跃越不明显；系数越大跟随越快
        double smoothingFactor = 0.15; // 可调整，15% 的权重给新值
        smoothedTargetY = smoothedTargetY + (realY - smoothedTargetY) * smoothingFactor;
        
        return smoothedTargetY;
    }

    /**
     * 重置平滑追踪（目标切换时调用）
     */
    public void resetSmoothedTargetY() {
        this.smoothedTargetY = Double.NaN;
    }


    // ========== NBT 读写 ==========
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) {
            tag.putUUID("Owner", this.getOwnerUUID());
        }
        // 保存完整数据
        tag.put("DollData", dataManager.save(this.level().registryAccess()));
        tag.putBoolean("IsEnraged", this.isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("DollData")) {
            dataManager.load(tag.getCompound("DollData"), this.level().registryAccess());
            setTethered(getDollData().isTethered());
            syncEquipmentToClient();
        } 
        if (tag.contains("IsEnraged")) {
            this.setEnraged(tag.getBoolean("IsEnraged"));
        }
    }

    // ========== 死亡管理 ==========
    @Override
    public void die(DamageSource source) {
        // 记录死亡位置和掉落信息
        if (!this.level().isClientSide) {
            // 生成破损人偶物品
            ItemStack brokenDollStack = createBrokenDollItem();
            
            // 作为掉落物生成
            this.spawnAtLocation(brokenDollStack, 1.0F);
            
            // 生成一些粒子效果
            this.level().addParticle(
                net.minecraft.core.particles.ParticleTypes.POOF,
                this.getX(), this.getY() + 0.5, this.getZ(),
                0, 0.1, 0
            );
        }
        
        // 调用父类方法完成死亡流程
        super.die(source);
    }

    /**
     * 创建破损人偶物品，保存当前所有人偶数据
     */

    private ItemStack createBrokenDollItem() {
        ItemStack stack = new ItemStack(ModItems.BROKEN_DOLL.get());

        CompoundTag entityTag = new CompoundTag();
        this.saveWithoutId(entityTag);

        // ===== 修复点：手动补全实体 ID =====
        entityTag.putString("id", EntityType.getKey(this.getType()).toString());

        // 确保破损标记
        if (entityTag.contains("DollData")) {
            entityTag.getCompound("DollData").putBoolean("IsBroken", true);
        } else {
            CompoundTag dollData = this.getDataManager().save(this.level().registryAccess());
            dollData.putBoolean("IsBroken", true);
            entityTag.put("DollData", dollData);
        }
        entityTag.putBoolean("IsBroken", true);
        entityTag.remove("UUID");

        stack.set(DataComponents.ENTITY_DATA, CustomData.of(entityTag));

        DollJobType jobType = this.getJobType();
        if (jobType != null) {
            stack.set(ModDataComponents.DOLL_TYPE.get(), jobType);
        }

        return stack;
    }
    
    @Override
    protected void dropEquipment() {
        // 不执行默认的装备掉落逻辑
        // 所有数据已经保存在破损人偶物品中
    }

    // 消耗工具耐久度
    @Override
    public boolean doHurtTarget(Entity target) {
        // 先调用父类方法完成实际伤害
        boolean result = super.doHurtTarget(target);
        
        if (result && !this.level().isClientSide) {
            // ===== 消耗主手武器耐久度 =====
            ItemStack weapon = getItemInHand();
            if (!weapon.isEmpty() && weapon.isDamageableItem()) {
                // 使用原版的 hurtAndBreak 方法，会触发耐久度减少和损坏音效/粒子
                weapon.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
                
                // 如果武器损坏，清空该槽位
                if (weapon.isEmpty()) {
                    setItemInHand(ItemStack.EMPTY);
                    // 刷新属性（因为攻击力可能变化）
                    this.getDataManager().applyDataToEntity();
                    this.syncEquipmentToClient();
                }
            }
        }
        
        return result;
    }

    // ========== GeckoLib 接口 ==========
    @Override public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {}
    @Override public AnimatableInstanceCache getAnimatableInstanceCache() { return cache; }

    // ========== 工具方法 ==========
    public int getAssignedTargetId() { return assignedTargetId; }
    public void setAssignedTargetId(int id) { this.assignedTargetId = id; }

    public boolean isPositionSafe() {
        return DollCollisionHelper.isPositionSafe(this);
    }
    public void tryEscapeFromBlock() {
        DollCollisionHelper.tryEscapeFromBlock(this);
    }
    
    public boolean isPlayerActivelyMoving() {
        return isPlayerMoving;
    }
    public void setPlayerMoving(boolean moving) {
        this.isPlayerMoving = moving;
    }

    public boolean isSameOwner(LivingEntity target) {
        if (target == null) return false;
        if (!(target instanceof DollEntity otherDoll)) return false;
        UUID thisOwner = this.getOwnerUUID();
        UUID otherOwner = otherDoll.getOwnerUUID();
        if (thisOwner == null || otherOwner == null) return false;
        return thisOwner.equals(otherOwner);
    }

    public int getObstructedTicks() {
        return stateManager.getObstructedTicks();
    }
    public boolean isInsideBlock() {
        return stateManager.isInsideBlock();
    }

    // 返回模式
    public boolean isReturning() { return isReturning; }
    public void setReturning(boolean returning) { this.isReturning = returning; }

    public long getEnrageTime() { return enrageTime; }
    public void setEnrageTime(long time) { this.enrageTime = time; }

    // =========== 便捷方法 ==========
    // ========== 移动处理器 ==========
    public DollMovementHandler getMovementHandler() {
        return movementHandler;
    }

    public void followOwner(LivingEntity owner, double speedMultiplier, double desiredDistance) {
        this.movementHandler.followOwner(this, owner, speedMultiplier, desiredDistance);
    }

    // ========== 战斗管理器 ==========
    public DollCombatManager getCombatManager() {
        return combatManager;
    }

    // ========== 装备处理器 ==========
    public DollEquipmentHandler getEquipmentHandler() {
        return equipmentHandler;
    }

    // ========== 拾取物品时的自动装备 ==========
    public void pickUpItem(ItemStack stack) {
        if (stack.isEmpty()) return;
        // 尝试自动装备
        if (equipmentHandler.tryEquip(stack)) {
            // 装备成功，从拾取的物品堆中移除一个
            stack.shrink(1);
        }
        // 装备失败则丢弃（或放入背包，未来实现）
    }

    public ItemStack getItemInHand() {
        return getDollData().getWeapon();
    }

    public ItemStack getItemInOffHand() {
        return getDollData().getOffHand();
    }

    public void setItemInHand(ItemStack stack) {
        getDataManager().setItem(DollSlots.MAIN_HAND, stack);
    }

    public void setItemInOffHand(ItemStack stack) {
        getDataManager().setItem(DollSlots.OFF_HAND, stack);
    }
    
    /**
     * 打印人偶所有槽位的物品信息到玩家的聊天框
     */
    public void printInventory(Player player) {
        player.sendSystemMessage(Component.literal("§6=== 人偶背包 ==="));
        DollData data = this.getDollData();
        ItemStack[] inv = data.getInventory();
        boolean hasItems = false;

        for (int i = 0; i < inv.length; i++) {
            ItemStack stack = inv[i];
            if (!stack.isEmpty()) {
                hasItems = true;
                String slotName = getSlotDisplayName(i);
                player.sendSystemMessage(Component.literal(
                    String.format("§7[%s] §r%s §7x§f%d", slotName, stack.getDisplayName().getString(), stack.getCount())
                ));
            }
        }

        if (!hasItems) {
            player.sendSystemMessage(Component.literal("§7背包为空"));
        }
    }

    /**
     * 根据槽位索引返回可读的名称
     */
    private String getSlotDisplayName(int slot) {
        if (slot == DollSlots.MAIN_HAND) return "主手";
        if (slot == DollSlots.OFF_HAND) return "副手";
        if (slot >= DollSlots.HELMET && slot <= DollSlots.BOOTS) {
            String[] armor = {"头盔", "胸甲", "护腿", "靴子"};
            return armor[slot - DollSlots.HELMET];
        }
        if (DollSlots.isBackpackSlot(slot)) {
            int index = slot - DollSlots.BACKPACK_START;
            int maxSlots = this.getDollData().getBackpackSlots();
            if (index < maxSlots) {
                return "背包" + (index + 1);
            } else {
                return "背包(已禁用)";
            }
        }
        return "槽" + slot;
    }
}