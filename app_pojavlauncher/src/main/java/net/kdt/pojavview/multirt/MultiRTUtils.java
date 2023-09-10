package net.kdt.pojavview.multirt;

import static net.kdt.pojavview.Tools.NATIVE_LIB_DIR;
import static org.apache.commons.io.FileUtils.listFiles;

import android.system.Os;
import android.util.Log;

import net.kdt.pojavview.R;
import net.kdt.pojavview.Tools;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class MultiRTUtils {
    private static final String JAVA_VERSION_STR = "JAVA_VERSION=\"";
    private static final String OS_ARCH_STR = "OS_ARCH=\"";

    public static void installRuntimeNamed(String path, InputStream runtimeInputStream) throws IOException {
        File dest = new File(path);
        if (dest.exists()) FileUtils.deleteDirectory(dest);
        uncompressTarXZ(runtimeInputStream, dest);
        runtimeInputStream.close();
        unpack200(Tools.NATIVE_LIB_DIR, path + "/");
        MultiRTUtils.postPrepare(path);
    }

    public static void uncompressTarXZ(final InputStream tarFileInputStream, final File dest) throws IOException {
        if(dest.isFile()) throw new IOException("Attempting to unpack into a file");
        if(!dest.exists() && !dest.mkdirs()) throw new IOException("Failed to create destination directory");

        byte[] buffer = new byte[8192];
        TarArchiveInputStream tarIn = new TarArchiveInputStream(
                new XZCompressorInputStream(tarFileInputStream)
        );
        TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
        // tarIn is a TarArchiveInputStream
        while (tarEntry != null) {

            final String tarEntryName = tarEntry.getName();

            File destPath = new File(dest, tarEntry.getName());
            File destParent = destPath.getParentFile();
            if (tarEntry.isSymbolicLink()) {
                if(destParent != null && !destParent.exists() && !destParent.mkdirs())
                    throw new IOException("Failed to create parent directory for symlink");
                try {
                    // android.system.Os
                    // Libcore one support all Android versions
                    Os.symlink(tarEntry.getName(), tarEntry.getLinkName());
                } catch (Throwable e) {
                    Log.e("MultiRT", e.toString());
                }

            } else if (tarEntry.isDirectory()) {
                if(!destPath.exists() && !destPath.mkdirs())
                    throw new IOException("Failed to create directory");
            } else if (!destPath.exists() || destPath.length() != tarEntry.getSize()) {
                if(destParent != null && !destParent.exists() && !destParent.mkdirs())
                    throw new IOException("Failed to create parent directory for file");

                FileOutputStream os = new FileOutputStream(destPath);
                IOUtils.copyLarge(tarIn, os, buffer);
                os.close();

            }
            tarEntry = tarIn.getNextTarEntry();
        }
        tarIn.close();
    }

    public static void postPrepare(String path) throws IOException {
        File dest = new File(path);
        if (!dest.exists()) return;
        Runtime runtime = read(path);
        if(runtime == null) return;
        String libFolder = "lib";
        if (new File(dest, libFolder + "/" + runtime.arch).exists()) libFolder = libFolder + "/" + runtime.arch;
        File ftIn = new File(dest, libFolder + "/libfreetype.so.6");
        File ftOut = new File(dest, libFolder + "/libfreetype.so");
        if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
            if (!ftIn.renameTo(ftOut)) throw new IOException("Failed to rename freetype");
        }

        // Refresh libraries
        copyDummyNativeLib("libawt_xawt.so", dest, libFolder);
    }

    public static Runtime read(String path) {
        File release = new File(path, "release");
        if (!release.exists()) {
            return null;
        }
        try {
            String content = Tools.read(release.getAbsolutePath());
            String javaVersion = Tools.extractUntilCharacter(content, JAVA_VERSION_STR, '"');
            String osArch = Tools.extractUntilCharacter(content, OS_ARCH_STR, '"');
            if (javaVersion != null && osArch != null) {
                String[] javaVersionSplit = javaVersion.split("\\.");
                int javaVersionInt;
                if (javaVersionSplit[0].equals("1")) {
                    javaVersionInt = Integer.parseInt(javaVersionSplit[1]);
                } else {
                    javaVersionInt = Integer.parseInt(javaVersionSplit[0]);
                }
                return new Runtime(javaVersion, osArch, path, javaVersionInt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Unpacks all .pack files into .jar Serves only for java 8, as java 9 brought project jigsaw
     * @param nativeLibraryDir The native lib path, required to execute the unpack200 binary
     * @param runtimePath The path to the runtime to walk into
     */
    private static void unpack200(String nativeLibraryDir, String runtimePath) {

        File basePath = new File(runtimePath);
        Collection<File> files = listFiles(basePath, new String[]{"pack"}, true);

        File workdir = new File(nativeLibraryDir);

        ProcessBuilder processBuilder = new ProcessBuilder().directory(workdir);
        for (File jarFile : files) {
            try {
                Process process = processBuilder.command("./libunpack200.so", "-r", jarFile.getAbsolutePath(), jarFile.getAbsolutePath().replace(".pack", "")).start();
                process.waitFor();
            } catch (InterruptedException | IOException e) {
                Log.e("MULTIRT", "Failed to unpack the runtime !");
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private static void copyDummyNativeLib(String name, File dest, String libFolder) throws IOException {
        File fileLib = new File(dest, "/"+libFolder + "/" + name);
        FileInputStream is = new FileInputStream(new File(NATIVE_LIB_DIR, name));
        FileOutputStream os = new FileOutputStream(fileLib);
        IOUtils.copy(is, os);
        is.close();
        os.close();
    }
}
