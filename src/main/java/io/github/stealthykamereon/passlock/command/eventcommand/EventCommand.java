package io.github.stealthykamereon.passlock.command.eventcommand;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.event.Event;

public abstract class EventCommand {


    protected PassLock passLock;

    public EventCommand(PassLock passLock) {
        this.passLock = passLock;
    }

    public abstract void trigger(Event event);

}
