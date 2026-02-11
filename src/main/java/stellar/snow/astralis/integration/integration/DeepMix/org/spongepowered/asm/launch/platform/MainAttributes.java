package org.spongepowered.asm.launch.platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.spongepowered.asm.util.JavaVersion;
import org.spongepowered.include.com.google.common.io.ByteSource;

public final class MainAttributes {
    private static final Map<URI, MainAttributes> instances = new HashMap<URI, MainAttributes>();
    protected final Attributes attributes;

    private MainAttributes() {
        this.attributes = new Attributes();
    }

    private MainAttributes(URI codeSource) {
        this.attributes = MainAttributes.getAttributes(codeSource);
    }

    public final String get(String name) {
        if (this.attributes != null) {
            return this.attributes.getValue(name);
        }
        return null;
    }

    public final String get(Attributes.Name name) {
        if (this.attributes != null) {
            return this.attributes.getValue(name);
        }
        return null;
    }

    private static Attributes getAttributes(URI codeSource) {
        Attributes attributes;
        if (codeSource == null) {
            return null;
        }
        if ("file".equals(codeSource.getScheme())) {
            Attributes attributes2;
            File file = org.spongepowered.asm.util.Files.toFile(codeSource);
            if (file.isFile()) {
                Attributes attributes3 = MainAttributes.getJarAttributes(file);
                if (attributes3 != null) {
                    return attributes3;
                }
            } else if (file.isDirectory() && (attributes2 = MainAttributes.getDirAttributes(file)) != null) {
                return attributes2;
            }
        } else if (JavaVersion.current() >= 1.7 && (attributes = MainAttributes.getNioAttributes(codeSource)) != null) {
            return attributes;
        }
        return new Attributes();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Attributes getJarAttributes(File jar) {
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(jar);
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attributes = manifest.getMainAttributes();
                return attributes;
            }
        }
        catch (IOException iOException) {
        }
        finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            }
            catch (IOException iOException) {}
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Attributes getDirAttributes(File dir) {
        File manifestFile = new File(dir, "META-INF/MANIFEST.MF");
        if (manifestFile.isFile()) {
            ByteSource source = org.spongepowered.include.com.google.common.io.Files.asByteSource(manifestFile);
            InputStream inputStream = null;
            try {
                inputStream = source.openBufferedStream();
                Manifest manifest = new Manifest(inputStream);
                Attributes attributes = manifest.getMainAttributes();
                return attributes;
            }
            catch (IOException iOException) {
            }
            finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                catch (IOException iOException) {}
            }
        }
        return null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled aggressive block sorting
     * Enabled unnecessary exception pruning
     * Enabled aggressive exception aggregation
     */
    private static Attributes getNioAttributes(URI uri) {
        try {
            Path manifestPath = Paths.get(uri).resolve("META-INF/MANIFEST.MF");
            BufferedInputStream inputStream = null;
            try {
                inputStream = new BufferedInputStream(Files.newInputStream(manifestPath, new OpenOption[0]));
                Manifest manifest = new Manifest(inputStream);
                Attributes attributes = manifest.getMainAttributes();
                return attributes;
            }
            catch (IOException iOException) {
                return null;
            }
            finally {
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                }
                catch (IOException iOException) {}
            }
        }
        catch (FileSystemNotFoundException ex) {
            ex.printStackTrace();
            return null;
        }
        catch (InvalidPathException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static MainAttributes of(File jar) {
        return MainAttributes.of(jar.toURI());
    }

    public static MainAttributes of(URI uri) {
        MainAttributes attributes = instances.get(uri);
        if (attributes == null) {
            attributes = new MainAttributes(uri);
            instances.put(uri, attributes);
        }
        return attributes;
    }
}

