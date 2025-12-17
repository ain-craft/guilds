package org.aincraft;

/**
 * Types of relationships between guilds.
 */
public enum RelationType {
    /**
     * Allied guilds - mutual cooperation, no PVP in claimed territory.
     */
    ALLY,

    /**
     * Enemy guilds - hostile relationship, PVP enabled, cannot claim adjacent.
     */
    ENEMY,

    /**
     * Explicitly neutral - no special relationship.
     */
    NEUTRAL
}
