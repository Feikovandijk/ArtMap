package me.Fupery.ArtMap.Compatability;

import com.wasteofplastic.askyblock.ASkyBlockAPI;
import com.wasteofplastic.askyblock.Island;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import me.Fupery.ArtMap.Easel.EaselEvent;

class ASkyBlockCompat implements RegionHandler {

    private final boolean loaded;

    public ASkyBlockCompat() {
        loaded = Bukkit.getPluginManager().isPluginEnabled("ASkyBlock");
    }

    @Override
    public boolean checkBuildAllowed(Player player, Location location) {
        Island island = ASkyBlockAPI.getInstance().getIslandAt(location);
        return island == null
                || (island.getOwner() == player.getUniqueId()
                || island.getMembers().contains(player.getUniqueId())
						|| island.getIgsFlag(Island.SettingsFlag.PLACE_BLOCKS));
    }

    @Override
    public boolean checkInteractAllowed(Player player, Entity entity, EaselEvent.ClickType click) {
        Island island = ASkyBlockAPI.getInstance().getIslandAt(entity.getLocation());
        return island == null
                || (island.getOwner() == player.getUniqueId()
                || island.getMembers().contains(player.getUniqueId())
						|| island.getIgsFlag(Island.SettingsFlag.ARMOR_STAND));
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }
}
