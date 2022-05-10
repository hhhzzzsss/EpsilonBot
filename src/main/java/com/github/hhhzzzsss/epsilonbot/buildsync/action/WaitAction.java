package com.github.hhhzzzsss.epsilonbot.buildsync.action;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WaitAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.WAIT;
    }

    public final int duration;
}
