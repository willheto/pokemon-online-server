package com.g8e.gameserver.network.actions.move;

public class PlayerTalkMoveActionData {

    final private String entityID;

    public PlayerTalkMoveActionData(String entityID) {
        this.entityID = entityID;
    }

    public String getEntityID() {
        return entityID;
    }

}
