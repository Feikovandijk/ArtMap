package me.Fupery.ArtMap.Recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map.Entry;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.meta.ItemMeta;

import me.Fupery.ArtMap.Config.Lang;

public class CustomItem {
    private final String key;
    private final Material material;
    private String name = null;
    private String[] tooltip = new String[0];
    private ItemFlag[] itemFlags = new ItemFlag[0];
    private HashMap<Enchantment, Integer> enchants = new HashMap<>();
    private int amount = 1;
    private SimpleRecipe recipe = null;
	protected Optional<ItemStack>			stack		= Optional.empty();

    public CustomItem(Material material, String uniqueKey) {
        this.material = material;
        this.key = uniqueKey;
    }

    public CustomItem(Material material, String key, String name) {
        this.material = material;
        this.key = key;
        this.name = name;
    }

    public CustomItem(Material material, String key, String... tooltip) {
        this.material = material;
        this.key = key;
        this.tooltip = tooltip;
    }

    public CustomItem(Material material, String key, String name, String... tooltip) {
        this.material = material;
        this.key = key;
        this.name = name;
        this.tooltip = tooltip;
    }

	public CustomItem(ItemStack stack, String key) {
		this.stack = Optional.of(stack);
		this.material = stack.getType();
		this.key = key;
	}

    public CustomItem name(String name) {
        this.name = name;
        return this;
    }

    public CustomItem name(Lang name) {
        this.name = name.get();
        return this;
    }

    public CustomItem tooltip(String... tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    public CustomItem tooltip(Lang.Array tooltip) {
        this.tooltip = tooltip.get();
        return this;
    }

    public CustomItem amount(int amount) {
        this.amount = amount;
        return this;
    }

    public CustomItem enchant(Enchantment enchantment, int level) {
        enchants.put(enchantment, level);
        return this;
    }

    public CustomItem flag(ItemFlag... itemFlags) {
        this.itemFlags = itemFlags;
        return this;
    }

    public CustomItem recipe(SimpleRecipe recipe) {
        this.recipe = recipe;
        return this;
    }

    public Recipe getBukkitRecipe() {
        return getRecipe().toBukkitRecipe(toItemStack());
    }

    public SimpleRecipe getRecipe() {
        return recipe;
    }

    public void addRecipe() {
        if (getRecipe() != null) Bukkit.getServer().addRecipe(getBukkitRecipe());
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }

    protected String getKey() {
        return key;
    }

    public boolean checkItem(ItemStack itemStack) {
        if (itemStack != null
                && itemStack.getType() == material
		        && itemStack.hasItemMeta()) {
            ItemMeta itemMeta = itemStack.getItemMeta();
            if (itemMeta.hasLore() && itemMeta.getLore().get(0).contains(key)) {
                return true;
            }
        }
        return false;
    }

    public ItemStack toItemStack() {
		// get the stack or create a new one.
		ItemStack item = stack.isPresent() ? stack.get() : new ItemStack(material, amount);
        ItemMeta meta = item.getItemMeta();
        if(meta == null) {
            return null;
        }
        if (name != null) meta.setDisplayName(name);
        List<String> lore = new ArrayList<>();
        lore.add(key);
        if (tooltip.length > 0) Collections.addAll(lore, tooltip);
        meta.setLore(lore);
        if (itemFlags.length > 0) meta.addItemFlags(itemFlags);
        if (enchants.size() > 0) {
            for (Entry<Enchantment,Integer> e : enchants.entrySet()) {
                meta.addEnchant(e.getKey(), e.getValue(), true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder(14, 293);
        builder.append(key);
        return builder.toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CustomItem)) return false;
        CustomItem item = (CustomItem) obj;
        return key.equals(item.key);
    }
}
