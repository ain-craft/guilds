package org.aincraft.map;

/**
 * Symbol set for guild map visualization.
 * Defines the characters used to represent different chunk claim states.
 */
public class MapSymbols {
    // Player position
    public static final String PLAYER = "@";

    // Own guild territory
    public static final String OWN_GUILD = "■";

    // Other guilds' territory
    public static final String OTHER_GUILD = "▪";

    // Wilderness / unclaimed
    public static final String WILDERNESS = "-";

    // Unknown / error state
    public static final String UNKNOWN = "?";

    private MapSymbols() {
        // Utility class
    }
}
