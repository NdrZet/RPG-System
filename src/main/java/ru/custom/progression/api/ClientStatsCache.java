package ru.custom.progression.api;

/**
 * A static cache on the client side to hold state for mixins (like inverted controls or fake health).
 */
public class ClientStatsCache {
    public static boolean fakeHealthEnabled = false;
    public static int fakeHealthTimerMs = 0;
    
    public static boolean controlsInverted = false;
    public static int invertTimerMs = 0;
    
    // activeTelegraphs list can be added here later
}
