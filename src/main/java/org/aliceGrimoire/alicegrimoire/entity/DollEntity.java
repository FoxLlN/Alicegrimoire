package org.aliceGrimoire.alicegrimoire.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.aliceGrimoire.alicegrimoire.entity.doll.data.*;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollState;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollStateManager;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.DollCombatManager;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.DollTargetSelector;
import org.aliceGrimoire.alicegrimoire.entity.doll.movement.DollMoveControl;
import org.aliceGrimoire.alicegrimoire.entity.doll.movement.DollMovementHandler;
import org.aliceGrimoire.alicegrimoire.entity.doll.util.DollCollisionHelper;
import org.jetbrains.annotations.Nullable;

import com.mojang.logging.LogUtils;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public class DollEntity extends PathfinderMob implements GeoEntity, OwnableEntity, RangedAttackMob {
    
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    // ========== 数据管理器 ==========
    private final DollDataManager dataManager;

    private int shieldDisableTicks = 0;
    private boolean isPlayerMoving = false;
    private int assignedTargetId = -1;

    // 返回模式（人偶哨）
    private boolean isReturning = false;
    // 激怒时间（用于发光等）
    private long enrageTime = 0;

    // ========== 同步数据字段 ==========
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> EVOKE_TIME =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> IS_ENRAGED =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
    // 保留 DOLL_TYPE 用于向后兼容（但实际数据由 DollData 管理）
    private static final EntityDataAccessor<String> DOLL_TYPE =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> IS_TETHERED =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);

    // ========== 核心管理器 ==========
    private final DollStateManager stateManager;
    private final DollMovementHandler movementHandler;
    private final DollCombatManager combatManager;

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
    }

    // ========== 数据同步 ==========
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(EVOKE_TIME, 0L);
        builder.define(IS_ENRAGED, false);
        builder.define(DOLL_TYPE, DollJobType.STANDARD.name());
        builder.define(IS_TETHERED, false);
    }

    // ========== 主要 tick 逻辑 ==========
    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            // 1. 更新拴住状态（检测护腿装备）
            LivingEntity owner = this.getOwner();
            boolean tethered = owner != null && isTethered(owner);
            this.entityData.set(IS_TETHERED, tethered);

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
        }

        // 7. 人偶互相排斥
        if (!this.level().isClientSide && this.noPhysics) {
            applyRepulsion();
        }
    }

    /**
     * 应用人偶之间的排斥力
     */
    private void applyRepulsion() {
        List<DollEntity> nearby = this.level().getEntitiesOfClass(
            DollEntity.class,
            this.getBoundingBox().inflate(1.5),
            other -> other != this && other.noPhysics
        );
        
        Vec3 totalRepel = Vec3.ZERO;
        for (DollEntity other : nearby) {
            Vec3 delta = this.position().subtract(other.position());
            double dist = delta.length();
            if (dist < 0.8 && dist > 0.01) {
                double strength = 0.2 / (dist + 0.1);
                Vec3 repel = delta.normalize().scale(strength);
                totalRepel = totalRepel.add(repel);
                // 同时给对方施加反向力
                other.applyRepulsionFrom(this, repel);
            }
        }
        
        if (totalRepel.lengthSqr() > 0) {
            // 直接修改位置（而不是速度），确保立即生效
            Vec3 newPos = this.position().add(totalRepel);
            this.moveTo(newPos.x, newPos.y, newPos.z);
            // 同时也设置速度，保持平滑
            this.setDeltaMovement(this.getDeltaMovement().add(totalRepel));
        }
    }

    /**
     * 从其他人偶接收排斥力
     */
    public void applyRepulsionFrom(DollEntity source, Vec3 repel) {
        if (this.noPhysics) {
            Vec3 newPos = this.position().subtract(repel);
            this.moveTo(newPos.x, newPos.y, newPos.z);
            this.setDeltaMovement(this.getDeltaMovement().subtract(repel));
        }
    }

    @Override
    public boolean isEffectiveAi() {
        return super.isEffectiveAi();
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

    // ========== 职业相关（推荐新代码使用） ==========
    public DollJobType getJobType() {
        return dataManager.getData().getJobType();
    }
    public void setJobType(DollJobType jobType) {
        dataManager.setJobType(jobType);
        // 更新同步字段（向后兼容）
        this.entityData.set(DOLL_TYPE, jobType.name());
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
    public boolean isTethered() { return this.entityData.get(IS_TETHERED); }
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
                    this.shieldDisableTicks = 100;
                }
            }
            if (reducedAmount > 0) {
                return super.hurt(source, reducedAmount);
            } else {
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    @Override
    public boolean isBlocking() {
        DollJobType job = getJobType();
        return (job == DollJobType.GUARD || job == DollJobType.DEFENDER)
                && shieldDisableTicks <= 0
                && stateManager.getCurrentState() == DollState.ENGAGING;
    }

    public int getShieldDisableTicks() { return shieldDisableTicks; }
    public void setShieldDisableTicks(int ticks) { this.shieldDisableTicks = ticks; }

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
        } else if (job == DollJobType.GUARD && (weapon == WeaponType.CROSSBOW || weapon == WeaponType.BOW)) {
            arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
            arrow.setBaseDamage(2.0D); // 近卫远程伤害略低
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
        // 向后兼容
        tag.putString("DollType", getJobType().name());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("DollData")) {
            dataManager.load(tag.getCompound("DollData"), this.level().registryAccess());
        } else {
            // 向后兼容：从旧格式读取
            if (tag.contains("DollType")) {
                try {
                    DollJobType job = DollJobType.valueOf(tag.getString("DollType"));
                    setJobType(job);
                } catch (IllegalArgumentException e) {
                    setJobType(DollJobType.STANDARD);
                }
            }
        }
        if (tag.contains("IsEnraged")) {
            this.setEnraged(tag.getBoolean("IsEnraged"));
        }
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
}