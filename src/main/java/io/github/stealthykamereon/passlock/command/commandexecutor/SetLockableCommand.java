package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import io.github.stealthykamereon.passlock.command.eventcommand.SetLockableEventCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class SetLockableCommand extends CommandExecutor {

    public SetLockableCommand(PassLock passLock) {
        super(passLock, SetLockableEventCommand.class);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        passLock.sendMessage(player, "setLockable");
        return true;
    }
}
