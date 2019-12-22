package io.github.stealthykamereon.passlock;


import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.Door;
import org.bukkit.material.Openable;
import org.bukkit.material.TrapDoor;
import org.bukkit.material.Wool;
import org.bukkit.permissions.Permission;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EventListener implements Listener{

    private PassLock main;
    private Logger log;
    public List<Player> addcommandPlayers;
    public List<Player> delcommandPlayers;
    public List<Player> setlockablePlayers;
    public List<Player> unsetlockablePlayers;
    public List<Player> watchingPlayer;
    public List<Player> ownerPlayer;
    private CodeManager codeManager;
    private DyeColor[] colors = {DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK, DyeColor.RED,
            DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.LIGHT_BLUE, DyeColor.CYAN,
            DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.BROWN};
    private Economy economy;

    public EventListener(PassLock main){
        this.main = main;
        codeManager = main.getCodeManager();
        log = main.getLogger();
        addcommandPlayers = new ArrayList<Player>();
        delcommandPlayers = new ArrayList<Player>();
        setlockablePlayers = new ArrayList<Player>();
        unsetlockablePlayers = new ArrayList<Player>();
        watchingPlayer = new ArrayList<Player>();
        ownerPlayer = new ArrayList<Player>();
        economy = main.getEconomy();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e){
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK){ //Clic droit
            Block clickedBlock = e.getClickedBlock();
            Material type = clickedBlock.getType();
            Location loc = clickedBlock.getLocation();
            Player p = e.getPlayer();

            if(codeManager.isLockable(type)){ //Lockable

                if(codeManager.isDoor(type)){
                    Door door = (Door)clickedBlock.getState().getData();
                    if (door.isTopHalf()){
                        clickedBlock = clickedBlock.getRelative(BlockFace.DOWN);
                        loc = clickedBlock.getLocation();
                    }
                }

                if (codeManager.isDoubleChest(clickedBlock)){
                    if (codeManager.isRegistered(clickedBlock.getRelative(codeManager.getDoubleChestFace(clickedBlock)).getLocation())){
                        BlockFace face = codeManager.getDoubleChestFace(clickedBlock);
                        clickedBlock = clickedBlock.getRelative(face);
                        loc = clickedBlock.getLocation();
                    }
                }

                if (codeManager.isRegistered(loc)){ //Enregistré

                    if (addcommandPlayers.contains(p)){ //Commande /passlock lock et enregistré
                        p.sendMessage(codeManager.PREFIX+codeManager.ALREADYLOCKED);
                        addcommandPlayers.remove(p);
                        e.setCancelled(true);
                        return;
                    }else if (delcommandPlayers.contains(p)){ //Commande /passlock unlock et enregistré
                        if (codeManager.isOwner(p, loc) || p.hasPermission("passlock.unlockEveryLocks")){ //Possède le lock
                            codeManager.unlock(loc);
                            p.sendMessage(codeManager.PREFIX+codeManager.UNLOCKED);
                            codeManager.decreaseLocksCount(p);
                            if (main.getConfig().getBoolean("useEconomy") && main.getConfig().getBoolean("unlockingPaysBack")){
                                economy.depositPlayer(p, codeManager.getLockPrice(p));
                            }

                            e.setCancelled(true);

                        }else{ //Ne possède pas le lock
                            p.sendMessage(codeManager.PREFIX+codeManager.DONTOWN);
                            e.setCancelled(true);

                        }
                        delcommandPlayers.remove(p);
                        return;
                    }else if(ownerPlayer.contains(p)){
                        e.setCancelled(true);
                        Player owner = main.getServer().getPlayer(codeManager.getOwner(clickedBlock.getLocation()));
                        String mes = codeManager.OWNERMESSAGE.replace("%p", owner.getDisplayName());
                        p.sendMessage(codeManager.PREFIX+mes);
                        ownerPlayer.remove(p);
                        return;
                    }
                    else{ //Pas de commande
                        if (p.hasPermission("passlock.watch") && !codeManager.isOwner(p, loc)) {
                            try {
                                p.openInventory(codeManager.getWatchingInventory(loc, p));
                                watchingPlayer.add(p);
                                e.setCancelled(true);
                                return;
                            } catch (ClassCastException cce) {}
                        }
                        if(codeManager.isOwner(p, loc) && !main.getConfig().getBoolean("ownerNeedCode")) {
                            e.setCancelled(false);
                            return;
                        }else if (p.hasPermission("passlock.open")){ //Permission d'ouvrir
                            if (codeManager.isDoor(type)){
                                Door door = (Door)clickedBlock.getState().getData();
                                if(door.isOpen()){
                                    e.setCancelled(false);
                                    return;
                                }
                            }else if(type == Material.OAK_TRAPDOOR){
                                TrapDoor trap = (TrapDoor)clickedBlock.getState().getData();
                                if (trap.isOpen()){
                                    e.setCancelled(false);
                                    return;
                                }
                            }
                            p.openInventory(codeManager.getBaseInventory(p, loc));
                            e.setCancelled(true);
                        }else{ //Pas de permission
                            p.sendMessage(codeManager.PREFIX+codeManager.NOPERMISSIONS);
                            e.setCancelled(true);
                            return;
                        }

                    }
                }else{ //Pas enregistré

                    if (addcommandPlayers.contains(p)){ //Commande /passlock lock
                        e.setCancelled(true);
                        addcommandPlayers.remove(p);
                        if (main.getConfig().getBoolean("useEconomy")){
                            if (!economy.has(p,codeManager.getLockPrice(p))){
                                p.sendMessage(codeManager.PREFIX+codeManager.NOTENOUGHMONEY);
                                return;
                            }
                        }
                        if (main.getConfig().getBoolean("lockingLimitEnabled") && codeManager.getLockCount(p) >= codeManager.getLockingLimit(p)){
                            p.sendMessage(codeManager.PREFIX+codeManager.LOCKINGLIMITREACHED+ " ("+ codeManager.getLockCount(p) +"/"+ codeManager.getLockingLimit(p) +")");
                            return;
                        }else{
                            p.openInventory(codeManager.getLockInventory(p, loc));
                            return;
                        }
                        /*if (codeManager.getCurrentLocks(p) < codeManager.getLockingLimit(p)){
                            p.openInventory(codeManager.getLockInventory(p, loc));
                            addcommandPlayers.remove(p);
                            if (main.getConfig().getBoolean("lockingLimitEnabled"))
                                codeManager.increaseLocksCount(p);
                            e.setCancelled(true);
                        }else{
                            p.sendMessage(codeManager.PREFIX+codeManager.LOCKINGLIMITREACHED+ " ("+ codeManager.getCurrentLocks(p) +"/"+ codeManager.getLockingLimit(p) +")");
                            addcommandPlayers.remove(p);
                            e.setCancelled(true);
                        }*/
                    }else if (delcommandPlayers.contains(p)){ //Commande /passlock unlock
                        p.sendMessage(codeManager.PREFIX+codeManager.NOTLOCKED);
                        delcommandPlayers.remove(p);
                        e.setCancelled(true);
                    }else if(ownerPlayer.contains(p)){
                        p.sendMessage(codeManager.PREFIX+codeManager.NOTLOCKED);
                        ownerPlayer.remove(p);
                        e.setCancelled(true);
                    }
                }
            }else{ //Pas lockable
                if (addcommandPlayers.contains(p)){ //Commande /passlock lock
                    addcommandPlayers.remove(p);
                    p.sendMessage(codeManager.PREFIX+codeManager.NOTLOCKABLE);
                    e.setCancelled(true);
                }else if (delcommandPlayers.contains(p)){ //Commande /passlock unlock
                    delcommandPlayers.remove(p);
                    p.sendMessage(codeManager.PREFIX+codeManager.NOTLOCKABLE);
                    e.setCancelled(true);
                }else if (ownerPlayer.contains(p)){
                    ownerPlayer.remove(p);
                    p.sendMessage(codeManager.PREFIX+codeManager.NOTLOCKABLE);
                    e.setCancelled(true);
                }
            }

            if (setlockablePlayers.contains(p)){
                codeManager.addLockable(type);
                e.setCancelled(true);
                setlockablePlayers.remove(p);
                p.sendMessage(codeManager.PREFIX + codeManager.NOWLOCKABLE);
            }

            if (unsetlockablePlayers.contains(p)){
                codeManager.removeLockable(type);
                e.setCancelled(true);
                unsetlockablePlayers.remove(p);
                p.sendMessage(codeManager.PREFIX + codeManager.NOLONGERLOCKABLE);
            }

        }
    }

    @EventHandler
    public void onHopperTakeItem(InventoryMoveItemEvent e){
        try {
            Location loc = null;
            InventoryHolder holder = e.getSource().getHolder();
            if (holder instanceof BlockState) {
                loc = ((BlockState) holder).getLocation();
            } else
                return;
            if (e.getDestination().getType().equals(InventoryType.HOPPER) && codeManager.isRegistered(loc)) {
                e.setCancelled(true);
            }
        }catch (NullPointerException ex){
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e){
        Block block = e.getBlock();
        Location loc = block.getLocation();

        if (codeManager.isDoubleChest(block)){
            if (codeManager.isRegistered(block.getRelative(codeManager.getDoubleChestFace(block)).getLocation())){
                BlockFace face = codeManager.getDoubleChestFace(block);
                block = block.getRelative(face);
                loc = block.getLocation();
            }

        }

        if (codeManager.isRegistered(loc)){
            if(codeManager.isOwner(e.getPlayer(), loc) && !main.getConfig().getBoolean("ownerNeedCode")){
                codeManager.unlock(loc);
                e.setCancelled(false);
            }else{
                e.getPlayer().sendMessage(codeManager.PREFIX+codeManager.BREAK);
                e.setCancelled(true);
            }

        }else {

            if (codeManager.isDoor(block.getRelative(BlockFace.UP).getType()) &&
                    codeManager.isRegistered(block.getRelative(BlockFace.UP).getLocation())){
                if (codeManager.isOwner(e.getPlayer(), block.getRelative(BlockFace.UP).getLocation()) && !main.getConfig().getBoolean("ownerNeedCode")){
                    codeManager.unlock(block.getRelative(BlockFace.UP).getLocation());
                    e.setCancelled(false);
                }else{
                    e.getPlayer().sendMessage(codeManager.PREFIX+codeManager.BREAK);
                    e.setCancelled(true);
                }

            }else if (codeManager.isDoor(block.getRelative(BlockFace.DOWN).getType()) &&
                    codeManager.isRegistered(block.getRelative(BlockFace.DOWN).getLocation())){
                if (codeManager.isOwner(e.getPlayer(), block.getRelative(BlockFace.DOWN).getLocation()) && !main.getConfig().getBoolean("ownerNeedCode")){
                    codeManager.unlock(block.getRelative(BlockFace.DOWN).getLocation());
                    e.setCancelled(false);
                }else{
                    e.getPlayer().sendMessage(codeManager.PREFIX+codeManager.BREAK);
                    e.setCancelled(true);
                }
            }

        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e){
        try {
            Player p = (Player)e.getWhoClicked();
            if (watchingPlayer.contains(p)){
                e.setCancelled(true);
                return;
            }
            if ((e.getCursor() != null && e.getCurrentItem() != null) && (e.getView().getTitle()) ==
                    codeManager.BASETITLE || e.getView().getTitle().startsWith(codeManager.LOCKTITLE) ||
                    e.getView().getTitle() == codeManager.CHANGETITLE){
                Inventory inv;
                ItemStack clicked;
                ItemMeta clickedMeta;
                String name;

                try{
                    p = (Player)e.getWhoClicked();
                    inv = e.getInventory();
                    clicked = e.getCurrentItem();
                    clickedMeta = clicked.getItemMeta();
                    name = clickedMeta.getDisplayName();
                }catch (NullPointerException exception){
                    return;
                }

                if(name.equals(ChatColor.RED+"This slot must stay empty") && e.getView().getTitle() == codeManager.CHANGETITLE){
                    e.setCancelled(true);

                }


                //si l'inventaire est celui du code
                if(e.getView().getTitle() == codeManager.BASETITLE){
                    if (name.equals(ChatColor.GRAY + "Code")){
                        e.setCancelled(true);
                        if (e.getClick().isLeftClick() && clicked.getAmount()<main.getConfig().getInt("itemMaxAmount")){
                            clicked.setAmount(clicked.getAmount()+1);
                            e.setCurrentItem(clicked);
                        }
                        if (e.getClick().isRightClick()){
                            if (!(clicked.getAmount() == 1))
                                clicked.setAmount(clicked.getAmount()-1);
                            clicked.setItemMeta(clickedMeta);
                            e.setCurrentItem(clicked);
                        }


                        //Si le joueur clique sur l'item de confirmation
                    }else if(name.equals(ChatColor.GREEN + "Apply")){

                        String code = "";

                        ItemStack[] items = {inv.getItem(0),inv.getItem(1),inv.getItem(2)};

                        for (ItemStack item : items){
                            code += item.getAmount()+",";
                        }

                        List<String> lore = clickedMeta.getLore();
                        Location loc = new Location(p.getWorld(),
                                Float.parseFloat(lore.get(0)), Float.parseFloat(lore.get(1)), Float.parseFloat(lore.get(2)));

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
                                p.closeInventory();
                        }else{
                            p.sendMessage(codeManager.PREFIX+codeManager.WRONG);
                            if (main.getConfig().getBoolean("ownerAlerted")){
                                Player owner = main.getServer().getPlayer(codeManager.getOwner(loc));
                                if (!owner.equals(p))
                                    owner.sendMessage(codeManager.PREFIX+codeManager.ROBBINGALERT.replace("%player", p.getDisplayName())
                                        .replace("%block", loc.getBlock().getType().name()).replace("%location", loc.getBlockX()+","+loc.getBlockY()+","+loc.getBlockZ()));
                            }
                        }

                        e.setCancelled(true);
                    }

                //Si l'inventaire est celui du verrouillage
                }else if (e.getView().getTitle().startsWith(codeManager.LOCKTITLE)){
                    if (name.equals (ChatColor.GRAY + "Code")){
                        e.setCancelled(true);

                        if (e.getClick().isLeftClick() && clicked.getAmount()<main.getConfig().getInt("itemMaxAmount")){
                            clicked.setAmount(clicked.getAmount()+1);
                            e.setCurrentItem(clicked);
                        }
                        if (e.getClick().isRightClick()){
                            if (!(clicked.getAmount() == 1))
                                clicked.setAmount(clicked.getAmount()-1);
                            clicked.setItemMeta(clickedMeta);
                            e.setCurrentItem(clicked);
                        }


                        //Si le joueur clique sur la laine verte
                    }else if(name.equals (ChatColor.GREEN + "Apply")){
                        e.setCancelled(true);

                        if (main.getConfig().getBoolean("useEconomy")){
                            if (economy.has(p, codeManager.getLockPrice(p)))
                                economy.withdrawPlayer(p, codeManager.getLockPrice(p));
                            else{
                                p.closeInventory();
                                p.sendMessage(codeManager.PREFIX+codeManager.NOTENOUGHMONEY);
                                return;
                            }
                        }

                        String code = "";

                        ItemStack[] items = {inv.getItem(0),inv.getItem(1),inv.getItem(2)};

                        for (ItemStack item : items){
                            code += item.getAmount()+",";
                        }

                        List<String> lore = clickedMeta.getLore();
                        codeManager.lock(new Location(p.getWorld(), Double.parseDouble(lore.get(0)),
                                Double.parseDouble(lore.get(1)), Double.parseDouble(lore.get(2))), p, code);
                        p.sendMessage(codeManager.PREFIX+codeManager.LOCKED);
                        p.closeInventory();
                        codeManager.increaseLocksCount(p);
                    }
                }
            }
        } catch (NullPointerException ex){
            ex.printStackTrace();
        }

    }

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

}
