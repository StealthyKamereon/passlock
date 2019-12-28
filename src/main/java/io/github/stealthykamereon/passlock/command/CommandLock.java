package io.github.stealthykamereon.passlock.command;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CommandLock extends Command {

    public CommandLock(PassLock passLock) {
        super(passLock);
    }

    @Override
    public void trigger(Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent)event;
            if (interactEvent.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = interactEvent.getPlayer();
                Block block = interactEvent.getClickedBlock();
                Material material = block.getType();

                if (!passLock.getLockableManager().isLockable(material)) {
                    player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockNotLockable")));
                    interactEvent.setCancelled(true);
                } else if (passLock.getLockManager().isLocked(block)){
                    player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockAlreadyLocked")));
                    interactEvent.setCancelled(true);
                } else if (passLock.getConfig().getBoolean("useEconomy") && !passLock.getEconomy().has(player, passLock.getLockManager().getLockPrice(player))){
                    player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("notEnoughMoney")));
                    interactEvent.setCancelled(true);
                } else if (passLock.getConfig().getBoolean("lockingLimitEnabled") && !passLock.getLockManager().hasRemainingLocks(player)){
                    player.sendMessage(
                            passLock.formatMessage(passLock.getLocaleManager().getString("lockingLimitReached")
                                    +" ("+ passLock.getLockManager().getLockCount(player)
                                    +"/"+ passLock.getLockManager().getLockingLimit(player)+")")
                    );
                } else
                    player.openInventory(passLock.getInventoryManager().createLockingInventory(player, block));
            }
        }
    }
}
