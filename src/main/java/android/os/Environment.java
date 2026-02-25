package android.os;

import java.io.File;

@SuppressWarnings("unused")
public class Environment {
    public static File getExternalStoragePublicDirectory(String file) {
        return new File(".");
    }
}
