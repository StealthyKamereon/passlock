package io.github.stealthykamereon.passlock;


import io.github.stealthykamereon.passlock.command.Command;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.DyeColor;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EventListener implements Listener{

    private PassLock passLock;
    private Logger log;
    public List<Player> watchingPlayer;
    private DyeColor[] colors = {DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.RED,
            DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.LIGHT_BLUE, DyeColor.CYAN,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.BROWN};
    private Economy economy;
    private InventoryManager inventoryManager;

    public EventListener(PassLock passLock){
        this.passLock = passLock;
        log = passLock.getLogger();
        watchingPlayer = new ArrayList<Player>();
        economy = passLock.getEconomy();
        inventoryManager = passLock.getInventoryManager();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK){
            Block block = e.getClickedBlock();
            Player player = e.getPlayer();
            if (passLock.getCommandMap().containsKey(player)) {
                Command command = passLock.getCommandMap().get(player);
                command.trigger(e);
                passLock.getCommandMap().remove(player);
                e.setCancelled(true);
            } else {
                if (passLock.getLockManager().isLocked(block)) {
                    if (player.hasPermission("passlock.watch") && !passLock.getLockManager().isOwner(player, block)) {
                        try {
                            player.openInventory(passLock.getInventoryManager().getWatchingInventory(block, player));
                            watchingPlayer.add(player);
                            e.setCancelled(true);
                        } catch (ClassCastException ignored) {}
                    } else if (passLock.getLockManager().isOwner(player, block) && !passLock.getConfig().getBoolean("ownerNeedCode")) {
                        e.setCancelled(false);
                    } else if (passLock.getWorldInteractor().canBeClosed(block)) {
                        Openable openable = (Openable) block.getBlockData();
                        if (openable.isOpen()) { //Close opened doors without code
                            openable.setOpen(false);
                            block.setBlockData(openable);
                        }
                        e.setCancelled(true);
                    } else if (player.hasPermission("passlock.open")) {
                        player.openInventory(passLock.getInventoryManager().createBasicInventory(player, block));
                        e.setCancelled(true);
                    } else {
                        player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("noPermissions")));
                        e.setCancelled(true);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHopperTakeItem(InventoryMoveItemEvent event){
        try {
            InventoryHolder holder = event.getSource().getHolder();
            if (holder instanceof BlockState) {
                BlockState blockState = (BlockState) holder;
                if (event.getDestination().getType().equals(InventoryType.HOPPER) && passLock.getLockManager().isLocked(blockState.getBlock())) {
                    event.setCancelled(true);
                }
            }
        }catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPistonRetractEvent(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (passLock.getLockManager().hasNearbyLockedBlockRelyingOn(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonExtendEvent(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (passLock.getLockManager().hasNearbyLockedBlockRelyingOn(block)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onRedstoneEvent(BlockRedstoneEvent event) {
        if (passLock.getLockManager().isLocked(event.getBlock())) {
            event.setNewCurrent(0);
        }
    }

    @EventHandler
    public void onBlockBurnEvent(BlockBurnEvent event) {
        if (passLock.getLockManager().hasNearbyLockedBlockRelyingOn(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplodeEvent(EntityExplodeEvent event) {
        event.blockList().removeIf(b -> passLock.getLockManager().hasNearbyLockedBlockRelyingOn(b));
    }

    @EventHandler
    public void onBlockExplodeEvent(BlockExplodeEvent event) {
        event.blockList().removeIf(b -> passLock.getLockManager().hasNearbyLockedBlockRelyingOn(b));
    }

    @EventHandler
    public void onEntityInteractEvent(EntityInteractEvent event) {
        if (passLock.getLockManager().isLocked(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onEntityBreakDoorEvent(EntityBreakDoorEvent event) {
        if (passLock.getLockManager().isLocked(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (passLock.getLockManager().hasNearbyLockedBlockRelyingOn(event.getBlock())) {
            event.setCancelled(true);
        }
    }
    @EventHandler
    public void onBlockFadeEvent(BlockFadeEvent event) {
        if (passLock.getLockManager().hasNearbyLockedBlockRelyingOn(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        Block block = e.getBlock();
        Player player = e.getPlayer();

        if (passLock.getLockManager().isLocked(block)) {
            if (passLock.getLockManager().isOwner(player, block) && !passLock.getConfig().getBoolean("ownerNeedCode")) {
                passLock.getLockManager().unlock(block);
                player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockUnlocked")));
                e.setCancelled(false);
            } else {
                player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("breakMessage")));
                e.setCancelled(true);
            }
        } else if (passLock.getLockManager().hasNearbyLockedBlockRelyingOn(block)) {
            // A block is attached to this one
            player.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("breakMessage")));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        try {
            Player p = (Player)event.getWhoClicked();
            if (watchingPlayer.contains(p)){
                event.setCancelled(true);
                return;
            }
            if (inventoryManager.isAPasslockInventory(event.getView())) {
                if (event.getCursor() != null && event.getCurrentItem() != null) {
                    inventoryManager.handleInventoryEvent(event);
                }
            }
        } catch (NullPointerException ex){
            ex.printStackTrace();
        }

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (passLock.getLockManager().checkForAttachedLockedChest(e.getBlock())) {
            passLock.getWorldInteractor().convertToSingleChest(e.getBlock());
        }
    }

    @EventHandler
    public void onPlayerCloseInventory (InventoryCloseEvent e){
        Inventory inventory = e.getInventory();
        Player player = (Player)e.getPlayer();
        watchingPlayer.remove(player);
        if (passLock.getInventoryManager().isChangingInventory(e.getView())){
            inventoryManager.setChangeInventory(inventory, player);
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e){
        passLock.getLockManager().saveLocks();
    }

}
