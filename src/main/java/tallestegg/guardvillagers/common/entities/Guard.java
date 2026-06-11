package tallestegg.guardvillagers.common.entities;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.golem.IronGolem;
// AbstractHorse import removed - MC 26.1.x restructured horse package
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.item.component.KineticWeapon;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import tallestegg.guardvillagers.GuardEntityType;
import tallestegg.guardvillagers.GuardVillagers;
import tallestegg.guardvillagers.GuardSounds;
import tallestegg.guardvillagers.common.entities.ai.goals.GuardRetreatGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GuardMountHorseGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.PickupBetterEquipmentGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GuardHelpNearbyGuardGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GuardShareFoodGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GetOutOfWaterGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GuardSquadGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.WeaponBehavior;
import tallestegg.guardvillagers.common.entities.ai.goals.GuardFormationGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.AntiCreeperGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.TargetPrioritizationGoal;
import tallestegg.guardvillagers.configuration.GuardConfig;
import tallestegg.guardvillagers.loot_tables.GuardLootTables;
import tallestegg.guardvillagers.networking.GuardOpenInventoryPacket;

import org.jetbrains.annotations.Nullable;
import java.util.*;
import java.util.function.Predicate;

public class Guard extends PathfinderMob implements CrossbowAttackMob, RangedAttackMob, ReputationEventHandler, NeutralMob {
    private static final AttributeModifier USE_ITEM_SPEED_PENALTY = new AttributeModifier(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "item_slow_down"), -0.3D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    private static final EntityDataAccessor<Optional<BlockPos>> GUARD_POS = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> PATROLLING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> GUARD_VARIANT = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> RUNNING_TO_EAT = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_STATE = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Pose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<Pose, EntityDimensions>builder().put(Pose.SLEEPING, SLEEPING_DIMENSIONS).put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F)).put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F)).put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F).withEyeHeight(0.4F)).put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.5F).withEyeHeight(1.27F).withAttachments(EntityAttachments.builder().attach(EntityAttachment.VEHICLE, new Vec3(0.0, 0.6, 0.0)))).put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F).withEyeHeight(1.62F)).build();
    private final GossipContainer gossips = new GossipContainer();
    public long lastGossipTime;
    public long lastGossipDecayTime;
    public SimpleContainer guardInventory = new SimpleContainer(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    protected boolean spawnWithArmor;
    @Nullable
    private UUID ownerId;
    @Nullable
    private EntityReference<LivingEntity> persistentAngerTarget;
    private static final EntityDataAccessor<Long> DATA_ANGER_END_TIME = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.LONG);
    private static final UniformInt PERSISTENT_ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final AttributeModifier HORSE_SPEED_COMPENSATOR = new AttributeModifier(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "horse_speed_compensator"), 1.5F, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    // Feature 1: Guard Leveling - Rank enum and data
    public enum GuardRank {
        RECRUIT(0), SOLDIER(1), VETERAN(2), CAPTAIN(3);
        public final int level;
        GuardRank(int level) { this.level = level; }
    }
    private static final EntityDataAccessor<Integer> KILL_COUNT = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> GUARD_RANK = SynchedEntityData.defineId(Guard.class, EntityDataSerializers.INT);

    // Feature 6: Wounded Behavior - state tracking
    private boolean wasWounded = false;

    // Feature 8: Night Watch - state tracking
    private boolean wasNight = false;

    // Feature 3: War Horn - combat stance timer
    private int combatStanceTicks = 0;

    // Feature 1 (Patrol Route): Patrol waypoints list and current index
    private List<BlockPos> patrolWaypoints = new ArrayList<>();
    private int currentWaypointIndex = 0;
    private int waypointWaitTicks = 0;

    // Feature 7 (Squad System): Squad leader reference
    @Nullable
    private EntityReference<Guard> squadLeader;

    // Feature 3 (Enhanced Rank): Captain's Inspiration tick tracker
    private int captainInspirationCheckTick = -1;

    // === Performance optimization: cached values ===
    private int cachedWoundedCheckTick = -1;
    private boolean cachedWoundedResult = false;
    private int cachedNightCheckTick = -1;
    private boolean cachedNightResult = false;

    // PERFORMANCE: Cache owner lookup - getPlayerByUUID is expensive (iterates all players)
    // and was called 3x per getOwner() invocation. Now we cache the result and refresh
    // every 100 ticks (5 seconds) or on demand.
    private int cachedOwnerCheckTick = -1;
    private LivingEntity cachedOwner = null;
    private static final int OWNER_CACHE_INTERVAL = 100; // ticks

    // Friendly fire check cache - prevents archer freeze bug where guards
    // repeatedly draw and cancel their bow shots when a friendly is in line of sight.
    // Previously, friendlyInLineOfSight() returned false on non-check ticks,
    // causing the bow goal to immediately re-draw after being stopped.
    private boolean cachedFriendlyInSight = false;
    private int cachedFriendlyCheckTick = -1;

    // Attribute modifiers for leveling
    private static final Identifier RANK_HEALTH_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "rank_health_bonus");
    private static final Identifier RANK_DAMAGE_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "rank_damage_bonus");
    private static final Identifier NIGHT_RANGE_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "night_follow_range");
    private static final Identifier WOUNDED_SPEED_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "wounded_speed_penalty");
    private static final Identifier CAPTAIN_INSPIRATION_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "captain_inspiration");
    private static final Identifier RANK_FOLLOW_RANGE_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "rank_follow_range_bonus");

    public Guard(EntityType<? extends Guard> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
        if (GuardConfig.COMMON.GuardsOpenDoors) this.getNavigation().setCanOpenDoors(true);
        this.setPathfindingMalus(PathType.POWDER_SNOW, -1.0F);
        this.setPathfindingMalus(PathType.ON_TOP_OF_POWDER_SNOW, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGING_IN_NEIGHBOR, -1.0F);
        this.setPathfindingMalus(PathType.DAMAGING, -1.0F);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new GuardGroundPathNavigation(this, level);
    }

    // 26.1.x: VillagerType.byBiome() returns ResourceKey<VillagerType>
    // In MC 26.1.x (1.21.4+), ResourceKey uses location() instead of identifier()
    public static String getVariantFromBiome(LevelAccessor world, BlockPos pos) {
        ResourceKey<VillagerType> type = VillagerType.byBiome(world.getBiome(pos));
        return GuardVillagers.removeModIdFromVillagerType(type.identifier().toString());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, GuardConfig.STARTUP.healthModifier).add(Attributes.MOVEMENT_SPEED, GuardConfig.STARTUP.speedModifier).add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.FOLLOW_RANGE, GuardConfig.STARTUP.followRangeModifier);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, EntitySpawnReason reason, @Nullable SpawnGroupData spawnDataIn) {
        this.setPersistenceRequired();
        String type = getVariantFromBiome(level(), this.blockPosition());
        this.setVariant(type);
        RandomSource randomsource = worldIn.getRandom();
        this.populateDefaultEquipmentSlots(randomsource, difficultyIn);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn);
    }

    @Override
    protected void doPush(Entity entityIn) {
        if (entityIn instanceof PathfinderMob living) {
            boolean attackTargets = living.getTarget() instanceof Villager || living.getTarget() instanceof IronGolem || living.getTarget() instanceof Guard;
            if (attackTargets) this.setTarget(living);
        }
        super.doPush(entityIn);
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.entityData.get(GUARD_POS).orElse(null);
    }

    public void setPatrolPos(BlockPos position) {
        this.entityData.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuardSounds.GUARD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return GuardSounds.GUARD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuardSounds.GUARD_DEATH;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHitIn) {
        for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.guardInventory.getItem(i);
            RandomSource randomsource = level().getRandom();
            if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) && randomsource.nextFloat() < GuardConfig.COMMON.chanceToDropEquipment)
                if (this.level() instanceof ServerLevel serverLevel) {
                    this.spawnAtLocation(serverLevel, itemstack);
                }
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        String ownerStr = input.getStringOr("Owner", "");
        if (!ownerStr.isEmpty()) {
            try {
                this.setOwnerId(UUID.fromString(ownerStr));
            } catch (Throwable t) {
                this.setOwnerId(null);
            }
        }
        this.kickTicks = input.getIntOr("KickTicks", 0);
        this.setFollowing(input.getBooleanOr("Following", false));
        this.interacting = input.getBooleanOr("Interacting", false);
        this.setPatrolling(input.getBooleanOr("Patrolling", false));
        this.shieldCoolDown = input.getIntOr("ShieldCooldown", 0);
        this.kickCoolDown = input.getIntOr("KickCooldown", 0);
        this.lastGossipDecayTime = input.getLongOr("LastGossipDecay", 0L);
        this.lastGossipTime = input.getLongOr("LastGossipTime", 0L);
        this.spawnWithArmor = input.getBooleanOr("SpawnWithArmor", false);
        input.getString("Variant").ifPresent(v -> this.setVariant(v));
        var optX = input.getInt("PatrolPosX");
        var optY = input.getInt("PatrolPosY");
        var optZ = input.getInt("PatrolPosZ");
        if (optX.isPresent() && optY.isPresent() && optZ.isPresent()) {
            this.entityData.set(GUARD_POS, Optional.of(new BlockPos(optX.get(), optY.get(), optZ.get())));
        }
        for (ItemStackWithSlot itemstackwithslot : input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC)) {
            if (itemstackwithslot.isValidInContainer(this.guardInventory.getContainerSize())) {
                this.guardInventory.setItem(itemstackwithslot.slot(), itemstackwithslot.stack());
            }
        }
        if (input.keySet().contains("equipment")) {
            input.read("equipment", EntityEquipment.CODEC).ifPresent(equipment -> {
                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR || slot.getType() == EquipmentSlot.Type.HAND) {
                        ItemStack stackFromslot = equipment.get(slot);
                        this.guardInventory.setItem(Guard.slotToInventoryIndex(slot), stackFromslot);
                    }
                }
            });
        }
        this.readPersistentAngerSaveData(this.level(), input);
        // Load gossip data
        this.gossips.clear();
        input.read("Gossips", GossipContainer.CODEC).ifPresent(this.gossips::putAll);
        // Feature 1: Load leveling data
        this.entityData.set(KILL_COUNT, input.getIntOr("KillCount", 0));
        this.entityData.set(GUARD_RANK, input.getIntOr("GuardRank", 0));
        // Feature 3: Load combat stance
        this.combatStanceTicks = input.getIntOr("CombatStanceTicks", 0);
        // Feature 1 (Patrol Route): Load patrol waypoints
        this.patrolWaypoints.clear();
        int wpCount = input.getIntOr("PatrolWaypointCount", 0);
        for (int i = 0; i < wpCount; i++) {
            int wx = input.getIntOr("PatrolWP" + i + "X", 0);
            int wy = input.getIntOr("PatrolWP" + i + "Y", 0);
            int wz = input.getIntOr("PatrolWP" + i + "Z", 0);
            this.patrolWaypoints.add(new BlockPos(wx, wy, wz));
        }
        this.currentWaypointIndex = input.getIntOr("CurrentWaypointIndex", 0);
        // Feature 7 (Squad): Load squad leader UUID
        String squadLeaderStr = input.getStringOr("SquadLeaderUUID", "");
        if (!squadLeaderStr.isEmpty()) {
            // Squad leader will be re-resolved when the entity ticks
            try { this.pendingSquadLeaderUUID = UUID.fromString(squadLeaderStr); } catch (Throwable t) { this.pendingSquadLeaderUUID = null; }
        }
        // BUG FIX: Re-apply rank modifiers on entity load, otherwise guards
        // lose their health/damage bonuses after server restart or chunk reload.
        this.applyRankModifiers();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        this.addPersistentAngerSaveData(output);
        output.putString("Variant", this.getVariant());
        output.putInt("KickTicks", this.kickTicks);
        output.putInt("ShieldCooldown", this.shieldCoolDown);
        output.putInt("KickCooldown", this.kickCoolDown);
        output.putBoolean("Following", this.isFollowing());
        output.putBoolean("Interacting", this.interacting);
        output.putBoolean("Patrolling", this.isPatrolling());
        output.putBoolean("SpawnWithArmor", this.spawnWithArmor);
        output.putLong("LastGossipTime", this.lastGossipTime);
        output.putLong("LastGossipDecay", this.lastGossipDecayTime);
        UUID owner = this.getOwnerId();
        if (owner != null) {
            output.putString("Owner", owner.toString());
        }
        var patrol = this.getPatrolPos();
        if (patrol != null) {
            output.putInt("PatrolPosX", patrol.getX());
            output.putInt("PatrolPosY", patrol.getY());
            output.putInt("PatrolPosZ", patrol.getZ());
        }
        ValueOutput.TypedOutputList<ItemStackWithSlot> typedoutputlist = output.list("Inventory", ItemStackWithSlot.CODEC);
        for (int i = 0; i < this.guardInventory.getContainerSize(); i++) {
            ItemStack itemstack = this.guardInventory.getItem(i);
            if (!itemstack.isEmpty()) {
                typedoutputlist.add(new ItemStackWithSlot(i, itemstack));
            }
        }
        // Save gossip data
        output.store("Gossips", GossipContainer.CODEC, this.gossips);
        // Feature 1: Save leveling data
        output.putInt("KillCount", this.entityData.get(KILL_COUNT));
        output.putInt("GuardRank", this.entityData.get(GUARD_RANK));
        // Feature 3: Save combat stance
        output.putInt("CombatStanceTicks", this.combatStanceTicks);
        // Feature 1 (Patrol Route): Save patrol waypoints
        output.putInt("PatrolWaypointCount", this.patrolWaypoints.size());
        for (int i = 0; i < this.patrolWaypoints.size(); i++) {
            BlockPos wp = this.patrolWaypoints.get(i);
            output.putInt("PatrolWP" + i + "X", wp.getX());
            output.putInt("PatrolWP" + i + "Y", wp.getY());
            output.putInt("PatrolWP" + i + "Z", wp.getZ());
        }
        output.putInt("CurrentWaypointIndex", this.currentWaypointIndex);
        // Feature 7 (Squad): Save squad leader UUID
        Guard leader = this.getSquadLeader();
        if (leader != null) {
            output.putString("SquadLeaderUUID", leader.getUUID().toString());
        }
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> 1;
            case FEET -> 3;
            case LEGS -> 2;
            case HEAD -> 0;
            case MAINHAND -> 5;
            case OFFHAND -> 4;
            default -> 0;
        };
    }

    private void maybeDecayGossip() {
        long i = level().getGameTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    @Override
    protected void completeUsingItem() {
        if (!this.isUsingItem()) {
            return;
        }
        InteractionHand hand = this.getUsedItemHand();
        ItemStack useItemStack = this.getItemInHand(hand);
        if (useItemStack.isEmpty()) {
            return;
        }
        ItemStack consumedStack = useItemStack.copy();
        if (Guard.isConsumable(consumedStack)) {
            FoodProperties food = consumedStack.get(DataComponents.FOOD);
            if (food != null) {
                // Only heal for half the nutrition value since finishUsingItem()
                // will also apply saturation and other vanilla food effects.
                // Without this reduction, guards get double-healing (manual heal + vanilla heal).
                this.heal((float) food.nutrition() * 0.5F);
            }
        }
        // Fabric: Removed EventHooks.onItemUseFinish() - just use finishUsingItem directly
        ItemStack result = useItemStack.finishUsingItem(this.level(), this);
        if (result != useItemStack) {
            this.setItemInHand(hand, result);
        }
        this.stopUsingItem();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot pSlot) {
        return switch (pSlot) {
            case HEAD -> this.guardInventory.getItem(0);
            case CHEST -> this.guardInventory.getItem(1);
            case LEGS -> this.guardInventory.getItem(2);
            case FEET -> this.guardInventory.getItem(3);
            case OFFHAND -> this.guardInventory.getItem(4);
            case MAINHAND -> this.guardInventory.getItem(5);
            default -> ItemStack.EMPTY;
        };
    }

    public GossipContainer getGossips() {
        return this.gossips;
    }

    public int getPlayerReputation(Player player) {
        return this.gossips.getReputation(player.getUUID(), (gossipType) -> true);
    }

    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            if (uuid == null) return null;

            // PERFORMANCE: Cache the owner lookup. Previously this method called
            // getPlayerByUUID() 3 times per invocation, which iterates over all
            // server players each time. Now we do ONE lookup and cache for 5 seconds.
            int currentTick = this.tickCount;
            if (currentTick != this.cachedOwnerCheckTick) {
                this.cachedOwnerCheckTick = currentTick;
                Player player = level().getPlayerByUUID(uuid);
                if (player == null) {
                    this.cachedOwner = null;
                } else {
                    boolean heroOfTheVillage = player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
                    // If followHero is enabled, owner only counts when they have HOTV
                    // If followHero is disabled, owner always counts
                    if (GuardConfig.COMMON.followHero && !heroOfTheVillage) {
                        this.cachedOwner = null; // Don't follow without HOTV
                    } else {
                        this.cachedOwner = player;
                    }
                }
            }
            return this.cachedOwner;
        } catch (IllegalArgumentException illegalargumentexception) {
            return null;
        }
    }

    public boolean isOwner(LivingEntity entityIn) {
        // PERFORMANCE: Quick UUID check before expensive getOwner() lookup.
        // Most entities are NOT the owner, so this cheap check avoids the
        // expensive getPlayerByUUID() call in 99% of cases.
        if (entityIn == null) return false;
        UUID uuid = this.getOwnerId();
        if (uuid == null) return false;
        if (!(entityIn instanceof Player player) || !player.getUUID().equals(uuid)) return false;
        return entityIn == this.getOwner();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(@Nullable UUID id) {
        this.ownerId = id;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        // Feature 5: Enhanced Mount - extra knockback when mounted
        float knockbackStrength = 1.0F;
        if (this.isMounted() && GuardConfig.COMMON.guardsAutoMountHorses) {
            knockbackStrength *= GuardConfig.COMMON.mountedKnockbackBonus;
        }
        if (this.isKicking()) {
            ((LivingEntity) target).knockback(knockbackStrength, Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), (-Mth.cos(this.getYRot() * ((float) Math.PI / 180F))));
            this.kickTicks = 10;
            level().broadcastEntityEvent(this, (byte) 4);
            this.lookAt(target, 90.0F, 90.0F);
        }
        // Feature 2: Berserker bonus damage
        float berserkerBonus = 0.0F;
        if (this.isBerserker()) {
            berserkerBonus = (float) GuardConfig.COMMON.berserkerDamageBonus;
        }
        // Feature 3: Combat Stance (War Horn) — damage boost while active
        float stanceBonus = 0.0F;
        if (this.isInCombatStance()) {
            stanceBonus = (float) GuardConfig.COMMON.combatStanceDamageBonus;
        }
        ItemStack hand = this.getMainHandItem();
        this.damageGuardItem(1, EquipmentSlot.MAINHAND, hand);

        // === Feature 6: Advanced Cavalry — lance charge bonus ===
        float cavalryBonus = 0.0F;
        if (this.isMounted() && (hand.getItem() instanceof TridentItem || hand.getItem() instanceof AxeItem)) {
            cavalryBonus = (float) GuardConfig.COMMON.cavalryChargeDamageBonus;
        }

        // === Feature 12: Advanced Weapon Balance — weapon-type specific bonuses ===
        float weaponBonus = 0.0F;
        // Trident/Spear vs mounted enemy: +100% damage (2x)
        if (hand.getItem() instanceof TridentItem && target instanceof LivingEntity livingTarget && livingTarget.getVehicle() != null) {
            weaponBonus += (float) GuardConfig.COMMON.spearVsMountedBonus;
        }
        // Axe vs shield-blocking enemy: +50% damage AND disable shield for 5 seconds
        if (hand.getItem() instanceof AxeItem && target instanceof LivingEntity livingTarget && isActivelyBlocking(livingTarget)) {
            weaponBonus += (float) GuardConfig.COMMON.axeVsShieldBonus;
            // Disable the target's shield
            if (livingTarget instanceof Player player) {
                player.getCooldowns().addCooldown(livingTarget.getUseItem(), GuardConfig.COMMON.axeShieldDisableSeconds * 20);
                livingTarget.stopUsingItem();
            } else if (livingTarget instanceof Guard guardTarget) {
                guardTarget.disableShieldFor(GuardConfig.COMMON.axeShieldDisableSeconds * 20);
            }
        }
        // Mace: +25% damage always (heavy weapon)
        if (hand.getItem() instanceof MaceItem) {
            weaponBonus += (float) GuardConfig.COMMON.maceDamageBonus;
        }

        // BUG FIX: Use the target parameter instead of getTarget() to ensure
        // we actually attack the entity that was passed in, not a potentially
        // different or stale target.
        // Apply all damage bonuses via temporary attribute modifier
        float totalBonus = berserkerBonus + stanceBonus + cavalryBonus + weaponBonus;
        if (totalBonus > 0.0F) {
            AttributeInstance dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) {
                dmgAttr.addTransientModifier(new AttributeModifier(
                        Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "temp_damage_boost"),
                        totalBonus, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        boolean result = super.doHurtTarget((ServerLevel) level(), target);
        if (totalBonus > 0.0F) {
            AttributeInstance dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null) {
                dmgAttr.removeModifier(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "temp_damage_boost"));
            }
        }
        // Feature 1: Kill counting for leveling
        if (result && target instanceof LivingEntity living && !living.isAlive() && GuardConfig.COMMON.guardLeveling) {
            this.incrementKillCount();
        }
        return result;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.kickTicks = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    public void die(DamageSource source) {
        // Fabric: Removed EventHooks.canLivingConvert() - always allow conversion
        if (GuardConfig.COMMON.convertGuardOnDeath && (level().getDifficulty() == Difficulty.NORMAL || level().getDifficulty() == Difficulty.HARD) && source.getEntity() instanceof Zombie) {
            if (this.level() instanceof ServerLevel serverLevel) {
                if (level().getDifficulty() != Difficulty.HARD && this.random.nextBoolean()) {
                    super.die(source);
                    return;
                }
                ZombieVillager zombieguard = EntityType.ZOMBIE_VILLAGER.create(serverLevel, EntitySpawnReason.CONVERSION);
                if (zombieguard == null) {
                    super.die(source);
                    return;
                }
                zombieguard.copyPosition(this);
                zombieguard.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(zombieguard.blockPosition()), EntitySpawnReason.CONVERSION, null);
                zombieguard.setNoAi(this.isNoAi());
                zombieguard.setCustomName(this.getCustomName());
                zombieguard.setCustomNameVisible(this.isCustomNameVisible());
                zombieguard.setPersistenceRequired();

                for (EquipmentSlot slot : EquipmentSlot.values()) {
                    ItemStack stack = this.getItemBySlot(slot);
                    if (!stack.isEmpty()) {
                        zombieguard.setItemSlot(slot, stack.copy());
                    }
                }

                if (!this.isSilent()) {
                    level().levelEvent(null, 1026, this.blockPosition(), 0);
                }

                serverLevel.addFreshEntityWithPassengers(zombieguard);
                this.discard();
                return;
            }
        }

        super.die(source);
        // Close any open guard inventory screens for players viewing this guard
        if (this.interacting && this.level() instanceof ServerLevel serverLevel) {
            for (ServerPlayer player : serverLevel.players()) {
                if (player.containerMenu instanceof GuardContainer gc && gc.getGuard() == this) {
                    player.closeContainer();
                }
            }
        }
        net.minecraft.network.chat.Component deathMessage = this.getCombatTracker().getDeathMessage();
        if (this.dead)
            if (this.level() instanceof ServerLevel serverLevel
                    && serverLevel.getGameRules().get(GameRules.SHOW_DEATH_MESSAGES)
                    && this.getOwner() instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(deathMessage);
            }
    }

    @Override
    public void aiStep() {
        if (this.kickTicks > 0) --this.kickTicks;
        if (this.kickCoolDown > 0) --this.kickCoolDown;
        if (this.shieldCoolDown > 0) --this.shieldCoolDown;
        // Feature 3: Combat stance tick down
        if (this.combatStanceTicks > 0) --this.combatStanceTicks;
        // PERFORMANCE: Only check health regen every 200 ticks (already modulo-based)
        if (this.tickCount % 200 == 0 && this.getHealth() < this.getMaxHealth()) {
            this.heal((float) GuardConfig.COMMON.amountOfHealthRegenerated);
        }
        if (spawnWithArmor) {
            getItemsFromLootTable(this);
            this.spawnWithArmor = false;
        }
        // Feature 6: Wounded Behavior (internally throttled)
        this.tickWoundedBehavior();
        // Feature 8: Night Watch (internally throttled)
        this.tickNightWatch();
        // Feature 3 (Enhanced Rank): Captain's Inspiration buff
        this.tickCaptainInspiration();
        // Feature 1 (Patrol Route): Waypoint wait timer
        if (this.waypointWaitTicks > 0) --this.waypointWaitTicks;
        // Feature 7 (Squad): Resolve pending squad leader
        this.resolveSquadLeader();
        this.updateSwingTime();
        super.aiStep();
        // Mount navigation: when riding a horse (or any Mob), forward the
        // guard's desired movement to the mount. Without this, the horse just
        // stands still or uses its own AI, and the guard can't go anywhere.
        this.tickMountNavigation();
    }

    @Override
    public void tick() {
        this.maybeDecayGossip();
        super.tick();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return SIZE_BY_POSE.getOrDefault(pose, EntityDimensions.scalable(0.6F, 1.95F));
    }

    @Override
    protected void blockUsingItem(ServerLevel level, LivingEntity entityIn) {
        super.blockUsingItem(level, entityIn);
        this.playSound(SoundEvents.SHIELD_BLOCK.value(), 1.0F, 1.0F);

        ItemStack blocking = this.getItemBlockingWith();
        if (blocking.isEmpty()) return;
        BlocksAttacks blocksAttacks = blocking.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) return;
        float disableSeconds = entityIn.getSecondsToDisableBlocking();
        if (disableSeconds <= 0.0F) return;
        float scale = blocksAttacks.disableCooldownScale();
        if (scale <= 0.0F) return;
        int disableTicks = Mth.ceil(disableSeconds * 20.0F * scale);
        this.disableShieldFor(disableTicks);
    }

    public void disableShieldFor(int ticks) {
        this.shieldCoolDown = ticks;
        this.stopUsingItem();
        this.level().broadcastEntityEvent(this, (byte) 30);
    }

    public static boolean isActivelyBlocking(LivingEntity e) {
        if (!e.isBlocking()) return false;
        ItemStack blocking = e.getItemBlockingWith();
        return !blocking.isEmpty() && blocking.has(DataComponents.BLOCKS_ATTACKS);
    }

    @Override
    public void startUsingItem(InteractionHand hand) {
        super.startUsingItem(hand);
        ItemStack itemstack = this.getItemInHand(hand);
        if (!itemstack.isEmpty() && itemstack.has(DataComponents.BLOCKS_ATTACKS)) {
            AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            assert modifiableattributeinstance != null;
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTransientModifier(USE_ITEM_SPEED_PENALTY);
        }
    }

    @Override
    public boolean startRiding(Entity vehicle, boolean force, boolean sendGameEvent) {
        if (vehicle instanceof LivingEntity living)
            living.getAttribute(Attributes.MOVEMENT_SPEED).addOrUpdateTransientModifier(HORSE_SPEED_COMPENSATOR);
        return super.startRiding(vehicle, force, sendGameEvent);
    }

    @Override
    public void stopRiding() {
        Entity entity = this.getVehicle();
        if (entity instanceof LivingEntity living)
            living.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(HORSE_SPEED_COMPENSATOR);
        super.stopRiding();
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        if (this.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY.id()))
            this.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder data) {
        super.defineSynchedData(data);
        data.define(GUARD_VARIANT, "plains");
        data.define(DATA_CHARGING_STATE, false);
        data.define(KICKING, false);
        data.define(FOLLOWING, false);
        data.define(GUARD_POS, Optional.empty());
        data.define(PATROLLING, false);
        data.define(RUNNING_TO_EAT, false);
        data.define(DATA_ANGER_END_TIME, -1L);
        // Feature 1: Leveling data
        data.define(KILL_COUNT, 0);
        data.define(GUARD_RANK, 0);
    }

    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.entityData.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.entityData.set(KICKING, kicking);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource source, DifficultyInstance instance) {
        this.spawnWithArmor = true;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new KickGoal(this));
        this.goalSelector.addGoal(0, new GuardEatFoodGoal(this));
        this.goalSelector.addGoal(0, new RaiseShieldGoal(this));
        // Anti-Creeper: highest priority after survival goals — flee charging creepers
        this.goalSelector.addGoal(1, new AntiCreeperGoal(this));
        this.goalSelector.addGoal(1, new GuardRunToEatGoal(this));
        // Formation at priority 2 (peacetime only — activates when no combat target)
        // Previously at priority 4, which was ALWAYS overridden by WalkBackToCheckPoint
        // (priority 3) and GuardMeleeGoal (priority 3), making formations non-functional.
        if (GuardConfig.COMMON.GuardFormation)
            this.goalSelector.addGoal(2, new GuardFormationGoal(this));
        this.goalSelector.addGoal(2, new GuardRetreatGoal(this));
        this.goalSelector.addGoal(2, new PickupBetterEquipmentGoal(this));
        this.goalSelector.addGoal(3, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, (float) GuardConfig.COMMON.guardCrossbowAttackRadius));
        this.goalSelector.addGoal(3, new PassiveMobSpearUseGoal<>(this, 1.0D, 0.8D, 10.0F, 2.0F));
        this.goalSelector.addGoal(3, new GuardBowAttack(this, 1.0D, 20, 15.0F));
        this.goalSelector.addGoal(3, new GuardMeleeGoal(this, 1.0D, true));
        // Flanking is now integrated into GuardMeleeGoal.tick() — a separate
        // GuardFlankingGoal at the same priority would always lose to GuardMeleeGoal
        // in GoalSelector (same priority, same MOVE+LOOK flags, registered after).
        this.goalSelector.addGoal(4, new FollowHeroGoal(this, 1.0F, 10.0F, 4.0F));
        if (GuardConfig.COMMON.GuardsRunFromPolarBears)
            this.goalSelector.addGoal(4, new AvoidEntityGoal<>(this, PolarBear.class, 12.0F, 1.0D, 1.4D));
        this.goalSelector.addGoal(4, new MoveBackToVillageGoal(this, 0.6D, false));
        if (GuardConfig.COMMON.GuardsOpenDoors)
            this.goalSelector.addGoal(4, new GuardInteractDoorGoal(this, true));
        this.goalSelector.addGoal(3, new WalkBackToCheckPointGoal(this, 0.6D));
        if (GuardConfig.COMMON.guardPatrolAroundVillageWorkstations)
            this.goalSelector.addGoal(5, new GolemRandomStrollInVillageGoal(this, 0.6D));
        if (GuardConfig.COMMON.guardPatrolVillageAi)
            this.goalSelector.addGoal(5, new MoveThroughVillageGoal(this, 0.6D, false, 4, () -> false));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, AbstractVillager.class, 8.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        // Feature 5: Auto-mount horses (priority 5 = peacetime, avoids conflict
        // with WalkBackToCheckPointGoal at priority 3 which also uses MOVE flag)
        this.goalSelector.addGoal(5, new GuardMountHorseGoal(this));
        // Feature 7: Squad system — captain organizes nearby guards
        this.goalSelector.addGoal(4, new GuardSquadGoal(this));
        // Feature 10: Share food with wounded guards
        this.goalSelector.addGoal(1, new GuardShareFoodGoal(this));
        this.goalSelector.addGoal(8, new GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(this));
        // FloatGoal at priority 0 (highest) — same as Pillager, Vindicator, etc.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Get out of water when idle — move toward land
        this.goalSelector.addGoal(5, new GetOutOfWaterGoal(this, 0.6D));
        // BUG FIX: Exclude Guard.class from HurtByTargetGoal to prevent guard-vs-guard fights.
        this.targetSelector.addGoal(2, (new HurtByTargetGoal(this, Guard.class, IronGolem.class)).setAlertOthers());
        this.targetSelector.addGoal(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HeroHurtTargetGoal(this));
        this.targetSelector.addGoal(5, new DefendVillageGuardGoal(this));
        // Guard-to-guard target sharing — nearby idle guards help fight
        this.targetSelector.addGoal(4, new GuardHelpNearbyGuardGoal(this));
        if (GuardConfig.COMMON.AttackAllMobs) {
            // Smart target prioritization: replaces basic NearestAttackableTargetGoal
            if (GuardConfig.COMMON.smartTargetPrioritization) {
                this.targetSelector.addGoal(5, new TargetPrioritizationGoal(this, 5, true, (target, serverLevel) -> target instanceof Enemy && !GuardConfig.COMMON.isBlackListed(GuardVillagers.getEntityTypeId(target))));
            } else {
                this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, true, (target, serverLevel) -> target instanceof Enemy && !GuardConfig.COMMON.isBlackListed(GuardVillagers.getEntityTypeId(target))));
            }
        } else {
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Ravager.class, true));
            this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Witch.class, true));
            if (GuardConfig.COMMON.smartTargetPrioritization) {
                this.targetSelector.addGoal(5, new TargetPrioritizationGoal(this, 5, true, (target, serverLevel) -> target instanceof Raider && !GuardConfig.COMMON.isBlackListed(GuardVillagers.getEntityTypeId(target))));
                this.targetSelector.addGoal(5, new TargetPrioritizationGoal(this, 10, true, (target, serverLevel) -> target instanceof Zombie && !(target instanceof ZombifiedPiglin) && !GuardConfig.COMMON.isBlackListed(GuardVillagers.getEntityTypeId(target))));
            } else {
                this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Raider.class, true));
                this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, Zombie.class, true, (target, serverLevel) -> !(target instanceof ZombifiedPiglin)));
            }
        }
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 5, true, true, (target, serverLevel) -> GuardConfig.COMMON.isWhiteListed(GuardVillagers.getEntityTypeId(target))));
        this.targetSelector.addGoal(8, new ResetUniversalAngerTargetGoal<>(this, true));
    }

    @Override
    public boolean mayBeLeashed() {
        return false;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        this.shieldCoolDown = 8;
        if (this.getMainHandItem().getItem() instanceof CrossbowItem) this.performCrossbowAttack(this, 1.6F);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            ItemStack hand = this.getMainHandItem();
            ItemStack itemstack = this.getProjectile(hand);
            AbstractArrow abstractarrowentity = ProjectileUtil.getMobArrow(this, itemstack, distanceFactor, hand);
            double d0 = target.getX() - this.getX();
            double d1 = target.getY(0.3333333333333333D) - abstractarrowentity.getY();
            double d2 = target.getZ() - this.getZ();
            double d3 = Mth.sqrt((float) (d0 * d0 + d2 * d2));
            // FEATURE: Ranged accuracy improves with guard rank.
            // Base inaccuracy is 8.0F (same as vanilla skeleton). Each rank reduces it by 2.0F.
            // Recruit: 8.0F, Soldier: 6.0F, Veteran: 4.0F, Captain: 2.0F (very accurate).
            float inaccuracy = (float) Math.max(1.0F, GuardConfig.COMMON.rangedAccuracyBase
                    - this.getGuardRank().level * GuardConfig.COMMON.rangedAccuracyPerRank);
            abstractarrowentity.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, inaccuracy);
            this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            // 26.1.x: addFreshEntity() is only on ServerLevel, not Level
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.addFreshEntity(abstractarrowentity);
            }
            this.damageGuardItem(1, EquipmentSlot.MAINHAND, hand);
        }
    }

    @Override
    public void performCrossbowAttack(LivingEntity p_32337_, float p_32338_) {
        InteractionHand interactionhand = ProjectileUtil.getWeaponHoldingHand(p_32337_, Items.CROSSBOW);
        ItemStack itemstack = p_32337_.getItemInHand(interactionhand);
        if (itemstack.getItem() instanceof CrossbowItem crossbowitem) {
            crossbowitem.performShooting(p_32337_.level(), p_32337_, interactionhand, itemstack, p_32338_, 0.0F, null);
        }
        this.onCrossbowAttackPerformed();
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        super.setItemSlot(slotIn, stack);
        switch (slotIn) {
            case CHEST -> this.guardInventory.setItem(1, stack);
            case FEET -> this.guardInventory.setItem(3, stack);
            case HEAD -> this.guardInventory.setItem(0, stack);
            case LEGS -> this.guardInventory.setItem(2, stack);
            case MAINHAND -> this.guardInventory.setItem(5, stack);
            case OFFHAND -> this.guardInventory.setItem(4, stack);
        }
    }

    @Override
    public ItemStack getProjectile(ItemStack shootable) {
        if (shootable.getItem() instanceof ProjectileWeaponItem) {
            Predicate<ItemStack> predicate = ((ProjectileWeaponItem) shootable.getItem()).getSupportedHeldProjectiles();
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? Items.ARROW.getDefaultInstance() : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return (!GuardConfig.COMMON.isBlackListed(GuardVillagers.getEntityTypeId(target)) && !target.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && super.canAttack(target));
    }

    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getControlledVehicle() instanceof PathfinderMob creatureentity) {
            this.yBodyRot = creatureentity.yBodyRot;
        }
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.entityData.get(DATA_ANGER_END_TIME);
    }

    @Override
    public void setPersistentAngerEndTime(long persistentAngerEndTime) {
        this.entityData.set(DATA_ANGER_END_TIME, persistentAngerEndTime);
    }

    @Override
    public @org.jspecify.annotations.Nullable EntityReference<LivingEntity> getPersistentAngerTarget() {
        return persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@org.jspecify.annotations.Nullable EntityReference<LivingEntity> persistentAngerTarget) {
        this.persistentAngerTarget = persistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(PERSISTENT_ANGER_TIME.sample(this.random));
    }

    @Override
    public void setTarget(LivingEntity entity) {
        if (entity != null && entity.isAlive() && (((this.getTeam() != null && entity.getTeam() != null && this.getTeam().isAlliedTo(entity.getTeam()))) || GuardConfig.COMMON.isBlackListed(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString()) || entity.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) || this.isOwner(entity) || (entity instanceof TamableAnimal tamed && (tamed.getOwner() != null && tamed.getOwner().getUUID().equals(this.getOwnerId())))))
            return;
        super.setTarget(entity);
    }

    public void gossip(Villager villager, long gameTime) {
        if (gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) {
            this.gossips.transferFrom(villager.getGossips(), this.random, 10);
            this.lastGossipTime = gameTime;
        }
    }

    @Override
    protected void blockedByItem(LivingEntity entityIn) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.blockedByItem(entityIn);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean configValues = player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.COMMON.giveGuardStuffHOTV || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.COMMON.setGuardPatrolHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.COMMON.giveGuardStuffHOTV && GuardConfig.COMMON.setGuardPatrolHotv || this.getPlayerReputation(player) >= GuardConfig.COMMON.reputationRequirement || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !GuardConfig.COMMON.giveGuardStuffHOTV && !GuardConfig.COMMON.setGuardPatrolHotv || this.getOwnerId() != null && this.getOwnerId().equals(player.getUUID());
        boolean inventoryRequirements = !player.isSecondaryUseActive();
        if (inventoryRequirements) {
            if (this.getTarget() != player && this.isEffectiveAi() && configValues) {
                if (player instanceof ServerPlayer) {
                    player.swing(hand, true);
                    this.openGui((ServerPlayer) player);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void onReputationEventFrom(ReputationEventType reputationEventType, Entity entity) {
    }

    // 26.1.x note: hurtArmor() may be removed in favor of doHurtEquipment() only.
    // If this override fails to compile, remove it and rely on the default doHurtEquipment() behavior.
    @Override
    public void hurtArmor(DamageSource damageSource, float damage) {
        if (this.random.nextFloat() < GuardConfig.COMMON.chanceToBreakEquipment)
            this.doHurtEquipment(damageSource, damage, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
    }

    @Override
    public void thunderHit(ServerLevel p_241841_1_, LightningBolt p_241841_2_) {
        // Fabric: Removed EventHooks.canLivingConvert() - always allow conversion
        if (p_241841_1_.getDifficulty() != Difficulty.PEACEFUL) {
            Witch witchentity = EntityType.WITCH.create(p_241841_1_, EntitySpawnReason.EVENT);
            if (witchentity == null) return;
            witchentity.copyPosition(this);
            witchentity.finalizeSpawn(p_241841_1_, p_241841_1_.getCurrentDifficultyAt(witchentity.blockPosition()), EntitySpawnReason.CONVERSION, null);
            witchentity.setNoAi(this.isNoAi());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistenceRequired();
            p_241841_1_.addFreshEntityWithPassengers(witchentity);
            this.discard();
        } else {
            super.thunderHit(p_241841_1_, p_241841_2_);
        }
    }

    public void openGui(ServerPlayer player) {
        this.setOwnerId(player.getUUID());
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        this.interacting = true;
        player.nextContainerCounter();
        // Fabric: Use ServerPlayNetworking instead of PacketDistributor
        ServerPlayNetworking.send(player, new GuardOpenInventoryPacket(player.containerCounter, this.guardInventory.getContainerSize(), this.getId()));
        player.containerMenu = new GuardContainer(player.containerCounter, player.getInventory(), this.guardInventory, this);
        player.initMenu(player.containerMenu);
        // Fabric: Removed NeoForge PlayerContainerEvent.Open post
    }

    public boolean isEating() {
        return isConsumable(this.getUseItem()) && this.isUsingItem();
    }

    public boolean isPatrolling() {
        return this.entityData.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.entityData.set(PATROLLING, patrolling);
    }

    public boolean canFireProjectileWeapon(ProjectileWeaponItem item) {
        return item instanceof BowItem || item instanceof CrossbowItem;
    }

    public static boolean isConsumable(ItemStack stack) {
        // Explicit parentheses for clarity: EAT items are always consumable;
        // DRINK items are consumable only if not a splash potion (which should be thrown, not drunk)
        return stack.getUseAnimation() == ItemUseAnimation.EAT
                || (stack.getUseAnimation() == ItemUseAnimation.DRINK && !(stack.getItem() instanceof SplashPotionItem));
    }

    // === Feature 1: Guard Leveling ===

    public int getKillCount() {
        return this.entityData.get(KILL_COUNT);
    }

    public void incrementKillCount() {
        int kills = this.entityData.get(KILL_COUNT) + 1;
        this.entityData.set(KILL_COUNT, kills);
        this.checkRankUp();
    }

    public GuardRank getGuardRank() {
        int rankLevel = this.entityData.get(GUARD_RANK);
        for (GuardRank rank : GuardRank.values()) {
            if (rank.level == rankLevel) return rank;
        }
        return GuardRank.RECRUIT;
    }

    private void checkRankUp() {
        if (!GuardConfig.COMMON.guardLeveling) return;
        int kills = this.entityData.get(KILL_COUNT);
        int currentRank = this.entityData.get(GUARD_RANK);
        int newRank = currentRank;
        if (kills >= GuardConfig.COMMON.killsForCaptain) newRank = GuardRank.CAPTAIN.level;
        else if (kills >= GuardConfig.COMMON.killsForVeteran) newRank = GuardRank.VETERAN.level;
        else if (kills >= GuardConfig.COMMON.killsForSoldier) newRank = GuardRank.SOLDIER.level;
        if (newRank > currentRank) {
            this.entityData.set(GUARD_RANK, newRank);
            this.applyRankModifiers();
            // Heal on rank up
            this.heal(this.getMaxHealth() - this.getHealth());
        }
    }

    private void applyRankModifiers() {
        if (!GuardConfig.COMMON.guardLeveling) return;
        AttributeInstance healthAttr = this.getAttribute(Attributes.MAX_HEALTH);
        AttributeInstance damageAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeInstance rangeAttr = this.getAttribute(Attributes.FOLLOW_RANGE);
        if (healthAttr == null || damageAttr == null) return;
        // Remove old modifiers
        healthAttr.removeModifier(RANK_HEALTH_ID);
        damageAttr.removeModifier(RANK_DAMAGE_ID);
        if (rangeAttr != null) rangeAttr.removeModifier(RANK_FOLLOW_RANGE_ID);
        // Apply new modifiers based on rank
        GuardRank rank = this.getGuardRank();
        double healthBonus = switch (rank) {
            case SOLDIER -> GuardConfig.COMMON.soldierHealthBonus;
            case VETERAN -> GuardConfig.COMMON.veteranHealthBonus;
            case CAPTAIN -> GuardConfig.COMMON.captainHealthBonus;
            default -> 0;
        };
        double damageBonus = switch (rank) {
            case SOLDIER -> GuardConfig.COMMON.soldierDamageBonus;
            case VETERAN -> GuardConfig.COMMON.veteranDamageBonus;
            case CAPTAIN -> GuardConfig.COMMON.captainDamageBonus;
            default -> 0;
        };
        if (healthBonus > 0) {
            healthAttr.addTransientModifier(new AttributeModifier(RANK_HEALTH_ID, healthBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        if (damageBonus > 0) {
            damageAttr.addTransientModifier(new AttributeModifier(RANK_DAMAGE_ID, damageBonus, AttributeModifier.Operation.ADD_VALUE));
        }
        // Feature 3 (Enhanced Rank): Captains get +4 blocks follow range bonus
        if (rank == GuardRank.CAPTAIN && rangeAttr != null) {
            rangeAttr.addTransientModifier(new AttributeModifier(RANK_FOLLOW_RANGE_ID, 4.0D, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    public String getRankDisplayName() {
        return switch (this.getGuardRank()) {
            case SOLDIER -> "\u00A77[Soldier]\u00A7r";
            case VETERAN -> "\u00A7e[Veteran]\u00A7r";
            case CAPTAIN -> "\u00A7b[Captain]\u00A7r";
            default -> "";
        };
    }

    // === Feature 2: Weapon-Based Specialization ===

    public boolean isHoldingRangedWeapon() {
        ItemStack mainHand = this.getMainHandItem();
        return mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem || mainHand.getItem() instanceof TridentItem;
    }

    public boolean isBerserker() {
        if (!GuardConfig.COMMON.weaponSpecialization) return false;
        ItemStack mainHand = this.getMainHandItem();
        ItemStack offHand = this.getOffhandItem();
        return mainHand.getItem() instanceof AxeItem && !offHand.has(DataComponents.BLOCKS_ATTACKS);
    }

    public boolean isShieldGuard() {
        if (!GuardConfig.COMMON.weaponSpecialization) return false;
        return this.getOffhandItem().has(DataComponents.BLOCKS_ATTACKS) && !this.isHoldingRangedWeapon() && !(this.getMainHandItem().getItem() instanceof AxeItem);
    }

    // === Feature 3: War Horn - Combat Stance ===

    public void setCombatStanceTicks(int ticks) {
        this.combatStanceTicks = ticks;
    }

    public boolean isInCombatStance() {
        return this.combatStanceTicks > 0;
    }

    // === Feature 5: Enhanced Mount ===

    public boolean isMounted() {
        return this.getVehicle() != null;
    }

    /**
     * Forward the guard's desired movement to the mounted horse.
     * Without this, horses just stand still or use their own AI when a guard
     * rides them, because Minecraft only lets Player passengers control horses.
     * <p>
     * How it works:
     * 1. The guard's goals have already run by this point in aiStep().
     * 2. The guard's MoveControl has been updated with the desired position.
     * 3. We read that position and forward it to the horse's navigation.
     * 4. The horse then walks toward where the guard wants to go.
     */
    private void tickMountNavigation() {
        if (!this.isPassenger()) return;
        Entity vehicle = this.getVehicle();
        if (!(vehicle instanceof Mob mount)) return;

        LivingEntity target = this.getTarget();

        if (target != null && target.isAlive()) {
            // Combat: make the horse chase the attack target
            // For ranged guards, maintain attack distance; for melee, get close
            double dist = this.distanceTo(target);
            boolean isRanged = this.getMainHandItem().getItem() instanceof BowItem
                    || this.getMainHandItem().getItem() instanceof CrossbowItem;
            double desiredDist = isRanged ? 10.0D : 2.0D;

            if (dist > desiredDist + 2.0D) {
                mount.getNavigation().moveTo(target, 1.2D);
            } else if (dist < desiredDist - 2.0D && isRanged) {
                // Ranged guard too close — back up via horse
                net.minecraft.world.phys.Vec3 away = net.minecraft.world.entity.ai.util.DefaultRandomPos
                        .getPosAway(this, 8, 4, target.position());
                if (away != null) {
                    mount.getNavigation().moveTo(away.x, away.y, away.z, 1.2D);
                }
            } else {
                mount.getNavigation().stop();
            }
            // Make the horse face the target
            mount.getLookControl().setLookAt(target, 30.0F, 30.0F);
            return;
        }

        // Peacetime: forward the guard's MoveControl target to the horse
        MoveControl mc = this.getMoveControl();
        if (mc.hasWanted()) {
            // Guard wants to move somewhere — forward to horse navigation
            // Throttle to every 10 ticks to avoid recomputing horse paths too often
            if (this.tickCount % 10 == 0 || mount.getNavigation().isDone()) {
                mount.getNavigation().moveTo(mc.getWantedX(), mc.getWantedY(), mc.getWantedZ(),
                        Math.max(mc.getSpeedModifier(), 1.0D));
            }
        } else {
            // Guard doesn't want to move — stop the horse
            if (this.tickCount % 20 == 0) {
                mount.getNavigation().stop();
            }
        }
    }

    // === Feature 6: Wounded Behavior ===

    private void tickWoundedBehavior() {
        if (!GuardConfig.COMMON.woundedBehavior) return;
        // PERFORMANCE: Only check wounded state every 20 ticks (1 second) instead of every tick.
        // Health changes are not instant, so checking every frame is unnecessary.
        // Also, we only need to do work on STATE TRANSITIONS (wounded→not, not→wounded),
        // not on every tick where the state hasn't changed.
        if (this.tickCount % 20 != 0) return;

        float healthRatio = this.getHealth() / this.getMaxHealth();
        boolean isWounded = this.getHealth() > 0 && healthRatio < GuardConfig.COMMON.woundedHealthThreshold;
        boolean hasRecovered = this.wasWounded && healthRatio >= GuardConfig.COMMON.recoveredHealthThreshold;

        if (isWounded && !this.wasWounded) {
            // Just entered wounded state - apply speed penalty
            AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && !speedAttr.hasModifier(WOUNDED_SPEED_ID)) {
                speedAttr.addTransientModifier(new AttributeModifier(WOUNDED_SPEED_ID, -0.3D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
            // Try to eat food immediately if available
            if (isConsumable(this.getOffhandItem()) && !this.isUsingItem()) {
                this.startUsingItem(InteractionHand.OFF_HAND);
            }
            this.wasWounded = true;
        } else if (hasRecovered) {
            AttributeInstance speedAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null) {
                speedAttr.removeModifier(WOUNDED_SPEED_ID);
            }
            this.wasWounded = false;
        }
    }

    public boolean isWounded() {
        return this.wasWounded;
    }

    // === Feature 3 (Enhanced Rank): Captain's Inspiration ===

    private void tickCaptainInspiration() {
        if (!GuardConfig.COMMON.guardLeveling) return;
        // Only check every 100 ticks (5 seconds)
        if (this.tickCount % 100 != 0) return;

        // BUG FIX: Non-captain guards must ALSO run this to clean up any
        // lingering inspiration buff they may have received from a now-dead
        // or out-of-range captain. Previously, if a captain died, the buff
        // was NEVER removed from nearby guards because only captains ran this code.
        double range = GuardConfig.COMMON.captainInspirationRange;

        if (this.getGuardRank() == GuardRank.CAPTAIN) {
            // Captain: apply buff to nearby guards and remove from out-of-range guards
            // PERFORMANCE: Combined into a single entity scan instead of two separate scans.
            List<Guard> nearbyGuards = this.level().getEntitiesOfClass(
                    Guard.class,
                    this.getBoundingBox().inflate(range + 10.0D, 6.0D, range + 10.0D),
                    g -> g != this && g.isAlive()
            );

            for (Guard nearby : nearbyGuards) {
                double dist = nearby.distanceTo(this);
                AttributeInstance dmgAttr = nearby.getAttribute(Attributes.ATTACK_DAMAGE);
                if (dmgAttr == null) continue;

                if (dist <= range) {
                    // In range: apply buff if not already applied
                    if (!dmgAttr.hasModifier(CAPTAIN_INSPIRATION_ID)) {
                        dmgAttr.addTransientModifier(new AttributeModifier(
                                CAPTAIN_INSPIRATION_ID,
                                GuardConfig.COMMON.captainInspirationDamageBonus,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                } else {
                    // Out of range: remove buff
                    if (dmgAttr.hasModifier(CAPTAIN_INSPIRATION_ID)) {
                        dmgAttr.removeModifier(CAPTAIN_INSPIRATION_ID);
                    }
                }
            }
        } else {
            // Non-captain: check if we have a stale inspiration buff from a dead/distant captain.
            // This is the ONLY place that cleans up buffs from dead captains.
            AttributeInstance dmgAttr = this.getAttribute(Attributes.ATTACK_DAMAGE);
            if (dmgAttr != null && dmgAttr.hasModifier(CAPTAIN_INSPIRATION_ID)) {
                // Check if any living captain is in range
                List<Guard> nearbyCaptains = this.level().getEntitiesOfClass(
                        Guard.class,
                        this.getBoundingBox().inflate(range + 5.0D, 6.0D, range + 5.0D),
                        g -> g != this && g.isAlive() && g.getGuardRank() == GuardRank.CAPTAIN
                );
                boolean hasNearbyCaptain = false;
                for (Guard captain : nearbyCaptains) {
                    if (captain.distanceTo(this) <= range) {
                        hasNearbyCaptain = true;
                        break;
                    }
                }
                if (!hasNearbyCaptain) {
                    dmgAttr.removeModifier(CAPTAIN_INSPIRATION_ID);
                }
            }
        }
    }

    // === Feature 1 (Patrol Route): Waypoint accessors ===

    public List<BlockPos> getPatrolWaypoints() {
        return Collections.unmodifiableList(this.patrolWaypoints);
    }

    public void setPatrolWaypoints(List<BlockPos> waypoints) {
        this.patrolWaypoints = new ArrayList<>(waypoints);
        this.currentWaypointIndex = 0;
        this.waypointWaitTicks = 0;
    }

    public int getCurrentWaypointIndex() {
        return this.currentWaypointIndex;
    }

    public void setCurrentWaypointIndex(int index) {
        this.currentWaypointIndex = index;
    }

    public int getWaypointWaitTicks() {
        return this.waypointWaitTicks;
    }

    public void setWaypointWaitTicks(int ticks) {
        this.waypointWaitTicks = ticks;
    }

    public boolean hasPatrolWaypoints() {
        return !this.patrolWaypoints.isEmpty();
    }

    /**
     * Advance to the next patrol waypoint and return it.
     * Loops back to the first waypoint after the last one.
     */
    public BlockPos advanceToNextWaypoint() {
        if (this.patrolWaypoints.isEmpty()) return null;
        this.currentWaypointIndex = (this.currentWaypointIndex + 1) % this.patrolWaypoints.size();
        this.waypointWaitTicks = GuardConfig.COMMON.patrolWaitTimeSeconds * 20; // Wait at this waypoint
        return this.patrolWaypoints.get(this.currentWaypointIndex);
    }

    /**
     * Get the current target waypoint, or null if no waypoints set.
     */
    @Nullable
    public BlockPos getCurrentWaypoint() {
        if (this.patrolWaypoints.isEmpty()) return null;
        if (this.currentWaypointIndex >= this.patrolWaypoints.size()) {
            this.currentWaypointIndex = 0;
        }
        return this.patrolWaypoints.get(this.currentWaypointIndex);
    }

    // === Feature 7 (Squad System): Squad accessors ===

    @Nullable
    private UUID pendingSquadLeaderUUID = null;

    public boolean isSquadMember() {
        return this.squadLeader != null && this.squadLeader.getEntity(this.level(), Guard.class) != null;
    }

    @Nullable
    public Guard getSquadLeader() {
        if (this.squadLeader == null) return null;
        return this.squadLeader.getEntity(this.level(), Guard.class);
    }

    public void setSquadLeader(@Nullable Guard leader) {
        if (leader == null) {
            this.squadLeader = null;
        } else {
            this.squadLeader = EntityReference.of(leader);
        }
    }

    private void resolveSquadLeader() {
        if (this.pendingSquadLeaderUUID != null && this.squadLeader == null) {
            // Try to resolve the squad leader from the saved UUID
            if (this.level() instanceof ServerLevel serverLevel) {
                Entity entity = serverLevel.getEntity(this.pendingSquadLeaderUUID);
                if (entity instanceof Guard guardLeader) {
                    this.squadLeader = EntityReference.of(guardLeader);
                }
            }
            // Clear the pending UUID after a reasonable attempt (either resolved or entity not loaded yet)
            if (this.tickCount > 200) {
                this.pendingSquadLeaderUUID = null;
            }
        }
    }

    // === Feature 8: Night Watch ===

    private void tickNightWatch() {
        if (!GuardConfig.COMMON.nightWatchEnabled) return;
        // PERFORMANCE: Only check day/night every 200 ticks (10 seconds) instead of 100.
        // Day/night transitions happen over ~6000 ticks, so checking every 100 ticks
        // is still 60x more frequent than needed. 200 ticks is more than sufficient
        // for detecting night onset.
        if (this.tickCount % 200 != 0) return;

        boolean isNight = this.level().isDarkOutside();

        if (isNight && !this.wasNight) {
            // Night started - boost follow range
            // PERFORMANCE: Cap the effective night range multiplier to 1.5x max.
            // Higher multipliers create massive AABB queries for target scanning
            // (e.g., 2.0x with default 20 range = 40 block range = 80x80x80 scan area).
            // This is the #1 cause of TPS drops in villages with many guards.
            AttributeInstance rangeAttr = this.getAttribute(Attributes.FOLLOW_RANGE);
            if (rangeAttr != null && !rangeAttr.hasModifier(NIGHT_RANGE_ID)) {
                double cappedMultiplier = Math.min(GuardConfig.COMMON.nightFollowRangeMultiplier, 1.5D);
                double bonus = (cappedMultiplier - 1.0) * GuardConfig.STARTUP.followRangeModifier;
                if (bonus > 0) {
                    rangeAttr.addTransientModifier(new AttributeModifier(NIGHT_RANGE_ID, bonus, AttributeModifier.Operation.ADD_VALUE));
                }
            }
            this.wasNight = true;
        } else if (!isNight && this.wasNight) {
            // Day started - remove follow range boost
            AttributeInstance rangeAttr = this.getAttribute(Attributes.FOLLOW_RANGE);
            if (rangeAttr != null) {
                rangeAttr.removeModifier(NIGHT_RANGE_ID);
            }
            this.wasNight = false;
        }
    }

    public void tryToTeleportToOwner() {
        LivingEntity livingentity = this.getOwner();
        if (livingentity != null) {
            this.teleportToAroundBlockPos(livingentity.blockPosition());
        }
    }

    public boolean shouldTryTeleportToOwner() {
        LivingEntity livingentity = this.getOwner();
        return livingentity != null && this.distanceToSqr(this.getOwner()) >= 144.0 && GuardConfig.COMMON.guardTeleport && this.getTarget() == null;
    }

    private void teleportToAroundBlockPos(BlockPos pos) {
        for (int i = 0; i < 10; i++) {
            int j = this.random.nextIntBetweenInclusive(-4, 4);
            int k = this.random.nextIntBetweenInclusive(-4, 4);
            if (Math.abs(j) >= 3 || Math.abs(k) >= 3) {
                int l = this.random.nextIntBetweenInclusive(-1, 1);
                if (this.maybeTeleportTo(pos.getX() + j, pos.getY() + l, pos.getZ() + k)) {
                    return;
                }
            }
        }
    }

    private boolean maybeTeleportTo(int x, int y, int z) {
        if (!this.canTeleportTo(new BlockPos(x, y, z))) {
            return false;
        } else {
            this.snapTo((double) x + 0.5, y, (double) z + 0.5, this.getYRot(), this.getXRot());
            this.navigation.stop();
            return true;
        }
    }

    private boolean canTeleportTo(BlockPos pos) {
        PathType pathtype = WalkNodeEvaluator.getPathTypeStatic(this, pos);
        if (pathtype != PathType.WALKABLE) {
            return false;
        } else {
            BlockState blockstate = this.level().getBlockState(pos.below());
            if (blockstate.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos blockpos = pos.subtract(this.blockPosition());
                return this.level().noCollision(this, this.getBoundingBox().move(blockpos));
            }
        }
    }

    public static List<ItemStack> getItemsFromLootTable(LivingEntity entity) {
        if (entity.level().getServer() == null) return List.of();
        LootTable loot = entity.level().getServer().reloadableRegistries().getLootTable(getLootTableFromData());
        if (loot == null) return List.of();
        LootParams.Builder lootcontext$builder = (new LootParams.Builder((ServerLevel) entity.level()).withParameter(LootContextParams.THIS_ENTITY, entity));
        return loot.getRandomItems(lootcontext$builder.create(GuardLootTables.SLOT));
    }

    public static ResourceKey<LootTable> getLootTableFromData() {
        Identifier lootTable = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "entities/guard_armor");
        return ResourceKey.create(Registries.LOOT_TABLE, lootTable);
    }

    public void setVariant(String variant) {
        this.entityData.set(GUARD_VARIANT, variant);
    }

    public String getVariant() {
        String variant = this.entityData.get(GUARD_VARIANT);
        return !variant.isEmpty() ? variant : "plains";
    }

    public void damageGuardItem(int damage, EquipmentSlot slotToDamage, ItemStack item) {
        if (this.random.nextFloat() < GuardConfig.COMMON.chanceToBreakEquipment) {
            item.hurtAndBreak(damage, this, slotToDamage);
        }
    }

    // === Inner Goal Classes ===

    public static class DefendVillageGuardGoal extends TargetGoal {
        private final Guard guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardGoal(Guard guardIn) {
            super(guardIn, true, true);
            this.guard = guardIn;
            this.setFlags(EnumSet.of(Flag.TARGET, Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // PERFORMANCE: Only scan every 40 ticks (2 seconds) instead of every tick.
            // Village defense doesn't need instant reaction time.
            if (guard.tickCount % 40 != 0 && this.villageAggressorTarget == null) return false;
            AABB axisalignedbb = this.guard.getBoundingBox().inflate(10.0D, 8.0D, 10.0D);
            List<Villager> list = guard.level().getEntitiesOfClass(Villager.class, axisalignedbb);
            List<Player> list1 = guard.level().getEntitiesOfClass(Player.class, axisalignedbb);
            for (Villager villager : list) {
                for (Player player : list1) {
                    int i = villager.getPlayerReputation(player);
                    if (i <= GuardConfig.COMMON.reputationRequirementToBeAttacked) {
                        this.villageAggressorTarget = player;
                        if (villageAggressorTarget.getTeam() != null && guard.getTeam() != null && guard.getTeam().isAlliedTo(villageAggressorTarget.getTeam()))
                            return false;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((Player) this.villageAggressorTarget).isCreative();
        }

        @Override
        public void start() {
            this.guard.setTarget(this.villageAggressorTarget);
            super.start();
        }
    }

    public static class FollowHeroGoal extends Goal {
        private final Guard guard;
        private LivingEntity owner;
        private final double speedModifier;
        private final PathNavigation navigation;
        private int timeToRecalcPath;
        private final float stopDistance;
        private final float startDistance;
        private float oldWaterCost;

        public FollowHeroGoal(Guard guard, double speedModifier, float startDistance, float stopDistance) {
            this.guard = guard;
            this.speedModifier = speedModifier;
            this.navigation = guard.getNavigation();
            this.startDistance = startDistance;
            this.stopDistance = stopDistance;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = this.guard.getOwner();
            if (livingentity == null) {
                return false;
            } else if (this.guard.distanceToSqr(livingentity) < (double) (this.startDistance * this.startDistance)) {
                return false;
            } else {
                this.owner = livingentity;
                return this.guard.isFollowing();
            }
        }

        @Override
        public boolean canContinueToUse() {
            if (!this.navigation.isDone()) {
                return this.guard.distanceToSqr(this.owner) >= (double) (this.stopDistance * this.stopDistance) && this.guard.isFollowing();
            } else {
                return false;
            }
        }

        @Override
        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.guard.getPathfindingMalus(PathType.WATER);
            this.guard.setPathfindingMalus(PathType.WATER, 0.0F);
        }

        @Override
        public void stop() {
            this.owner = null;
            this.navigation.stop();
            this.guard.setPathfindingMalus(PathType.WATER, this.oldWaterCost);
        }

        @Override
        public void tick() {
            boolean shouldTryTeleportToOwner = this.guard.shouldTryTeleportToOwner();
            if (!shouldTryTeleportToOwner) {
                this.guard.getLookControl().setLookAt(this.owner, 10.0F, (float) this.guard.getMaxHeadXRot());
            }
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = this.adjustedTickDelay(10);
                if (shouldTryTeleportToOwner) {
                    this.guard.tryToTeleportToOwner();
                } else {
                    this.navigation.moveTo(this.owner, this.speedModifier);
                }
            }
        }
    }

    public static class GuardMeleeGoal extends MeleeAttackGoal {
        private static final double DEFAULT_ATTACK_REACH = Math.sqrt(2.04F) - (double) 0.6F;
        public final Guard guard;
        private int shieldRaiseDelay = 0;
        // Integrated flanking state (was separate GuardFlankingGoal)
        private boolean isFlanking = false;
        private Vec3 flankTarget = null;
        private int flankCooldown = 0;

        @Override
        public void start() {
            super.start();
            this.guard.setAggressive(true);
            this.shieldRaiseDelay = 0;
            this.isFlanking = false;
            this.flankTarget = null;
        }

        @Override
        public void stop() {
            super.stop();
            this.guard.setAggressive(false);
            this.shieldRaiseDelay = 0;
            this.isFlanking = false;
            this.flankTarget = null;
        }

        public GuardMeleeGoal(Guard guard, double speedIn, boolean useLongMemory) {
            super(guard, speedIn, useLongMemory);
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            return !(mob.getMainHandItem().getItem() instanceof CrossbowItem) && !(mob.getMainHandItem().getItem() instanceof BowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.guard.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                WeaponBehavior.WeaponType weaponType = WeaponBehavior.getWeaponType(guard);
                double dist = target.distanceTo(guard);

                // === INTEGRATED FLANKING (was GuardFlankingGoal) ===
                // Flanking is integrated here because a separate GuardFlankingGoal
                // at the same priority would always lose to GuardMeleeGoal in
                // GoalSelector (same priority, same flags, registered after).
                if (GuardConfig.COMMON.weaponSpecificBehavior && GuardConfig.COMMON.guardFlanking) {
                    if (this.flankCooldown > 0) {
                        this.flankCooldown--;
                    }

                    if (this.isFlanking && this.flankTarget != null) {
                        // Still moving to flank position
                        double distToFlank = guard.distanceToSqr(this.flankTarget);
                        if (distToFlank < 4.0D) {
                            // Reached flank position — switch to normal combat
                            this.isFlanking = false;
                            this.flankTarget = null;
                        } else {
                            // Continue moving to flank position
                            guard.getNavigation().moveTo(this.flankTarget.x, this.flankTarget.y, this.flankTarget.z, 1.2D);
                            guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
                            return; // Don't do normal combat while flanking
                        }
                    } else if (this.flankCooldown <= 0 && dist > 3.0D && dist <= 10.0D) {
                        // Try to start flanking if we're at medium range
                        if (shouldFlank(weaponType, target)) {
                            this.flankTarget = calculateFlankPosition(target);
                            if (this.flankTarget != null) {
                                this.isFlanking = true;
                                guard.getNavigation().moveTo(this.flankTarget.x, this.flankTarget.y, this.flankTarget.z, 1.2D);
                                guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
                                return;
                            }
                        }
                    }
                }

                // === WEAPON-SPECIFIC COMBAT POSITIONING ===
                if (GuardConfig.COMMON.weaponSpecificBehavior) {
                    double optimalDist = WeaponBehavior.getOptimalCombatDistance(guard);

                    if (weaponType == WeaponBehavior.WeaponType.SPEAR && dist <= optimalDist + 0.5D) {
                        // Spear guards: stab and step back (hit-and-run)
                        if (dist <= 2.5D) {
                            guard.getNavigation().stop();
                            guard.lookAt(target, 30.0F, 30.0F);
                        }
                    } else if (WeaponBehavior.shouldStrafe(guard) && dist <= optimalDist + 1.0D) {
                        // Sword/spear guards: circle strafe around the target
                        float strafeDir = WeaponBehavior.getStrafeDirection(guard);
                        guard.getMoveControl().strafe(0.5F, strafeDir);
                        guard.lookAt(target, 30.0F, 30.0F);
                    } else if (weaponType == WeaponBehavior.WeaponType.AXE && guard.isBerserker()) {
                        // Berserker axes: charge straight in, no retreating
                        if (dist > 2.0D) {
                            guard.getNavigation().moveTo(target, 1.2D);
                        } else {
                            guard.getNavigation().stop();
                            guard.lookAt(target, 30.0F, 30.0F);
                        }
                    } else if (weaponType == WeaponBehavior.WeaponType.MACE) {
                        // Mace guards: aggressive push forward
                        if (dist > 2.0D) {
                            guard.getNavigation().moveTo(target, 1.1D);
                        } else {
                            guard.getNavigation().stop();
                            guard.lookAt(target, 30.0F, 30.0F);
                        }
                    } else {
                        // Default melee behavior
                        if (dist <= optimalDist) {
                            guard.getNavigation().stop();
                            guard.lookAt(target, 30.0F, 30.0F);
                        }
                    }
                } else {
                    // Original behavior (no weapon specialization)
                    if (path != null && dist <= 2.5D) guard.getNavigation().stop();
                }

                // === SHIELD BETWEEN ATTACKS ===
                // Sword+shield and spear+shield guards raise their shield between attacks
                if (WeaponBehavior.shouldShieldBetweenAttacks(guard) && this.ticksUntilNextAttack > 5) {
                    this.shieldRaiseDelay++;
                    if (this.shieldRaiseDelay > 5 && !guard.isBlocking() && guard.shieldCoolDown == 0) {
                        guard.startUsingItem(InteractionHand.OFF_HAND);
                    }
                } else if (guard.isBlocking() && this.ticksUntilNextAttack <= 5) {
                    // Lower shield when about to attack
                    guard.stopUsingItem();
                    this.shieldRaiseDelay = 0;
                }

                super.tick();
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy) {
            if (canPerformAttack(enemy)) {
                // Lower shield before attacking
                if (guard.isBlocking()) {
                    guard.stopUsingItem();
                }

                this.resetAttackCooldown();
                this.guard.stopUsingItem();
                if (guard.shieldCoolDown == 0) this.guard.shieldCoolDown = 8;
                this.guard.swing(InteractionHand.MAIN_HAND);
                if (this.guard.level() instanceof ServerLevel serverLevel) {
                    this.guard.doHurtTarget(serverLevel, enemy);
                }
                this.shieldRaiseDelay = 0; // Reset shield delay after attack

                // Apply weapon-specific attack cooldown for next attack
                if (GuardConfig.COMMON.weaponSpecificBehavior) {
                    int weaponCooldown = WeaponBehavior.getAttackCooldown(WeaponBehavior.getWeaponType(guard));
                    this.ticksUntilNextAttack = weaponCooldown;
                }
            }
        }

        @Override
        protected boolean canPerformAttack(LivingEntity mob) {
            // Weapon-specific reach bonus for spears/tridents
            double reachBonus = WeaponBehavior.getAttackReachBonus(guard);
            double inflateAmount = 0.65D + reachBonus;
            return this.isTimeToAttack() && this.mobHitBox(this.mob).inflate(inflateAmount).intersects(this.mobHitBox(mob)) && this.mob.getSensing().hasLineOfSight(mob);
        }

        protected AABB mobHitBox(LivingEntity mob) {
            Entity entity = mob.getVehicle();
            AABB aabb;
            if (entity != null) {
                AABB aabb1 = entity.getBoundingBox();
                AABB aabb2 = mob.getBoundingBox();
                aabb = new AABB(Math.min(aabb2.minX, aabb1.minX), aabb2.minY, Math.min(aabb2.minZ, aabb1.minZ), Math.max(aabb2.maxX, aabb1.maxX), aabb2.maxY, Math.max(aabb2.maxZ, aabb1.maxZ));
            } else {
                aabb = mob.getBoundingBox();
            }
            return aabb.inflate(DEFAULT_ATTACK_REACH, 0.0D, DEFAULT_ATTACK_REACH);
        }

        // === Integrated Flanking Methods (was GuardFlankingGoal) ===

        /**
         * Determine if this guard should flank based on weapon type and combat situation.
         * - Sword guards: 50% chance to flank (others hold the line)
         * - Berserker axes: Always try to flank for devastating rear attacks
         * - Trident guards: Circle at extended range to find openings
         * Only flanks if there are other guards engaging from the front.
         */
        private boolean shouldFlank(WeaponBehavior.WeaponType weaponType, LivingEntity target) {
            // Check if there are other guards already engaging from the front
            long nearbyFightingGuards = guard.level().getEntitiesOfClass(
                    Guard.class,
                    guard.getBoundingBox().inflate(10.0D, 4.0D, 10.0D),
                    g -> g != this.guard && g.isAlive() && g.getTarget() != null
            ).size();

            if (nearbyFightingGuards < 1) return false; // No one holding the front

            return switch (weaponType) {
                case SWORD -> guard.getRandom().nextFloat() < 0.5F; // 50% chance
                case AXE -> guard.isBerserker(); // Berserkers always flank
                case TRIDENT -> guard.getRandom().nextFloat() < 0.6F; // 60% chance
                default -> false;
            };
        }

        /**
         * Calculate a position to the side or behind the target.
         * Uses the target's facing direction to determine "behind".
         */
        private Vec3 calculateFlankPosition(LivingEntity target) {
            float enemyYaw = target.getYRot() * ((float) Math.PI / 180F);
            boolean goLeft = (guard.getId() % 2 == 0);
            double flankAngle = enemyYaw + (goLeft ? Math.PI * 0.6 : -Math.PI * 0.6);
            double flankDistance = 3.0D;

            double targetX = target.getX() + Math.cos(flankAngle) * flankDistance;
            double targetZ = target.getZ() + Math.sin(flankAngle) * flankDistance;

            Vec3 candidate = new Vec3(targetX, target.getY(), targetZ);
            Vec3 safePos = net.minecraft.world.entity.ai.util.DefaultRandomPos.getPosTowards(guard, 8, 4, candidate, (float) Math.PI / 4F);
            return safePos != null ? safePos : candidate;
        }
    }

    public static class GuardBowAttack extends Goal {
        protected final Guard guard;
        private final double speedModifier;
        private final int attackIntervalMin;
        private final float attackRadiusSqr;
        private int attackInterval;
        private float attackRadius;
        // BUG FIX: Cooldown after stopping bow due to friendly fire.
        // Without this, the guard would stop drawing (friendlyInLineOfSight=true)
        // and immediately re-draw on the next tick (friendlyInLineOfSight=false
        // on non-check ticks), creating an infinite draw-cancel cycle.
        private int friendlyFireCooldown = 0;

        public GuardBowAttack(Guard mob, double speedModifier, int attackIntervalMin, float attackRadius) {
            this.guard = mob;
            this.speedModifier = speedModifier;
            this.attackIntervalMin = attackIntervalMin;
            this.attackRadius = attackRadius;
            this.attackRadiusSqr = attackRadius * attackRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return guard.getTarget() != null && this.isBowInMainhand() && !guard.isEating() && !guard.isBlocking();
        }

        protected boolean isBowInMainhand() {
            return guard.getMainHandItem().getItem() instanceof BowItem;
        }

        @Override
        public boolean canContinueToUse() {
            return (this.canUse()) && this.isBowInMainhand() && guard.getTarget() != null && guard.getTarget().isAlive();
        }

        @Override
        public void start() {
            super.start();
            this.guard.setAggressive(true);
        }

        @Override
        public void stop() {
            super.stop();
            this.guard.stopUsingItem();
            this.guard.setAggressive(false);
            this.friendlyFireCooldown = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                double distanceSq = guard.distanceToSqr(target);
                double distance = Math.sqrt(distanceSq);
                boolean canSee = guard.getSensing().hasLineOfSight(target);

                // Handle friendly fire cooldown: don't draw the bow while waiting
                if (this.friendlyFireCooldown > 0) {
                    this.friendlyFireCooldown--;
                    if (guard.isUsingItem()) {
                        guard.stopUsingItem();
                    }
                    // Still look at target and maintain distance
                    guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
                    guard.lookAt(target, 30.0F, 30.0F);
                    if (distanceSq > (double) this.attackRadiusSqr) {
                        guard.getNavigation().moveTo(target, this.speedModifier);
                    } else {
                        guard.getNavigation().stop();
                    }
                    return;
                }

                // Retreat behavior: if target is too close, move away (kiting)
                // IMPROVED: Use weapon-specific minimum combat distance instead of
                // a single config value. Bow guards maintain 5+ blocks, crossbow 4+.
                double minCombatDist = WeaponBehavior.getMinCombatDistance(guard);
                boolean tooClose = distance < minCombatDist
                        && GuardConfig.COMMON.weaponSpecificBehavior
                        && !guard.isWounded();

                if (tooClose && !guard.isPatrolling()) {
                    Vec3 away = DefaultRandomPos.getPosAway(guard, 8, 4, target.position());
                    if (away != null) {
                        guard.getNavigation().moveTo(away.x, away.y, away.z, 1.0D);
                    }
                } else if (distanceSq > (double) this.attackRadiusSqr) {
                    guard.getNavigation().moveTo(target, this.speedModifier);
                } else {
                    guard.getNavigation().stop();
                }

                guard.getLookControl().setLookAt(target, 30.0F, 30.0F);
                guard.lookAt(target, 30.0F, 30.0F);

                // Bow drawing and shooting logic
                boolean isUsing = guard.isUsingItem();
                if (canSee && isUsing) {
                    if (distanceSq <= (double) this.attackRadiusSqr && this.attackInterval > 0) {
                        --this.attackInterval;
                    }
                } else if (canSee && !isUsing && distanceSq <= (double) this.attackRadiusSqr) {
                    guard.startUsingItem(ProjectileUtil.getWeaponHoldingHand(guard, Items.BOW));
                }

                if (guard.isUsingItem()) {
                    int useTicks = guard.getTicksUsingItem();
                    if (useTicks >= 20) {
                        guard.stopUsingItem();
                        guard.performRangedAttack(target, BowItem.getPowerForTime(useTicks));
                        this.attackInterval = this.attackIntervalMin;
                    }
                } else if (this.attackInterval > 0) {
                    --this.attackInterval;
                }
            }

            if (guard.isPatrolling()) {
                guard.getNavigation().stop();
                guard.getMoveControl().strafe(0.0F, 0.0F);
            }

            // Check for friendly in line of sight (cached, persists across ticks)
            if (this.friendlyFireCooldown <= 0 && RangedCrossbowAttackPassiveGoal.friendlyInLineOfSight(guard)) {
                guard.stopUsingItem();
                this.friendlyFireCooldown = 40; // 2 second cooldown before re-drawing
                this.attackInterval = this.attackIntervalMin; // Reset attack interval
            }
        }
    }

    public static class GuardInteractDoorGoal extends OpenDoorGoal {
        private final Guard guard;

        public GuardInteractDoorGoal(Guard pMob, boolean pCloseDoor) {
            super(pMob, pCloseDoor);
            this.guard = pMob;
        }

        @Override
        public boolean canUse() {
            return super.canUse();
        }

        @Override
        public void start() {
            if (areOtherMobsComingThroughDoor(guard)) {
                super.start();
                guard.swing(InteractionHand.MAIN_HAND);
            }
        }

        private boolean areOtherMobsComingThroughDoor(Guard pEntity) {
            List<? extends PathfinderMob> nearbyEntityList = pEntity.level().getEntitiesOfClass(PathfinderMob.class, pEntity.getBoundingBox().inflate(4.0D));
            if (!nearbyEntityList.isEmpty()) {
                for (PathfinderMob mob : nearbyEntityList) {
                    if (mob.blockPosition().closerToCenterThan(pEntity.position(), 2.0D))
                        return isMobComingThroughDoor(mob);
                }
            }
            return false;
        }

        private boolean isMobComingThroughDoor(PathfinderMob pEntity) {
            if (pEntity.getNavigation() == null) {
                return false;
            } else {
                Path path = pEntity.getNavigation().getPath();
                if (path == null || path.isDone()) {
                    return false;
                } else {
                    Node node = path.getPreviousNode();
                    if (node == null) {
                        return false;
                    } else {
                        Node node1 = path.getNextNode();
                        return pEntity.blockPosition().equals(node.asBlockPos()) || pEntity.blockPosition().equals(node1.asBlockPos());
                    }
                }
            }
        }
    }

    public static class GuardLookAtAndStopMovingWhenBeingTheInteractionTarget extends Goal {
        private final Guard guard;
        private Villager villager;

        public GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(Guard guard) {
            this.guard = guard;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            // PERFORMANCE: Only scan every 20 ticks (1 second) instead of every tick.
            // This goal is low-priority interaction — don't need instant response.
            if (guard.tickCount % 20 != 0 && this.villager == null) return false;
            List<Villager> list = this.guard.level().getEntitiesOfClass(Villager.class, guard.getBoundingBox().inflate(10.0D));
            if (!list.isEmpty()) {
                for (Villager villager : list) {
                    if (villager.getBrain().hasMemoryValue(MemoryModuleType.INTERACTION_TARGET) && villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).get().is(guard)) {
                        this.villager = villager;
                        return true;
                    }
                }
            }
            this.villager = null;
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            super.tick();
            guard.getNavigation().stop();
            guard.lookAt(villager, 30.0F, 30.0F);
            guard.getLookControl().setLookAt(villager);
        }
    }

    public static class GuardEatFoodGoal extends Goal {
        public final Guard guard;

        public GuardEatFoodGoal(Guard guard) {
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            // BUG FIX: Added explicit parentheses for clarity and correctness.
            // Previously the && and || had no grouping, making the intent ambiguous.
            // Intent: Eat if (already eating + needs health + has food) OR (needs health + has food + no target + not aggressive)
            return (guard.getHealth() < guard.getMaxHealth() && isConsumable(guard.getOffhandItem()) && guard.isEating())
                    || (guard.getHealth() < guard.getMaxHealth() && isConsumable(guard.getOffhandItem()) && guard.getTarget() == null && !guard.isAggressive());
        }

        @Override
        public boolean canContinueToUse() {
            // PERFORMANCE: Only scan for threatening mobs every 10 ticks instead of every tick.
            // getEntitiesOfClass is very expensive when called every tick for every guard.
            // Between scans, use the simple condition (no new threat detected).
            if (guard.tickCount % 10 != 0) {
                return (guard.isUsingItem() && guard.getTarget() == null && guard.getHealth() < guard.getMaxHealth())
                        || (guard.getTarget() != null && guard.getHealth() < guard.getMaxHealth() / 2 + 2 && guard.isEating());
            }
            List<LivingEntity> list = this.guard.level().getEntitiesOfClass(LivingEntity.class, this.guard.getBoundingBox().inflate(5.0D, 3.0D, 5.0D));
            if (!list.isEmpty()) {
                for (LivingEntity mob : list) {
                    if (mob != null) {
                        if (mob instanceof Mob && ((Mob) mob).getTarget() instanceof Guard) {
                            return false;
                        }
                    }
                }
            }
            return (guard.isUsingItem() && guard.getTarget() == null && guard.getHealth() < guard.getMaxHealth())
                    || (guard.getTarget() != null && guard.getHealth() < guard.getMaxHealth() / 2 + 2 && guard.isEating());
        }

        @Override
        public void start() {
            guard.startUsingItem(InteractionHand.OFF_HAND);
        }
    }

    public static class GuardRunToEatGoal extends RandomStrollGoal {
        private final Guard guard;
        private int walkTimer;

        public GuardRunToEatGoal(Guard guard) {
            super(guard, 0.8D);
            this.guard = guard;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return guard.getHealth() < (guard.getMaxHealth() / 2) && isConsumable(guard.getOffhandItem()) && !guard.isEating() && guard.getTarget() != null && this.getPosition() != null;
        }

        @Override
        public void start() {
            super.start();
            this.guard.setTarget(null);
            if (this.walkTimer <= 0) {
                this.walkTimer = 20;
            }
        }

        @Override
        public void tick() {
            --walkTimer;
            // PERFORMANCE: Only scan for nearby threats every 10 ticks
            if (guard.tickCount % 10 == 0) {
                List<LivingEntity> list = this.guard.level().getEntitiesOfClass(LivingEntity.class, this.guard.getBoundingBox().inflate(5.0D, 3.0D, 5.0D));
                if (!list.isEmpty()) {
                    for (LivingEntity mob : list) {
                        if (mob != null) {
                            if (mob.getLastHurtMob() instanceof Guard || mob instanceof Mob && ((Mob) mob).getTarget() instanceof Guard) {
                                if (walkTimer < 20) this.walkTimer += 5;
                            }
                        }
                    }
                }
            }
        }

        @Override
        protected Vec3 getPosition() {
            // PERFORMANCE: Only scan when we actually need a position
            // This is called less frequently than tick, but still keep it efficient
            // by limiting the search to a smaller radius
            List<LivingEntity> list = this.guard.level().getEntitiesOfClass(LivingEntity.class, this.guard.getBoundingBox().inflate(4.0D, 2.0D, 4.0D));
            if (!list.isEmpty()) {
                for (LivingEntity mob : list) {
                    if (mob != null) {
                        if (mob.getLastHurtMob() instanceof Guard || mob instanceof Mob && ((Mob) mob).getTarget() instanceof Guard) {
                            return DefaultRandomPos.getPosAway(guard, 16, 7, mob.position());
                        }
                    }
                }
            }
            return super.getPosition();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.walkTimer > 0 && !guard.isEating();
        }

        @Override
        public void stop() {
            super.stop();
            this.guard.startUsingItem(InteractionHand.OFF_HAND);
            this.guard.getNavigation().stop();
        }
    }

    public static class FollowShieldGuards extends Goal {
        private static final TargetingConditions NEARBY_GUARDS = TargetingConditions.forNonCombat().range(8.0D).ignoreLineOfSight();
        private final Guard taskOwner;
        private Guard guardtofollow;
        private double x;
        private double y;
        private double z;

        public FollowShieldGuards(Guard taskOwnerIn) {
            this.taskOwner = taskOwnerIn;
        }

        @Override
        public boolean canUse() {
            // PERFORMANCE: Only scan every 20 ticks instead of every tick.
            // Formation following is not time-critical.
            if (taskOwner.tickCount % 20 != 0 && this.guardtofollow == null) return false;
            List<? extends Guard> list = this.taskOwner.level().getEntitiesOfClass(this.taskOwner.getClass(), this.taskOwner.getBoundingBox().inflate(8.0D, 8.0D, 8.0D));
            if (!list.isEmpty()) {
                for (Guard guard : list) {
                    if (!guard.isInvisible() && isActivelyBlocking(guard) && guard.isBlocking()) {
                        // PERFORMANCE: Removed nested getEntitiesOfClass call inside the loop.
                        // Instead of counting nearby guards every iteration, just check
                        // the shield guard directly.
                        if (!(taskOwner.getMainHandItem().getItem() instanceof ProjectileWeaponItem)) {
                            this.guardtofollow = guard;
                            Vec3 vec3d = this.getPosition();
                            if (vec3d == null) {
                                return false;
                            } else {
                                this.x = vec3d.x;
                                this.y = vec3d.y;
                                this.z = vec3d.z;
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Nullable
        protected Vec3 getPosition() {
            return DefaultRandomPos.getPosTowards(this.taskOwner, 16, 7, this.guardtofollow.position(), (float) Math.PI / 2F);
        }

        @Override
        public boolean canContinueToUse() {
            return !this.taskOwner.getNavigation().isDone() && !this.taskOwner.isVehicle();
        }

        @Override
        public void stop() {
            this.guardtofollow = null;
            this.taskOwner.getNavigation().stop();
            super.stop();
        }

        @Override
        public void start() {
            this.taskOwner.getNavigation().moveTo(x, y, z, 0.5D);
        }
    }

    public static class RangedCrossbowAttackPassiveGoal<T extends Guard> extends Goal {
        public static final UniformInt PATHFINDING_DELAY_RANGE = TimeUtil.rangeOfSeconds(1, 2);
        private final T mob;
        private final double speedModifier;
        private final float attackRadiusSqr;
        protected double wantedX;
        protected double wantedY;
        protected double wantedZ;
        private CrossbowState crossbowState = CrossbowState.UNCHARGED;
        private int seeTime;
        private int attackDelay;
        private int updatePathDelay;

        public RangedCrossbowAttackPassiveGoal(T pMob, double pSpeedModifier, float pAttackRadius) {
            this.mob = pMob;
            this.speedModifier = pSpeedModifier;
            this.attackRadiusSqr = pAttackRadius * pAttackRadius;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.isValidTarget() && this.isHoldingCrossbow() && !this.mob.isEating();
        }

        private boolean isHoldingCrossbow() {
            return this.mob.isHolding(is -> is.getItem() instanceof CrossbowItem);
        }

        @Override
        public boolean canContinueToUse() {
            return this.isValidTarget() && (this.canUse() || !this.mob.getNavigation().isDone()) && this.isHoldingCrossbow();
        }

        private boolean isValidTarget() {
            return this.mob.getTarget() != null && this.mob.getTarget().isAlive();
        }

        @Override
        public void stop() {
            super.stop();
            this.mob.setAggressive(false);
            // BUG FIX: Do NOT clear the target when the crossbow goal stops.
            // Previously this caused crossbow guards to "forget" what they were fighting
            // and stand idle, only re-acquiring targets after a long delay.
            // Let the targetSelector goals handle target clearing instead.
            this.seeTime = 0;
            if (this.mob.isUsingItem()) {
                this.mob.stopUsingItem();
                this.mob.setChargingCrossbow(false);
            }
            this.mob.setPose(Pose.STANDING);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            this.mob.setAggressive(true);
        }

        @Override
        public void tick() {
            this.mob.setAggressive(true);
            LivingEntity livingentity = this.mob.getTarget();
            if (livingentity != null) {
                boolean canSee = this.mob.getSensing().hasLineOfSight(livingentity);
                boolean hasSeenEntityRecently = this.seeTime > 0;
                if (canSee != hasSeenEntityRecently) {
                    this.seeTime = 0;
                }
                if (canSee) {
                    ++this.seeTime;
                } else {
                    --this.seeTime;
                }
                double d0 = this.mob.distanceToSqr(livingentity);
                double d1 = livingentity.distanceTo(this.mob);
                if (d1 <= 4.0D && !this.mob.isPatrolling()) {
                    this.mob.getMoveControl().strafe(this.mob.isUsingItem() ? -0.5F : -3.0F, 0.0F);
                    this.mob.lookAt(livingentity, 30.0F, 30.0F);
                }
                boolean canSee2 = (d0 > (double) this.attackRadiusSqr || this.seeTime < 5) && this.attackDelay == 0;
                if (canSee2) {
                    --this.updatePathDelay;
                    if (this.updatePathDelay <= 0 && !this.mob.isPatrolling()) {
                        this.mob.getNavigation().moveTo(livingentity, this.canRun() ? this.speedModifier : this.speedModifier * 0.5D);
                        this.updatePathDelay = PATHFINDING_DELAY_RANGE.sample(this.mob.getRandom());
                    }
                } else {
                    this.updatePathDelay = 0;
                    this.mob.getNavigation().stop();
                }
                this.mob.lookAt(livingentity, 30.0F, 30.0F);
                this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                if (this.crossbowState == CrossbowState.FIND_NEW_POSITION) {
                    this.mob.stopUsingItem();
                    this.mob.setChargingCrossbow(false);
                    if (this.findPosition())
                        this.mob.getNavigation().moveTo(this.wantedX, this.wantedY, this.wantedZ, this.mob.isCrouching() ? 0.5D : 0.9D);
                    this.crossbowState = CrossbowState.UNCHARGED;
                } else if (this.crossbowState == CrossbowState.UNCHARGED) {
                    if (!canSee2 && !this.mob.isPatrolling() || this.mob.isPatrolling() && canSee && !friendlyInLineOfSight(this.mob)) {
                        this.mob.startUsingItem(ProjectileUtil.getWeaponHoldingHand(this.mob, Items.CROSSBOW));
                        this.crossbowState = CrossbowState.CHARGING;
                        this.mob.setChargingCrossbow(true);
                    }
                } else if (this.crossbowState == CrossbowState.CHARGING) {
                    if (!this.mob.isUsingItem()) {
                        this.crossbowState = CrossbowState.UNCHARGED;
                    }
                    int i = this.mob.getTicksUsingItem();
                    ItemStack itemstack = this.mob.getUseItem();
                    if (i >= CrossbowItem.getChargeDuration(itemstack, this.mob) || CrossbowItem.isCharged(itemstack)) {
                        this.mob.releaseUsingItem();
                        this.crossbowState = CrossbowState.CHARGED;
                        this.attackDelay = 10;
                        this.mob.setChargingCrossbow(false);
                    }
                } else if (this.crossbowState == CrossbowState.CHARGED) {
                    --this.attackDelay;
                    if (this.attackDelay == 0) {
                        this.crossbowState = CrossbowState.READY_TO_ATTACK;
                    }
                } else if (this.crossbowState == CrossbowState.READY_TO_ATTACK && canSee) {
                    if (friendlyInLineOfSight(this.mob) && !this.mob.isPatrolling())
                        this.crossbowState = CrossbowState.FIND_NEW_POSITION;
                    else {
                        this.mob.performRangedAttack(livingentity, 1.0F);
                        this.crossbowState = CrossbowState.UNCHARGED;
                    }
                }
            }
        }

        public static boolean friendlyInLineOfSight(Mob mob) {
            // PERFORMANCE: Only perform the expensive entity scan every 5 ticks.
            // BUG FIX: On non-check ticks, return the CACHED result instead of false.
            // Previously, returning false on non-check ticks caused archers to
            // repeatedly draw and cancel their bows — the bow goal saw "no friendly"
            // on 4/5 ticks and started drawing, only to be cancelled again on check ticks.
            if (mob instanceof Guard guardMob) {
                // Use per-guard cache to persist results across ticks
                if (mob.tickCount % 5 != 0) {
                    return guardMob.cachedFriendlyInSight;
                }
            } else {
                // Fallback for non-Guard mobs (shouldn't happen, but safe)
                if (mob.tickCount % 5 != 0) return false;
            }
            // Actual check
            boolean result = checkFriendlyInLineOfSight(mob);
            if (mob instanceof Guard guardMob) {
                guardMob.cachedFriendlyInSight = result;
                guardMob.cachedFriendlyCheckTick = mob.tickCount;
            }
            return result;
        }

        private static boolean checkFriendlyInLineOfSight(Mob mob) {
            Vec3 lookAngle = mob.getViewVector(1.0F);
            AABB aabb = mob.getBoundingBox().expandTowards(lookAngle.scale(6.0D)).inflate(1.0, 1.0, 1.0);
            List<Entity> list = mob.level().getEntities(mob, aabb);
            for (Entity entity : list) {
                if (entity != mob.getTarget()) {
                    boolean isOwner = mob instanceof Guard guardMob && guardMob.getOwner() == entity;
                    boolean isVillager = isOwner || entity.getType() == EntityType.VILLAGER || entity.getType() == GuardEntityType.GUARD || entity.getType() == EntityType.IRON_GOLEM;
                    if (isVillager) {
                        Vec3 vector3d = mob.getLookAngle();
                        Vec3 vector3d1 = entity.position().vectorTo(mob.position()).normalize();
                        vector3d1 = new Vec3(vector3d1.x, vector3d1.y, vector3d1.z);
                        if (vector3d1.dot(vector3d) < GuardConfig.COMMON.friendlyFireCheckValue && mob.hasLineOfSight(entity))
                            return GuardConfig.COMMON.FriendlyFire;
                    }
                }
            }
            return false;
        }

        public boolean findPosition() {
            Vec3 vector3d = this.getPosition();
            if (vector3d == null) {
                return false;
            } else {
                this.wantedX = vector3d.x;
                this.wantedY = vector3d.y;
                this.wantedZ = vector3d.z;
                return true;
            }
        }

        @Nullable
        protected Vec3 getPosition() {
            return DefaultRandomPos.getPos(this.mob, 16, 7);
        }

        private boolean canRun() {
            return this.crossbowState == CrossbowState.UNCHARGED;
        }

        public enum CrossbowState {
            UNCHARGED, CHARGING, CHARGED, READY_TO_ATTACK, FIND_NEW_POSITION
        }
    }

    public static class KickGoal extends Goal {
        public final Guard guard;

        public KickGoal(Guard guard) {
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            if (guard.getTarget() == null || guard.getTarget().distanceTo(guard) > 2.5D) return false;
            if (guard.isBlocking()) return false;
            if (guard.kickCoolDown != 0) return false;
            // Only kick when holding a ranged weapon (bow/crossbow) — these guards
            // can't melee effectively, so kicking is their close-range defense.
            // Melee guards (sword/axe/mace) should use their weapons instead.
            ItemStack mainHand = guard.getMainHandItem();
            boolean isRanged = mainHand.getItem() instanceof BowItem || mainHand.getItem() instanceof CrossbowItem;
            // Also allow kicking if the item has useOnRelease (like trident)
            boolean isUseOnRelease = mainHand.getItem().useOnRelease(mainHand);
            return isRanged || isUseOnRelease;
        }

        @Override
        public void start() {
            guard.setKicking(true);
            if (guard.kickTicks <= 0) {
                guard.kickTicks = 10;
            }
            if (guard.level() instanceof ServerLevel serverLevel && guard.getTarget() != null) {
                guard.doHurtTarget(serverLevel, guard.getTarget());
            }
        }

        @Override
        public void stop() {
            guard.setKicking(false);
            guard.kickCoolDown = 50;
        }
    }

    public static class RaiseShieldGoal extends Goal {
        public final Guard guard;

        public RaiseShieldGoal(Guard guard) {
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            ItemStack off = guard.getOffhandItem();
            boolean hasBlockItem = !off.isEmpty() && off.has(DataComponents.BLOCKS_ATTACKS);
            return !CrossbowItem.isCharged(guard.getMainHandItem()) && (hasBlockItem && raiseShield() && guard.shieldCoolDown == 0);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public void start() {
            ItemStack off = guard.getOffhandItem();
            if (!off.isEmpty() && off.has(DataComponents.BLOCKS_ATTACKS))
                guard.startUsingItem(InteractionHand.OFF_HAND);
        }

        @Override
        public void stop() {
            // BUG FIX: ALWAYS lower the shield when the goal stops.
            // The old code only lowered the shield when GuardRaiseShield was FALSE,
            // meaning when the config was enabled (normal case), the shield stayed
            // raised forever after this goal stopped. This caused bow guards to
            // permanently freeze — GuardBowAttack.canUse() checks !isBlocking(),
            // so a permanently raised shield blocks the bow goal from ever activating.
            // Now the shield is always lowered when the goal stops, regardless of config.
            guard.stopUsingItem();
        }

        protected boolean raiseShield() {
            LivingEntity target = guard.getTarget();
            if (target != null && guard.shieldCoolDown == 0) {
                boolean ranged = guard.getMainHandItem().getItem() instanceof CrossbowItem || guard.getMainHandItem().getItem() instanceof BowItem;
                double dist = guard.distanceTo(target);

                // Bow guards: only raise shield when enemies are very close (melee range).
                // At range, the bow guard should be shooting, not blocking.
                // Without this check, bow guards would raise shield at any distance
                // when GuardRaiseShield config is enabled, making them unable to shoot.
                if (ranged && dist > 5.0D) {
                    return false; // Too far for shield — use ranged weapon instead
                }

                return dist <= 4.0D || target instanceof Creeper || target instanceof RangedAttackMob && dist >= 5.0D && !ranged || target instanceof Ravager || GuardConfig.COMMON.GuardRaiseShield;
            }
            return false;
        }
    }

    public static class HeroHurtByTargetGoal extends TargetGoal {
        private final Guard guard;
        private LivingEntity attacker;
        private int timestamp;

        public HeroHurtByTargetGoal(Guard guard) {
            super(guard, false);
            this.guard = guard;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            LivingEntity livingentity = this.guard.getOwner();
            if (livingentity == null) {
                return false;
            } else {
                this.attacker = livingentity.getLastHurtByMob();
                int i = livingentity.getLastHurtByMobTimestamp();
                return i != this.timestamp && this.canAttack(this.attacker, TargetingConditions.DEFAULT);
            }
        }

        @Override
        protected boolean canAttack(@Nullable LivingEntity potentialTarget, TargetingConditions targetPredicate) {
            return super.canAttack(potentialTarget, targetPredicate) && !(potentialTarget instanceof IronGolem) && !(potentialTarget instanceof Guard);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.attacker);
            LivingEntity livingentity = this.guard.getOwner();
            if (livingentity != null) {
                this.timestamp = livingentity.getLastHurtByMobTimestamp();
            }
            super.start();
        }
    }

    public static class HeroHurtTargetGoal extends TargetGoal {
        private final Guard guard;
        private LivingEntity attacker;
        private int timestamp;

        public HeroHurtTargetGoal(Guard theEntityTameableIn) {
            super(theEntityTameableIn, false);
            this.guard = theEntityTameableIn;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        public boolean canUse() {
            LivingEntity livingentity = this.guard.getOwner();
            if (livingentity == null) {
                return false;
            } else {
                this.attacker = livingentity.getLastHurtMob();
                int i = livingentity.getLastHurtMobTimestamp();
                return i != this.timestamp && this.canAttack(this.attacker, TargetingConditions.DEFAULT);
            }
        }

        @Override
        protected boolean canAttack(@Nullable LivingEntity potentialTarget, TargetingConditions targetPredicate) {
            return super.canAttack(potentialTarget, targetPredicate) && !(potentialTarget instanceof AbstractVillager) && !(potentialTarget instanceof Guard);
        }

        @Override
        public void start() {
            this.mob.setTarget(this.attacker);
            LivingEntity livingentity = this.guard.getOwner();
            if (livingentity != null) {
                this.timestamp = livingentity.getLastHurtMobTimestamp();
            }
            super.start();
        }
    }

    public static class WalkBackToCheckPointGoal extends Goal {
        private final Guard guard;
        private final double speed;
        private long delayTime = 0L;
        private int ticksRan = 0;
        private boolean shouldStop = false;

        public WalkBackToCheckPointGoal(Guard guard, double speedIn) {
            this.guard = guard;
            this.speed = speedIn;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // If guard has patrol waypoints, use the waypoint system
            if (guard.hasPatrolWaypoints()) {
                // BUG FIX: Check waypointWaitTicks > 0 means we are WAITING at a waypoint.
                // The wait timer is decremented in aiStep() even when this goal is not running,
                // so the guard will properly wait before moving to the next waypoint.
                return guard.getTarget() == null && !guard.isFollowing() && guard.isPatrolling()
                        && guard.getWaypointWaitTicks() <= 0
                        && (guard.level().getGameTime() - delayTime) > 60L;
            }
            // Original behavior: single patrol position
            return guard.getTarget() == null && this.guard.getPatrolPos() != null && !this.guard.blockPosition().equals(this.guard.getPatrolPos()) && !guard.isFollowing() && guard.isPatrolling() && (guard.level().getGameTime() - delayTime) > 200L;
        }

        @Override
        public boolean canContinueToUse() {
            // Continue as long as we haven't been told to stop and still have a path
            return !this.shouldStop && !this.guard.getNavigation().isDone();
        }

        @Override
        public void start() {
            this.shouldStop = false;
            if (ticksRan > 200) this.ticksRan = 0;

            if (guard.hasPatrolWaypoints()) {
                // Waypoint system: navigate to current waypoint
                BlockPos target = guard.getCurrentWaypoint();
                if (target != null) {
                    Path path = this.guard.getNavigation().createPath(target, 0);
                    this.guard.getNavigation().moveTo(path, this.speed);
                }
            } else {
                // Original behavior: single patrol position
                BlockPos blockpos = this.guard.getPatrolPos();
                if (blockpos != null && !this.guard.blockPosition().equals(this.guard.getPatrolPos())) {
                    Path path = this.guard.getNavigation().createPath(blockpos, 0);
                    this.guard.getNavigation().moveTo(path, this.speed);
                }
            }
        }

        @Override
        public void tick() {
            if (this.guard.getNavigation().getPath() != null && !this.guard.getNavigation().getPath().canReach())
                this.ticksRan++;

            if (guard.hasPatrolWaypoints()) {
                // Check if guard has reached the current waypoint
                BlockPos currentWaypoint = guard.getCurrentWaypoint();
                if (currentWaypoint != null && guard.blockPosition().closerThan(currentWaypoint, 3.0D)) {
                    // Reached waypoint — set wait timer, then advance to next.
                    // BUG FIX: advanceToNextWaypoint() already sets waypointWaitTicks,
                    // so the guard will WAIT at this waypoint before the goal can re-activate.
                    // The wait timer is decremented in aiStep() every tick, so it works
                    // even when this goal is not running.
                    BlockPos nextWaypoint = guard.advanceToNextWaypoint();
                    if (nextWaypoint != null) {
                        guard.setPatrolPos(nextWaypoint);
                    }
                    this.shouldStop = true;
                }
            } else {
                // Original behavior
                if (this.guard.getNavigation().getPath() != null && !this.guard.getNavigation().getPath().canReach() && !this.guard.blockPosition().equals(this.guard.getPatrolPos()) && ticksRan > 200)
                    this.shouldStop = true;
            }
        }

        @Override
        public void stop() {
            if (shouldStop) this.delayTime = this.guard.level().getGameTime();
            this.guard.getNavigation().stop();
            this.shouldStop = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }

    public static class PassiveMobSpearUseGoal<T extends Guard> extends Goal {
        static final double MAX_FLEEING_TIME = reducedTickDelay(100);
        private final T mob;
        private SpearUseState state;
        double speedModifierWhenCharging;
        double speedModifierWhenRepositioning;
        float approachDistanceSq;
        float targetInRangeRadiusSq;

        public PassiveMobSpearUseGoal(T mob, double speedModifierWhenCharging, double speedModifierWhenRepositioning, float attackRadius, float targetInRange) {
            this.mob = mob;
            this.speedModifierWhenCharging = speedModifierWhenCharging;
            this.speedModifierWhenRepositioning = speedModifierWhenRepositioning;
            this.approachDistanceSq = attackRadius * attackRadius;
            this.targetInRangeRadiusSq = targetInRange * targetInRange;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.ableToAttack() && !this.mob.isUsingItem() && !RangedCrossbowAttackPassiveGoal.friendlyInLineOfSight(this.mob);
        }

        private boolean ableToAttack() {
            return this.mob.getTarget() != null && this.mob.getMainHandItem().has(DataComponents.KINETIC_WEAPON);
        }

        private int getKineticWeaponUseDuration() {
            int i = Optional.ofNullable(this.mob.getMainHandItem().get(DataComponents.KINETIC_WEAPON)).map(KineticWeapon::computeDamageUseDuration).orElse(0);
            return reducedTickDelay(i);
        }

        @Override
        public boolean canContinueToUse() {
            return this.state != null && !this.state.done && this.ableToAttack() && !RangedCrossbowAttackPassiveGoal.friendlyInLineOfSight(this.mob);
        }

        @Override
        public void start() {
            super.start();
            this.mob.setAggressive(true);
            this.state = new SpearUseState();
        }

        @Override
        public void stop() {
            super.stop();
            this.mob.getNavigation().stop();
            this.mob.setAggressive(false);
            this.state = null;
            this.mob.stopUsingItem();
        }

        @Override
        public void tick() {
            if (this.state != null) {
                LivingEntity livingentity = this.mob.getTarget();
                double d0 = this.mob.distanceToSqr(livingentity.getX(), livingentity.getY(), livingentity.getZ());
                Entity entity = this.mob.getRootVehicle();
                float f = 1.0F;
                if (entity instanceof Mob mob) {
                    f = 1.4F;
                }

                int i = this.mob.isPassenger() ? 2 : 0;
                this.mob.lookAt(livingentity, 30.0F, 30.0F);
                this.mob.getLookControl().setLookAt(livingentity, 30.0F, 30.0F);
                if (this.state.notEngagedYet()) {
                    if (d0 > this.approachDistanceSq && !RangedCrossbowAttackPassiveGoal.friendlyInLineOfSight(this.mob)) {
                        this.mob.getNavigation().moveTo(livingentity, f * this.speedModifierWhenRepositioning);
                        return;
                    }

                    this.state.startEngagement(this.getKineticWeaponUseDuration());
                    this.mob.startUsingItem(InteractionHand.MAIN_HAND);
                }

                if (this.state.tickAndCheckEngagement()) {
                    this.mob.stopUsingItem();
                    double d1 = Math.sqrt(d0);
                    this.state.awayPos = LandRandomPos.getPosAway(this.mob, Math.max(0.0, 9 + i - d1), Math.max(1.0, 11 + i - d1), 7, livingentity.position());
                    this.state.fleeingTime = 1;
                }

                if (!this.state.tickAndCheckFleeing()) {
                    if (this.state.awayPos != null) {
                        this.mob.getNavigation().moveTo(this.state.awayPos.x, this.state.awayPos.y, this.state.awayPos.z, f * this.speedModifierWhenRepositioning);
                        if (this.mob.getNavigation().isDone()) {
                            if (this.state.fleeingTime > 0) {
                                this.state.done = true;
                                return;
                            }

                            this.state.awayPos = null;
                        }
                    } else {
                        this.mob.getNavigation().moveTo(livingentity, f * this.speedModifierWhenCharging);
                        if (d0 < this.targetInRangeRadiusSq || this.mob.getNavigation().isDone()) {
                            double d2 = Math.sqrt(d0);
                            this.state.awayPos = LandRandomPos.getPosAway(this.mob, 6 + i - d2, 7 + i - d2, 7, livingentity.position());
                        }
                    }
                }
            }
        }

        public static class SpearUseState {
            private int engageTime = -1;
            int fleeingTime = -1;
            @org.jspecify.annotations.Nullable
            Vec3 awayPos;
            boolean done = false;

            public boolean notEngagedYet() {
                return this.engageTime < 0;
            }

            public void startEngagement(int engageTime) {
                this.engageTime = engageTime;
            }

            public boolean tickAndCheckEngagement() {
                if (this.engageTime > 0) {
                    this.engageTime--;
                    if (this.engageTime == 0) {
                        return true;
                    }
                }
                return false;
            }

            public boolean tickAndCheckFleeing() {
                if (this.fleeingTime > 0) {
                    this.fleeingTime++;
                    if (this.fleeingTime > MAX_FLEEING_TIME) {
                        this.done = true;
                        return true;
                    }
                }
                return false;
            }
        }
    }

    public static class GuardGroundPathNavigation extends GroundPathNavigation {
        private final Guard guard;

        public GuardGroundPathNavigation(Guard guard, Level level) {
            super(guard, level);
            this.guard = guard;
        }

        @Override
        public boolean isDone() {
            return (guard.isPatrolling() && guard.getTarget() == null && guard.blockPosition().equals(guard.getPatrolPos())) || super.isDone();
        }
    }
}
