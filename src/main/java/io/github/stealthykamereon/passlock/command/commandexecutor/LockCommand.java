package io.github.stealthykamereon.passlock.command.commandexecutor;

import io.github.stealthykamereon.passlock.PassLock;
import io.github.stealthykamereon.passlock.command.eventcommand.LockEventCommand;
import org.bukkit.command.Command;
import org.bukkit.entity.Player;

public class LockCommand extends CommandExecutor {

    public LockCommand(PassLock passLock) {
        super(passLock, LockEventCommand.class);
    }

    @Override
    protected boolean onCommand(Player player, Command command, String[] args) {
        if (passLock.getConfig().getBoolean("useEconomy")) {
            passLock.sendMessage(player, "priceMessage", "%p", passLock.getLockManager().getLockPrice(player) + "");
        }
        if (passLock.getConfig().getBoolean("useEconomy") && !passLock.getEconomy().has(player, passLock.getLockManager().getLockPrice(player))) {
            passLock.sendMessage(player, "notEnoughMoney");
        } else if (passLock.getConfig().getBoolean("lockingLimitEnabled") && !passLock.getLockManager().hasRemainingLocks(player)) {
            passLock.sendMessage(player, "lockingLimitReached",
                    "%c", passLock.getLockManager().getLockCount(player) + "",
                    "%l", passLock.getLockManager().getLockingLimit(player) + "");
        } else {
            passLock.sendMessage(player, "lockCommand");
        }
        return true;
    }
}
