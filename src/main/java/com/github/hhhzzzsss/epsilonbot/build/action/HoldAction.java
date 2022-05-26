package com.github.hhhzzzsss.epsilonbot.build.action;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class HoldAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.HOLD;
    }

    public final int itemId;
}
