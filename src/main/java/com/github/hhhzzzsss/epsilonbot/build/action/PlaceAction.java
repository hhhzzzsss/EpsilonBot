package com.github.hhhzzzsss.epsilonbot.build.action;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlaceAction extends Action {
    @Override
    public ActionType getActionType() {
        return ActionType.PLACE;
    }

    public final int x;
    public final int y;
    public final int z;
    public final int stateId;
}
