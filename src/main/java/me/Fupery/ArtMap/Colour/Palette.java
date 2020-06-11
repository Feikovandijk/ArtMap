package me.Fupery.ArtMap.Colour;

import org.bukkit.inventory.ItemStack;

public interface Palette {

    /**
     * @param item The itemstack to check
     * @return the corresponding dye colour, or null if the item is not a valid dye.
     */
    ArtDye getDye(ItemStack item);

    /**
	 * @param dyeType The type of the dye.
	 * @return a list of all possible dyes in this palette
	 */
    ArtDye[] getDyes(DyeType dyeType);

    BasicDye getDefaultColour();
}
