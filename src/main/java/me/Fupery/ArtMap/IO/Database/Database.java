package me.Fupery.ArtMap.IO.Database;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.Config.Lang;
import me.Fupery.ArtMap.Easel.Canvas;
import me.Fupery.ArtMap.Easel.Canvas.CanvasCopy;
import me.Fupery.ArtMap.Exception.DuplicateArtworkException;
import me.Fupery.ArtMap.Exception.PermissionException;
import me.Fupery.ArtMap.IO.CompressedMap;
import me.Fupery.ArtMap.IO.MapArt;

/**
 * Database entry point for interacting with saved artwork.
 */
public final class Database {
    private final ArtTable artworks;
    private final MapTable maps;
    private BukkitTask autosaveTask;
    private final Runnable AUTO_SAVE = new Runnable() {
        @Override
        public void run() {
            for (UUID uuid : ArtMap.instance().getArtistHandler().getArtists()) {
                try {
                    ArtMap.instance().getArtistHandler().getCurrentSession(uuid).persistMap(false);
                } catch (SQLException | IOException | NoSuchFieldException | IllegalAccessException e) {
                    ArtMap.instance().getLogger().log(Level.SEVERE,"Error saving artwork!",e);
                }
            }
        }
    };

    public Database(Plugin plugin) throws SQLException {
        SQLiteDatabase database;
        database = new SQLiteDatabase(new File(plugin.getDataFolder(), "Art.db"));
        database.initialize(artworks = new ArtTable(database), maps = new MapTable(database));
        int delay = ArtMap.instance().getConfiguration().ARTWORK_AUTO_SAVE;
        this.autosaveTask = ArtMap.instance().getScheduler().ASYNC.runTimer(AUTO_SAVE , delay, delay);
    }

    /**
     * Retrieve artwork by its title.
     * 
     * @param title The title of the artwork.
     * @return The artwork or null if it is not in the database.
     * @throws SQLException
     */
    public MapArt getArtwork(String title) throws SQLException {
        return artworks.getArtwork(title);
    }

    /**
     * Retrieve artwork by its title.
     * 
     * @param id The ID of the artwork.
     * @return The artwork or null if it is not in the database.
     * @throws SQLException
     */
    public MapArt getArtwork(int id) throws SQLException {
        return artworks.getArtwork(id);
    }

    /**
     * Retrieve the compressed map of the artwork
     * 
     * @param id The id of the artwork.
     * @return The compressed map or null if it is not found.
     * @throws SQLException
     */
    public CompressedMap getArtworkCompressedMap(int id) throws SQLException {
        return maps.getMap(id);
    }

    /**
     * Save a completed piece of art. This method will also update a previous piece
     * of art if the title matches the previous piece.
     * 
     * @param art    The artwork to save.
     * @param title  The title to the artwork to save.
     * @param player The player saving the artwork.
     * @return A copy of the saved artwork or null if the save failed.
     * @throws DuplicateArtworkException
     * @throws PermissionException
     * @throws SQLException
     * @throws IOException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public MapArt saveArtwork(Canvas art, String title, Player player) throws DuplicateArtworkException,
            PermissionException, SQLException, IOException, NoSuchFieldException, IllegalAccessException {
		// handle update case or all ready used name
		MapArt mapArt = this.getArtwork(title);
		if (mapArt != null) { // same name
			if (art instanceof Canvas.CanvasCopy) {
				CanvasCopy copy = CanvasCopy.class.cast(art);
				if (copy.getOriginalId() == mapArt.getMapId()) {
					if (mapArt.getArtist().equals(player.getUniqueId()) || player.isOp()
							|| player.hasPermission("artmap.admin")) {
						// update
						MapView newView = ArtMap.getMap(art.getMapId());
						// Force update of map data
						mapArt.getMap().setMap(ArtMap.instance().getReflection().getMap(newView), true);
						// Update database
						CompressedMap map = CompressedMap.compress(copy.getOriginalId(), newView);
						maps.updateMap(map);
						this.deleteInProgressArt(new Map(copy.getMapId())); // recycle the copy
						return mapArt;
					}
                    throw new PermissionException(Lang.NO_PERM.get());
				}
			} else {
				throw new DuplicateArtworkException(Lang.TITLE_USED.get());
			}
		}
		// new artwork
		mapArt = new MapArt(art.getMapId(), title, player.getUniqueId(),player.getName(),new Date());
		MapView mapView = ArtMap.getMap(art.getMapId());
		CompressedMap map = CompressedMap.compress(mapView);
		artworks.addArtwork(mapArt);
		if (maps.containsMap(map.getId())) {
			maps.updateMap(map);
		} else {
			maps.addMap(map);
		}
		return mapArt;
    }
    
    /**
     * Save artwork but fail if all ready present. This method is used when
     * importing artwork.
     * 
     * @param art  The artwork to save.
     * @param cMap The compressed map of the artwork data.
     * @return True if the artwork was saved. False if it all ready exists.
     * @throws DuplicateArtworkException
     * @throws SQLException
     */
    public void saveArtwork(MapArt art, CompressedMap cMap) throws DuplicateArtworkException, SQLException {
        if (maps.containsMap(cMap.getId())) {
			throw new DuplicateArtworkException(MessageFormat.format("Map with ID {0} all ready exists.",cMap.getId()));
		} else {
			maps.addMap(cMap);
		}
        artworks.addArtwork(art);
    }

    /**
     * Delete an artwork from the database.
     * 
     * @param art The piece of art to delete.
     * @return True if deleted false otherwise.
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public void deleteArtwork(MapArt art) throws SQLException, NoSuchFieldException, IllegalAccessException {
        artworks.deleteArtwork(art.getTitle());
        maps.deleteMap(art.getMapId());
        art.getMap().setMap(new byte[Map.Size.MAX.value]);
    }

    /**
     * Rename an artwork.
     * 
     * @param art   The artwork to rename.
     * @param title The new title for the artwork.
     * @return True if successful. False otherwise.
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public void renameArtwork(MapArt art, String title)
            throws SQLException, NoSuchFieldException, IllegalAccessException {
		artworks.renameArtwork(art, title);
		art.getMap().setMap(art.getMap().readData(), true);
    }

    /**
     * Check if the database contains the provided piece of art.
     * 
     * @param artwork     The artwork to check.
     * @param ignoreMapId Whether to ignore the map id when checking if the artwork
     *                    is present.
     * @return True if the artwork is in the datbase. False otherwise.
     * @throws SQLException
     */
    public boolean containsArtwork(MapArt artwork, boolean ignoreMapId) throws SQLException {
        return artworks.containsArtwork(artwork, ignoreMapId);
    }

    /**
     * Check if the database contains an Artwork matching the provided mapId.
     * 
     * @param mapId The mapId to check.
     * @return True if the database contains artwork matching the provided ID. False
     *         if it does not.
     * @throws SQLException
     */
    public boolean containsArtwork(int mapId) throws SQLException {
        return artworks.containsMapID(mapId);
    }

    /**
     * List artwork for the provided artist.
     * 
     * @param artist The UUID of the artisst to lookup.
     * @return A list of the artwork for the artist.
     * @throws SQLException
     */
    public MapArt[] listMapArt(UUID artist) throws SQLException {
        return artworks.listMapArt(artist);
    }

    /**
     * List all artwork.
     * 
     * @return A array of all artwrok in the database.
     * @throws SQLException
     */
    public MapArt[] listMapArt() throws SQLException {
        return artworks.listMapArt();
    }

    /**
     * List Artists placing the provided player first.
     * 
     * @param player The artist which is first.
     * @return List of artists.
     * @throws SQLException
     */
    public UUID[] listArtists(UUID player) throws SQLException {
        return artworks.listArtists(player);
    }

    /**
     * List all Artists in the database.
     * 
     * @return List of artists.
     * @throws SQLException
     */
    public UUID[] listArtists() throws SQLException {
		return artworks.listArtists();
	}

    /**
     * Prepare to close the database.
     */
    public void close() {
        this.autosaveTask.cancel();
    }

    //Not assuming that createMap is threadsafe
    /**
     * Create a new Map and initialize it.
     * 
     * @return The newly created Map.
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public synchronized Map createMap() throws NoSuchFieldException, IllegalAccessException {
        //This is where you would lookup unused map ids
        MapView mapView = null;
        World world = Bukkit.getServer().getWorld(ArtMap.instance().getConfiguration().WORLD);
        if (world != null) {
            mapView = Bukkit.getServer().createMap(world);
        } else {
            ArtMap.instance().getLogger()
                    .severe("MapView creation Failed! World is null! Please check that the world: option is set correctly in config.yml");
        }
        
		if (mapView == null) {
            ArtMap.instance().getLogger().severe("MapView creation Failed!");
            return null;
		}
        ArtMap.instance().getReflection().setWorldMap(mapView, Map.BLANK_MAP);
        return new Map(mapView);
    }

    /**
     * Save an inprogress piece of art to the database.
     * 
     * @param map  The artwork to save.
     * @param data The map data to save.
     * @throws SQLException
     * @throws IOException
     */
    public void saveInProgressArt(Map map, byte[] data) throws SQLException, IOException {
        CompressedMap compressedMap = CompressedMap.compress(map.getMapId(), data);
        if (maps.containsMap(map.getMapId())) {
            maps.updateMap(compressedMap);
        } else {
            maps.addMap(compressedMap);
        } 
    }

    /**
     * Delete an inprogress piece artwork and clear its data.
     * 
     * @param map The artwork to delete.
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public void deleteInProgressArt(Map map) throws SQLException, NoSuchFieldException, IllegalAccessException {
        //double check we are not deleting a saved artwork with this method
        if(artworks.getArtwork(map.getMapId()) == null) {
            map.setMap(Map.BLANK_MAP);
            maps.deleteMap(map.getMapId());
            //idQueue.offer(map.getMapId());
        }
    }

    /**
     * Restore an artwork from the database if it is found to be corrupted. Used in
     * cases were something has happened to damage or delete an artwork's map data
     * file.
     * 
     * @param map The map to restore.
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    public void restoreMap(Map map) throws SQLException, NoSuchFieldException, IllegalAccessException {
        byte[] data = map.readData();
        int oldMapHash = Arrays.hashCode(data);
        if (maps.containsMap(map.getMapId())
                && maps.getHash(map.getMapId()) != oldMapHash) {
            ArtMap.instance().getLogger().info("Map ID:" + map.getMapId() + " is corrupted! Restoring data file...");
            map.setMap(maps.getMap(map.getMapId()).decompressMap(), true);
        }
    }
}
