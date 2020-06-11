package me.Fupery.ArtMap.Colour;

import java.util.logging.Level;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Painting.Pixel;

public abstract class ArtDye {
    private final String localizedName;
    private final String englishName;   //Minecraft servers do not like localized names as recipe keys
    private final ChatColor chatColour;
    private Material material;

    /**
     * Durability value of -1 indicates that items of any durability will be accepted
     */
	protected ArtDye(String localizedName, String englishName, ChatColor chatColor, Material material) {
		if (englishName == null) {
			ArtMap.instance().getLogger().log(Level.SEVERE,
					"Dye with material: " + material + " does not have a name set!");
		}
        this.localizedName = localizedName;
        this.englishName = englishName;
        this.chatColour = chatColor;
        this.material = material;
    }

    public abstract void apply(Pixel pixel);

    public abstract byte getDyeColour(byte currentPixelColour);

    public String name() {
        return chatColour + localizedName;
    }

    public String rawName() {
        return localizedName;
    }

    public String englishName() {
        return englishName.toUpperCase();
    }


    public ChatColor getDisplayColour() {
        return chatColour;
    }

    public Material getMaterial() {
        return material;
    }

    public ItemStack toItem() {
		ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(chatColour + localizedName);
        item.setItemMeta(meta);
        return item;
    }
}
