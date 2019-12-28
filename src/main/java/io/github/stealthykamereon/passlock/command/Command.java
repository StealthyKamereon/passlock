package io.github.stealthykamereon.passlock.command;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.event.Event;

public abstract class Command {

    protected PassLock passLock;

    public Command(PassLock passLock){
        this.passLock = passLock;
    }

    public abstract void trigger(Event event);

}
