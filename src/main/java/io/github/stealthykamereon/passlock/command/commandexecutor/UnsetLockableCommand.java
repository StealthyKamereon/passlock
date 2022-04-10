package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import io.github.stealthykamereon.passlock.command.eventcommand.UnsetLockableEventCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class UnsetLockableCommand extends CommandExecutor {

    public UnsetLockableCommand(PassLock passLock) {
        super(passLock, UnsetLockableEventCommand.class);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        passLock.sendMessage(player, "removeLockable");
        return true;
    }
}
