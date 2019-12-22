package io.github.stealthykamereon.passlock;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;


import java.io.*;
import java.util.logging.Level;

public class PassLock extends JavaPlugin{

    private EventListener listener;
    public static Economy economy = null;
    private CodeManager code;
    private LocaleManager localeManager;
    public FileConfiguration data, locks;
    private String helpMessage;
    private Permission permissionLock = new Permission("passlock.lock");
    private Permission permissionUnlock = new Permission("passlock.unlock");
    private Permission permissionOpen = new Permission("passlock.open");
    private Permission permissionSetLockable = new Permission("passlock.setLockable");
    private Permission permissionUnsetLockable = new Permission("passlock.unsetLockable");
    private Permission permissionChangeLockingInventory = new Permission("passlock.changeLockingInventory");
    private Permission permissionUnlockEveryLocks = new Permission("passlock.unlockEveryLocks");
    private Permission permissionReset = new Permission("passlock.resetConfig");
    private Permission permissionReload = new Permission("passlock.reload");
    private Permission permissionRecalculate = new Permission("passlock.recalculate");
    private Permission permissionWatch = new Permission("passlock.watch");

    public void onEnable(){
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (!setupEconomy())
            this.getLogger().log(Level.SEVERE, "Can't load economy !");

        File helpMessageFile = new File("plugins/PassLock/helpCommand.txt");
        if (!helpMessageFile.exists()){
            try{
                getLogger().log(Level.INFO, "Help message file not found ! Created a new one.");
                helpMessageFile.createNewFile();
                helpMessage = "#6====================#nPassLock Plugin#r#6====================\n" +
                        "#7PassLock allows you to lock your blocks thanks to a item based code.\n" +
                        "#7Use right-click and left-click to change the item's number.\n" +
                        "\n" +
                        "List of commands :\n" +
                        "#flock : #7To lock block \n" +
                        "#funlock : #7To unlock block \n" +
                        "#fhelp : #7Show this message\n" +
                        "#freload : #7Apply changes to the config and reload the plugin\n" +
                        "#fsetLockable : #7Allow to lock a type of block \n" +
                        "#funsetLockable : #7Disable the \"lockability\" of a type of block\n" +
                        "#fchangeLockingInventory : #7Change the locking window \n" +
                        "#fresetConfig : #7Reset the configuration file without restarting server\n" +
                        "#frecalculate : #7Recalculate the number of locks of every players";
                FileWriter writer = new FileWriter(helpMessageFile);
                writer.write(helpMessage);
                writer.close();
                helpMessage = ChatColor.translateAlternateColorCodes('#', helpMessage);
            }catch (IOException e){
                e.printStackTrace();
            }
        }else{
            try {
                FileReader reader = new FileReader(helpMessageFile);
                int a = reader.read();
                helpMessage = "";
                while (a!=-1){
                    helpMessage= helpMessage+(char)a;
                    a = reader.read();
                }
                helpMessage = ChatColor.translateAlternateColorCodes('#', helpMessage);
                getLogger().log(Level.INFO, "Help message file loaded !");
            }catch (FileNotFoundException e){
                e.printStackTrace();
            }catch (IOException e){
                e.printStackTrace();
            }
        }
        this.loadLocale();
        data = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "data.yml"));
        locks = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "locks.yml"));
        this.code = new CodeManager(this, data, locks);

        listener = new EventListener(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(listener, this);
        pm.addPermission(permissionLock);
        pm.addPermission(permissionUnlock);
        pm.addPermission(permissionOpen);
        pm.addPermission(permissionSetLockable);
        pm.addPermission(permissionUnsetLockable);
        pm.addPermission(permissionChangeLockingInventory);
        pm.addPermission(permissionUnlockEveryLocks);
        pm.addPermission(permissionReset);
        pm.addPermission(permissionReload);
        pm.addPermission(permissionRecalculate);
        pm.addPermission(permissionWatch);

    }

    private void loadLocale() {
        !todo
    }

    public void onDisable(){
        this.saveConfig();
        try {
            data.save(new File(getDataFolder(), "data.yml"));
            locks.save(new File(getDataFolder(), "locks.yml"));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Economy getEconomy(){
        return economy;
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> economyProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            economy = economyProvider.getProvider();
        }

        return (economy != null);
    }

    private void resetConfig(){
        File config = new File(getDataFolder().getAbsolutePath()+"\\config.yml");
        config.delete();
        saveDefaultConfig();
        reloadConfig();
        code.reload();
    }

    private void resetLockCount(){
        for (String s : locks.getKeys(false)){
            locks.set(s, 0);
        }
    }

    private void recalculateLockCounts(){
        resetLockCount();
        for (String s : data.getKeys(false)){
            String playerName = code.getOwner(s);
            Player player = this.getServer().getPlayer(playerName);
            code.increaseLocksCount(player);
        }
    }

    protected CodeManager getCodeManager(){
        return this.code;
    }

    protected LocaleManager getLocaleManager(){
        return this.localeManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage("Console commands support is not implemented, if you need it, please tell me on the spigot page.");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("passlock") && args.length!=0){
            if(args[0].equalsIgnoreCase("lock")) {
                if (sender.hasPermission("passlock.lock")){
                    Player p = (Player)sender;
                    if (getConfig().getBoolean("useEconomy"))
                        p.sendMessage(code.PREFIX+code.PRICEMESSAGE.replace("%p", code.getLockPrice(p)+""));
                    p.sendMessage(code.PREFIX+code.LOCKCOMMAND);
                    if (listener.delcommandPlayers.contains(p))
                        listener.delcommandPlayers.remove(p);
                    else if (listener.unsetlockablePlayers.contains(p))
                        listener.unsetlockablePlayers.remove(p);
                    else if (listener.setlockablePlayers.contains(p))
                        listener.setlockablePlayers.remove(p);
                    if (!listener.addcommandPlayers.contains(p))
                        listener.addcommandPlayers.add(p);
                    return true;
                }else{
                    sender.sendMessage(code.PREFIX+code.NOPERMISSIONS);
                    return true;
                }

            }else if(args[0].equalsIgnoreCase("unlock")) {
                if(sender.hasPermission("passlock.unlock")){
                    Player p = (Player) sender;
                    p.sendMessage(code.PREFIX+code.UNLOCKCOMMAND);
                    if (listener.addcommandPlayers.contains(p))
                        listener.addcommandPlayers.remove(p);
                    else if (listener.unsetlockablePlayers.contains(p))
                        listener.unsetlockablePlayers.remove(p);
                    else if (listener.setlockablePlayers.contains(p))
                        listener.setlockablePlayers.remove(p);
                    if (!listener.delcommandPlayers.contains(p))
                        listener.delcommandPlayers.add(p);
                    return true;
                }else{
                    sender.sendMessage(code.PREFIX+code.NOPERMISSIONS);
                    return true;
                }

            }else if (args[0].equalsIgnoreCase("help")){
                Player p = (Player) sender;
                p.sendMessage(helpMessage);
                return true;
            }else if (args[0].equalsIgnoreCase("reload")){
                Player p = (Player)sender;
                if (p.hasPermission("passlock.reload")){
                    reloadConfig();
                    locks = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "locks.yml"));
                    code.reload();
                    sender.sendMessage(code.PREFIX+code.RELOAD);
                    return true;
                }else{
                    p.sendMessage(code.PREFIX+code.NOPERMISSIONS);
                    return true;
                }
            }else if (args[0].equalsIgnoreCase("setLockable")) {
                Player p = (Player) sender;
                if (p.hasPermission("passlock.setLockable")) {
                    if (listener.addcommandPlayers.contains(p))
                        listener.addcommandPlayers.remove(p);
                    else if (listener.delcommandPlayers.contains(p))
                        listener.delcommandPlayers.remove(p);
                    else if (listener.unsetlockablePlayers.contains(p))
                        listener.unsetlockablePlayers.remove(p);
                    if (!listener.setlockablePlayers.contains(p))
                        listener.setlockablePlayers.add(p);
                    p.sendMessage(code.PREFIX + code.SETLOCKABLE);
                    return true;
                } else {
                    sender.sendMessage(code.PREFIX + code.NOPERMISSIONS);
                    return true;
                }
            }else if(args[0].equalsIgnoreCase("unsetLockable")) {
                Player p = (Player) sender;
                if (p.hasPermission("passlock.unsetLockable")) {
                    if (listener.addcommandPlayers.contains(p))
                        listener.addcommandPlayers.remove(p);
                    else if (listener.delcommandPlayers.contains(p))
                        listener.delcommandPlayers.remove(p);
                    else if (listener.setlockablePlayers.contains(p))
                        listener.setlockablePlayers.remove(p);
                    if (!listener.unsetlockablePlayers.contains(p))
                        listener.unsetlockablePlayers.add(p);
                    p.sendMessage(code.PREFIX + code.UNSETLOCKABLE);
                    return true;
                } else {
                    sender.sendMessage(code.PREFIX + code.NOPERMISSIONS);
                    return true;
                }
            }else if(args[0].equalsIgnoreCase("changeLockingInventory")) {
                Player p = (Player) sender;
                if (p.hasPermission("passlock.changeLockingInventory")) {
                    p.openInventory(code.getChangeInventory(p));
                    return true;
                }else {
                    p.sendMessage(code.PREFIX+code.NOPERMISSIONS);
                    return false;
                }

            }else if (args[0].equalsIgnoreCase("resetConfig")){
                Player p = (Player) sender;
                if (p.hasPermission("passlock.resetConfig")){
                    resetConfig();
                    p.sendMessage(code.PREFIX+code.RESETCONFIG);
                    return true;
                }else {
                    p.sendMessage(code.PREFIX+code.NOPERMISSIONS);
                    return false;
                }
            }else if(args[0].equalsIgnoreCase("recalculate")){
                Player p = (Player)sender;
                if (p.hasPermission("passlock.recalculate")){
                    recalculateLockCounts();
                    p.sendMessage(code.PREFIX+code.CALCULATION);
                }else
                    p.sendMessage(code.PREFIX+code.NOPERMISSIONS);
                return true;
            }else if (args[0].equalsIgnoreCase("owner")){
                Player p = (Player)sender;
                if(!listener.ownerPlayer.contains(p))
                    listener.ownerPlayer.add(p);
                p.sendMessage(code.PREFIX+code.OWNERASKING);
                return true;
            }
            else
                return false;
        }else
            return false;

    }

}
