package io.github.stealthykamereon.passlock;

import com.udojava.evalex.Expression;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.material.Attachable;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class LockManager {

    private final PassLock passLock;
    private Map<Location, Lock> locks;

    public LockManager(PassLock passLock) {
        this.passLock = passLock;
        loadLocks();
    }

    private void loadLocks() {
        this.locks = new HashMap<>();
        FileConfiguration lockFile = YamlConfiguration.loadConfiguration(new File(passLock.getDataFolder(), "locks.yml"));
        locks = new HashMap<>();
        if (lockFile.contains("locks")) {
            for (Object obj : lockFile.getList("locks")) {
                Lock lock = (Lock)obj;
                locks.put(lock.getLocation(), lock);
            }
        }
        passLock.getLogger().info(String.format("Loaded %s locks", locks.keySet().size()));
    }

    public void saveLocks() {
        FileConfiguration lockFile = YamlConfiguration.loadConfiguration(new File(passLock.getDataFolder(), "locks.yml"));
        List<Lock> savedLocks = new ArrayList<>();
        for (Lock lock : locks.values()){
            savedLocks.add(lock);
        }
        lockFile.set("locks", savedLocks);
        try {
            lockFile.save(new File(passLock.getDataFolder(), "locks.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getLockCount(Player p) {
        int count = 0;
        for (Lock lock : this.locks.values()) {
            if (lock.getOwner().equals(p)) {
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
            passLock.getWorldInteractor().openBlock(blockToOpen, player);
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
            return this.locks.get(location).getOwner().getUniqueId().equals(player.getUniqueId());
        } else
            return false;
    }

    public OfflinePlayer getLockOwner(Block block) {
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        return this.locks.get(location).getOwner();
    }

    public boolean isPasswordCorrect(Block block, int[] password) {
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        if (this.locks.containsKey(location)) {
            return Arrays.equals(this.locks.get(location).getPassword(), password);
        } else {
            return false;
        }
    }

    public int getLockingLimit(Player p) {
        if (passLock.getConfig().getBoolean("lockingLimitEnabled")) {
            for (PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
                if (perm.getPermission().startsWith("passlock.locklimit.")) {
                    return Integer.parseInt(perm.getPermission().substring(perm.getPermission().lastIndexOf(".") + 1));
                }
            }
        }
        return -1;
    }

    public float getLockPrice(Player p) {
        int lockCount = getLockCount(p) + 1;
        String priceExpression = "0";
        for (PermissionAttachmentInfo perm : p.getEffectivePermissions()) {
            if (perm.getPermission().startsWith("passlock.lockprice.")) {
                priceExpression = perm.getPermission().substring(perm.getPermission().lastIndexOf(".") + 1);
            }
        }
        priceExpression = priceExpression.replace("n", lockCount + "");
        Expression exp = new Expression(priceExpression);
        BigDecimal result = exp.eval();
        return result.floatValue();
    }

    public long getDoorClosingTime(Player p) {
        try {
            double defaultDelay = passLock.getConfig().getDouble("defaultDoorClosingTimeSeconds");
            return (long)defaultDelay*20;
        } catch (NullPointerException e) {
            System.out.println("Missing config parameter 'defaultDoorClosingTimeSeconds' !");
            System.out.println("Add it to make this message disappear.");
        }
        return 0;
    }

    public boolean isLocked(Block block){
        Location location = passLock.getWorldInteractor().getLockingLocation(block);
        return isLocked(location);
    }

    public boolean isLocked(Location location) {
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


    public boolean hasRemainingLocks(Player player) {
        return getLockCount(player) < getLockingLimit(player);
    }
}
