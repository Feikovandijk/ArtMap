package me.Fupery.ArtMap.Painting;

import me.Fupery.ArtMap.ArtMap;
import me.Fupery.ArtMap.IO.Database.Map;
import me.Fupery.ArtMap.IO.PixelTableManager;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class CanvasRenderer extends MapRenderer {
    // todo refine producer/consumer pattern
    private final int resolutionFactor;
    private final int axisLength;
    private final int maxUpdate;
    private final Map map;
    private byte[][] pixelBuffer;
    private ConcurrentLinkedQueue<byte[]> dirtyPixels;
    private AtomicBoolean active;
    private Cursor cursor;

    CanvasRenderer(Map map, int yawOffset) {
        this.map = map;
        PixelTableManager pixelTable = ArtMap.instance().getPixelTable();
        if (pixelTable == null) {
            resolutionFactor = 0;
            axisLength = 0;
            maxUpdate = 0;
            return;
        }
        resolutionFactor = pixelTable.getResolutionFactor();
        axisLength = 128 / resolutionFactor;
        maxUpdate = 16384;// TODO: 22/09/2016 magic value
        loadMap();
        cursor = new Cursor(yawOffset);
        active = new AtomicBoolean(true);
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {

        if (dirtyPixels == null || pixelBuffer == null || !active.get() || dirtyPixels.peek() == null) {
            return;
        }
        for (int i = 0; i < maxUpdate; i++) {
            byte[] pixel = dirtyPixels.poll();
            if (pixel == null) return;
            int px = pixel[0] * resolutionFactor;
            int py = pixel[1] * resolutionFactor;

            for (int x = 0; x < resolutionFactor; x++) {
                for (int y = 0; y < resolutionFactor; y++) {
                    canvas.setPixel(px + x, py + y, pixelBuffer[pixel[0]][pixel[1]]);
                }
            }
        }
    }

    //adds pixel at location
    void addPixel(int x, int y, byte colour) {
        pixelBuffer[x][y] = colour;
        dirtyPixels.add(new byte[]{((byte) x), ((byte) y)});
    }

    byte getPixel(int x, int y) {
        return pixelBuffer[x][y];
    }

    //finds the corresponding pixel for the yaw & pitch clicked
    byte[] getCurrentPixel() {
        byte[] pixel = new byte[2];

        pixel[0] = ((byte) cursor.getX());
        pixel[1] = ((byte) cursor.getY());

        for (byte b : pixel) {

            if (b >= axisLength || b < 0) {
                return null;
            }
        }
        return pixel;
    }

    byte[] getMap() {
        byte[] colours = new byte[128 * 128];
        boolean wasActive = active.compareAndSet(true, false);
        for (int x = 0; x < (axisLength); x++) {
            for (int y = 0; y < (axisLength); y++) {

                int ix = x * resolutionFactor;
                int iy = y * resolutionFactor;

                for (int px = 0; px < resolutionFactor; px++) {
                    for (int py = 0; py < resolutionFactor; py++) {
                        colours[(px + ix) + ((py + iy) * 128)] = pixelBuffer[x][y];
                    }
                }
            }
        }
        if (wasActive) active.set(true);
        return colours;
    }

    final private void loadMap() {
        byte[] colours = map.readData();

        pixelBuffer = new byte[axisLength][axisLength];
        dirtyPixels = new ConcurrentLinkedQueue<>();

        int px, py;
        for (int x = 0; x < 128; x++) {

            for (int y = 0; y < 128; y++) {

                px = x / resolutionFactor;
                py = y / resolutionFactor;
                addPixel(px, py, colours[x + (y * 128)]);
            }
        }
    }

    void stop() {
        active.set(false);
        dirtyPixels.clear();
        cursor = null;
    }

    public Pixel getPixelAt(int x, int y) {
        return new Pixel(this, x, y, getPixel(x, y));
    }

    boolean isDirty() {
        return dirtyPixels.size() > 0;
    }

    boolean isOffCanvas() {
        return cursor.isOffCanvas();
    }

    byte[][] getPixelBuffer() {
        return pixelBuffer.clone();
    }

    void setYaw(float yaw) {
        cursor.setYaw(yaw);
    }

    void setPitch(float pitch) {
        cursor.setPitch(pitch);
    }

    int getAxisLength() {
        return axisLength;
    }

    MapView getMapView() {
        return map.getMap();
    }
}