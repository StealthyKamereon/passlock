package io.github.stealthykamereon.passlock;


import io.github.stealthykamereon.passlock.command.Command;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Openable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Attachable;

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

    public EventListener(PassLock passLock){
        this.passLock = passLock;
        log = passLock.getLogger();
        watchingPlayer = new ArrayList<Player>();
        economy = passLock.getEconomy();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK){ //Clic droit
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
                    } else if (passLock.getLockableManager().isOpenable(block.getType())) {
                        Openable openable = (Openable) block.getState().getData();
                        if (openable.isOpen()) { //Close opened doors without code
                            openable.setOpen(false);
                        }
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
                Block block = (Block)holder;
                if (event.getDestination().getType().equals(InventoryType.HOPPER) && passLock.getLockManager().isLocked(block)) {
                    event.setCancelled(true);
                }
            }
        }catch (NullPointerException e){
            e.printStackTrace();
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
        } else if (checkForAttachableBreaking(block)) {
            // A block is attached to this one
            e.setCancelled(true);
        }
    }

    private boolean checkForAttachableBreaking(Block block) {
        BlockFace[] blockFaces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
        for (BlockFace blockFace : blockFaces) {
            Block blockNearby = block.getRelative(blockFace);
            if (blockNearby.getState().getData() instanceof Attachable) {
                Attachable attachable = (Attachable) blockNearby.getState().getData();
                if (attachable.getAttachedFace() == blockFace.getOppositeFace())
                    return true;
            }
        }
        return false;
    }

    private void openLockedBlock(Block block, Player player){

    }

    private void alertOwner(Block block, Player robber) {
        Player owner = passLock.getLockManager().getLockOwner(block);
        if (!owner.equals(robber)) {
            Location location = block.getLocation();
            String message = passLock.getLocaleManager().getString("robbingAlert")
                    .replace("%player", robber.getDisplayName())
                    .replace("%block", block.getType().name())
                    .replace("%location", String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            owner.sendMessage(passLock.formatMessage(message));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event){
        try {
            Player p = (Player)event.getWhoClicked();
            InventoryManager inventoryManager = passLock.getInventoryManager();
            if (watchingPlayer.contains(p)){
                event.setCancelled(true);
                return;
            }
            if (inventoryManager.isAPasslockInventory(event.getView())) {
                if (event.getCursor() != null && event.getCurrentItem() != null) {
                    ItemStack itemStackClicked = event.getCurrentItem();
                    ItemMeta itemMeta = itemStackClicked.getItemMeta();
                    if (itemMeta != null) {
                        String itemName = itemMeta.getDisplayName();
                        if (inventoryManager.isCodeItem(itemName)) {
                            if (event.getClick().isLeftClick() && itemStackClicked.getAmount() < passLock.getConfig().getInt("itemMaxAmount"))
                                itemStackClicked.setAmount(itemStackClicked.getAmount() + 1);
                            else if (event.getClick().isRightClick() && itemStackClicked.getAmount() > 1)
                                itemStackClicked.setAmount(itemStackClicked.getAmount() - 1);
                        } else if (inventoryManager.isConfirmationItem(itemName)) {
                            System.out.println("Confirmation item");
                            if (inventoryManager.isBasicInventory(event.getView())) {
                                int[] password = passLock.getInventoryManager().getPassword(event.getView());
                                Block lockedBlock = inventoryManager.getBlockFromConfirmationItem(itemStackClicked);
                                if (passLock.getLockManager().isPasswordCorrect(lockedBlock, password)) {
                                    openLockedBlock(lockedBlock, p);
                                } else {
                                    p.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("wrongCode")));
                                    if (passLock.getConfig().getBoolean("ownerAlerted")) {
                                        alertOwner(lockedBlock, p);
                                    }
                                }
                            } else if (inventoryManager.isLockingInventory(event.getView())) {
                                int[] password = inventoryManager.getPassword(event.getView());
                                Block blockToLock = inventoryManager.getBlockFromConfirmationItem(itemStackClicked);
                                if (passLock.getConfig().getBoolean("useEconomy")) {
                                    if (economy.has(p, passLock.getLockManager().getLockPrice(p))) {
                                        economy.withdrawPlayer(p, passLock.getLockManager().getLockPrice(p));
                                        passLock.getLockManager().lock(blockToLock, p, password);
                                        p.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockLocked")));
                                    } else{
                                        p.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("notEnoughMoney")));
                                    }
                                } else {
                                    passLock.getLockManager().lock(blockToLock, p, password);
                                    p.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("blockLocked")));
                                }
                            }
                            p.closeInventory();
                        }
                        if (inventoryManager.isCodeItem(itemName) || inventoryManager.isConfirmationItem(itemName) || inventoryManager.isBarrierItem(itemName)) {
                            event.setCancelled(true);
                        }
                    }
                }
            }
/*
                        if(code.equals(codeManager.getPass(loc))){
                            p.sendMessage(codeManager.PREFIX+codeManager.RIGHT);
                            Block block = loc.getBlock();
                            if (codeManager.isDoor(block.getType()) || block.getType() == Material.OAK_TRAPDOOR){
                                BlockState state = loc.getBlock().getState();
                                Openable open = (Openable) state.getData();
                                open.setOpen(true);
                                state.update();
                                p.closeInventory();
                            }else if (block.getType() == Material.CHEST || block.getType() == Material.TRAPPED_CHEST) {
                                p.closeInventory();
                                Chest chest = (Chest) block.getState();

                                p.openInventory(chest.getInventory());
                            }else if(block.getType() == Material.FURNACE) {
                                Furnace fur = (Furnace) block.getState();
                                p.openInventory(fur.getInventory());
                            }else if(block.getType() == Material.CRAFTING_TABLE){
                                p.openWorkbench(block.getLocation(),true);
                            }else
                                p.closeInventory();*/
        } catch (NullPointerException ex){
            ex.printStackTrace();
        }

    }

    /*
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e){
        Block block = e.getBlock();
        if (block.getType() == Material.TRAPPED_CHEST){
            if ((block.getRelative(BlockFace.EAST).getType() == Material.TRAPPED_CHEST && codeManager.isRegistered(block.getRelative(BlockFace.EAST).getLocation()))
                    || (block.getRelative(BlockFace.NORTH).getType() == Material.TRAPPED_CHEST && codeManager.isRegistered(block.getRelative(BlockFace.NORTH).getLocation()))
                    || (block.getRelative(BlockFace.WEST).getType() == Material.TRAPPED_CHEST && codeManager.isRegistered(block.getRelative(BlockFace.WEST).getLocation()))
                    || (block.getRelative(BlockFace.SOUTH).getType() == Material.TRAPPED_CHEST && codeManager.isRegistered(block.getRelative(BlockFace.SOUTH).getLocation())))
                e.setCancelled(true);
        }
        if (block.getType() == Material.CHEST){
            if ((block.getRelative(BlockFace.EAST).getType() == Material.CHEST && codeManager.isRegistered(block.getRelative(BlockFace.EAST).getLocation()))
                    || (block.getRelative(BlockFace.NORTH).getType() == Material.CHEST && codeManager.isRegistered(block.getRelative(BlockFace.NORTH).getLocation()))
                    || (block.getRelative(BlockFace.WEST).getType() == Material.CHEST && codeManager.isRegistered(block.getRelative(BlockFace.WEST).getLocation()))
                    || (block.getRelative(BlockFace.SOUTH).getType() == Material.CHEST && codeManager.isRegistered(block.getRelative(BlockFace.SOUTH).getLocation())))
                e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerCloseInventory (InventoryCloseEvent e){
        Inventory inv = e.getInventory();
        Player p = (Player)e.getPlayer();
        if(watchingPlayer.contains(p)){
            watchingPlayer.remove(p);
        }
        if (e.getView().getTitle().equals(codeManager.CHANGETITLE) && inv.getType() == InventoryType.HOPPER){
            codeManager.setChangeInventory(inv, (Player)e.getPlayer());
        }
    }
    */

    @EventHandler
    public void onWorldSave(WorldSaveEvent e){
        passLock.getLockManager().saveLocks();
    }

}
