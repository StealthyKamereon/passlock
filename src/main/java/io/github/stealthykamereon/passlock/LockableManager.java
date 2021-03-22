package io.github.stealthykamereon.passlock;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class LockableManager {

    private PassLock passLock;
    private List<Material> lockableMaterials;

    public LockableManager(PassLock passLock){
        this.passLock = passLock;
        loadLockableMaterials();

    }

    private void loadLockableMaterials() {
        lockableMaterials = new ArrayList<>();
        List<String> materialNames = passLock.getConfig().getStringList("lockables");
        for (String name : materialNames){
            Material material = Material.getMaterial(name);
            if (material == null) {
                passLock.getLogger().log(Level.SEVERE, String.format("Wrong material : %s", name));
            } else {
                lockableMaterials.add(material);
            }
        }
    }

    public boolean isLockable(Material type){
        return this.lockableMaterials.contains(type);
    }

    public void addLockable(Material material){
        this.lockableMaterials.add(material);
        passLock.getConfig().set("lockables", passLock.getMaterialNames(this.lockableMaterials));
        passLock.saveConfig();
    }

    public void removeLockable(Material material){
        this.lockableMaterials.remove(material);
        passLock.getConfig().set("lockables", passLock.getMaterialNames(this.lockableMaterials));
        passLock.saveConfig();
    }

    public boolean isDoubleChest(Block block){
        try {
            Chest chest = (Chest) block.getState().getBlockData();
            return chest.getType() != Chest.Type.SINGLE;
        } catch (ClassCastException exception) {
            return false;
        }
    }

    public boolean hasInventory(Block block) {
        return block.getState() instanceof Container;
    }

    public Inventory getInventory(Block block) {
        return ((Container)block.getState()).getInventory();
    }

    public boolean isDoor(Material material){
        return material.createBlockData() instanceof Door;
    }

    public boolean isOpenable(Material material) {
        return material.createBlockData() instanceof Openable;
    }

}
