package io.github.stealthykamereon.passlock;

import org.bukkit.Material;

import java.util.ArrayList;
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
        if (material == Material.AIR)
            return;
        this.lockableMaterials.add(material);
        passLock.getConfig().set("lockables", passLock.getMaterialNames(this.lockableMaterials));
        passLock.saveConfig();
    }

    public void removeLockable(Material material){
        this.lockableMaterials.remove(material);
        passLock.getConfig().set("lockables", passLock.getMaterialNames(this.lockableMaterials));
        passLock.saveConfig();
    }


}
