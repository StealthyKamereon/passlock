package io.github.stealthykamereon.passlock;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.logging.Level;

public class LocaleManager {

    private FileConfiguration localeFile;

    private HashMap<String, String> locale;

    public LocaleManager(FileConfiguration localeFile){
        this.localeFile = localeFile;
        this.locale = new HashMap<>();
        this.loadLocale();
    }

    private void loadLocale(){
        for (String key : localeFile.getKeys(false)){
            locale.put(key, loadLocaleEntry(key));
        }
    }

    private String loadLocaleEntry(String field){
        String parameter = localeFile.getString(field);
        if (parameter != null){
            return ChatColor.translateAlternateColorCodes('#', parameter);
        } else {
            Bukkit.getLogger().log(Level.SEVERE, String.format("Field {%s} not found in the locale file !", field));
        }
        return "";
    }

    public String getString(String key){
        return locale.get(key);
    }
}
