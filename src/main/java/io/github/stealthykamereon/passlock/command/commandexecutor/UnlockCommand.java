package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import io.github.stealthykamereon.passlock.command.eventcommand.UnlockEventCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class UnlockCommand extends CommandExecutor {

    public UnlockCommand(PassLock passLock) {
        super(passLock, UnlockEventCommand.class);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        passLock.sendMessage(player, "unlockCommand");
        return true;
    }
}
