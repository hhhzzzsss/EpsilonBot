package com.github.hhhzzzsss.epsilonbot.build.action;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CommandAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.COMMAND;
    }

    public final String command;
    public final boolean awaitWE;
}
