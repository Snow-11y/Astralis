package org.spongepowered.asm.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public final class Files {
    private Files() {
    }

    public static File toFile(URL url) throws URISyntaxException {
        return url != null ? Files.toFile(url.toURI()) : null;
    }

    public static File toFile(URI uri) {
        String strUri;
        if (uri == null) {
            return null;
        }
        if ("file".equals(uri.getScheme()) && uri.getAuthority() != null && (strUri = uri.toString()).startsWith("file://") && !strUri.startsWith("file:///")) {
            try {
                uri = new URI("file:////" + strUri.substring(7));
            }
            catch (URISyntaxException ex) {
                throw new IllegalArgumentException(ex.getMessage());
            }
        }
        return new File(uri);
    }

    public static void deleteRecursively(File dir) throws IOException {
        if (dir == null || !dir.isDirectory()) {
            return;
        }
        try {
            File[] files = dir.listFiles();
            if (files == null) {
                throw new IOException("Error enumerating directory during recursive delete operation: " + dir.getAbsolutePath());
            }
            for (File child : files) {
                if (child.isDirectory()) {
                    Files.deleteRecursively(child);
                    continue;
                }
                if (!child.isFile() || child.delete()) continue;
                throw new IOException("Error deleting file during recursive delete operation: " + child.getAbsolutePath());
            }
            if (!dir.delete()) {
                throw new IOException("Error deleting directory during recursive delete operation: " + dir.getAbsolutePath());
            }
        }
        catch (SecurityException ex) {
            throw new IOException("Security error during recursive delete operation", ex);
        }
    }
}

