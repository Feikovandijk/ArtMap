package me.Fupery.ArtMap.IO.ColourMap;

import me.Fupery.ArtMap.IO.Database.Map;

import java.io.IOException;

/**
 * Compresses 32x32 pixel maps into a byte array to be stored as a SQL BLOB
 */
public class f32x32 implements MapFormatter {
    private static byte[] foldMap(byte[] mapData, int magnitude) {
        byte[] foldedData = new byte[Map.Size.STANDARD.value];
        for (int x = 0; x < 128; x += magnitude) {
            for (int y = 0; y < 128; y += magnitude) {
                foldedData[(x / magnitude) + ((y / magnitude) * 32)] = mapData[x + (y * 128)];
            }
        }
        return foldedData;
    }

    private static byte[] unfoldMap(byte[] mapData, int magnitude) {
        byte[] unfoldedData = new byte[Map.Size.MAX.value];
        for (int x = 0; x < 32; x++) {
            for (int y = 0; y < 32; y++) {
                int ix = x * magnitude;
                int iy = y * magnitude;
                for (int px = 0; px < magnitude; px++) {
                    for (int py = 0; py < magnitude; py++) {
                        unfoldedData[(px + ix) + ((py + iy) * 128)] = mapData[x + (y * 32)];
                    }
                }
            }
        }
        return unfoldedData;
    }

    @Override
    public byte[] generateBLOB(byte[] mapData) throws IOException {
        byte[] compressedData;
        if (mapData.length == Map.Size.STANDARD.value) {
            compressedData = Compressor.compress(mapData);
        } else if (mapData.length == Map.Size.MAX.value) {
            compressedData = Compressor.compress(foldMap(mapData, 4));
        } else {
            throw new IOException("Invalid MapData!");
        }
        return compressedData;
    }

    @Override
    public byte[] readBLOB(byte[] blobData) {
        byte[] decompressedData = Compressor.decompress(blobData);
        return unfoldMap(decompressedData, 4);
    }
}
