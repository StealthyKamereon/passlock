package io.github.stealthykamereon.passlock;


import io.github.stealthykamereon.passlock.command.eventcommand.EventCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Attachable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EventListener implements Listener {

    private PassLock passLock;
    private Logger log;
    public List<Player> watchingPlayer;
    private Economy economy;

    public EventListener(PassLock passLock) {
        this.passLock = passLock;
        log = passLock.getLogger();
        watchingPlayer = new ArrayList<Player>();
        economy = passLock.getEconomy();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) { //Clic droit
            Block block = e.getClickedBlock();
            if (block == null) {
                return;
            }
            Player player = e.getPlayer();
            // The player emitted a command
            if (passLock.getCommandMap().containsKey(player)) {
                EventCommand command = passLock.getCommandMap().get(player);
                command.trigger(e);
                passLock.getCommandMap().remove(player);
                e.setCancelled(true);
            } else {
                if (passLock.getLockManager().isLocked(block)) {
                    // The player want to watch and is not the owner
                    if (passLock.getLockableManager().hasInventory(block) && player.hasPermission("passlock.watch") && !passLock.getLockManager().isOwner(player, block)) {
                        try {
                            player.openInventory(passLock.getInventoryManager().getWatchingInventory(block, player));
                            watchingPlayer.add(player);
                            e.setCancelled(true);
                        } catch (ClassCastException ignored) {
                        }
                    // The player want to open and is the owner
                    } else if (passLock.getLockManager().isOwner(player, block) && !passLock.getConfig().getBoolean("ownerNeedCode")) {
                        e.setCancelled(false);
                    } else if (player.hasPermission("passlock.open")) {
                        if (passLock.getLockableManager().isOpenable(block.getType())) {
                            Openable openable = (Openable) block.getState().getBlockData();
                            if (openable.isOpen()) { //Close opened doors without code
                                openable.setOpen(false);
                                block.setBlockData(openable);
                                return;
                            }
                        }
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
    public void onHopperTakeItem(InventoryMoveItemEvent event) {
        try {
            InventoryHolder holder = event.getSource().getHolder();
            if (holder instanceof BlockState) {
                BlockState block = (BlockState) holder;
                if (event.getDestination().getType().equals(InventoryType.HOPPER) && passLock.getLockManager().isLocked(block.getBlock())) {
                    event.setCancelled(true);
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> passLock.getLockManager().isLocked(block));
    }

    @EventHandler
    public  void onRedstonePower(BlockRedstoneEvent event) {
        if (passLock.getLockManager().isLocked(event.getBlock())) {
            event.setNewCurrent(0);
        }
    }

    @EventHandler
    public void onPistonPush(BlockPistonExtendEvent event) {
        for (Block pushedBlock : event.getBlocks()) {
            if (passLock.getLockManager().isLocked(pushedBlock)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block retractedBlock : event.getBlocks()) {
            if (passLock.getLockManager().isLocked(retractedBlock)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        if (passLock.getLockManager().isLocked(event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Block block = e.getBlock();
        Player player = e.getPlayer();

        if (passLock.getLockManager().isLocked(block)) {
            if (passLock.getLockManager().isOwner(player, block) && !passLock.getConfig().getBoolean("ownerNeedCode")) {
                passLock.getLockManager().unlock(block);
                passLock.sendMessage(player, "blockUnlocked");
                e.setCancelled(false);
            } else {
                passLock.sendMessage(player, "breakMessage");
                e.setCancelled(true);
            }
        } else if (checkForAttachableBreaking(block)) {
            // A block is attached to this one
            e.setCancelled(true);
        }
        if (e.isCancelled()) {
            passLock.sendMessage(player, "breakMessage");
        }
    }

    private boolean checkForAttachableBreaking(Block block) {
        Block upperBlock = block.getRelative(BlockFace.UP);
        if (passLock.getLockableManager().isDoor(upperBlock.getType()) && passLock.getLockManager().isLocked(upperBlock)) {
            return true;
        }
        BlockFace[] blockFaces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.WEST, BlockFace.SOUTH, BlockFace.EAST};
        for (BlockFace blockFace : blockFaces) {
            Block blockNearby = block.getRelative(blockFace);
            if (blockNearby.getState().getData() instanceof Attachable) {
                Attachable attachable = (Attachable) blockNearby.getState().getData();
                if (attachable.getAttachedFace() == blockFace.getOppositeFace() && passLock.getLockManager().isLocked(blockNearby))
                    return true;
            }
        }
        return false;
    }

    private void openLockedBlock(Block block, Player player) {
        if (passLock.getLockableManager().isOpenable(block.getType())) {
            Openable openable = (Openable)block.getState().getBlockData();
            openable.setOpen(true);
            block.setBlockData(openable);
            player.closeInventory();
        } else if (passLock.getLockableManager().hasInventory(block)) {
            player.openInventory(passLock.getLockableManager().getInventory(block));
        }
    }

    private void alertOwner(Block block, Player robber) {
        Player owner = passLock.getLockManager().getLockOwner(block).getPlayer();
        if (owner != null && !owner.equals(robber)) {
            Location location = block.getLocation();
            passLock.sendMessage(owner, "robbingAlert",
                    "%player", robber.getDisplayName(),
                    "%block", block.getType().name(),
                    "%location", String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            Player p = (Player) event.getWhoClicked();
            InventoryManager inventoryManager = passLock.getInventoryManager();
            if (watchingPlayer.contains(p)) {
                event.setCancelled(true);
                return;
            }
            if (inventoryManager.isAPasslockInventory(event.getView())) {
                if (event.getClick() == ClickType.DOUBLE_CLICK) {
                    event.setCancelled(true);
                    return;
                }
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
                            if (inventoryManager.isBasicInventory(event.getView())) {
                                int[] password = passLock.getInventoryManager().getPassword(event.getView());
                                Block lockedBlock = inventoryManager.getBlockFromConfirmationItem(itemStackClicked);
                                if (passLock.getLockManager().isPasswordCorrect(lockedBlock, password)) {
                                    openLockedBlock(lockedBlock, p);
                                    event.setCancelled(true);
                                    return;
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
                                    } else {
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
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }

    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        Player player = event.getPlayer();
        if (passLock.getLockableManager().isDoubleChest(block)) {
            Chest chest = (Chest) block.getBlockData();
            BlockFace facing = chest.getFacing();
            Block otherBlock;
            if (chest.getType() == Chest.Type.LEFT) {
                otherBlock = block.getRelative(rotateRight(facing));
            } else {
                otherBlock = block.getRelative(rotateLeft(facing));
            }
            if (passLock.getLockManager().isLocked(otherBlock.getLocation())) {
                passLock.sendMessage(player, "noPermissions");
                event.setCancelled(true);
            }
        }
    }

    public BlockFace rotateLeft(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.EAST;
            case WEST:
                return BlockFace.SOUTH;
        }
        return null;
    }

    public BlockFace rotateRight(BlockFace face) {
        switch (face) {
            case NORTH:
                return BlockFace.EAST;
            case EAST:
                return BlockFace.SOUTH;
            case SOUTH:
                return BlockFace.WEST;
            case WEST:
                return BlockFace.NORTH;
        }
        return null;
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
    }*/

    @EventHandler
    public void onPlayerCloseInventory (InventoryCloseEvent e){
        Inventory inventory = e.getInventory();
        Player p = (Player)e.getPlayer();
        if(watchingPlayer.contains(p)){
            watchingPlayer.remove(p);
        }
        if (passLock.getInventoryManager().isChangingInventory(inventory, e.getView())){
            passLock.getInventoryManager().setChangeInventory(inventory, p);
        }
    }

    @EventHandler
    public void onWorldSave(WorldSaveEvent e) {
        passLock.getLockManager().saveLocks();
    }

}
