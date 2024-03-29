package io.github.stealthykamereon.passlock;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;

public class WorldInteractor {

    private PassLock passLock;

    private Map<Location, BukkitTask> doorClosingTasks;

    public WorldInteractor(PassLock passLock) {
        this.passLock = passLock;
        doorClosingTasks = new HashMap<>();
    }

    public boolean isDoubleChest(Block block){
        return block.getState() instanceof DoubleChest;
    }

    public boolean isDoor(Block block){
        try {
            Door door = (Door) block.getBlockData();
            return true;
        } catch (ClassCastException e) {}
        return false;
    }

    public boolean isOpenable(Block block) {
        try {
            Openable openable = (Openable) block.getBlockData();
            return true;
        } catch (ClassCastException e) {}
        return false;
    }

    public boolean hasInventory(Block block) {
        try {
            InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
            return true;
        } catch (ClassCastException e) {}
        return false;
    }

    public boolean canBeClosed(Block block) {
        if (isOpenable(block)) {
            Openable openable = (Openable) block.getBlockData();
            return openable.isOpen();
        }
        return false;
    }

    public void openBlockInventory(Block block, Player player) {
        try {
            InventoryHolder inventoryHolder = (InventoryHolder) block.getState();
            player.openInventory(inventoryHolder.getInventory());
        } catch (ClassCastException e) {}
    }

    public void openBlock(Block block, Player player) {
        try {
            Openable openable = (Openable) block.getBlockData();
            openable.setOpen(true);
            block.setBlockData(openable);
            registerDoorClosing(block, player);
        } catch (ClassCastException e) {}
    }

    public void registerDoorClosing(Block block, Player player) {
        try {
            long delay = passLock.getLockManager().getDoorClosingTime(player);
            if (delay > 0) {
                Block blockToBeLocked = player.getWorld().getBlockAt(getLockingLocation(block));
                BukkitTask task = new BukkitRunnable(){
                    @Override
                    public void run() {
                        Openable openable = (Openable) blockToBeLocked.getBlockData();
                        openable.setOpen(false);
                        blockToBeLocked.setBlockData(openable);
                    }
                }.runTaskLater(passLock, delay);
                if (doorClosingTasks.containsKey(blockToBeLocked.getLocation())) {
                    doorClosingTasks.get(blockToBeLocked.getLocation()).cancel();
                }
                doorClosingTasks.put(blockToBeLocked.getLocation(), task);
            }
        } catch (ClassCastException e) {}
    }

    public void clearDoorClosing(Block block) {
        Location lockingLocation = getLockingLocation(block);
        if (doorClosingTasks.containsKey(lockingLocation)) {
            doorClosingTasks.get(lockingLocation).cancel();
            doorClosingTasks.remove(lockingLocation);
        }
    }

    public Location getDoorLocation(Block block) {
        Door door = (Door) block.getBlockData();
        if (door.getHalf().equals(Bisected.Half.BOTTOM)){
            return block.getLocation();
        }else {
            Location location = block.getRelative(BlockFace.DOWN).getLocation();
            return location;
        }
    }

    public Location getDoubleChestLocation(Block block){
        DoubleChest doubleChest = (DoubleChest)block.getState();
        DoubleChest leftSide = (DoubleChest)doubleChest.getLeftSide();
        return leftSide.getLocation();
    }

    public Location getLockingLocation(Block block) {
        if (isDoor(block)) {
            return getDoorLocation(block);
        }
        else if (isDoubleChest(block))
            return getDoubleChestLocation(block);
        else
            return block.getLocation();
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

    public Block getOtherChestSide(Block block, Chest chest) {
        if (chest.getType() == Chest.Type.SINGLE) {
            return null;
        } else {
            BlockFace facing = chest.getFacing();
            if (chest.getType() == Chest.Type.LEFT) {
                return block.getRelative(rotateRight(facing));
            } else if (chest.getType() == Chest.Type.RIGHT) {
                return block.getRelative(rotateLeft(facing));
            }
        }
        return null;
    }

    public void convertToSingleChest(Block block) {
        try {
            Chest chest = (Chest) block.getBlockData();
            if (chest.getType() != Chest.Type.SINGLE) {
                Block otherSide = getOtherChestSide(block, chest);
                chest.setType(Chest.Type.SINGLE);
                block.setBlockData(chest);
                otherSide.getState().update();
            }
        } catch (ClassCastException e) {}
    }

}
