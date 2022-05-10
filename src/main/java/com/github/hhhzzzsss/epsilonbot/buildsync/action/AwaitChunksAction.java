package com.github.hhhzzzsss.epsilonbot.buildsync.action;

import lombok.RequiredArgsConstructor;

// This action currently isn't used because I just poll for chunk loading in my action dispatch code
@RequiredArgsConstructor
public class AwaitChunksAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.AWAIT_CHUNKS;
    }
}
