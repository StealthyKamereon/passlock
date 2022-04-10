package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import io.github.stealthykamereon.passlock.command.eventcommand.OwnerEventCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class OwnerCommand extends CommandExecutor {

    public OwnerCommand(PassLock passLock) {
        super(passLock, OwnerEventCommand.class);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        passLock.sendMessage(player, "ownerAsking");
        return true;
    }
}
