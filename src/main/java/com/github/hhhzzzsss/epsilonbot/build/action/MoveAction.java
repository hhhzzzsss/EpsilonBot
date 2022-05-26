package com.github.hhhzzzsss.epsilonbot.build.action;

import lombok.RequiredArgsConstructor;

public class MoveAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.MOVE;
    }

    public MoveAction(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.breaking = true;
        this.yPriority = true;
    }

    public MoveAction(double x, double y, double z, boolean breaking, boolean yPriority) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.breaking = breaking;
        this.yPriority = yPriority;
    }

    public final double x;
    public final double y;
    public final double z;
    public final boolean breaking;
    public final boolean yPriority;
}
