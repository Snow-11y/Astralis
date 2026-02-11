package org.spongepowered.asm.util.asm;

import java.lang.reflect.Field;
import java.util.jar.Attributes;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.launch.platform.MainAttributes;
import org.spongepowered.asm.util.VersionNumber;

public final class ASM {
    private static int majorVersion = 5;
    private static int minorVersion = 0;
    private static int implMinorVersion = 0;
    private static int patchVersion = 0;
    private static String maxVersion = "FALLBACK";
    private static int maxClassVersion = 50;
    private static int maxClassMajorVersion = 50;
    private static int maxClassMinorVersion = 0;
    private static String maxJavaVersion = "V1.6";
    public static final int API_VERSION = ASM.detectVersion();

    private ASM() {
    }

    public static boolean isAtLeastVersion(int majorVersion) {
        return ASM.majorVersion >= majorVersion;
    }

    public static boolean isAtLeastVersion(int majorVersion, int minorVersion) {
        return ASM.majorVersion >= majorVersion && (ASM.majorVersion > majorVersion || implMinorVersion >= minorVersion);
    }

    public static boolean isAtLeastVersion(int majorVersion, int minorVersion, int patchVersion) {
        if (ASM.majorVersion == majorVersion) {
            return implMinorVersion >= minorVersion && (implMinorVersion > minorVersion || ASM.patchVersion >= patchVersion);
        }
        return ASM.majorVersion > majorVersion;
    }

    public static int getApiVersionMajor() {
        return majorVersion;
    }

    public static int getApiVersionMinor() {
        return minorVersion;
    }

    public static String getApiVersionString() {
        return String.format("%d.%d", majorVersion, minorVersion);
    }

    public static String getVersionString() {
        return String.format("ASM %d.%d%s (%s)", majorVersion, implMinorVersion, patchVersion > 0 ? "." + patchVersion : "", maxVersion);
    }

    public static int getMaxSupportedClassVersion() {
        return maxClassVersion;
    }

    public static int getMaxSupportedClassVersionMajor() {
        return maxClassMajorVersion;
    }

    public static int getMaxSupportedClassVersionMinor() {
        return maxClassMinorVersion;
    }

    public static String getClassVersionString() {
        return String.format("Up to Java %s (class file version %d.%d)", maxJavaVersion, maxClassMajorVersion, maxClassMinorVersion);
    }

    private static int detectVersion() {
        int apiVersion = 262144;
        VersionNumber packageVersion = ASM.getPackageVersion(Opcodes.class);
        for (Field field : Opcodes.class.getDeclaredFields()) {
            if (field.getType() != Integer.TYPE) continue;
            try {
                int major;
                int minor;
                String name = field.getName();
                int version = field.getInt(null);
                if (name.startsWith("ASM")) {
                    boolean experimental;
                    minor = version >> 8 & 0xFF;
                    major = version >> 16 & 0xFF;
                    boolean bl = experimental = (version >> 24 & 0xFF) != 0;
                    if (major < majorVersion) continue;
                    maxVersion = name;
                    if (experimental) continue;
                    apiVersion = version;
                    majorVersion = major;
                    minorVersion = implMinorVersion = minor;
                    if (packageVersion.getMajor() != major || minor != 0) continue;
                    implMinorVersion = packageVersion.getMinor();
                    patchVersion = packageVersion.getPatch();
                    continue;
                }
                if (name.matches("V([0-9_]+)")) {
                    minor = version >> 16 & 0xFFFF;
                    major = version & 0xFFFF;
                    if (major <= maxClassMajorVersion && (major != maxClassMajorVersion || minor <= maxClassMinorVersion)) continue;
                    maxClassMajorVersion = major;
                    maxClassMinorVersion = minor;
                    maxClassVersion = version;
                    maxJavaVersion = name.replace('_', '.').substring(1);
                    continue;
                }
                if (!"ACC_PUBLIC".equals(name)) continue;
                break;
            }
            catch (ReflectiveOperationException ex) {
                throw new Error(ex);
            }
        }
        return apiVersion;
    }

    private static VersionNumber getPackageVersion(Class<?> clazz) {
        String implVersion = clazz.getPackage().getImplementationVersion();
        if (implVersion != null) {
            return VersionNumber.parse(implVersion);
        }
        try {
            MainAttributes manifest = MainAttributes.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
            return VersionNumber.parse(manifest.get(Attributes.Name.IMPLEMENTATION_VERSION));
        }
        catch (Exception ex) {
            return VersionNumber.NONE;
        }
    }
}

