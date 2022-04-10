package io.github.stealthykamereon.passlock.command.eventcommand;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class LockEventCommand extends EventCommand {


    public LockEventCommand(PassLock passLock) {
        super(passLock);
    }

    @Override
    public void trigger(Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = interactEvent.getPlayer();
                Block block = interactEvent.getClickedBlock();
                if (block != null) {
                    Material material = block.getType();

                    if (!passLock.getLockableManager().isLockable(material)) {
                        passLock.sendMessage(player, "blockNotLockable");
                        interactEvent.setCancelled(true);
                    } else if (passLock.getLockManager().isLocked(block)) {
                        passLock.sendMessage(player, "blockAlreadyLocked");
                        interactEvent.setCancelled(true);
                    } else if (passLock.getConfig().getBoolean("useEconomy") && !passLock.getEconomy().has(player, passLock.getLockManager().getLockPrice(player))){
                        passLock.sendMessage(player, "notEnoughMoney");
                        interactEvent.setCancelled(true);
                    } else
                        player.openInventory(passLock.getInventoryManager().createLockingInventory(player, block));
                }
            }
        }
    }
}
