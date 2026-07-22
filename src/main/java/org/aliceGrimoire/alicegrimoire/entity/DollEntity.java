package org.aliceGrimoire.alicegrimoire.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollState;
import org.aliceGrimoire.alicegrimoire.entity.doll.state.DollStateManager;
import org.aliceGrimoire.alicegrimoire.entity.doll.DollType;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.DollCombatManager;
import org.aliceGrimoire.alicegrimoire.entity.doll.combat.DollTargetSelector;
import org.aliceGrimoire.alicegrimoire.entity.doll.movement.DollMoveControl;
import org.aliceGrimoire.alicegrimoire.entity.doll.movement.DollMovementHandler;
import org.aliceGrimoire.alicegrimoire.entity.doll.util.DollCollisionHelper;
import org.aliceGrimoire.alicegrimoire.registry.ModAttachments;
import org.jetbrains.annotations.Nullable;

import com.mojang.logging.LogUtils;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DollEntity extends PathfinderMob implements GeoEntity, OwnableEntity, RangedAttackMob {
    
    private static final org.slf4j.Logger LOGGER = LogUtils.getLogger();

    private int shieldDisableTicks = 0;
    private boolean isPlayerMoving = false;

    // 返回模式（人偶哨）
    private boolean isReturning = false;
    // 激怒时间（用于发光等，保留但无实际逻辑）
    private long enrageTime = 0;

    // ========== 同步数据字段 ==========
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Long> EVOKE_TIME =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Boolean> IS_ENRAGED =
            SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
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
        // 初始化管理器（互相引用需小心，先构造再设置）
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
        builder.define(DOLL_TYPE, DollType.STANDARD.name());
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

            // 3. 状态机更新（决定碰撞箱、状态切换）
            stateManager.tick();

            // 4. 移动控制（根据当前状态计算移动目标）
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
    }

    @Override
    public boolean isEffectiveAi() {
        boolean result = super.isEffectiveAi();
        return result;
    }

    // ========== 目标选择（注册Goal） ==========
    @Override
    protected void registerGoals() {
        LOGGER.info("[DollEntity] registerGoals() called!");
        this.goalSelector.addGoal(1, new DollStateGoal());
        this.goalSelector.addGoal(2, new DollMoveGoal());
        this.goalSelector.addGoal(3, new DollCombatGoal());

        this.targetSelector.addGoal(1, new DollTargetSelector(this));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this).setAlertOthers());
    }

    // ========== 简单的内部 Goal 类（委托给管理器） ==========
    private class DollStateGoal extends Goal {
        @Override
        public boolean canUse() {
            return true;
        }
        
        @Override
        public boolean canContinueToUse() {
            return true;
        }
        
        @Override
        public void tick() {
            stateManager.tick();
        }
    }

    private class DollMoveGoal extends Goal {
        @Override
        public boolean canUse() { return true; }
        @Override
        public boolean canContinueToUse() { return true; }
        @Override
        public void tick() {
            movementHandler.tick();
        }
    }

    private class DollCombatGoal extends Goal {
        @Override
        public boolean canUse() {
            return stateManager.getCurrentState() == DollState.ENGAGING && getTarget() != null;
        }
        @Override
        public boolean canContinueToUse() {
            return stateManager.getCurrentState() == DollState.ENGAGING && getTarget() != null && getTarget().isAlive();
        }
        @Override
        public void tick() {
            combatManager.tick();
        }
    }

    // ========== 属性 ==========
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    // ========== 接口实现：OwnableEntity ==========
    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
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
        this.entityData.set(IS_ENRAGED, enraged);
        if (!enraged) {
            // 清除目标，但状态机会在 tick 中处理
            this.setTarget(null);
            this.enrageTime = 0; // 重置激怒时间
        }
        // 刷新属性（例如近卫的持盾状态）
    }

    /**
     * 判断人偶是否可以被激怒
     * 检查：未破损、未被禁用、处于可战斗状态等
     * 未来可扩展：破损状态、被禁用、冷却中等
     */
    public boolean canBeEnraged() {
        // 1. 如果已经激怒，不能再被激怒（除非目标不同，但由外部控制）
        if (this.isEnraged()) {
            return false;
        }
        
        // 2. 检查是否存活
        if (!this.isAlive()) {
            return false;
        }
        
        // 3. 如果处于返回模式（人偶哨），可以激怒吗？
        // 策划要求：返回模式仍可被激怒，但会优先返回
        // 所以这里不限制 isReturning()
        
        // 4. 未来扩展：检查破损状态
        // if (this.isBroken()) return false;
        
        // 5. 未来扩展：检查是否被禁用（例如被某种魔法封印）
        // if (this.isDisabled()) return false;
        
        // 6. 默认允许激怒
        return true;
    }

    // ========== 唤起时间 ==========
    public long getEvokeTime() {
        return this.entityData.get(EVOKE_TIME);
    }

    public void setEvokeTime(long time) {
        this.entityData.set(EVOKE_TIME, time);
    }

    // ========== 人偶类型 ==========
    public DollType getDollType() {
        try {
            return DollType.valueOf(this.entityData.get(DOLL_TYPE));
        } catch (Exception e) {
            return DollType.STANDARD;
        }
    }

    public void setDollType(DollType type) {
        this.entityData.set(DOLL_TYPE, type.name());
        // 刷新属性
        refreshAttributes();
    }

    // ========== 拴住状态 ==========
    public boolean isTethered() {
        return this.entityData.get(IS_TETHERED);
    }

    public boolean isTethered(LivingEntity owner) {
        if (owner == null) return false;
        ItemStack leggings = owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        return !leggings.isEmpty() && leggings.getItem() instanceof org.aliceGrimoire.alicegrimoire.item.DollStringItem;
    }

    // ========== 属性刷新 ==========
    public void refreshAttributes() {
        DollType type = getDollType();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.getMaxHealth());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(type.getDamage());
        this.getAttribute(Attributes.ARMOR).setBaseValue(type.getArmor());
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    // ========== 伤害处理（近卫盾牌反击） ==========
    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 掉落伤害免疫
        if (source.is(DamageTypeTags.IS_FALL)) return false;

        // 近卫盾牌格挡逻辑（仅在持盾状态下生效）
        if (this.isBlocking() && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            // 1. 伤害减半（格挡减免）
            float reducedAmount = amount * 0.5f;

            // 2. 反击（近战攻击者）
            if (source.getDirectEntity() instanceof LivingEntity attacker && !isSameOwner(attacker)) {
                attacker.hurt(this.damageSources().mobAttack(this), 4.0F);
                // 击退
                Vec3 dir = attacker.position().subtract(this.position()).normalize();
                attacker.knockback(0.5, -dir.x, -dir.z);
            }

            // 3. 检测是否为斧头攻击（禁用盾牌）
            if (source.getDirectEntity() instanceof LivingEntity attacker) {
                ItemStack weapon = attacker.getMainHandItem();
                if (weapon.canDisableShield(ItemStack.EMPTY, this, attacker)) {
                    this.shieldDisableTicks = 100; // 5秒禁用
                }
            }

            // 4. 如果减伤后仍有伤害，交给原版系统（护甲、保护附魔等会生效）
            if (reducedAmount > 0) {
                return super.hurt(source, reducedAmount);
            } else {
                // 伤害被完全格挡，不触发伤害事件
                return false;
            }
        }

        // 非格挡状态，正常处理伤害
        return super.hurt(source, amount);
    }

    @Override
    public boolean isBlocking() {
        return getDollType() == DollType.GUARD 
            && shieldDisableTicks <= 0 
            && stateManager.getCurrentState() == DollState.ENGAGING;
    }

    public int getShieldDisableTicks() { return shieldDisableTicks; }
    public void setShieldDisableTicks(int ticks) { this.shieldDisableTicks = ticks; }

    // ========== 远程攻击（RangedAttackMob） ==========
    @Override
    public void performRangedAttack(LivingEntity target, float velocity) {
        DollType type = getDollType();
        AbstractArrow arrow;
        if (type == DollType.PILLAGER) {
            arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
            arrow.setBaseDamage(4.0D);
        } else if (type == DollType.SHARPSHOOTER) {
            arrow = new Arrow(this.level(), this, new ItemStack(Items.ARROW), null);
            arrow.setBaseDamage(3.0D);
        } else {
            return;
        }
        // 瞄准计算
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.33D) - arrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        arrow.shoot(d0, d1 + d3 * 0.2D, d2, velocity, (float)(14 - this.level().getDifficulty().getId() * 4));
        this.level().addFreshEntity(arrow);
    }

    // ========== NBT 读写 ==========
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) {
            tag.putUUID("Owner", this.getOwnerUUID());
        }
        tag.putString("DollType", getDollType().name());
        tag.putBoolean("IsEnraged", this.isEnraged());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("Owner")) {
            this.setOwnerUUID(tag.getUUID("Owner"));
        }
        if (tag.contains("DollType")) {
            try {
                setDollType(DollType.valueOf(tag.getString("DollType")));
            } catch (Exception e) {
                setDollType(DollType.STANDARD);
            }
        }
        if (tag.contains("IsEnraged")) {
            this.setEnraged(tag.getBoolean("IsEnraged"));
        }
    }

    // ========== GeckoLib 接口 ==========
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 动画由策略控制，这里留空
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ========== 工具方法（供管理器调用） ==========
    public boolean isPositionSafe() {
        return DollCollisionHelper.isPositionSafe(this);
    }

    public void tryEscapeFromBlock() {
        DollCollisionHelper.tryEscapeFromBlock(this);
    }
    
    public boolean isPlayerActivelyMoving() {
        return isPlayerMoving; // 由外部更新
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

    /**
     * 获取栓绳被阻挡的累计时间（用于渲染拴绳颜色）
     */
    public int getObstructedTicks() {
        return stateManager.getObstructedTicks();
    }

    /**
     * 判断人偶是否在方块内部（用于渲染拴绳颜色）
     */
    public boolean isInsideBlock() {
        return stateManager.isInsideBlock();
    }

    // ========== 返回模式（人偶哨） ==========
    public boolean isReturning() {
        return isReturning;
    }

    public void setReturning(boolean returning) {
        this.isReturning = returning;
    }

    // ========== 激怒时间（供发光等使用） ==========
    public long getEnrageTime() {
        return enrageTime;
    }

    public void setEnrageTime(long time) {
        this.enrageTime = time;
    }
}