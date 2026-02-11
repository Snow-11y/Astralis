package org.spongepowered.asm.util;

public final class Quantifier {
    public static Quantifier DEFAULT = new Quantifier(0, -1);
    public static Quantifier NONE = new Quantifier(0, 0);
    public static Quantifier SINGLE = new Quantifier(0, 1);
    public static Quantifier ANY = new Quantifier(0, Integer.MAX_VALUE);
    public static Quantifier PLUS = new Quantifier(1, Integer.MAX_VALUE);
    private final int min;
    private final int max;

    public Quantifier(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public boolean isDefault() {
        return this.min == 0 && this.max < 0;
    }

    public int getMin() {
        return this.min;
    }

    public int getMax() {
        return this.max;
    }

    public int getClampedMin() {
        return Math.max(0, this.min);
    }

    public int getClampedMax() {
        return this.max < 0 ? 1 : Math.max(this.min, this.max);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        if (this.min == this.max) {
            sb.append(this.min);
        } else {
            if (this.max < this.min) {
                return "";
            }
            if (this.min == 0) {
                if (this.max == 1) {
                    return "";
                }
                if (this.max == Integer.MAX_VALUE) {
                    return "*";
                }
            }
            if (this.min == 1 && this.max == Integer.MAX_VALUE) {
                return "+";
            }
            if (this.min > 0) {
                sb.append(this.min);
            }
            if (this.min >= 0) {
                sb.append(',');
            }
            if (this.max < Integer.MAX_VALUE) {
                sb.append(this.max);
            }
        }
        return sb.append('}').toString();
    }

    public static Quantifier parse(String string) {
        if (string == null || (string = string.trim()).length() == 0) {
            return DEFAULT;
        }
        if ("*".equals(string)) {
            return ANY;
        }
        if ("+".equals(string)) {
            return PLUS;
        }
        if (!string.startsWith("{") || !string.endsWith("}") || string.length() < 3) {
            return NONE;
        }
        String inner = string.substring(1, string.length() - 1).trim();
        if (inner.isEmpty()) {
            return NONE;
        }
        String strMin = inner;
        String strMax = inner;
        int comma = inner.indexOf(44);
        if (comma > -1) {
            strMin = inner.substring(0, comma).trim();
            strMax = inner.substring(comma + 1).trim();
        }
        try {
            int min = strMin.length() > 0 ? Integer.parseInt(strMin) : 0;
            int max = strMax.length() > 0 ? Integer.parseInt(strMax) : Integer.MAX_VALUE;
            return new Quantifier(min, max);
        }
        catch (NumberFormatException ex) {
            return NONE;
        }
    }

    public boolean equals(Object obj) {
        if (obj instanceof Quantifier) {
            Quantifier other = (Quantifier)obj;
            return other.min == this.min && other.max == this.max;
        }
        if (obj instanceof Number) {
            int intValue = ((Number)obj).intValue();
            return intValue == this.min && intValue == this.max;
        }
        return false;
    }

    public int hashCode() {
        return 31 * this.min * this.max;
    }
}

