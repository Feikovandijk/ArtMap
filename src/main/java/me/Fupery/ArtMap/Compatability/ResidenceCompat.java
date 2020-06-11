package me.Fupery.ArtMap.Compatability;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.containers.Flags;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;
import com.bekvon.bukkit.residence.protection.FlagPermissions;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Easel.EaselEvent;
import me.Fupery.ArtMap.Utils.Version;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

class ResidenceCompat implements RegionHandler {

    private boolean loaded = false;
    private Residence plugin;
    private Flags artmapPlace;
    private Flags artmapUse;

    public ResidenceCompat() {
        plugin = ((Residence) Bukkit.getPluginManager().getPlugin("Residence"));
        Version version = new Version(plugin);
        if (version.isLessThan(4, 5, 13, 0)) {
            ArtMap.instance().getLogger().warning(String.format("Invalid Residence version: " +
                    "'%s'. ArtMap requires version 4.5.13.0 or above.", version.toString()));
            loaded = false;
            plugin = null;
            return;
        }
        artmapPlace = Flags.getFlag("artmap-place");
        artmapUse = Flags.getFlag("artmap-use");
        FlagPermissions.addFlag(artmapPlace);
        FlagPermissions.addFlag(artmapUse);
        loaded = true;
    }

    @Override
    public boolean checkBuildAllowed(Player player, Location location) {
        ClaimedResidence residence = plugin.getResidenceManager().getByLoc(location);
        if (residence == null) return true;
        FlagPermissions perms = plugin.getPermsByLoc(location);
        return perms.playerHas(player, artmapPlace, false);
    }

    @Override
    public boolean checkInteractAllowed(Player player, Entity entity, EaselEvent.ClickType click) {
        ClaimedResidence residence = plugin.getResidenceManager().getByLoc(entity.getLocation());
        if (residence == null) return true;
        FlagPermissions perms = plugin.getPermsByLoc(entity.getLocation());
        return perms.playerHas(player, artmapUse, false);
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }
}
