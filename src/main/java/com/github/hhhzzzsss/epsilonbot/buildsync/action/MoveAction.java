package com.github.hhhzzzsss.epsilonbot.buildsync.action;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class MoveAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.MOVE;
    }

    public final double x;
    public final double y;
    public final double z;
}
