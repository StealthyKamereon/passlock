package io.github.stealthykamereon.passlock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class InventoryManager {

    private PassLock passLock;
    private List<Material> lockingInventory;

    public InventoryManager(PassLock passLock){
        this.passLock = passLock;
        loadLockingInventory();
    }

    private void loadLockingInventory() {
        lockingInventory = new ArrayList<>();
        List<String> materialNames = passLock.getConfig().getStringList("inventory");
        for (String name : materialNames){
            lockingInventory.add(Material.getMaterial(name));
        }
    }

    public Inventory createBasicInventory(Player owner, Block block){
        Location location = block.getLocation();
        return createInventory(owner, location, passLock.getLocaleManager().getString("basicInventoryTitle"));
    }

    public Inventory createLockingInventory(Player owner, Block block){
        Location location = block.getLocation();
        String title = passLock.getLocaleManager().getString("lockingInventoryTitle");
        if(passLock.getConfig().getBoolean("useEconomy")){
            title+= passLock.getLocaleManager().getString("priceTitle").replace("%c", passLock.getLockManager().getLockPrice(owner)+"");
        }
        return createInventory(owner, location, title);
    }

    private Inventory createInventory(Player owner, Location location, String title){
        Inventory inv = Bukkit.createInventory(owner, InventoryType.HOPPER, title);

        ItemStack code0 = createPasswordItem(lockingInventory.get(0));
        ItemStack code1 = createPasswordItem(lockingInventory.get(1));
        ItemStack code2 = createPasswordItem(lockingInventory.get(2));
        ItemStack itemVal = createValidationItem(lockingInventory.get(3), location);

        ItemStack[] items = {code0, code1, code2, null, itemVal};
        inv.setContents(items);
        return inv;
    }

    private ItemStack createPasswordItem(Material material){
        ItemStack code = new ItemStack(material, 1);
        ItemMeta meta = code.getItemMeta();
        if (meta != null){
            meta.setDisplayName(passLock.getLocaleManager().getString("codeItem"));
            code.setItemMeta(meta);
        } else {
            Bukkit.getLogger().log(Level.SEVERE, "Got null item metadata... Something went wrong !");
        }
        return code;
    }

    private ItemStack createValidationItem(Material material, Location location){
        ItemStack itemVal = new ItemStack(material, 1);
        ItemMeta metaVal = itemVal.getItemMeta();
        if (metaVal != null) {
            metaVal.setDisplayName(passLock.getLocaleManager().getString("confirmationItem"));
            ArrayList<String> lore = new ArrayList<String>();
            lore.add(0, location.getWorld().getName());
            lore.add(1, (int) location.getX() + "");
            lore.add(2, (int) location.getY() + "");
            lore.add(3, (int) location.getZ() + "");
            metaVal.setLore(lore);
            itemVal.setItemMeta(metaVal);
        } else {
            Bukkit.getLogger().log(Level.SEVERE, "Got null item metadata... Something went wrong !");
        }
        return itemVal;
    }

    public Inventory getWatchingInventory(Block block, Player p){
        Location location = block.getLocation();
        String title = passLock.getLocaleManager().getString("watchTitle").replace("%p", passLock.getLockManager().getLockOwner(block).getName());
        Inventory inv = Bukkit.createInventory(p, 54, title);
        Chest chest = (Chest)location.getBlock().getState();
        inv.setContents(chest.getInventory().getContents());
        return inv;
    }

    public Inventory getChangingInventory(Player owner){
        Inventory inventory = Bukkit.createInventory(owner, InventoryType.HOPPER, passLock.getLocaleManager().getString("changingInventoryTitle"));

        ItemStack nullItem = new ItemStack(Material.BARRIER,1);
        ItemMeta nullMeta = nullItem.getItemMeta();
        if (nullMeta != null) {
            nullMeta.setDisplayName(passLock.getLocaleManager().getString("changingInventoryItemBarrier"));
            nullItem.setItemMeta(nullMeta);
            inventory.setItem(3, nullItem);
        }
        return inventory;
    }

    public void setChangeInventory(Inventory inv, Player owner){
        ItemStack[] items = inv.getContents();
        if (!(items[0] == null || items[1] == null || items[2] == null || items[4] == null)){
            lockingInventory.set(0,items[0].getType());
            lockingInventory.set(1,items[1].getType());
            lockingInventory.set(2,items[2].getType());
            lockingInventory.set(3,items[4].getType());
            owner.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("changedLockingInventory")));
            passLock.getConfig().set("inventory", passLock.getMaterialNames(lockingInventory));
            passLock.saveConfig();
        }else
            owner.sendMessage(passLock.formatMessage(passLock.getLocaleManager().getString("changingFailed")));
    }

    public boolean isChangingInventory(Inventory inventory, InventoryView inventoryView) {
        return inventoryView.getTitle().equals(passLock.getLocaleManager().getString("changingInventoryTitle"))
                && inventory.getType() == InventoryType.HOPPER;
    }

    public int[] getPassword(InventoryView inventoryView) {
        int[] password = {
                inventoryView.getItem(0).getAmount(),
                inventoryView.getItem(1).getAmount(),
                inventoryView.getItem(2).getAmount()};
        return password;
    }

    public boolean isBasicInventory(InventoryView inventoryView){
        return inventoryView.getTitle().equals(passLock.getLocaleManager().getString("basicInventoryTitle"));
    }

    public boolean isLockingInventory(InventoryView inventoryView){
        return inventoryView.getTitle().equals(passLock.getLocaleManager().getString("lockingInventoryTitle"));
    }

    public boolean isChangingInventory(InventoryView inventoryView){
        return inventoryView.getTitle().equals(passLock.getLocaleManager().getString("changingInventoryTitle"));
    }

    public boolean isAPasslockInventory(InventoryView inventoryView){
        return isBasicInventory(inventoryView) || isLockingInventory(inventoryView) || isChangingInventory(inventoryView);
    }

    public boolean isCodeItem(String itemStackName) {
        return itemStackName.equals(passLock.getLocaleManager().getString("codeItem"));
    }

    public boolean isBarrierItem(String itemStackName) {
        return itemStackName.equals(passLock.getLocaleManager().getString("changingInventoryItemBarrier"));
    }

    public boolean isConfirmationItem(String itemStackName) {
        return itemStackName.equals(passLock.getLocaleManager().getString("confirmationItem"));
    }

    public Block getBlockFromConfirmationItem(ItemStack confirmationItem) {
        ItemMeta itemMeta = confirmationItem.getItemMeta();
        if (itemMeta != null) {
            List<String> lore = itemMeta.getLore();
            Location location = new Location(
                    passLock.getServer().getWorld(lore.get(0)),
                    Double.parseDouble(lore.get(1)),
                    Double.parseDouble(lore.get(2)),
                    Double.parseDouble(lore.get(3)));
            return location.getBlock();
        }
        return null;
    }

}
