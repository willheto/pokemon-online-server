package com.g8e.gameserver.models.events;

public class SoundEvent {
    public String soundName;
    public boolean isSfx = true;
    public boolean shouldInterrupt = false;
    public String entityID;

    public SoundEvent(String soundName, boolean isSfx, boolean shouldInterrupt, String entityID) {
        this.soundName = soundName;
        this.isSfx = isSfx;
        this.shouldInterrupt = shouldInterrupt;
        this.entityID = entityID;
    }

}
