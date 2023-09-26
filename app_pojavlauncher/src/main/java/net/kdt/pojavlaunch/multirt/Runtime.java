package net.kdt.pojavlaunch.multirt;

import java.util.Objects;

public class Runtime {
    public final String versionString;
    public final String arch;
    public final int javaVersion;
    public final String path;

    public Runtime(String versionString, String arch, String path, int javaVersion) {
        this.versionString = versionString;
        this.arch = arch;
        this.javaVersion = javaVersion;
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Runtime runtime = (Runtime) o;
        return path.equals(runtime.path);
    }
    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}