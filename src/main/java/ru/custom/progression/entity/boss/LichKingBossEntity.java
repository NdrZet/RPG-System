package ru.custom.progression.entity.boss;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.HostileEntity;
import net.minecraft.world.level.Level;
import ru.custom.progression.api.Faction;

public class LichKingBossEntity extends SpaTwoPhaseBoss {

    public LichKingBossEntity(EntityType<? extends HostileEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected Faction getFaction() {
        return Faction.UNDEAD;
    }

    @Override
    protected void onPhaseTransitionStart() {

    }

    @Override
    protected void onPhaseTransitionTick(int tick) {

    }

    @Override
    protected void onPhaseTwoStart() {

    }

    @Override
    protected void registerPhaseTwoGoals() {

    }

    @Override
    protected void applyPhaseTwoModifiers() {

    }
}
