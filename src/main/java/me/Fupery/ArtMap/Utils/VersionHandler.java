package me.Fupery.ArtMap.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.bukkit.Material;

public class VersionHandler {


    private final BukkitVersion version;

    public VersionHandler() {
        version = checkVersion();
    }

    private static BukkitVersion checkVersion() {
        Version version = Version.getBukkitVersion();
        if (version.isLessThan(1, 14)) return BukkitVersion.v1_13;
        else if (version.isLessThan(1, 15)) return BukkitVersion.v1_14;
		else
			return BukkitVersion.v1_15;
    }

    public static BukkitVersion getLatest() {
        BukkitVersion[] handlers = BukkitVersion.values();
        return handlers[handlers.length - 1];
    }

    public BukkitVersion getVersion() {
        return version;
    }

    public enum BukkitVersion {
		UNKNOWN, v1_13, v1_14, v1_15;

        public boolean isGreaterThan(BukkitVersion version) {
            return ordinal() > version.ordinal();
        }

        public boolean isGreaterOrEqualTo(BukkitVersion version) {
            return ordinal() >= version.ordinal();
        }

        public boolean isEqualTo(BukkitVersion version) {
            return ordinal() == version.ordinal();
        }

        public boolean isLessOrEqualTo(BukkitVersion version) {
            return ordinal() <= version.ordinal();
        }

        public boolean isLessThan(BukkitVersion version) {
            return ordinal() < version.ordinal();
        }

        public float getEulerValue(Object packet, String methodName) throws NoSuchMethodException,
                InvocationTargetException, IllegalAccessException {
            Method method = packet.getClass().getMethod(methodName, float.class);
            method.setAccessible(true);
            return (float) method.invoke(packet, (float) 0);
        }

        public float getYaw(Object packet) throws NoSuchMethodException,
                IllegalAccessException, InvocationTargetException {
            return getEulerValue(packet, "a");
        }

        public float getPitch(Object packet) throws NoSuchMethodException,
                IllegalAccessException, InvocationTargetException {
            return getEulerValue(packet, "b");
        }

        public double getSeatXOffset() {
            return 1.219;
        }

        public double getSeatYOffset() {
            return -2.24979;
        }

        public Material getWallSign() {
            if( this == v1_13) {
                return Material.getMaterial("WALL_SIGN", false);
            } else {
                return Material.getMaterial("OAK_WALL_SIGN", false);
            }
        }

        public Material getSign() {
            if( this == v1_13) {
                return Material.getMaterial("SIGN", false);
            } else {
                return Material.getMaterial("OAK_SIGN", false);
            }
        }

        public Material getRedDye() {
            if( this == v1_13) {
                return Material.getMaterial("ROSE_DYE", false);
            } else {
                return Material.getMaterial("RED_DYE", false);
            }
        }

        public Material getGreenDye() {
            if( this == v1_13) {
                return Material.getMaterial("CACTUS_GREEN", false);
            } else {
                return Material.getMaterial("GREEN_DYE", false);
            }
        }

        public Material getYellowDye() {
            if( this == v1_13) {
                return Material.getMaterial("DANDELION_YELLOW", false);
            } else {
                return Material.getMaterial("YELLOW_DYE", false);
            }
        }

    }

}
