package io.github.stealthykamereon.passlock.command.eventcommand;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class OwnerEventCommand extends EventCommand {

    public OwnerEventCommand(PassLock passLock) {
        super(passLock);
    }

    @Override
    public void trigger(Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = interactEvent.getPlayer();
                Block block = interactEvent.getClickedBlock();
                if (passLock.getLockManager().isLocked(block)) {
                    passLock.sendMessage(player, "ownerMessage",
                            "%p", passLock.getLockManager().getLockOwner(block).getName());
                } else {
                    passLock.sendMessage(player, "blockNotLocked");
                }
                interactEvent.setCancelled(true);
            }
        }
    }
}
