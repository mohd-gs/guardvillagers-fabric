package tallestegg.guardvillagers.common.entities.ai.tasks;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import tallestegg.guardvillagers.GuardDataAttachments;
import tallestegg.guardvillagers.configuration.GuardConfig;

import java.util.List;

public class RepairGolem extends VillagerHelp {
    private LivingEntity golem;

    public RepairGolem() {
        super(ImmutableMap.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.VALUE_PRESENT), GuardConfig.COMMON.professionsThatRepairGolems);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, Villager owner) {
        if (owner.getBrain().hasMemoryValue(MemoryModuleType.NEAREST_LIVING_ENTITIES)) {
            List<LivingEntity> list = owner.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).get();
            if (!list.isEmpty()) {
                for (LivingEntity golem : list) {
                    if (!golem.isInvisible() && golem.isAlive() && golem.getType() == EntityType.IRON_GOLEM) {
                        if (golem.getHealth() <= (golem.getMaxHealth() * 0.75F)) {
                            this.golem = golem;
                            return super.checkExtraStartConditions(worldIn, owner);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected long timeToCheck(LivingEntity owner) {
        return GuardDataAttachments.getLastRepairedGolem(owner.getUUID());
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager entity, long gameTime) {
        return GuardDataAttachments.getTimesHealedGolem(entity.getUUID()) < GuardConfig.COMMON.maxGolemRepair && this.golem.getHealth() <= this.golem.getMaxHealth();
    }

    @Override
    protected void stop(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        if (GuardDataAttachments.getTimesHealedGolem(entityIn.getUUID()) >= GuardConfig.COMMON.maxGolemRepair) {
            entityIn.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            GuardDataAttachments.setLastRepairedGolem(entityIn.getUUID(), worldIn.getOverworldClockTime());
            entityIn.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            entityIn.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            GuardDataAttachments.setTimesHealedGolem(entityIn.getUUID(), 0);
        }
    }

    @Override
    protected void start(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        if (golem == null) return;
    }

    @Override
    protected void tick(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        this.healGolem(entityIn);
    }

    public void healGolem(Villager healer) {
        BehaviorUtils.setWalkAndLookTargetMemories(healer, golem, 0.5F, 0);
        if (healer.distanceTo(golem) <= 2.0D) {
            GuardDataAttachments.incrementTimesHealedGolem(healer.getUUID());
            healer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
            healer.swing(InteractionHand.MAIN_HAND);
            golem.heal(15.0F);
            float pitch = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
            golem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
        }
    }
}
