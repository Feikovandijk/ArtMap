package me.Fupery.ArtMap.Painting;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Config.Lang;
import me.Fupery.ArtMap.Easel.Easel;
import me.Fupery.ArtMap.Event.PlayerMountEaselEvent;
import me.Fupery.ArtMap.IO.Database.Map;
import me.Fupery.ArtMap.Painting.Brushes.Dropper;
import me.Fupery.ArtMap.Painting.Brushes.Dye;
import me.Fupery.ArtMap.Painting.Brushes.Fill;
import me.Fupery.ArtMap.Painting.Brushes.Flip;
import me.Fupery.ArtMap.Recipe.ArtItem;

public class ArtSession {
    private final CanvasRenderer canvas;
    private final Brush DYE;
    private final Brush FILL;
    private final Brush FLIP;
    private final Brush DROPPER;
    private final Easel easel;
    private final Map map;
    private Brush currentBrush;
    private long lastStroke;
    private ItemStack[] inventory;
    private static final HashMap<UUID, ItemStack[]> artkitHotbars = new HashMap<>();

    private boolean active = false;
    private boolean dirty = true;
    private int artkitPage = 0;

    ArtSession(Player player, Easel easel, Map map, int yawOffset) {
        this.easel = easel;
        canvas = new CanvasRenderer(map, yawOffset);
        currentBrush = null;
        lastStroke = System.currentTimeMillis();
        DYE = new Dye(canvas, player);
        DROPPER = new Dropper(canvas, player);
        FILL = new Fill(canvas, player, (Dropper) DROPPER);
        FLIP = new Flip(canvas, player);
        this.map = map;
    }

    boolean start(Player player) throws SQLException, IOException {
        PlayerMountEaselEvent event = new PlayerMountEaselEvent(player, easel);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        boolean seated = easel.seatUser(player);
        if (!seated) {
            return false;
        }

        // Run tasks
        try {
            ArtMap.instance().getArtDatabase().restoreMap(map);
            ArtMap.instance().getScheduler().SYNC.runLater(() -> {
                if (player.getVehicle() != null)
                    Lang.ActionBar.PAINTING.send(player);
            }, 30);
            if (ArtMap.instance().getConfiguration().FORCE_ART_KIT && player.hasPermission("artmap.artkit")) {
                addKit(player);
            }
            map.setRenderer(canvas);
            persistMap(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            player.sendMessage("Error restoring painting! Check server logs for more details!");
            ArtMap.instance().getLogger().log(Level.SEVERE, "Error restoring painting on easel.", e);
            event.setCancelled(true);
            return false;
        }
        return true;
    }

    void paint(ItemStack brush, Brush.BrushAction action) {
        if (!dirty)
            dirty = true;
        if (currentBrush == null || !currentBrush.checkMaterial(brush)) {
            if (currentBrush != null)
                currentBrush.clean();
            currentBrush = getBrushType(brush);
        }
        if (currentBrush == null || canvas.isOffCanvas())
            return;

        long currentTime = System.currentTimeMillis();
        long strokeTime = currentTime - lastStroke;
        if (strokeTime > currentBrush.getCooldown()) {
            currentBrush.paint(action, brush, strokeTime);
        }
        lastStroke = System.currentTimeMillis();
    }

    private Brush getBrushType(ItemStack item) {
        for (Brush brush : new Brush[] { DYE, FILL, FLIP, DROPPER }) {
            if (brush.checkMaterial(item)) {
                return brush;
            }
        }
        return null;
    }

    void updatePosition(float yaw, float pitch) {
        canvas.setYaw(yaw);
        canvas.setPitch(pitch);
    }

    private void addKit(Player player) {
        PlayerInventory inventory = player.getInventory();
        /*
         * ItemStack leftOver = inventory.addItem(inventory.getItemInOffHand()).get(0);
         * inventory.setItemInOffHand(new ItemStack(Material.AIR)); if (leftOver != null
         * && leftOver.getType() != Material.AIR)
         * player.getWorld().dropItemNaturally(player.getLocation(), leftOver);
         */
        this.inventory = inventory.getContents();
        inventory.setStorageContents(ArtItem.getArtKit(0));
        // restore hotbar
        if (artkitHotbars.containsKey(player.getUniqueId())) {
            ItemStack[] hotbar = artkitHotbars.get(player.getUniqueId());
            for (int i = 0; i < 9; i++) {
                player.getInventory().setItem(i, hotbar[i]);
            }
            player.getInventory().setItemInOffHand(hotbar[9]);
        }
    }

    public void nextKitPage(Player player) {
        this.artkitPage++;
        this.setKitPage(player, this.artkitPage);
    }

    public void prevKitPage(Player player) {
        if (this.artkitPage > 0) {
            this.artkitPage--;
            this.setKitPage(player, this.artkitPage);
        }
    }

    /*
     * Set the contents of the inventory without replacing the hotkey bar.
     */
    private void setKitPage(Player player, int page) {
        ItemStack[] kit = ArtItem.getArtKit(this.artkitPage);
            ItemStack[] current = player.getInventory().getStorageContents();
            System.arraycopy(current, 0, kit, 0, 9);
            player.getInventory().setStorageContents(kit);
    }

    public boolean removeKit(Player player) {
        if (inventory == null) {
            return false;
        }
        // save hotbar + offhand
        ItemStack[] hotbar = new ItemStack[10];
        for (int i = 0; i < 9; i++) {
            hotbar[i] = player.getInventory().getItem(i);
        }
        hotbar[9] = player.getInventory().getItemInOffHand();
        artkitHotbars.put(player.getUniqueId(), hotbar);
        // clear item on cursor
        player.getOpenInventory().setCursor(null);
        
        player.getInventory().setContents(inventory);
        inventory = null;
        return true;
    }

    /**
     * Clear a players hotbar save. For instance on logout.
     * 
     * @param player The player to remove.
     */
    public static void clearHotbar(Player player) {
        artkitHotbars.remove(player.getUniqueId());
    }

    /**
     * @return True if the artsession has the artkit in use.
     */
    public boolean isInArtKit() {
        return this.inventory != null;
    }

    public Easel getEasel() {
        return easel;
    }

    void end(Player player) throws SQLException, IOException {
        try {
            player.leaveVehicle();
            removeKit(player);
            easel.removeUser();
            canvas.stop();
            persistMap(true);
            active = false;
        } catch(Exception e) {
            player.sendMessage("Error saving painting on easel. Check logs for more details.");
            ArtMap.instance().getLogger().log(Level.SEVERE, "Error saving painting on easel.", e);
        }
        // todo map renderer getting killed after save
    }

    public void persistMap(boolean resetRenderer) throws SQLException, IOException, NoSuchFieldException,
            IllegalAccessException {
        if (!dirty) return; //no caching required
        byte[] mapData = canvas.getMap();
        map.setMap(mapData, resetRenderer);
        ArtMap.instance().getArtDatabase().saveInProgressArt(this.map, mapData);
        dirty = false;
    }

    boolean isActive() {
        return active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }

    void sendMap(Player player) {
        if (dirty) map.update(player);
    }
}
