package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class ResetCommand extends CommandExecutor {

    public ResetCommand(PassLock passLock) {
        super(passLock);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        passLock.resetConfig();
        passLock.sendMessage(player, "resetConfig");
        return true;
    }
}
