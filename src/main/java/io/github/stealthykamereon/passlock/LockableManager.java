package io.github.stealthykamereon.passlock;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.DoubleChest;
import org.bukkit.material.Openable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

public class LockableManager {

    private PassLock passLock;
    private List<Material> lockableMaterials;
    private List<Material> doors, trapdoors;

    public LockableManager(PassLock passLock){
        this.passLock = passLock;
        loadLockableMaterials();

        doors = new LinkedList<>();
        doors.add(Material.DARK_OAK_DOOR);
        doors.add(Material.ACACIA_DOOR);
        doors.add(Material.BIRCH_DOOR);
        doors.add(Material.IRON_DOOR);
        doors.add(Material.JUNGLE_DOOR);
        doors.add(Material.OAK_DOOR);
        doors.add(Material.SPRUCE_DOOR);
        trapdoors = new LinkedList<>();
        trapdoors.add(Material.OAK_TRAPDOOR);
        trapdoors.add(Material.ACACIA_TRAPDOOR);
        trapdoors.add(Material.BIRCH_TRAPDOOR);
        trapdoors.add(Material.DARK_OAK_TRAPDOOR);
        trapdoors.add(Material.IRON_TRAPDOOR);
        trapdoors.add(Material.JUNGLE_TRAPDOOR);
        trapdoors.add(Material.SPRUCE_TRAPDOOR);
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
            DoubleChest chest = (DoubleChest) block.getState();
            return true;
        } catch (ClassCastException ignored){}
        return false;
    }

    public boolean isDoor(Material material){
        return doors.contains(material);
    }

    public boolean isTrapdoor(Material material){
        return trapdoors.contains(material);
    }

    public boolean isOpenable(Material material) {
        return isDoor(material) || isTrapdoor(material);
    }

}
