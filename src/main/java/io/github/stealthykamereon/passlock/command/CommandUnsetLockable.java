package io.github.stealthykamereon.passlock.command;

import io.github.stealthykamereon.passlock.PassLock;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class CommandUnsetLockable extends Command {
    public CommandUnsetLockable(PassLock passLock) {
        super(passLock);
    }

    @Override
    public void trigger(Event event) {
        if (event instanceof PlayerInteractEvent) {
            PlayerInteractEvent interactEvent = (PlayerInteractEvent) event;
            if (interactEvent.getAction() == Action.RIGHT_CLICK_BLOCK) {
                Player player = interactEvent.getPlayer();
                Block block = interactEvent.getClickedBlock();
                passLock.getLockableManager().removeLockable(block.getType());
                player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("nowLockable")));
                interactEvent.setCancelled(true);
            }
        }
    }
}
