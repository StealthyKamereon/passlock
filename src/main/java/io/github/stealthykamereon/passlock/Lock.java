package io.github.stealthykamereon.passlock;

import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Lock implements ConfigurationSerializable {

    private final OfflinePlayer owner;
    private final int[] password;
    private final Location location;

    public Lock(Player owner, int[] password, Location location) {
        this.owner = owner;
        this.password = password;
        this.location = location;
    }

    public Lock(Map<String, Object> serializedObject) {
        this.owner = (OfflinePlayer) serializedObject.get("owner");
        this.password = new int[]{0, 0, 0};
        int i = 0;
        for (Integer digit : (ArrayList<Integer>)serializedObject.get("password")) {
            this.password[i] = digit;
            i++;
        }
        this.location = (Location) serializedObject.get("location");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("owner", owner);
        serialized.put("password", password);
        serialized.put("location", location);
        return serialized;
    }

    public OfflinePlayer getOwner() {
        return owner;
    }

    public int[] getPassword() {
        return password;
    }

    public Location getLocation() {
        return location;
    }

}
