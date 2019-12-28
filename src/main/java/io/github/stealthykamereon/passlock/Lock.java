package io.github.stealthykamereon.passlock;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class Lock implements ConfigurationSerializable {

    private Player owner;
    private int[] password;
    private Location location;

    public Lock(Player owner, int[] password, Location location){
        this.owner = owner;
        this.password = password;
        this.location = location;
    }

    public Lock(Map<String, Object> serializedObject){
        this.owner = (Player)serializedObject.get("owner");
        this.password = (int[])serializedObject.get("password");
        this.location = (Location)serializedObject.get("location");
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> serialized = new HashMap<>();
        serialized.put("owner", owner);
        serialized.put("password", password);
        serialized.put("location", location);
        return serialized;
    }

    public Player getOwner() {
        return owner;
    }

    public int[] getPassword() {
        return password;
    }

    public Location getLocation() {
        return location;
    }
}
