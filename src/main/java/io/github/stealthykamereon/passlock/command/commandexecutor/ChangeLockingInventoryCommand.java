package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class ChangeLockingInventoryCommand extends CommandExecutor {

    public ChangeLockingInventoryCommand(PassLock passLock) {
        super(passLock);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        player.openInventory(passLock.getInventoryManager().getChangingInventory(player));
        return true;
    }
}
