package org.spongepowered.asm.util;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class VersionNumber
implements Serializable,
Comparable<VersionNumber> {
    public static final VersionNumber NONE = new VersionNumber();
    private static final Pattern PATTERN = Pattern.compile("^(\\d{1,5})(?:\\.(\\d{1,5})(?:\\.(\\d{1,5})(?:\\.(\\d{1,5}))?)?)?(-[a-zA-Z0-9_\\-]+)?$");
    private final long value;
    private final String suffix;

    private VersionNumber() {
        this.value = 0L;
        this.suffix = "";
    }

    private VersionNumber(short[] parts, String suffix) {
        this.value = VersionNumber.pack(parts);
        this.suffix = suffix != null ? suffix : "";
    }

    public short getMajor() {
        return (short)(this.value >> 48);
    }

    public short getMinor() {
        return (short)(this.value >> 32 & 0x7FFFL);
    }

    public short getPatch() {
        return (short)(this.value >> 16 & 0x7FFFL);
    }

    public short getRevision() {
        return (short)(this.value & 0x7FFFL);
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String toString() {
        short[] parts = VersionNumber.unpack(this.value);
        return String.format("%d.%d%3$s%4$s%5$s", parts[0], parts[1], (this.value & Integer.MAX_VALUE) > 0L ? String.format(".%d", parts[2]) : "", (this.value & 0x7FFFL) > 0L ? String.format(".%d", parts[3]) : "", this.suffix);
    }

    @Override
    public int compareTo(VersionNumber other) {
        if (other == null) {
            return 1;
        }
        return Long.compare(this.value, other.value);
    }

    public boolean equals(Object other) {
        if (!(other instanceof VersionNumber)) {
            return false;
        }
        return ((VersionNumber)other).value == this.value;
    }

    public int hashCode() {
        return (int)(this.value >> 32) ^ (int)(this.value & 0xFFFFFFFFL);
    }

    private static long pack(short ... shorts) {
        return (long)shorts[0] << 48 | (long)shorts[1] << 32 | (long)(shorts[2] << 16) | (long)shorts[3];
    }

    private static short[] unpack(long along) {
        return new short[]{(short)(along >> 48), (short)(along >> 32 & 0x7FFFL), (short)(along >> 16 & 0x7FFFL), (short)(along & 0x7FFFL)};
    }

    public static VersionNumber parse(String version) {
        return VersionNumber.parse(version, NONE);
    }

    public static VersionNumber parse(String version, String defaultVersion) {
        return VersionNumber.parse(version, VersionNumber.parse(defaultVersion));
    }

    private static VersionNumber parse(String version, VersionNumber defaultVersion) {
        if (version == null) {
            return defaultVersion;
        }
        Matcher versionNumberPatternMatcher = PATTERN.matcher(version);
        if (!versionNumberPatternMatcher.matches()) {
            return defaultVersion;
        }
        short[] parts = new short[4];
        for (int pos = 0; pos < 4; ++pos) {
            String part = versionNumberPatternMatcher.group(pos + 1);
            if (part == null) continue;
            int value = Integer.parseInt(part);
            if (value > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Version parts cannot exceed 32767, found " + value);
            }
            parts[pos] = (short)value;
        }
        return new VersionNumber(parts, versionNumberPatternMatcher.group(5));
    }
}

