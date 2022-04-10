package io.github.stealthykamereon.passlock.command.eventcommand;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import static io.github.stealthykamereon.passlock.PassLock.economy;

public class UnlockEventCommand extends EventCommand {

    public UnlockEventCommand(PassLock passLock) {
        super(passLock);
    }

    @Override
    public void trigger(Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = interactEvent.getPlayer();
                Block block = interactEvent.getClickedBlock();
                interactEvent.setCancelled(true);
                if (passLock.getLockManager().isLocked(block)) {
                    if (passLock.getLockManager().isOwner(player, block)
                            || player.hasPermission("passlock.unlockEveryLocks")) {
                        passLock.getLockManager().unlock(block);
                        player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockUnlocked")));
                        if (passLock.getConfig().getBoolean("useEconomy") && passLock.getConfig().getBoolean("unlockingPaysBack")) {
                            economy.depositPlayer(player, passLock.getLockManager().getLockPrice(player));
                        }
                    } else {
                        player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("dontOwnMessage")));
                    }
                } else {
                    player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockNotLocked")));
                }
            }
        }
    }
}
