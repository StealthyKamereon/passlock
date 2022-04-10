package io.github.stealthykamereon.passlock;

import io.github.stealthykamereon.passlock.command.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;

public class PassLock extends JavaPlugin{

    private EventListener listener;
    private Map<Player, io.github.stealthykamereon.passlock.command.Command> commandMap;
    public static Economy economy = null;
    private LocaleManager localeManager;
    private LockManager lockManager;
    private LockableManager lockableManager;
    private InventoryManager inventoryManager;
    private WorldInteractor worldInteractor;
    private List<String> helpMessage;
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
        //Generate config if missing
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (!setupEconomy())
            this.getLogger().log(Level.SEVERE, "Can't load economy !");

        this.loadResources();
        this.loadHelp();

        this.loadLocale();
        this.lockableManager = new LockableManager(this);
        this.lockManager = new LockManager(this);
        this.inventoryManager = new InventoryManager(this);
        this.worldInteractor = new WorldInteractor();
        commandMap = new HashMap<>();

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

    private void loadResources() {
        generateFile("helpCommand.txt");
        generateDirectory("locales");
        generateFile("locales/en_EN.yml");
        generateFile("locales/fr_FR.yml");
    }

    private void loadHelp() {
        try {
            helpMessage = Files.readAllLines(Paths.get(getDataFolder().getAbsolutePath()+"/helpCommand.txt"), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
            helpMessage = new ArrayList<String>();
            helpMessage.add("#l#cError loading help message");
        }
    }

    private void generateDirectory(String name) {
        File file = new File(getDataFolder().getAbsolutePath()+"/"+name);
        file.mkdir();
    }

    private void generateFile(String filename) {
        File file = new File(this.getDataFolder().getAbsolutePath()+"/"+filename);
        try {
            if (file.createNewFile()) {
                FileOutputStream outputStream = new FileOutputStream(file);
                InputStream inputStream = this.getResource(filename);
                int read = -1;
                while ((read = inputStream.read()) != -1)
                    outputStream.write(read);
                outputStream.flush();
                outputStream.close();
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLocale() {
        String localeFilePath = "/locales/"+getConfig().getString("locale")+".yml";
        File localeFile = new File(getDataFolder().getAbsolutePath()+localeFilePath);
        if (!localeFile.exists()) {
            getLogger().log(Level.SEVERE, String.format("Locale not found : %s", getConfig().getString(localeFilePath)));
        }
        FileConfiguration locale = YamlConfiguration.loadConfiguration(localeFile);
        this.localeManager = new LocaleManager(locale);
    }

    public void onDisable(){
        this.saveConfig();
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
        File config = new File(getDataFolder().getAbsolutePath()+"/config.yml");
        config.delete();
        saveDefaultConfig();
        reloadConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
        if(!(sender instanceof Player)){
            sender.sendMessage("Console commands support is not implemented, if you need it, please tell me on the spigot page.");
            return true;
        }
        Player p = (Player)sender;
        boolean hasPermission = true;
        boolean commandSuccessful = true;

        if (!cmd.getName().equalsIgnoreCase("passlock") || args.length==0)
            return false;

        String command = args[0].toLowerCase();
        switch (command) {
            case "lock":
                if (sender.hasPermission("passlock.lock")){
                    if (getConfig().getBoolean("useEconomy"))
                        p.sendMessage(formatMessage(localeManager.getString("priceMessage").replace("%p", lockManager.getLockPrice(p)+"")));
                    p.sendMessage(formatMessage(localeManager.getString("lockCommand")));
                    commandMap.put(p, new CommandLock(this));
                }else
                    hasPermission = false;
                break;
            case "unlock":
                if(sender.hasPermission("passlock.unlock")){
                    commandMap.put(p, new CommandUnlock(this));
                    p.sendMessage(formatMessage(localeManager.getString("unlockCommand")));
                }else
                    hasPermission = false;
                break;
            case "reload":
                if (p.hasPermission("passlock.reload")){
                    reloadConfig();
                    p.sendMessage(formatMessage(localeManager.getString("reload")));
                }else
                    hasPermission = false;
                break;
            case "setlockable":
                if (p.hasPermission("passlock.setLockable")) {
                    commandMap.put(p, new CommandSetLockable(this));
                    p.sendMessage(formatMessage(localeManager.getString("setLockable")));
                } else
                    hasPermission = false;
                break;
            case "unsetlockable":
                if (p.hasPermission("passlock.unsetLockable")) {
                    commandMap.put(p, new CommandUnsetLockable(this));
                    p.sendMessage(formatMessage(localeManager.getString("removeLockable")));
                } else
                    hasPermission = false;
                break;
            case "changelockinginventory":
                if (p.hasPermission("passlock.changeLockingInventory")) {
                    p.openInventory(inventoryManager.getChangingInventory(p));
                }else
                    hasPermission = false;
                break;
            case "resetconfig":
                if (p.hasPermission("passlock.resetConfig")){
                    resetConfig();
                    p.sendMessage(formatMessage(localeManager.getString("resetConfig")));
                }else
                    hasPermission = false;
                break;
            case "owner":
                commandMap.put(p, new CommandOwner(this));
                p.sendMessage(formatMessage(localeManager.getString("ownerAsking")));
                break;
            case "help":
                for (String line : helpMessage) {
                    p.sendMessage(ChatColor.translateAlternateColorCodes('#', line));
                }
                break;
            default:
                commandSuccessful = false;
        }
        if (!hasPermission)
            p.sendMessage(formatMessage(localeManager.getString("noPermissions")));
        return commandSuccessful;
    }

    public String formatMessage(String message){
        return localeManager.getString("prefix")+message;
    }

    public List<String> getMaterialNames(List<Material> materials){
        List<String> materialNames = new LinkedList<>();
        for (Material mat : materials)
            materialNames.add(mat.toString());
        return materialNames;
    }


    public LockManager getLockManager() {
        return this.lockManager;
    }

    public WorldInteractor getWorldInteractor() {
        return this.worldInteractor;
    }

    public LockableManager getLockableManager() {
        return lockableManager;
    }

    public LocaleManager getLocaleManager(){
        return this.localeManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public Map<Player, io.github.stealthykamereon.passlock.command.Command> getCommandMap() {
        return commandMap;
    }
}
