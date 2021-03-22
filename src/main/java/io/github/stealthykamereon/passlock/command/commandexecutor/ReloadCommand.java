package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class ReloadCommand extends CommandExecutor {

    public ReloadCommand(PassLock passLock) {
        super(passLock);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        passLock.reloadConfig();
        passLock.sendMessage(player, "reload");
        return true;
    }
}
