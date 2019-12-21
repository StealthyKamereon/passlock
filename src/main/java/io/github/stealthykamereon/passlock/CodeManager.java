package io.github.stealthykamereon.passlock;


import com.udojava.evalex.Expression;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class CodeManager {

    private PassLock main;
    private FileConfiguration dataFile, locksFile;
    private ArrayList<String> lockable;
    private ArrayList<String> lockInv;

    public String PREFIX, LOCKTITLE, BASETITLE, LOCKCOMMAND, UNLOCKCOMMAND, WRONG, RIGHT, BREAK, LOCKED, UNLOCKED,
            ALREADYLOCKED, NOTLOCKED, NOTLOCKABLE, DONTOWN, RELOAD, NOPERMISSIONS, SETLOCKABLE, UNSETLOCKABLE,
            NOWLOCKABLE, NOLONGERLOCKABLE, CHANGETITLE, CHANGEDINVENTORY, CHANGINGFAILED, LOCKINGLIMITREACHED, ROBBINGALERT,
            RESETCONFIG, NOTENOUGHMONEY, PRICEMESSAGE, PRICETITLE, CALCULATION, WATCHING, OWNERASKING, OWNERMESSAGE;

    public CodeManager(PassLock main, FileConfiguration data, FileConfiguration locks){
        this.main = main;
        this.dataFile = data;
        this.locksFile = locks;
        loadLang();
        loadLockables();
        loadInv();
    }

    public void reload(){
        loadLang();
        loadLockables();
        loadInv();
        locksFile = main.locks;
    }

    private void loadInv() {
        lockInv = new ArrayList<String>();
        List<String> inv = main.getConfig().getStringList("inventory");

        for (String item : inv){
            lockInv.add(item);
        }
    }

    public void loadLockables(){
        this.lockable = new ArrayList<String>();
        List<String> list = main.getConfig().getStringList("lockables");

        for (String item : list) {
            lockable.add(item);
        }


    }

    public void loadLang(){
        FileConfiguration config = main.getConfig();
        PREFIX = ChatColor.translateAlternateColorCodes('#',config.getString("prefix"));
        LOCKTITLE = ChatColor.translateAlternateColorCodes('#',config.getString("lockInventoryTitle"));
        BASETITLE = ChatColor.translateAlternateColorCodes('#',config.getString("baseInventoryTitle"));
        LOCKCOMMAND = ChatColor.translateAlternateColorCodes('#',config.getString("plockCommand"));
        UNLOCKCOMMAND = ChatColor.translateAlternateColorCodes('#',config.getString("punlockCommand"));
        WRONG = ChatColor.translateAlternateColorCodes('#',config.getString("wrongCode"));
        RIGHT = ChatColor.translateAlternateColorCodes('#',config.getString("rightCode"));
        BREAK = ChatColor.translateAlternateColorCodes('#',config.getString("breakMessage"));
        LOCKED = ChatColor.translateAlternateColorCodes('#',config.getString("blockLocked"));
        UNLOCKED = ChatColor.translateAlternateColorCodes('#',config.getString("blockUnlocked"));
        ALREADYLOCKED = ChatColor.translateAlternateColorCodes('#',config.getString("blockAlreadyLocked"));
        NOTLOCKED = ChatColor.translateAlternateColorCodes('#',config.getString("blockNotLocked"));
        NOTLOCKABLE = ChatColor.translateAlternateColorCodes('#',config.getString("blockNotLockable"));
        DONTOWN = ChatColor.translateAlternateColorCodes('#',config.getString("dontOwnMessage"));
        RELOAD = ChatColor.translateAlternateColorCodes('#',config.getString("reload"));
        NOPERMISSIONS = ChatColor.translateAlternateColorCodes('#',config.getString("noPermissions"));
        SETLOCKABLE = ChatColor.translateAlternateColorCodes('#', config.getString("setLockable"));
        UNSETLOCKABLE = ChatColor.translateAlternateColorCodes('#', config.getString("removeLockable"));
        NOWLOCKABLE = ChatColor.translateAlternateColorCodes('#', config.getString("nowLockable"));
        NOLONGERLOCKABLE = ChatColor.translateAlternateColorCodes('#', config.getString("noLongerLockable"));
        CHANGETITLE = ChatColor.translateAlternateColorCodes('#', config.getString("changeTitle"));
        CHANGEDINVENTORY = ChatColor.translateAlternateColorCodes('#', config.getString("changedLockingInventory"));
        CHANGINGFAILED = ChatColor.translateAlternateColorCodes('#', config.getString("changingFailed"));
        LOCKINGLIMITREACHED = ChatColor.translateAlternateColorCodes('#', config.getString("lockingLimitReached"));
        ROBBINGALERT = ChatColor.translateAlternateColorCodes('#', config.getString("robbingAlert"));
        RESETCONFIG = ChatColor.translateAlternateColorCodes('#', config.getString("resetConfig"));
        NOTENOUGHMONEY = ChatColor.translateAlternateColorCodes('#', config.getString("notEnoughMoney"));
        PRICEMESSAGE = ChatColor.translateAlternateColorCodes('#', config.getString("priceMessage"));
        PRICETITLE = ChatColor.translateAlternateColorCodes('#', config.getString("priceTitle"));
        CALCULATION = ChatColor.translateAlternateColorCodes('#', config.getString("calculation"));
        WATCHING = ChatColor.translateAlternateColorCodes('#', config.getString("watchTitle"));
        OWNERASKING = ChatColor.translateAlternateColorCodes('#', config.getString("ownerAsking"));
        OWNERMESSAGE = ChatColor.translateAlternateColorCodes('#', config.getString("ownerMessage"));
    }

    public int getLockCount(Player p){
        try{
            return Integer.parseInt(locksFile.get(p.getName()).toString());
        }catch (NullPointerException e){
            locksFile.set(p.getName(), 0);
            main.saveConfig();
            return 0;
        }
    }

    public BlockFace getDoubleChestFace(Block block){
        if (block.getRelative(BlockFace.EAST).getType() == Material.CHEST || block.getRelative(BlockFace.EAST).getType() == Material.TRAPPED_CHEST)
            return BlockFace.EAST;
        if (block.getRelative(BlockFace.NORTH).getType() == Material.CHEST || block.getRelative(BlockFace.NORTH).getType() == Material.TRAPPED_CHEST)
            return BlockFace.NORTH;
        if (block.getRelative(BlockFace.WEST).getType() == Material.CHEST || block.getRelative(BlockFace.WEST).getType() == Material.TRAPPED_CHEST)
            return BlockFace.WEST;
        if (block.getRelative(BlockFace.SOUTH).getType() == Material.CHEST || block.getRelative(BlockFace.SOUTH).getType() == Material.TRAPPED_CHEST)
            return BlockFace.SOUTH;
        return null;
    }

    public boolean isDoubleChest(Block block){
        if (block.getType()==Material.TRAPPED_CHEST || block.getType()==Material.CHEST){
            if ((block.getRelative(BlockFace.EAST).getType() == Material.CHEST || block.getRelative(BlockFace.EAST).getType() == Material.TRAPPED_CHEST)
                    || (block.getRelative(BlockFace.NORTH).getType() == Material.CHEST || block.getRelative(BlockFace.NORTH).getType() == Material.TRAPPED_CHEST)
                    || (block.getRelative(BlockFace.WEST).getType() == Material.CHEST || block.getRelative(BlockFace.WEST).getType() == Material.TRAPPED_CHEST)
                    || (block.getRelative(BlockFace.SOUTH).getType() == Material.CHEST || block.getRelative(BlockFace.SOUTH).getType() == Material.TRAPPED_CHEST))
                return true;
        }
        return false;
    }

    public void increaseLocksCount(Player p){
        int lockcount;
        try{
            lockcount = Integer.parseInt(locksFile.get(p.getName()).toString());
        }catch (NullPointerException e){
            lockcount = 0;
        }
        locksFile.set(p.getName(), lockcount+1);
    }

    public void decreaseLocksCount(Player p){
        int lockcount;
        try{
            lockcount = Integer.parseInt(locksFile.get(p.getName()).toString());
        }catch (NullPointerException e){
            lockcount = 0;
        }
        lockcount-=1;
        if (lockcount <= 0){
            lockcount = 0;
        }
        locksFile.set(p.getName(), lockcount);
    }

    private String encode(Location loc){
        return "x"+(int)loc.getX()+"y"+(int)loc.getY()+"z"+(int)loc.getZ();
    }
    private Integer[] decode(String str){
        int x = Integer.parseInt(str.substring(1,str.indexOf("y")-1));
        int y = Integer.parseInt(str.substring(str.indexOf("y"),str.indexOf("z")-1));
        int z = Integer.parseInt(str.substring(str.indexOf("z"),str.length()));
        Integer[] integers = {x,y,z};
        return integers;
    }

    public void lock(Location loc, Player owner, String pass){
        ArrayList data = new ArrayList();
        data.add(0, owner.getName());
        data.add(1, pass);
        dataFile.set(encode(loc), data);
        main.saveConfig();
    }

    public void unlock(Location loc){
        String coords = encode(loc);
        if(dataFile.contains(coords)){
            dataFile.set(coords, null);
            main.saveConfig();
        }

    }

    public boolean isOwner(Player p, Location loc){
        String coords = encode(loc);
        if(dataFile.contains(coords)){
            if (p.getName().equals(dataFile.getList(coords).get(0))){
                return true;
            }else
                return false;
        }else
            return false;
    }

    public String getOwner(Location loc){
        String coords = encode(loc);
        return (String)dataFile.getList(coords).get(0);
    }

    public String getOwner(String loc){
        return (String)dataFile.getList(loc).get(0);
    }

    public String getPass(Location loc){
        String coords = encode(loc);
        if (dataFile.contains(coords)){
            return (String) dataFile.getList(coords).get(1);
        }else
            return null;
    }

    public boolean isDoor(Material type){
        boolean isDoor = false;
        Material[] doors = {Material.WOODEN_DOOR, Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR,
                Material.JUNGLE_DOOR, Material.SPRUCE_DOOR, Material.IRON_DOOR};
        for (Material door : doors){
            if(type == door){
                isDoor = true;
            }
        }
        return isDoor;
    }

    public boolean isLockable(Material type){
        boolean isLockable = false;
        if(!(lockable==null)){
            for (String lock : this.lockable){
                if(type == Material.getMaterial(lock)){
                    isLockable = true;
                }
            }
        }

        return isLockable;
    }

    public void addLockable(Material mat){
        this.lockable.add(mat.name());
        main.getConfig().set("lockables", this.lockable);
        main.saveConfig();
    }

    public void removeLockable(Material mat){
        if (this.lockable.contains(mat.name()))
            this.lockable.remove(mat.name());
        main.getConfig().set("lockables", this.lockable);
        main.saveConfig();
    }

    public Inventory getBaseInventory(Player owner, Location loc){
        Inventory inv = Bukkit.createInventory(owner, InventoryType.HOPPER, this.BASETITLE);

        /*
        //Code
        Wool wool = new Wool(DyeColor.WHITE);
        ItemStack whiteWool = wool.toItemStack(1);
        ItemMeta whiteMeta = whiteWool.getItemMeta();
        whiteMeta.setDisplayName(ChatColor.GRAY + "Code");
        whiteWool.setItemMeta(whiteMeta);
        */


        //Code 0
        ItemStack code0 = new ItemStack(Material.getMaterial(lockInv.get(0)), 1);
        ItemMeta meta0 = code0.getItemMeta();
        meta0.setDisplayName(ChatColor.GRAY + "Code");
        code0.setItemMeta(meta0);

        //Code 1
        ItemStack code1 = new ItemStack(Material.getMaterial(lockInv.get(1)), 1);
        ItemMeta meta1 = code1.getItemMeta();
        meta1.setDisplayName(ChatColor.GRAY + "Code");
        code1.setItemMeta(meta1);

        //Code 2
        ItemStack code2 = new ItemStack(Material.getMaterial(lockInv.get(2)), 1);
        ItemMeta meta2 = code2.getItemMeta();
        meta2.setDisplayName(ChatColor.GRAY + "Code");
        code2.setItemMeta(meta2);

        //Valider
        ItemStack itemVal = new ItemStack(Material.getMaterial(lockInv.get(3)), 1);
        ItemMeta metaVal = itemVal.getItemMeta();
        metaVal.setDisplayName(ChatColor.GREEN + "Apply");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(0, (int) loc.getX() + "");
        lore.add(1, (int) loc.getY() + "");
        lore.add(2, (int) loc.getZ() + "");
        metaVal.setLore(lore);
        itemVal.setItemMeta(metaVal);

        /*
        //Item de confirmation
        wool.setColor(DyeColor.LIME);
        ItemStack confirmWool = wool.toItemStack(1);
        ItemMeta confirmMeta = confirmWool.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Apply");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(0, (int)loc.getX()+"");
        lore.add(1, (int)loc.getY()+"");
        lore.add(2, (int)loc.getZ()+"");
        confirmMeta.setLore(lore);
        confirmWool.setItemMeta(confirmMeta);
        */

        //Contenu de l'inventaire
        ItemStack[] items = {code0, code1, code2, null, itemVal};
        inv.setContents(items);
        return inv;

    }

    public Inventory getLockInventory(Player owner, Location loc){
        String title = this.LOCKTITLE;
        if(main.getConfig().getBoolean("useEconomy")){
            title+= this.PRICETITLE.replace("%c", this.getLockPrice(owner)+"");
        }
        Inventory inv = Bukkit.createInventory(owner, InventoryType.HOPPER, title);

        /*//Code
        Wool wool = new Wool(DyeColor.WHITE);
        ItemStack whiteWool = wool.toItemStack(1);
        ItemMeta whiteMeta = whiteWool.getItemMeta();
        whiteMeta.setDisplayName(ChatColor.GRAY + "Code");
        whiteWool.setItemMeta(whiteMeta);

        //Item de confirmation
        wool.setColor(DyeColor.LIME);
        ItemStack confirmWool = wool.toItemStack(1);
        ItemMeta confirmMeta = confirmWool.getItemMeta();
        confirmMeta.setDisplayName(ChatColor.GREEN + "Apply");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(0, (int)loc.getX()+"");
        lore.add(1, (int)loc.getY()+"");
        lore.add(2, (int)loc.getZ()+"");
        confirmMeta.setLore(lore);
        confirmWool.setItemMeta(confirmMeta);*/

        //Code 0
        ItemStack code0 = new ItemStack(Material.getMaterial(lockInv.get(0)), 1);
        ItemMeta meta0 = code0.getItemMeta();
        meta0.setDisplayName(ChatColor.GRAY + "Code");
        code0.setItemMeta(meta0);

        //Code 1
        ItemStack code1 = new ItemStack(Material.getMaterial(lockInv.get(1)), 1);
        ItemMeta meta1 = code1.getItemMeta();
        meta1.setDisplayName(ChatColor.GRAY + "Code");
        code1.setItemMeta(meta1);

        //Code 2
        ItemStack code2 = new ItemStack(Material.getMaterial(lockInv.get(2)), 1);
        ItemMeta meta2 = code2.getItemMeta();
        meta2.setDisplayName(ChatColor.GRAY + "Code");
        code2.setItemMeta(meta2);

        //Valider
        ItemStack itemVal = new ItemStack(Material.getMaterial(lockInv.get(3)), 1);
        ItemMeta metaVal = itemVal.getItemMeta();
        metaVal.setDisplayName(ChatColor.GREEN + "Apply");
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(0, (int) loc.getX() + "");
        lore.add(1, (int) loc.getY() + "");
        lore.add(2, (int) loc.getZ() + "");
        metaVal.setLore(lore);
        itemVal.setItemMeta(metaVal);

        //Contenu de l'inventaire
        ItemStack[] items = {code0, code1, code2, null, itemVal};
        inv.setContents(items);
        return inv;
    }

    public Inventory getChangeInventory(Player owner){
        Inventory inventory = Bukkit.createInventory(owner, InventoryType.HOPPER, this.CHANGETITLE);

        ItemStack nullItem = new ItemStack(Material.BARRIER,1);
        ItemMeta nullMeta = nullItem.getItemMeta();
        nullMeta.setDisplayName(ChatColor.RED+"This slot must stay empty");
        nullItem.setItemMeta(nullMeta);
        inventory.setItem(3, nullItem);

        return inventory;
    }

    public Inventory getWatchingInventory(Location loc, Player p){
        String title = WATCHING.replace("%p", getOwner(loc));
        Inventory inv = Bukkit.createInventory(p, 54, title);
        Chest chest = (Chest)loc.getBlock().getState();
        inv.setContents(chest.getInventory().getContents());
        return inv;
    }

    public int getLockingLimit(Player p){
        if (main.getConfig().getBoolean("lockingLimitEnabled")){
            for (PermissionAttachmentInfo perm : p.getEffectivePermissions()){
                /*if(perm.getPermission().startsWith("passlock"))
                    p.sendMessage(perm.getPermission());*/
                if (perm.getPermission().startsWith("passlock.locklimit.")){
                    return Integer.parseInt(perm.getPermission().substring(perm.getPermission().lastIndexOf(".")+1));
                }
            }
        }
        return -1;
    }

    public float getLockPrice(Player p){
        int lockcount = getLockCount(p)+1;
        String priceExpression = "0";
        for (PermissionAttachmentInfo perm : p.getEffectivePermissions()){
            if (perm.getPermission().startsWith("passlock.lockprice.")){
                priceExpression = perm.getPermission().substring(19);
            }
        }
        priceExpression = priceExpression.replace("n", lockcount+"");
        Expression exp = new Expression(priceExpression);
        BigDecimal result = exp.eval();
        return result.floatValue();
    }

    public void setChangeInventory(Inventory inv, Player owner){
        ItemStack[] items = inv.getContents();
        if (!(items[0] == null || items[1] == null || items[2] == null || items[4] == null)){
            lockInv.set(0,items[0].getType().name());
            lockInv.set(1,items[1].getType().name());
            lockInv.set(2,items[2].getType().name());
            lockInv.set(3,items[4].getType().name());
            owner.sendMessage(this.PREFIX+this.CHANGEDINVENTORY);
            main.getConfig().set("inventory", lockInv);
            main.saveConfig();
        }else
            owner.sendMessage(this.PREFIX+this.CHANGINGFAILED);
    }

    public boolean isRegistered(Location loc){
        if (dataFile.contains(encode(loc)))
            return true;
        else
            return false;
    }


}
