package com.g8e;

import com.g8e.util.Logger;

public class MemoryLogger {
    public static void logMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();

        Logger.printDebug("Used memory: " + usedMemory / (1024 * 1024) + " MB");
    }
}
