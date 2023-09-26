package net.kdt.pojavlaunch.forgedisplay;

public interface IForgeUpdate {
    void forgeUpdate(int type, boolean have, String title, String message, int step,
                     int steps, int maxMemory, int totalMemory, int freeMemory);
}
