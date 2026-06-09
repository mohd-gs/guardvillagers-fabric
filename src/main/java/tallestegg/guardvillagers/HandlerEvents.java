package tallestegg.guardvillagers;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableWitchTargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.feline.Cat;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombifiedPiglin;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import tallestegg.guardvillagers.GuardSounds;
import tallestegg.guardvillagers.common.entities.Guard;
import tallestegg.guardvillagers.common.entities.ai.goals.AttackEntityDaytimeGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GetOutOfWaterGoal;
import tallestegg.guardvillagers.common.entities.ai.goals.GolemFloatWaterGoal;
import tallestegg.guardvillagers.configuration.GuardConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class HandlerEvents {
    private static final Predicate<LivingEntity> ISNT_BABY = mob -> !mob.isBaby();

    public static void register() {
        // Events are registered via callbacks in GuardVillagers.onInitialize()
    }

    // Replaces NeoForge @SubscribeEvent LivingChangeTargetEvent
    // Fabric has no direct equivalent, so we use a Mixin on Mob.setTarget() instead.
    // The logic is handled in the Guard entity's setTarget override and via
    // a mixin that calls GuardVillagers.onMobSetTarget()

    public static void onMobSetTarget(Mob mob, @Nullable LivingEntity newTarget) {
        if (mob instanceof Raider raider && raider.hasActiveRaid()) {
            return;
        }
        if (newTarget == null || mob.getType() == GuardEntityType.GUARD || mob instanceof IronGolem) return;

        boolean isVillager = GuardConfig.COMMON.mobsGuardsProtectTargeted.contains(newTarget.getEncodeId());
        if (isVillager) {
            List<Mob> list = mob.level().getEntitiesOfClass(
                    Mob.class,
                    mob.getBoundingBox().inflate(
                            GuardConfig.COMMON.GuardVillagerHelpRange, 5.0D, GuardConfig.COMMON.GuardVillagerHelpRange
                    )
            );
            for (Mob nearbyMob : list) {
                if ((nearbyMob.getTarget() == null) && (nearbyMob.getType() == GuardEntityType.GUARD || nearbyMob.getType() == EntityType.IRON_GOLEM)) {
                    if (nearbyMob.getTeam() != null && mob.getTeam() != null && mob.getTeam().isAlliedTo(nearbyMob.getTeam()))
                        return;
                    else
                        nearbyMob.setTarget(mob);
                }
            }
        }

        if (mob instanceof IronGolem golem && newTarget instanceof Guard) {
            // Iron golems should not target guards
        }
    }

    // ServerEntityEvents.ENTITY_LOAD callback
    public static void onEntityLoad(Entity entity, ServerLevel level) {
        if (entity instanceof Mob mob) {
            if (mob instanceof Raider raider && raider.hasActiveRaid()) return;

            if (mob instanceof Raider) {
                if (GuardConfig.COMMON.RaidAnimals) {
                    mob.targetSelector.addGoal(5, new NearestAttackableTargetGoal<>(((Raider) mob), Animal.class, false));
                }
            }

            if (GuardConfig.COMMON.MobsAttackGuards) {
                if (mob instanceof Enemy && !GuardConfig.COMMON.MobBlackList.contains(mob.getEncodeId())) {
                    if (!(mob instanceof Spider))
                        mob.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(mob, Guard.class, false));
                    else
                        mob.targetSelector.addGoal(3, new AttackEntityDaytimeGoal<>((Spider) mob, Guard.class));
                }
            }

            if (mob instanceof AbstractIllager illager) {
                if (GuardConfig.COMMON.IllagersRunFromPolarBears)
                    illager.goalSelector.addGoal(2, new AvoidEntityGoal<>(illager, PolarBear.class, 6.0F, 1.0D, 1.2D));
                illager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(illager, Guard.class, false));
            }

            if (mob instanceof AbstractVillager abstractvillager) {
                if (GuardConfig.COMMON.VillagersRunFromPolarBears)
                    abstractvillager.goalSelector.addGoal(2, new AvoidEntityGoal<>(abstractvillager, PolarBear.class, 6.0F, 1.0D, 1.2D));
                if (GuardConfig.COMMON.WitchesVillager)
                    abstractvillager.goalSelector.addGoal(2, new AvoidEntityGoal<>(abstractvillager, Witch.class, 6.0F, 1.0D, 1.2D));
            }

            if (mob instanceof IronGolem golem) {
                HurtByTargetGoal tolerateFriendlyFire = new HurtByTargetGoal(golem, Guard.class).setAlertOthers();
                golem.targetSelector.getAvailableGoals().stream().map(it -> it.getGoal()).filter(it -> it instanceof HurtByTargetGoal).findFirst().ifPresent(angerGoal -> {
                    golem.targetSelector.removeGoal(angerGoal);
                    golem.targetSelector.addGoal(2, tolerateFriendlyFire);
                });
                if (GuardConfig.COMMON.golemFloat) {
                    golem.goalSelector.addGoal(0, new GetOutOfWaterGoal(golem, 1.0D));
                    golem.goalSelector.addGoal(1, new GolemFloatWaterGoal(golem));
                }
            }

            if (mob instanceof Zombie zombie && !(zombie instanceof ZombifiedPiglin)) {
                zombie.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(zombie, Guard.class, false));
            }

            if (mob instanceof Ravager ravager) {
                ravager.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(ravager, Guard.class, false));
            }

            if (mob instanceof Witch witch) {
                if (GuardConfig.COMMON.WitchesVillager) {
                    witch.targetSelector.addGoal(3, new NearestAttackableWitchTargetGoal<>(witch, AbstractVillager.class, 10, true, false, (serverLevel, target) -> ISNT_BABY.test(serverLevel)));
                    witch.targetSelector.addGoal(3, new NearestAttackableWitchTargetGoal<>(witch, IronGolem.class, 10, true, false, null));
                    witch.targetSelector.addGoal(2, new NearestAttackableWitchTargetGoal<>(witch, Guard.class, 10, true, false, null));
                }
            }

            if (mob instanceof Cat cat) {
                cat.goalSelector.addGoal(1, new AvoidEntityGoal<>(cat, AbstractIllager.class, 12.0F, 1.0D, 1.2D));
            }
        }
    }

    // UseEntityCallback
    public static InteractionResult onEntityInteract(Player player, Level level, InteractionHand hand, Entity target, @Nullable EntityHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack itemstack = player.getMainHandItem();
        if (itemstack.is(GuardVillagerTags.GUARD_CONVERT) && player.isCrouching()) {
            if (target instanceof Villager villager) {
                if (!villager.isBaby()) {
                    if (GuardConfig.COMMON.convertibleProfessions.contains(professionId(villager))) {
                        if (!GuardConfig.COMMON.ConvertVillagerIfHaveHOTV || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardConfig.COMMON.ConvertVillagerIfHaveHOTV) {
                            convertVillager(villager, player);
                            if (!player.getAbilities().instabuild)
                                itemstack.shrink(1);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    // UseBlockCallback
    public static InteractionResult onBlockInteract(Player player, Level level, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        BlockPos pos = hitResult.getBlockPos();
        BlockState originalBlock = level.getBlockState(pos);
        if (GuardConfig.COMMON.multiFollow) {
            if (originalBlock.getBlock() instanceof BellBlock && level.getBlockEntity(pos) instanceof BellBlockEntity bellBlockEntity) {
                if (!bellBlockEntity.shaking) {
                    List<Guard> list = level.getEntitiesOfClass(Guard.class, player.getBoundingBox().inflate(32.0D, 32.0D, 32.0D));
                    for (Guard guard : list) {
                        if (GuardVillagers.canFollow(player)) {
                            guard.setFollowing(!guard.isFollowing());
                            guard.playSound(GuardSounds.GUARD_YES, 1.0F, 1.0F);
                            if (guard.isFollowing()) {
                                guard.setOwnerId(player.getUUID());
                                guard.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 1));
                                level.playSound(null, pos, SoundEvents.BELL_RESONATE, SoundSource.BLOCKS, 1.0F, 1.0F);
                            } else {
                                guard.removeEffect(MobEffects.GLOWING);
                            }
                        }
                    }
                    return InteractionResult.SUCCESS_SERVER;
                }
            }
        }
        return InteractionResult.PASS;
    }

    private static void convertVillager(LivingEntity entity, Player player) {
        Level level = entity.level();
        if (level.isClientSide()) return;
        player.swing(InteractionHand.MAIN_HAND, true);
        ItemStack itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        Guard guard = GuardEntityType.GUARD.create(entity.level(), EntitySpawnReason.EVENT);
        Villager villager = (Villager) entity;
        if (guard == null) return;
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    villager.getX(), villager.getY() + 0.5D, villager.getZ(),
                    10,
                    villager.getBbWidth(), villager.getBbHeight() * 0.5D, villager.getBbWidth(),
                    0.02D
            );
        }
        guard.copyPosition(villager);
        guard.playSound(GuardSounds.GUARD_YES, 1.0F, 1.0F);
        guard.setItemSlot(EquipmentSlot.MAINHAND, itemstack.copy());
        guard.setVariant(Guard.getVariantFromBiome(villager.level(), villager.blockPosition()));
        guard.setPersistenceRequired();
        guard.setCustomName(villager.getCustomName());
        guard.setCustomNameVisible(villager.isCustomNameVisible());
        guard.setDropChance(EquipmentSlot.HEAD, 1.0F);
        guard.setDropChance(EquipmentSlot.CHEST, 1.0F);
        guard.setDropChance(EquipmentSlot.FEET, 1.0F);
        guard.setDropChance(EquipmentSlot.LEGS, 1.0F);
        guard.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        guard.setDropChance(EquipmentSlot.OFFHAND, 1.0F);
        guard.getGossips().add(player.getUUID(), GossipType.MINOR_POSITIVE, GuardConfig.COMMON.reputationRequirement);
        villager.level().addFreshEntity(guard);
        villager.discard();
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.SUMMONED_ENTITY.trigger(serverPlayer, guard);
            player.awardStat(GuardStats.GUARDS_MADE);
        }
    }

    private static String professionId(Villager villager) {
        return holderPath(villager.getVillagerData().profession());
    }

    private static String typeId(Villager villager) {
        return holderPath(villager.getVillagerData().type());
    }

    private static <T> String holderPath(Holder<T> holder) {
        String fullId = holder.unwrapKey()
                .map(ResourceKey::identifier)
                .map(Object::toString)
                .orElseGet(holder::getRegisteredName);
        int idx = fullId.indexOf(':');
        return (idx >= 0) ? fullId.substring(idx + 1) : fullId;
    }
}
