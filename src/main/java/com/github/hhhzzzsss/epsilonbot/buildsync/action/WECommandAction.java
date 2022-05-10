package com.github.hhhzzzsss.epsilonbot.buildsync.action;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WECommandAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.WE_COMMAND;
    }

    public final String command;
    public final boolean awaitWE;
}
