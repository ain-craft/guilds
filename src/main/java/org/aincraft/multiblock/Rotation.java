package org.aincraft.multiblock;

import org.bukkit.util.Vector;

/**
 * Rotation transformations for multiblock patterns around the Y axis.
 */
public enum Rotation {
    NONE(0),
    CLOCKWISE_90(90),
    CLOCKWISE_180(180),
    CLOCKWISE_270(270);

    private final int degrees;

    Rotation(int degrees) {
        this.degrees = degrees;
    }

    public int getDegrees() {
        return degrees;
    }

    /**
     * Rotates a relative vector around the Y axis.
     *
     * @param v the vector to rotate
     * @return rotated vector (new instance)
     */
    public Vector rotate(Vector v) {
        return switch (this) {
            case NONE -> new Vector(v.getX(), v.getY(), v.getZ());
            case CLOCKWISE_90 -> new Vector(-v.getZ(), v.getY(), v.getX());
            case CLOCKWISE_180 -> new Vector(-v.getX(), v.getY(), -v.getZ());
            case CLOCKWISE_270 -> new Vector(v.getZ(), v.getY(), -v.getX());
        };
    }

    /**
     * Mirrors a vector on the X axis.
     *
     * @param v the vector to mirror
     * @return mirrored vector (new instance)
     */
    public static Vector mirror(Vector v) {
        return new Vector(-v.getX(), v.getY(), v.getZ());
    }
}
