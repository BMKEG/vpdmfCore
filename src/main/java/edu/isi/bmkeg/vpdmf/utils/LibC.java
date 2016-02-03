package edu.isi.bmkeg.vpdmf.utils;

import com.sun.jna.Library;
import com.sun.jna.Native;

public class LibC {
    private static CLibrary libc = (CLibrary) Native.loadLibrary("c", CLibrary.class);

    public static int chmod(String path, int mode) {
        return libc.chmod(path, mode);
    }

    public static int cd(String path) {
        return libc.cd(path);
    }

    public static int pwd() {
        return libc.pwd();
    }

}

interface CLibrary extends Library {
    public int chmod(String path, int mode);
    public int cd(String path);
    public int pwd();
}
