package com.llamalad7.mixinextras.lib.semver;

import com.llamalad7.mixinextras.lib.semver.ParseException;
import com.llamalad7.mixinextras.lib.semver.UnexpectedCharacterException;
import com.llamalad7.mixinextras.lib.semver.Version;
import com.llamalad7.mixinextras.lib.semver.util.Stream;
import com.llamalad7.mixinextras.lib.semver.util.UnexpectedElementException;
import java.util.ArrayList;
import java.util.EnumSet;

class VersionParser {
    private final boolean isStrictModeOn;
    private final Stream<Character> chars;

    VersionParser(String input, boolean strictModeOn) {
        this.isStrictModeOn = strictModeOn;
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Input string is NULL or empty");
        }
        Character[] elements = new Character[input.length()];
        for (int i = 0; i < input.length(); ++i) {
            elements[i] = Character.valueOf(input.charAt(i));
        }
        this.chars = new Stream<Character>(elements);
    }

    static Version parseValidSemVer(String version, boolean strictModeOn) {
        VersionParser parser = new VersionParser(version, strictModeOn);
        return parser.parseValidSemVer();
    }

    private Version parseValidSemVer() {
        long[] versionParts = this.parseVersionCore();
        String[] preRelease = new String[]{};
        String[] build = new String[]{};
        Character next = this.consumeNextCharacter(CharType.HYPHEN, CharType.PLUS, CharType.EOI);
        if (CharType.HYPHEN.isMatchedBy(next)) {
            preRelease = this.parsePreRelease();
            next = this.consumeNextCharacter(CharType.PLUS, CharType.EOI);
        }
        if (CharType.PLUS.isMatchedBy(next)) {
            build = this.parseBuild();
        }
        this.consumeNextCharacter(CharType.EOI);
        return new Version(versionParts[0], versionParts[1], versionParts[2], preRelease, build);
    }

    private long[] parseVersionCore() {
        long major = this.numericIdentifier();
        long minor = 0L;
        if (this.isStrictModeOn || this.chars.positiveLookahead(new CharType[]{CharType.DOT})) {
            this.consumeNextCharacter(CharType.DOT);
            minor = this.numericIdentifier();
        }
        long patch = 0L;
        if (this.isStrictModeOn || this.chars.positiveLookahead(new CharType[]{CharType.DOT})) {
            this.consumeNextCharacter(CharType.DOT);
            patch = this.numericIdentifier();
        }
        return new long[]{major, minor, patch};
    }

    private String[] parsePreRelease() {
        this.ensureValidLookahead(CharType.DIGIT, CharType.LETTER, CharType.HYPHEN);
        ArrayList<String> idents = new ArrayList<String>();
        while (true) {
            idents.add(this.preReleaseIdentifier());
            if (!this.chars.positiveLookahead(new CharType[]{CharType.DOT})) break;
            this.consumeNextCharacter(CharType.DOT);
        }
        return idents.toArray(new String[0]);
    }

    private String preReleaseIdentifier() {
        this.checkForEmptyIdentifier();
        CharType boundary = this.nearestCharType(CharType.DOT, CharType.PLUS, CharType.EOI);
        if (this.chars.positiveLookaheadBefore(boundary, new CharType[]{CharType.LETTER, CharType.HYPHEN})) {
            return this.alphanumericIdentifier();
        }
        return String.valueOf(this.numericIdentifier());
    }

    private String[] parseBuild() {
        this.ensureValidLookahead(CharType.DIGIT, CharType.LETTER, CharType.HYPHEN);
        ArrayList<String> idents = new ArrayList<String>();
        while (true) {
            idents.add(this.buildIdentifier());
            if (!this.chars.positiveLookahead(new CharType[]{CharType.DOT})) break;
            this.consumeNextCharacter(CharType.DOT);
        }
        return idents.toArray(new String[0]);
    }

    private String buildIdentifier() {
        this.checkForEmptyIdentifier();
        CharType boundary = this.nearestCharType(CharType.DOT, CharType.EOI);
        if (this.chars.positiveLookaheadBefore(boundary, new CharType[]{CharType.LETTER, CharType.HYPHEN})) {
            return this.alphanumericIdentifier();
        }
        return this.digits();
    }

    private long numericIdentifier() {
        this.checkForLeadingZeroes();
        try {
            return Long.parseLong(this.digits());
        }
        catch (NumberFormatException e) {
            throw new ParseException("Numeric identifier overflow");
        }
    }

    private String alphanumericIdentifier() {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(this.consumeNextCharacter(CharType.DIGIT, CharType.LETTER, CharType.HYPHEN));
        } while (this.chars.positiveLookahead(new CharType[]{CharType.DIGIT, CharType.LETTER, CharType.HYPHEN}));
        return sb.toString();
    }

    private String digits() {
        StringBuilder sb = new StringBuilder();
        do {
            sb.append(this.consumeNextCharacter(CharType.DIGIT));
        } while (this.chars.positiveLookahead(new CharType[]{CharType.DIGIT}));
        return sb.toString();
    }

    private CharType nearestCharType(CharType ... types) {
        for (Character chr : this.chars) {
            for (CharType type : types) {
                if (!type.isMatchedBy(chr)) continue;
                return type;
            }
        }
        return CharType.EOI;
    }

    private void checkForLeadingZeroes() {
        Character la1 = this.chars.lookahead(1);
        Character la2 = this.chars.lookahead(2);
        if (la1 != null && la1.charValue() == '0' && CharType.DIGIT.isMatchedBy(la2)) {
            throw new ParseException("Numeric identifier MUST NOT contain leading zeroes");
        }
    }

    private void checkForEmptyIdentifier() {
        Character la = this.chars.lookahead(1);
        if (CharType.DOT.isMatchedBy(la) || CharType.PLUS.isMatchedBy(la) || CharType.EOI.isMatchedBy(la)) {
            throw new ParseException("Identifiers MUST NOT be empty", new UnexpectedCharacterException(la, this.chars.currentOffset(), CharType.DIGIT, CharType.LETTER, CharType.HYPHEN));
        }
    }

    private Character consumeNextCharacter(CharType ... expected) {
        try {
            return (Character)this.chars.consume(expected);
        }
        catch (UnexpectedElementException e) {
            throw new UnexpectedCharacterException(e);
        }
    }

    private void ensureValidLookahead(CharType ... expected) {
        if (!this.chars.positiveLookahead(expected)) {
            throw new UnexpectedCharacterException(this.chars.lookahead(1), this.chars.currentOffset(), expected);
        }
    }

    static abstract class CharType
    extends Enum<CharType>
    implements Stream.ElementType<Character> {
        public static final /* enum */ CharType DIGIT = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr.charValue() >= '0' && chr.charValue() <= '9';
            }
        };
        public static final /* enum */ CharType LETTER = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr.charValue() >= 'a' && chr.charValue() <= 'z' || chr.charValue() >= 'A' && chr.charValue() <= 'Z';
            }
        };
        public static final /* enum */ CharType DOT = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr.charValue() == '.';
            }
        };
        public static final /* enum */ CharType HYPHEN = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr.charValue() == '-';
            }
        };
        public static final /* enum */ CharType PLUS = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                if (chr == null) {
                    return false;
                }
                return chr.charValue() == '+';
            }
        };
        public static final /* enum */ CharType EOI = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                return chr == null;
            }
        };
        public static final /* enum */ CharType ILLEGAL = new CharType(){

            @Override
            public boolean isMatchedBy(Character chr) {
                EnumSet<CharType> itself = EnumSet.of(ILLEGAL);
                for (CharType type : EnumSet.complementOf(itself)) {
                    if (!type.isMatchedBy(chr)) continue;
                    return false;
                }
                return true;
            }
        };
        private static final /* synthetic */ CharType[] $VALUES;

        public static CharType[] values() {
            return (CharType[])$VALUES.clone();
        }

        static CharType forCharacter(Character chr) {
            for (CharType type : CharType.values()) {
                if (!type.isMatchedBy(chr)) continue;
                return type;
            }
            return null;
        }

        static {
            $VALUES = new CharType[]{DIGIT, LETTER, DOT, HYPHEN, PLUS, EOI, ILLEGAL};
        }
    }
}

