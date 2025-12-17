package org.aincraft.util;

import net.kyori.adventure.text.format.NamedTextColor;

/**
 * Utility for converting color names and hex codes to canonical hex format.
 * All colors are stored and resolved as hex strings (#RRGGBB).
 */
public class ColorConverter {
    private static final int HEX_COLOR_LENGTH = 7; // #RRGGBB format
    private static final int HEX_PREFIX_LENGTH = 1; // # prefix
    private static final int HEX_RADIX = 16; // Hexadecimal base

    private ColorConverter() {
        // Utility class
    }

    /**
     * Converts a color input (named or hex) to canonical hex format.
     *
     * @param colorInput the input color (named color like 'red' or hex like '#FF0000')
     * @return the hex color in format #RRGGBB, or null if invalid
     */
    public static String toHex(String colorInput) {
        if (colorInput == null) {
            return null;
        }

        String color = colorInput.toLowerCase().trim();

        // Already in hex format
        if (color.startsWith("#")) {
            if (isValidHex(color)) {
                return color.toUpperCase();
            }
            return null;
        }

        // Try to resolve as named color
        NamedTextColor namedColor = NamedTextColor.NAMES.value(color);
        if (namedColor != null) {
            return rgbToHex(namedColor.value());
        }

        return null;
    }

    /**
     * Checks if a string is a valid hex color.
     *
     * @param hex the hex string to validate
     * @return true if valid (#RRGGBB format)
     */
    public static boolean isValidHex(String hex) {
        if (hex == null || !hex.startsWith("#") || hex.length() != HEX_COLOR_LENGTH) {
            return false;
        }
        try {
            Integer.parseInt(hex.substring(HEX_PREFIX_LENGTH), HEX_RADIX);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if a string is a valid color (named or hex).
     *
     * @param color the color string to validate
     * @return true if valid
     */
    public static boolean isValidColor(String color) {
        if (color == null) {
            return false;
        }

        String normalized = color.toLowerCase().trim();

        // Check if hex
        if (normalized.startsWith("#")) {
            return isValidHex(normalized);
        }

        // Check if named color
        return NamedTextColor.NAMES.value(normalized) != null;
    }

    /**
     * Converts RGB integer to hex string.
     *
     * @param rgb the RGB value
     * @return hex string in format #RRGGBB
     */
    private static String rgbToHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Gets the display name for a hex color.
     * If it matches a named color, returns the name. Otherwise returns the hex.
     *
     * @param hex the hex color string
     * @return display name for the color
     */
    public static String getDisplayName(String hex) {
        if (hex == null) {
            return "None";
        }

        if (!isValidHex(hex)) {
            return hex;
        }

        try {
            int rgbValue = Integer.parseInt(hex.substring(HEX_PREFIX_LENGTH), HEX_RADIX);
            for (NamedTextColor named : NamedTextColor.NAMES.values()) {
                if (named.value() == rgbValue) {
                    return named.toString();
                }
            }
        } catch (NumberFormatException e) {
            // Fall through to return hex
        }

        return hex;
    }
}
