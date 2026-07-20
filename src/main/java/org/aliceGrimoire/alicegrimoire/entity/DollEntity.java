package org.aliceGrimoire.alicegrimoire.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.effect.MobEffects;
import org.aliceGrimoire.alicegrimoire.item.DollWandItem;
import org.aliceGrimoire.alicegrimoire.registry.ModAttachments;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.*;

public class DollEntity extends PathfinderMob implements GeoEntity, OwnableEntity, RangedAttackMob {
    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    protected static final EntityDataAccessor<Long> EVOKE_TIME = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.LONG);
    protected static final EntityDataAccessor<Boolean> IS_ENRAGED = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Boolean> IS_RETURNING = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<String> DOLL_TYPE = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.STRING);
    protected static final EntityDataAccessor<Integer> OBSTRUCTED_TICKS = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Boolean> IS_INSIDE_BLOCK = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.BOOLEAN);
    protected static final EntityDataAccessor<Long> ENRAGE_TIME = SynchedEntityData.defineId(DollEntity.class, EntityDataSerializers.LONG);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private int stuckTime = 0;
    private int shieldDisableTicks = 0;
    private int chargeTicks = 0;
    private int reloadTicks = 0;
    private boolean isCharging = false;
        
    private float lockedYaw = 0;
    private boolean isYawLocked = false;

    private int stuckInBlockTicks = 0;

    private static final int COOLDOWN_DURATION = 100; // 5秒 = 100 tick
    // 冷却计时器（用于战斗结束后恢复无碰撞箱）
    private int noPhysicsCooldown = 0;

    // 是否处于"恢复中"状态（有碰撞箱但无攻击目标，等待恢复）
    private boolean isRecovering = false;

    public DollEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new DollMoveControl(this);
        this.setNoGravity(true);
        this.refreshAttributes();
        
        // 禁用默认的随机注视，避免静止时乱转
        this.lookControl = new net.minecraft.world.entity.ai.control.LookControl(this) {
            @Override
            public void tick() {
                // 空方法，不改变旋转
            }
        };
    }

    public void setEnrageTime(long time) {
        this.entityData.set(ENRAGE_TIME, time);
    }

    public long getEnrageTime() {
        return this.entityData.get(ENRAGE_TIME);
    }

    public long getEvokeTime() {
        return this.entityData.get(EVOKE_TIME);
    }

    public void setEvokeTime(long time) {
        this.entityData.set(EVOKE_TIME, time);
    }

    public boolean isEnraged() {
        return this.entityData.get(IS_ENRAGED);
    }

    public void setEnraged(boolean enraged) {
        if (enraged) {
            // 被激怒：取消冷却，恢复状态
            this.noPhysicsCooldown = 0;
            this.isRecovering = false;
            this.entityData.set(IS_ENRAGED, true);
        } else {
            // 解除激怒：进入冷却期，而不是立即切回无碰撞
            this.entityData.set(IS_ENRAGED, false);
            this.noPhysicsCooldown = COOLDOWN_DURATION;
            this.isRecovering = true;
        }
    }

    public boolean isReturning() {
        return this.entityData.get(IS_RETURNING);
    }

    public void setReturning(boolean returning) {
        this.entityData.set(IS_RETURNING, returning);
    }

    public DollType getDollType() {
        try {
            return DollType.valueOf(this.entityData.get(DOLL_TYPE));
        } catch (Exception e) {
            return DollType.STANDARD;
        }
    }

    public boolean isInsideBlock() {
        return this.entityData.get(IS_INSIDE_BLOCK);
    }

    public int getObstructedTicks() {
        return this.entityData.get(OBSTRUCTED_TICKS);
    }

    public void setDollType(DollType type) {
        this.entityData.set(DOLL_TYPE, type.name());
        this.refreshAttributes();
    }

    public boolean isRecovering() {
        return this.isRecovering;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(EVOKE_TIME, 0L);
        builder.define(IS_ENRAGED, false);
        builder.define(IS_RETURNING, false);
        builder.define(DOLL_TYPE, DollType.STANDARD.name());
        builder.define(OBSTRUCTED_TICKS, 0);
        builder.define(IS_INSIDE_BLOCK, false);
        builder.define(ENRAGE_TIME, 0L);
    }

    @Override
    public void tick() {
        // === 碰撞箱状态机 ===
        boolean shouldHavePhysics = false;
        boolean isInSafePosition = isPositionSafe();
                
        if (this.isEnraged()) {
            // 激怒状态需要碰撞箱，但要确保位置安全
            shouldHavePhysics = true;
            if (this.isRecovering) {
                this.isRecovering = false;
                this.noPhysicsCooldown = 0;
            }
            // 如果位置不安全（卡墙），延迟切换或强制脱困
            if (!isInSafePosition) {
                tryEscapeFromBlock();
                // 延迟一帧再切换，给脱困时间
                shouldHavePhysics = false;
            }
        } else if (this.isRecovering) {
            shouldHavePhysics = true;
            this.noPhysicsCooldown--;
            if (this.noPhysicsCooldown <= 0) {
                this.isRecovering = false;
                shouldHavePhysics = false;
            }
            // 恢复过程中如果卡墙，强制脱困
            if (!isInSafePosition) {
                tryEscapeFromBlock();
            }
        }
        
        if (this.isReturning()) {
            shouldHavePhysics = true;
        }
        
        // 只在安全位置切换碰撞箱，避免卡墙
        if (shouldHavePhysics && isInSafePosition) {
            this.noPhysics = false;
        } else if (!shouldHavePhysics) {
            this.noPhysics = true;
        }
        // 如果 shouldHavePhysics 为 true 但位置不安全，保持无碰撞直到脱困
        // ===== 碰撞箱状态机结束 =====
        
        super.tick();

        if (!this.level().isClientSide) {
            if (shieldDisableTicks > 0) shieldDisableTicks--;
            
            LivingEntity owner = this.getOwner();
            if (owner != null) {
                // 指令 10：拴绳隔断检测
                Vec3 start = this.position().add(0, 1.0, 0);
                Vec3 end = owner.getEyePosition();
                boolean obstructed = this.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this)).getType() == HitResult.Type.BLOCK;
                
                int currentTicks = getObstructedTicks();
                if (obstructed) {
                    this.entityData.set(OBSTRUCTED_TICKS, currentTicks + 1);
                    if (currentTicks + 1 > 60) { // 3秒
                        if (this.isEnraged()) {
                            this.setEnraged(false);
                            this.setTarget(null);
                        }
                    }
                } else {
                    this.entityData.set(OBSTRUCTED_TICKS, 0);
                }
                
                // 是否在方块内部
                boolean inside = this.level().getBlockState(this.blockPosition()).isSolidRender(this.level(), this.blockPosition());
                this.entityData.set(IS_INSIDE_BLOCK, inside);
            }
            
            if (owner == null || !owner.isAlive()) {
                this.discard();
                return;
            }

            // 指令 7：如果玩家处于失明或黑暗状态，所有人偶都会暂时失去视野和目标
            if (owner.hasEffect(MobEffects.BLINDNESS) || owner.hasEffect(MobEffects.DARKNESS)) {
                if (this.isEnraged()) {
                    this.setEnraged(false);
                }
                this.setTarget(null);
            }

            // 指令 10：如果被隔断持续时间超过3秒，则会自动去除其自身的激怒状态
            // TODO: Implement leash obstruction logic here later

            // 6、卡在实心方块内5秒以上时会主动从方块内离开
            if (this.level().getBlockState(this.blockPosition()).isSolidRender(this.level(), this.blockPosition())) {
                stuckTime++;
                if (stuckTime > 100) { // 5秒，更快反应
                    // 多方向尝试脱困：先尝试上，再尝试四周
                    Vec3[] escapeAttempts = new Vec3[]{
                        new Vec3(0, 0.5, 0),   // 上
                        new Vec3(0.5, 0, 0),   // 右
                        new Vec3(-0.5, 0, 0),  // 左
                        new Vec3(0, 0, 0.5),   // 前
                        new Vec3(0, 0, -0.5)   // 后
                    };
                    
                    // 尝试找到一个不会卡住的方位置
                    BlockPos currentPos = this.blockPosition();
                    Vec3 bestDirection = new Vec3(0, 0.5, 0);
                    double bestDistance = Double.MAX_VALUE;
                    
                    for (Vec3 dir : escapeAttempts) {
                        BlockPos testPos = currentPos.offset((int) dir.x, (int) dir.y, (int) dir.z);
                        if (!this.level().getBlockState(testPos).isSolidRender(this.level(), testPos)) {
                            // 找到一个可以站的位置
                            Vec3 targetPos = this.position().add(dir);
                            this.setDeltaMovement(dir.scale(0.8));
                            // 强制传送到目标位置（防卡墙）
                            this.moveTo(targetPos.x, targetPos.y, targetPos.z);
                            break;
                        }
                    }
                    
                    stuckTime = 0; // 重置计时器，避免重复触发
                }
            } else {
                stuckTime = 0;
            }

            if (isTethered(owner)) {
                boolean holdingWand = owner.getMainHandItem().getItem() instanceof DollWandItem || 
                                    owner.getOffhandItem().getItem() instanceof DollWandItem;
                
                if (holdingWand && !this.isEnraged()) {
                    this.setTarget(null);
                }

                if (this.isEnraged() && (this.getTarget() == null || !this.getTarget().isAlive())) {
                    this.setEnraged(false);
                }

                // 视野限制与速度控制
                double playerSpeedRatio = owner.getAttributeValue(Attributes.MOVEMENT_SPEED) / 0.1D;

                if (this.isEnraged() && this.getTarget() != null) {
                    // 出击档位：只有激怒状态才能出击
                    double baseStrikeSpeed = 0.525; // 0.35 * 1.5
                    double finalStrikeSpeed = baseStrikeSpeed * playerSpeedRatio;
                    Vec3 moveDir = this.getTarget().position().subtract(this.position()).normalize();
                    this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(moveDir.scale(finalStrikeSpeed)));
                    
                    // 检查标记目标是否依然有效
                    Set<Integer> marked = owner.getData(ModAttachments.MARKED_TARGETS);
                    if (!marked.isEmpty() && !marked.contains(this.getTarget().getId())) {
                        // 目标不在标记列表中，解除激怒
                        this.setEnraged(false);
                        this.setTarget(null);
                    }
                } else {
                    // 非激怒状态：游荡或跟随
                    double wanderRange = isReturning() ? 4.0D : 64.0D;
                    if (this.distanceToSqr(owner) > wanderRange) {
                        Vec3 moveDir = owner.position().subtract(this.position()).normalize();
                        double baseSpeed = isReturning() ? 0.35 : 0.2;
                        double finalSpeed = baseSpeed * playerSpeedRatio;
                        this.setDeltaMovement(this.getDeltaMovement().scale(0.5).add(moveDir.scale(finalSpeed)));
                        
                        if (isReturning() && this.distanceToSqr(owner) > 25.0D) {
                            this.moveTo(owner.getX(), owner.getY() + 1.5, owner.getZ());
                        }
                    }
                }
            }
        }

        if (!this.noPhysics && this.isEnraged()) {
            // 检测是否卡在方块内部
            BlockState state = this.level().getBlockState(this.blockPosition());
            if (state.isSolidRender(this.level(), this.blockPosition())) {
                // 卡墙尝试向外推
                this.stuckInBlockTicks++;
                if (this.stuckInBlockTicks > 20) { // 1秒后开始尝试脱困
                    // 向最近的非方块区域移动
                    tryEscapeFromBlock();
                }
            } else {
                this.stuckInBlockTicks = 0;
            }
        }

        // 添加人偶间排斥
        if (!this.noPhysics) {
            // 检测附近的人偶，施加排斥力
            List<DollEntity> nearbyDolls = this.level().getEntitiesOfClass(
                DollEntity.class, 
                this.getBoundingBox().inflate(1.5D),
                doll -> doll != this && doll.isAlive() && !doll.noPhysics
            );
            
            for (DollEntity other : nearbyDolls) {
                Vec3 delta = this.position().subtract(other.position());
                double distance = delta.length();
                if (distance < 1.0D && distance > 0.01D) {
                    // 距离太近，施加排斥力
                    Vec3 repel = delta.normalize().scale(0.1D / (distance + 0.1D));
                    this.setDeltaMovement(this.getDeltaMovement().add(repel));
                    other.setDeltaMovement(other.getDeltaMovement().subtract(repel));
                }
            }
        }
    }

    public boolean isTethered(LivingEntity owner) {
        ItemStack leggings = owner.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.LEGS);
        return !leggings.isEmpty() && leggings.getItem() instanceof org.aliceGrimoire.alicegrimoire.item.DollStringItem;
    }

    @Override
    public boolean isBlocking() {
        return getDollType() == DollType.GUARD && shieldDisableTicks <= 0 && this.isEnraged();
    }

    /**
     * 判断人偶是否可以被激怒（未被禁用、未破损等）
     */
    public boolean canBeEnraged() {
        // 未来可扩展：检查破损状态、是否被禁用等
        return true;
    }

    /**
    * 尝试从方块内部脱困，向四周寻找空位移动
    */
    private void tryEscapeFromBlock() {
        Vec3[] escapeAttempts = new Vec3[]{
            new Vec3(0, 0.5, 0),   // 上
            new Vec3(0.5, 0, 0),   // 右
            new Vec3(-0.5, 0, 0),  // 左
            new Vec3(0, 0, 0.5),   // 前
            new Vec3(0, 0, -0.5)   // 后
        };
        
        BlockPos currentPos = this.blockPosition();
        for (Vec3 dir : escapeAttempts) {
            BlockPos testPos = currentPos.offset((int) dir.x, (int) dir.y, (int) dir.z);
            if (!this.level().getBlockState(testPos).isSolidRender(this.level(), testPos)) {
                Vec3 targetPos = this.position().add(dir);
                this.setDeltaMovement(dir.scale(0.8));
                this.moveTo(targetPos.x, targetPos.y, targetPos.z);
                return;
            }
        }
    }

    /**
     * 检查当前位置是否安全（不在方块内部，不与其他实体严重重叠）
     */
    private boolean isPositionSafe() {
        // 检查是否在方块内部
        BlockState state = this.level().getBlockState(this.blockPosition());
        if (state.isSolidRender(this.level(), this.blockPosition())) {
            return false;
        }
        
        // 检查是否与其他实体严重重叠（可选）
        // 如果重叠太多，也视为不安全
        return true;
    }

    private boolean isReflecting = false;

    @Override
    public boolean hurt(DamageSource source, float amount) {
        // 免疫掉落伤害
        if (source.is(DamageTypeTags.IS_FALL)) {
            return false;
        }

        if (!isReflecting && this.isBlocking() && amount > 0.0F && !source.is(DamageTypeTags.BYPASSES_ARMOR)) {
            if (source.getDirectEntity() instanceof LivingEntity attacker) {
                // 抵挡近战攻击
                isReflecting = true;
                try {
                    attacker.hurt(this.damageSources().mobAttack(this), 4.0F);
                } finally {
                    isReflecting = false;
                }
                Vec3 knockback = attacker.position().subtract(this.position()).normalize().scale(0.5D);
                attacker.push(knockback.x, 0.1, knockback.z);
                
                if (attacker.getMainHandItem().canDisableShield(ItemStack.EMPTY, this, attacker)) {
                    this.shieldDisableTicks = 100; // 5秒禁用
                }
                return false;
            } else if (source.is(DamageTypeTags.IS_PROJECTILE)) {
                // 抵挡弹射物
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.FLYING_SPEED, 0.4D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(5, new DollRandomFlyGoal(this));
        
        // 不同人偶类型的专用攻击 Goal
        this.goalSelector.addGoal(4, new DollAttackGoal(this));
        this.goalSelector.addGoal(4, new DollGuardAttackGoal(this));
        this.goalSelector.addGoal(4, new DollPillagerAttackGoal(this));
        this.goalSelector.addGoal(4, new DollLancerAttackGoal(this));
        this.goalSelector.addGoal(4, new DollSharpshooterAttackGoal(this));
        
        this.targetSelector.addGoal(1, new DollMarkedTargetGoal(this));
        this.targetSelector.addGoal(2, new DollOwnerTargetGoal(this));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this)).setAlertOthers());
        // TODO: 自动反击未做过滤友军
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // TODO: Register animations
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Nullable
    @Override
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    public void setOwnerUUID(@Nullable UUID uuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

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
        
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.33D) - arrow.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = Math.sqrt(d0 * d0 + d2 * d2);
        arrow.shoot(d0, d1 + d3 * 0.2D, d2, 1.6F, (float)(14 - this.level().getDifficulty().getId() * 4));
        this.level().addFreshEntity(arrow);
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerUUID();
            return uuid == null ? null : this.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException var2) {
            return null;
        }
    }

    /**
     * 检查主人是否处于失明或黑暗状态
     */
    private boolean isOwnerBlindOrDark() {
        LivingEntity owner = this.getOwner();
        if (owner == null) return false;
        return owner.hasEffect(MobEffects.BLINDNESS) || owner.hasEffect(MobEffects.DARKNESS);
    }

    public void refreshAttributes() {
        DollType type = getDollType();
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(type.getMaxHealth());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(type.getDamage());
        this.getAttribute(Attributes.ARMOR).setBaseValue(type.getArmor());
        // Reset health if it's higher than new max
        if (this.getHealth() > this.getMaxHealth()) {
            this.setHealth(this.getMaxHealth());
        }
    }

    /**
     * 判断目标生物是否与本人偶属于同一个主人
     */
    private boolean isSameOwner(LivingEntity target) {
        if (target == null) return false;
        if (!(target instanceof DollEntity otherDoll)) return false;
        
        UUID thisOwner = this.getOwnerUUID();
        UUID otherOwner = otherDoll.getOwnerUUID();
        if (thisOwner == null || otherOwner == null) return false;
        return thisOwner.equals(otherOwner);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.getOwnerUUID() != null) {
            tag.putUUID("Owner", this.getOwnerUUID());
        }
        tag.putString("DollType", getDollType().name());
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
    }

    class DollMoveControl extends MoveControl {
        public DollMoveControl(DollEntity doll) {
            super(doll);
        }

        @Override
        public void tick() {
            if (this.operation == Operation.MOVE_TO) {
                Vec3 vec3 = new Vec3(this.wantedX - DollEntity.this.getX(), this.wantedY - DollEntity.this.getY(), this.wantedZ - DollEntity.this.getZ());
                double d = vec3.length();
                if (d < DollEntity.this.getBoundingBox().getSize()) {
                    this.operation = Operation.WAIT;
                    DollEntity.this.setDeltaMovement(DollEntity.this.getDeltaMovement().scale(0.5D));
                } else {
                    DollEntity.this.setDeltaMovement(DollEntity.this.getDeltaMovement().add(vec3.scale(this.speedModifier * 0.05D / d)));
                    if (DollEntity.this.getTarget() == null) {
                        Vec3 vec31 = DollEntity.this.getDeltaMovement();
                        DollEntity.this.setYRot(-((float)Math.atan2(vec31.x, vec31.z)) * (180F / (float)Math.PI));
                        DollEntity.this.yBodyRot = DollEntity.this.getYRot();
                    } else {
                        double d1 = DollEntity.this.getTarget().getX() - DollEntity.this.getX();
                        double d2 = DollEntity.this.getTarget().getZ() - DollEntity.this.getZ();
                        DollEntity.this.setYRot(-((float)Math.atan2(d1, d2)) * (180F / (float)Math.PI));
                        DollEntity.this.yBodyRot = DollEntity.this.getYRot();
                    }
                }
            }
        }
    }

    class DollRandomFlyGoal extends Goal {
        private final DollEntity doll;

        public DollRandomFlyGoal(DollEntity doll) {
            this.doll = doll;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            MoveControl movecontrol = this.doll.getMoveControl();
            if (!movecontrol.hasWanted()) {
                return true;
            } else {
                double d0 = movecontrol.getWantedX() - this.doll.getX();
                double d1 = movecontrol.getWantedY() - this.doll.getY();
                double d2 = movecontrol.getWantedZ() - this.doll.getZ();
                double d3 = d0 * d0 + d1 * d2 + d2 * d2;
                return d3 < 1.0D || d3 > 3600.0D;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }

        @Override
        public void start() {
            LivingEntity owner = this.doll.getOwner();
            Vec3 targetPos;
            if (owner != null) {
                targetPos = owner.position().add((this.doll.getRandom().nextFloat() * 2.0F - 1.0F) * 8.0F, 
                                               1.0D + this.doll.getRandom().nextFloat() * 3.0D, 
                                               (this.doll.getRandom().nextFloat() * 2.0F - 1.0F) * 8.0F);
            } else {
                targetPos = this.doll.position().add((this.doll.getRandom().nextFloat() * 2.0F - 1.0F) * 16.0F, 
                                               (this.doll.getRandom().nextFloat() * 2.0F - 1.0F) * 16.0F, 
                                               (this.doll.getRandom().nextFloat() * 2.0F - 1.0F) * 16.0F);
            }
            this.doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y, targetPos.z, 1.0D);
        }
    }

    class DollAttackGoal extends Goal {
        private final DollEntity doll;

        public DollAttackGoal(DollEntity doll) {
            this.doll = doll;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            DollType type = doll.getDollType();
            // 必须处于激怒状态
            if (!doll.isEnraged()) {
                return false;
            }
            return (type == DollType.STANDARD || type == DollType.SWORD) &&
                this.doll.getTarget() != null && 
                this.doll.getMoveControl().hasWanted() && 
                this.doll.getRandom().nextInt(reducedTickDelay(7)) == 0 && 
                this.doll.distanceToSqr(this.doll.getTarget()) > 4.0D;
        }

        @Override
        public boolean canContinueToUse() {
            // 只要目标存活且仍处于激怒状态，就继续
            return this.doll.isEnraged() && this.doll.getTarget() != null && this.doll.getTarget().isAlive();
        }

        @Override
        public void start() {
            LivingEntity target = this.doll.getTarget();
            if (target != null) {
                Vec3 moveDir = target.position().subtract(this.doll.position()).normalize();
                Vec3 targetPos = target.position().subtract(moveDir.scale(2.0D));
                this.doll.getMoveControl().setWantedPosition(targetPos.x, targetPos.y + 0.5, targetPos.z, 1.0D);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.doll.getTarget();
            if (target != null) {
                double distToTarget = this.doll.distanceTo(target);
                
                // 攻击判定：范围扩大到 1.5 格
                if (this.doll.getBoundingBox().inflate(0.8D).intersects(target.getBoundingBox())) {
                    if (!isSameOwner(target)) {
                        this.doll.doHurtTarget(target);
                    }
                } else {
                    // 移动逻辑：停在目标周围 1.5~2.5 格处，而非贴脸
                    if (distToTarget > 2.5D) {
                        // 远离时靠近
                        Vec3 moveDir = target.position().subtract(this.doll.position()).normalize();
                        Vec3 targetPos = target.position().subtract(moveDir.scale(2.0D)); // 停在2格外
                        this.doll.getMoveControl().setWantedPosition(
                            targetPos.x, 
                            targetPos.y + 0.5, 
                            targetPos.z, 
                            1.0D
                        );
                    } else if (distToTarget < 1.5D) {
                        // 太近了！后退
                        Vec3 awayDir = this.doll.position().subtract(target.position()).normalize();
                        Vec3 targetPos = this.doll.position().add(awayDir.scale(1.0D));
                        this.doll.getMoveControl().setWantedPosition(
                            targetPos.x, 
                            targetPos.y + 0.5, 
                            targetPos.z, 
                            1.0D
                        );
                    }
                    // 在 1.5~2.5 格之间时，保持位置
                }
            }
        }
    }

    class DollGuardAttackGoal extends Goal {
        private final DollEntity doll;

        public DollGuardAttackGoal(DollEntity doll) {
            this.doll = doll;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 必须处于激怒状态
            if (!doll.isEnraged()) {
                return false;
            }
            
            return this.doll.getDollType() == DollType.GUARD && this.doll.getTarget() != null;
        }

        @Override
        public void start() {
            LivingEntity target = this.doll.getTarget();
            LivingEntity owner = this.doll.getOwner();
            if (target != null && owner != null) {
                // 初始停在玩家与目标之间
                Vec3 dir = target.position().subtract(owner.position()).normalize();
                Vec3 shieldPos = owner.position().add(dir.scale(2.0D));
                this.doll.getMoveControl().setWantedPosition(shieldPos.x, owner.getY() + 1.5, shieldPos.z, 1.0D);
            }
        }

        @Override
        public void tick() {
            LivingEntity target = this.doll.getTarget();
            LivingEntity owner = this.doll.getOwner();
            if (target != null && owner != null) {
                this.doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
                
                double distToTarget = this.doll.distanceTo(target);
                double distToOwner = this.doll.distanceTo(owner);
                
                // 停在目标周围 2 格处，同时保持在玩家 4 格内
                if (distToOwner > 4.0D) {
                    // 先跟紧玩家
                    Vec3 toOwner = owner.position().subtract(this.doll.position()).normalize();
                    this.doll.getMoveControl().setWantedPosition(
                        owner.getX() + toOwner.x * 1.5, 
                        owner.getY() + 1.5, 
                        owner.getZ() + toOwner.z * 1.5, 
                        1.0D
                    );
                } else if (distToTarget > 2.5D) {
                    // 靠近目标，但停在2.5格外
                    Vec3 dir = target.position().subtract(this.doll.position()).normalize();
                    Vec3 targetPos = target.position().subtract(dir.scale(2.0D));
                    this.doll.getMoveControl().setWantedPosition(
                        targetPos.x, 
                        owner.getY() + 1.5, 
                        targetPos.z, 
                        1.0D
                    );
                } else if (distToTarget < 1.5D) {
                    // 太近，后退
                    Vec3 awayDir = this.doll.position().subtract(target.position()).normalize();
                    Vec3 targetPos = this.doll.position().add(awayDir.scale(1.5D));
                    this.doll.getMoveControl().setWantedPosition(
                        targetPos.x, 
                        owner.getY() + 1.5, 
                        targetPos.z, 
                        1.0D
                    );
                }

                // 攻击判定
                if (this.doll.getBoundingBox().inflate(0.8D).intersects(target.getBoundingBox())) {
                    if (!isSameOwner(target)) {
                        this.doll.doHurtTarget(target);
                    }
                }
            }
        }
    }

    class DollPillagerAttackGoal extends Goal {
        private final DollEntity doll;
        private int attackDelay = 0;

        public DollPillagerAttackGoal(DollEntity doll) {
            this.doll = doll;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 必须处于激怒状态
            if (!doll.isEnraged()) {
                return false;
            }
            return this.doll.getDollType() == DollType.PILLAGER && this.doll.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.doll.getTarget();
            if (target != null) {
                double dist = this.doll.distanceTo(target);
                boolean canSee = this.doll.getSensing().hasLineOfSight(target);
                
                if (dist > 8.0D) {
                    this.doll.getMoveControl().setWantedPosition(target.getX(), target.getY() + 1.5, target.getZ(), 1.0D);
                } else if (dist < 5.0D) {
                    Vec3 away = this.doll.position().subtract(target.position()).normalize().scale(3.0D);
                    this.doll.getMoveControl().setWantedPosition(this.doll.getX() + away.x, this.doll.getY(), this.doll.getZ() + away.z, 1.0D);
                }

                this.doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
                
                if (attackDelay > 0) attackDelay--;
                if (canSee && attackDelay <= 0 && dist <= 12.0D) {
                    if (!isSameOwner(target)) {
                        this.doll.performRangedAttack(target, 1.0F);
                    }
                    attackDelay = 40;
                }
            }
        }
    }

    class DollLancerAttackGoal extends Goal {
        private final DollEntity doll;
        private int chargeCooldown = 0;

        public DollLancerAttackGoal(DollEntity doll) {
            this.doll = doll;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 必须处于激怒状态
            if (!doll.isEnraged()) {
                return false;
            }
            return this.doll.getDollType() == DollType.LANCER && this.doll.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.doll.getTarget();
            if (target != null) {
                this.doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
                double dist = this.doll.distanceTo(target);

                if (!this.doll.isCharging) {
                    if (dist < 10.0D) {
                        Vec3 away = this.doll.position().subtract(target.position()).normalize().scale(2.0D);
                        this.doll.getMoveControl().setWantedPosition(this.doll.getX() + away.x, this.doll.getY(), this.doll.getZ() + away.z, 0.8D);
                    }
                    chargeCooldown++;
                    if (chargeCooldown > 100) { // 5 seconds
                        this.doll.isCharging = true;
                        this.doll.chargeTicks = 20;
                        chargeCooldown = 0;
                    }
                } else {
                    // 冲锋逻辑
                    Vec3 chargeDir = target.position().subtract(this.doll.position()).normalize().scale(1.2D);
                    this.doll.setDeltaMovement(chargeDir);
                    if (this.doll.getBoundingBox().inflate(1.2D).intersects(target.getBoundingBox())) {
                        if (!isSameOwner(target)) {
                            this.doll.doHurtTarget(target);
                        }
                        this.doll.isCharging = false;
                    }
                    this.doll.chargeTicks--;
                    if (this.doll.chargeTicks <= 0) this.doll.isCharging = false;
                }
            }
        }
    }

    class DollSharpshooterAttackGoal extends Goal {
        private final DollEntity doll;
        private int attackDelay = 0;

        public DollSharpshooterAttackGoal(DollEntity doll) {
            this.doll = doll;
            this.setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // 必须处于激怒状态
            if (!doll.isEnraged()) {
                return false;
            }
            return this.doll.getDollType() == DollType.SHARPSHOOTER && this.doll.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = this.doll.getTarget();
            if (target != null) {
                this.doll.getLookControl().setLookAt(target, 30.0F, 30.0F);
                if (attackDelay > 0) attackDelay--;
                if (attackDelay <= 0 && this.doll.getSensing().hasLineOfSight(target)) {
                    if (!isSameOwner(target)) {
                        this.doll.performRangedAttack(target, 1.0F);
                    }
                    attackDelay = 30;
                }
            }
        }
    }

    class DollMarkedTargetGoal extends TargetGoal {
        private final DollEntity doll;

        public DollMarkedTargetGoal(DollEntity doll) {
            super(doll, false);
            this.doll = doll;
        }

        @Override
        public boolean canUse() {
            // 只有激怒的人偶才使用此目标
            if (!doll.isEnraged()) {
                return false;
            }
            
            if (doll.isOwnerBlindOrDark()) {
                return false;
            }
            
            LivingEntity owner = this.doll.getOwner();
            if (owner instanceof Player player) {
                Set<Integer> marked = player.getData(ModAttachments.MARKED_TARGETS);
                if (!marked.isEmpty()) {
                    for (int id : marked) {
                        Entity target = player.level().getEntity(id);
                        if (target instanceof LivingEntity living && living.isAlive() && this.doll.canAttack(living) && !isSameOwner(living)) {
                            this.targetMob = living;
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public void start() {
            // 只有已经激怒的人偶才设置目标
            if (this.doll.isEnraged()) {
                this.doll.setTarget(this.targetMob);
            }
            // 如果未激怒，不设置 target
            super.start();
        }
    }

    class DollOwnerTargetGoal extends TargetGoal {
        private final DollEntity doll;

        public DollOwnerTargetGoal(DollEntity doll) {
            super(doll, false);
            this.doll = doll;
        }

        @Override
        public boolean canUse() {
            // 主人失明或黑暗时无法锁定新目标
            if (doll.isOwnerBlindOrDark()) {
                return false;
            }
            // 必须处于激怒状态
            if (!doll.isEnraged()) {
                return false;
            }
            LivingEntity owner = this.doll.getOwner();
            if (owner != null) {
                LivingEntity target = owner.getLastHurtMob();
                if (target != null && target != owner && this.doll.canAttack(target) && !isSameOwner(target)) {
                    return true;
                }
                target = owner.getKillCredit();
                if (target != null && target != owner && this.doll.canAttack(target) && !isSameOwner(target)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            LivingEntity owner = this.doll.getOwner();
            if (owner != null) {
                LivingEntity target = owner.getLastHurtMob();
                if (target == null) target = owner.getKillCredit();
                if (!isSameOwner(target)) {
                    this.doll.setTarget(target);
                }
            }
            super.start();
        }
    }
}
