package net.manno.simplemobsiege.entity.ai;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;

import java.util.EnumSet;

public class SiegeAttackGoal extends Goal {
    private final Mob mob;
    private final BlockPos siegePos;
    private final double speedModifier;

    public SiegeAttackGoal(Mob mob, BlockPos siegePos) {
        this.mob = mob;
        this.siegePos = siegePos;
        this.speedModifier = 1.2D;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return mob.isAlive() && mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse() && !this.mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.mob.getNavigation().moveTo(siegePos.getX(), siegePos.getY(), siegePos.getZ(), speedModifier);
    }

    @Override
    public void tick() {
        if (this.mob.getNavigation().isDone() || this.mob.distanceToSqr(siegePos.getX(), siegePos.getY(), siegePos.getZ()) > 100) {
            this.mob.getNavigation().moveTo(siegePos.getX(), siegePos.getY(), siegePos.getZ(), speedModifier);
        }
    }
}

