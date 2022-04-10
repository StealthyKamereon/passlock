package io.github.stealthykamereon.passlock;

import com.udojava.evalex.Expression;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.Attachable;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LockManager {

    private PassLock passLock;
    private Map<Location, Lock> locks;

    public LockManager(PassLock passLock){
        this.passLock = passLock;
        loadLocks();
    }

    private void loadLocks() {
        this.locks = new HashMap<>();
        FileConfiguration lockFile = YamlConfiguration.loadConfiguration(new File(passLock.getDataFolder(), "locks.yml"));
        List<?> objList = lockFile.getList("locks");
        if (objList != null) {
            for (Object object : objList) {
                Lock lock = (Lock) object;
                this.locks.put(lock.getLocation(), lock);
            }
        }
    }

    public void saveLocks() {
        FileConfiguration lockFile = YamlConfiguration.loadConfiguration(new File(passLock.getDataFolder(), "locks.yml"));
        lockFile.set("locks", this.locks);
    }

    public int getLockCount(Player p){
        int count = 0;
        for (Lock lock : this.locks.values()){
            if (lock.getOwner().equals(p)){
                count++;
            }
        }
        return count;
    }

    public void lock(Block block, Player owner, int[] password) {
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        Lock lock = new Lock(owner, password, location);
        this.locks.put(location, lock);
    }

    public void unlock(Block block){
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        this.locks.remove(location);
    }

    public boolean openLockedBlock(Block blockToOpen, Player player) {
        if (passLock.getWorldInteractor().isOpenable(blockToOpen)) {
            passLock.getWorldInteractor().openBlock(blockToOpen);
            return false;
        } else if (passLock.getWorldInteractor().hasInventory(blockToOpen)) {
            passLock.getWorldInteractor().openBlockInventory(blockToOpen, player);
            return true;
        } else if (blockToOpen.getState().getType() == Material.CRAFTING_TABLE) {
            player.openWorkbench(blockToOpen.getLocation(), false);
            return true;
        }
        return false;
    }

    public boolean isOwner(Player player, Block block){
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        if (this.locks.containsKey(location)) {
            return this.locks.get(location).getOwner().equals(player);
        } else
            return false;
    }

    public Player getLockOwner(Block block){
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        return this.locks.get(location).getOwner();
    }

    public boolean isPasswordCorrect(Block block, int[] password){
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        if (this.locks.containsKey(location)){
            return Arrays.equals(this.locks.get(location).getPassword(), password);
        } else {
            return false;
        }
    }

    public int getLockingLimit(Player p){
        if (passLock.getConfig().getBoolean("lockingLimitEnabled")){
            for (PermissionAttachmentInfo perm : p.getEffectivePermissions()){
                if (perm.getPermission().startsWith("passlock.locklimit.")){
                    return Integer.parseInt(perm.getPermission().substring(perm.getPermission().lastIndexOf(".")+1));
                }
            }
        }
        return -1;
    }

    public float getLockPrice(Player p){
        int lockCount = getLockCount(p)+1;
        String priceExpression = "0";
        for (PermissionAttachmentInfo perm : p.getEffectivePermissions()){
            if (perm.getPermission().startsWith("passlock.lockprice.")){
                priceExpression = perm.getPermission().substring(perm.getPermission().lastIndexOf(".")+1);
            }
        }
        priceExpression = priceExpression.replace("n", lockCount+"");
        Expression exp = new Expression(priceExpression);
        BigDecimal result = exp.eval();
        return result.floatValue();
    }

    public boolean isLocked(Block block){
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        return locks.containsKey(location);
    }

    public boolean hasNearbyLockedBlockRelyingOn(Block block) {
        return isLocked(block) || checkForLockedDoorAbove(block) || checkForAttachableBreaking(block);
    }

    public boolean checkForAttachableBreaking(Block block) {
        for (BlockFace blockFace : BlockFace.values()) {
            Block blockNearby = block.getRelative(blockFace);
            if (blockNearby.getState().getData() instanceof Attachable) {
                Attachable attachable = (Attachable) blockNearby.getState().getData();
                if (attachable.getAttachedFace() == blockFace.getOppositeFace() && isLocked(blockNearby))
                    return true;
            }
        }
        return false;
    }

    public boolean checkForLockedDoorAbove(Block block) {
        Block blockAbove = block.getRelative(BlockFace.UP);
        return passLock.getWorldInteractor().isDoor(blockAbove) && isLocked(blockAbove);
    }

    public boolean checkForAttachedLockedChest(Block block) {
        try {
            Chest placedChest = (Chest) block.getBlockData();
            Block otherSide = passLock.getWorldInteractor().getOtherChestSide(block, placedChest);
            if (otherSide == null) {
                return false;
            } else {
                return isLocked(otherSide);
            }
        } catch (ClassCastException e) {
            return false;
        }
    }


    public boolean hasRemainingLocks(Player player){
        return getLockCount(player) < getLockingLimit(player);
    }
}
