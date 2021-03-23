package io.github.stealthykamereon.passlock;

import io.github.stealthykamereon.passlock.command.commandexecutor.*;
import io.github.stealthykamereon.passlock.command.eventcommand.*;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class PassLock extends JavaPlugin {

    private EventListener listener;
    private Map<Player, EventCommand> commandMap;
    public static Economy economy = null;
    private LocaleManager localeManager;
    private LockManager lockManager;
    private LockableManager lockableManager;
    private InventoryManager inventoryManager;

    public void onEnable() {
        //Generate config if missing
        getConfig().options().copyDefaults(true);
        saveConfig();

        if (!setupEconomy())
            this.getLogger().log(Level.SEVERE, "Can't load economy !");

        this.loadResources();

        this.loadLocale();
        this.lockableManager = new LockableManager(this);
        ConfigurationSerialization.registerClass(Lock.class);
        this.lockManager = new LockManager(this);
        this.inventoryManager = new InventoryManager(this);
        commandMap = new HashMap<>();

        this.getCommand("lock").setExecutor(new LockCommand(this));
        this.getCommand("unlock").setExecutor(new UnlockCommand(this));
        this.getCommand("owner").setExecutor(new OwnerCommand(this));
        this.getCommand("setLockable").setExecutor(new SetLockableCommand(this));
        this.getCommand("unsetLockable").setExecutor(new UnsetLockableCommand(this));
        this.getCommand("changeLockingInventory").setExecutor(new ChangeLockingInventoryCommand(this));
        this.getCommand("reset").setExecutor(new ResetCommand(this));
        this.getCommand("reload").setExecutor(new ReloadCommand(this));

        listener = new EventListener(this);
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(listener, this);

    }

    private void loadResources() {
        generateDirectory("locales");
        generateFile("locales/en_EN.yml");
        generateFile("locales/fr_FR.yml");
    }

    private void generateDirectory(String name) {
        File file = new File(getDataFolder().getAbsolutePath() + "/" + name);
        file.mkdir();
    }

    private void generateFile(String filename) {
        File file = new File(this.getDataFolder().getAbsolutePath() + "/" + filename);
        try {
            if (file.createNewFile()) {
                FileOutputStream outputStream = new FileOutputStream(file);
                InputStream inputStream = this.getResource(filename);
                if (inputStream == null) {
                    getLogger().severe(String.format("Can't generate file %s !", filename));
                    return;
                }
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
        String localeFilePath = "/locales/" + getConfig().getString("locale") + ".yml";
        File localeFile = new File(getDataFolder().getAbsolutePath() + localeFilePath);
        if (!localeFile.exists()) {
            getLogger().log(Level.SEVERE, String.format("Locale not found : %s", getConfig().getString(localeFilePath)));
        }
        FileConfiguration locale = YamlConfiguration.loadConfiguration(localeFile);
        this.localeManager = new LocaleManager(locale);
    }

    public void onDisable() {
        this.saveConfig();
        getLockManager().saveLocks();
    }

    public Economy getEconomy() {
        return economy;
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }

    public Location deserialiseLocation(String string) {
        String[] fields = string.split(":");
        return new Location(getServer().getWorld(fields[0]),
                Double.parseDouble(fields[1]),
                Double.parseDouble(fields[2]),
                Double.parseDouble(fields[3]));
    }

    public String serialiseLocation(Location location) {
        StringBuilder sb = new StringBuilder();
        sb.append(location.getWorld().getName());
        sb.append(":");
        sb.append(location.getX());
        sb.append(":");
        sb.append(location.getY());
        sb.append(":");
        sb.append(location.getZ());
        return sb.toString();
    }

    public void resetConfig() {
        File config = new File(getDataFolder().getAbsolutePath() + "/config.yml");
        config.delete();
        saveDefaultConfig();
        reloadConfig();
    }

    public void sendMessage(Player player, String messageID) {
        player.sendMessage(formatMessage(localeManager.getString(messageID)));
    }

    public void sendMessage(Player player, String messageID, String... parseArguments) {
        String message = localeManager.getString(messageID);
        for (int i = 0; i < parseArguments.length; i = i + 2) {
            message = message.replace(parseArguments[i], parseArguments[i + 1]);
        }
        player.sendMessage(formatMessage(message));
    }

    public String formatMessage(String message) {
        return localeManager.getString("prefix") + message;
    }

    public List<String> getMaterialNames(List<Material> materials) {
        List<String> materialNames = new LinkedList<>();
        for (Material mat : materials)
            materialNames.add(mat.name());
        return materialNames;
    }

    public LockManager getLockManager() {
        return this.lockManager;
    }

    public LockableManager getLockableManager() {
        return lockableManager;
    }

    public LocaleManager getLocaleManager() {
        return this.localeManager;
    }

    public InventoryManager getInventoryManager() {
        return inventoryManager;
    }

    public Map<Player, EventCommand> getCommandMap() {
        return commandMap;
    }

}
