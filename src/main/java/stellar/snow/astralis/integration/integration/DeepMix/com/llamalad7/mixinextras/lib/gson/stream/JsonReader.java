package com.llamalad7.mixinextras.lib.gson.stream;

import com.llamalad7.mixinextras.lib.gson.Strictness;
import com.llamalad7.mixinextras.lib.gson.internal.JsonReaderInternalAccess;
import com.llamalad7.mixinextras.lib.gson.internal.TroubleshootingGuide;
import com.llamalad7.mixinextras.lib.gson.stream.JsonToken;
import com.llamalad7.mixinextras.lib.gson.stream.MalformedJsonException;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Objects;

public class JsonReader
implements Closeable {
    private final Reader in;
    private Strictness strictness = Strictness.LEGACY_STRICT;
    private final char[] buffer = new char[1024];
    private int pos = 0;
    private int limit = 0;
    private int lineNumber = 0;
    private int lineStart = 0;
    int peeked = 0;
    private long peekedLong;
    private int peekedNumberLength;
    private String peekedString;
    private int[] stack = new int[32];
    private int stackSize = 0;
    private String[] pathNames;
    private int[] pathIndices;

    public JsonReader(Reader in) {
        this.stack[this.stackSize++] = 6;
        this.pathNames = new String[32];
        this.pathIndices = new int[32];
        this.in = Objects.requireNonNull(in, "in == null");
    }

    public final void setStrictness(Strictness strictness) {
        Objects.requireNonNull(strictness);
        this.strictness = strictness;
    }

    public void beginObject() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = this.doPeek();
        }
        if (p != 1) {
            throw this.unexpectedTokenError("BEGIN_OBJECT");
        }
        this.push(3);
        this.peeked = 0;
    }

    public void endObject() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = this.doPeek();
        }
        if (p == 2) {
            --this.stackSize;
        } else {
            throw this.unexpectedTokenError("END_OBJECT");
        }
        this.pathNames[this.stackSize] = null;
        int n = this.stackSize - 1;
        this.pathIndices[n] = this.pathIndices[n] + 1;
        this.peeked = 0;
    }

    public boolean hasNext() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = this.doPeek();
        }
        return p != 2 && p != 4 && p != 17;
    }

    public JsonToken peek() throws IOException {
        int p = this.peeked;
        if (p == 0) {
            p = this.doPeek();
        }
        switch (p) {
            case 1: {
                return JsonToken.BEGIN_OBJECT;
            }
            case 2: {
                return JsonToken.END_OBJECT;
            }
            case 3: {
                return JsonToken.BEGIN_ARRAY;
            }
            case 4: {
                return JsonToken.END_ARRAY;
            }
            case 12: 
            case 13: 
            case 14: {
                return JsonToken.NAME;
            }
            case 5: 
            case 6: {
                return JsonToken.BOOLEAN;
            }
            case 7: {
                return JsonToken.NULL;
            }
            case 8: 
            case 9: 
            case 10: 
            case 11: {
                return JsonToken.STRING;
            }
            case 15: 
            case 16: {
                return JsonToken.NUMBER;
            }
            case 17: {
                return JsonToken.END_DOCUMENT;
            }
        }
        throw new AssertionError();
    }

    int doPeek() throws IOException {
        int c;
        int peekStack;
        block45: {
            block47: {
                block46: {
                    block44: {
                        peekStack = this.stack[this.stackSize - 1];
                        if (peekStack != 1) break block44;
                        this.stack[this.stackSize - 1] = 2;
                        break block45;
                    }
                    if (peekStack != 2) break block46;
                    c = this.nextNonWhitespace(true);
                    switch (c) {
                        case 93: {
                            this.peeked = 4;
                            return 4;
                        }
                        case 59: {
                            this.checkLenient();
                        }
                        case 44: {
                            break;
                        }
                        default: {
                            throw this.syntaxError("Unterminated array");
                        }
                    }
                    break block45;
                }
                if (peekStack == 3 || peekStack == 5) {
                    int c2;
                    this.stack[this.stackSize - 1] = 4;
                    if (peekStack == 5) {
                        c2 = this.nextNonWhitespace(true);
                        switch (c2) {
                            case 125: {
                                this.peeked = 2;
                                return 2;
                            }
                            case 59: {
                                this.checkLenient();
                            }
                            case 44: {
                                break;
                            }
                            default: {
                                throw this.syntaxError("Unterminated object");
                            }
                        }
                    }
                    c2 = this.nextNonWhitespace(true);
                    switch (c2) {
                        case 34: {
                            this.peeked = 13;
                            return 13;
                        }
                        case 39: {
                            this.checkLenient();
                            this.peeked = 12;
                            return 12;
                        }
                        case 125: {
                            if (peekStack != 5) {
                                this.peeked = 2;
                                return 2;
                            }
                            throw this.syntaxError("Expected name");
                        }
                    }
                    this.checkLenient();
                    --this.pos;
                    if (this.isLiteral((char)c2)) {
                        this.peeked = 14;
                        return 14;
                    }
                    throw this.syntaxError("Expected name");
                }
                if (peekStack != 4) break block47;
                this.stack[this.stackSize - 1] = 5;
                c = this.nextNonWhitespace(true);
                switch (c) {
                    case 58: {
                        break;
                    }
                    case 61: {
                        this.checkLenient();
                        if ((this.pos < this.limit || this.fillBuffer(1)) && this.buffer[this.pos] == '>') {
                            ++this.pos;
                            break;
                        }
                        break block45;
                    }
                    default: {
                        throw this.syntaxError("Expected ':'");
                    }
                }
                break block45;
            }
            if (peekStack == 6) {
                if (this.strictness == Strictness.LENIENT) {
                    this.consumeNonExecutePrefix();
                }
                this.stack[this.stackSize - 1] = 7;
            } else if (peekStack == 7) {
                c = this.nextNonWhitespace(false);
                if (c == -1) {
                    this.peeked = 17;
                    return 17;
                }
                this.checkLenient();
                --this.pos;
            } else if (peekStack == 8) {
                throw new IllegalStateException("JsonReader is closed");
            }
        }
        c = this.nextNonWhitespace(true);
        switch (c) {
            case 93: {
                if (peekStack == 1) {
                    this.peeked = 4;
                    return 4;
                }
            }
            case 44: 
            case 59: {
                if (peekStack == 1 || peekStack == 2) {
                    this.checkLenient();
                    --this.pos;
                    this.peeked = 7;
                    return 7;
                }
                throw this.syntaxError("Unexpected value");
            }
            case 39: {
                this.checkLenient();
                this.peeked = 8;
                return 8;
            }
            case 34: {
                this.peeked = 9;
                return 9;
            }
            case 91: {
                this.peeked = 3;
                return 3;
            }
            case 123: {
                this.peeked = 1;
                return 1;
            }
        }
        --this.pos;
        int result = this.peekKeyword();
        if (result != 0) {
            return result;
        }
        result = this.peekNumber();
        if (result != 0) {
            return result;
        }
        if (!this.isLiteral(this.buffer[this.pos])) {
            throw this.syntaxError("Expected value");
        }
        this.checkLenient();
        this.peeked = 10;
        return 10;
    }

    private int peekKeyword() throws IOException {
        int peeking;
        String keywordUpper;
        String keyword;
        char c = this.buffer[this.pos];
        if (c == 't' || c == 'T') {
            keyword = "true";
            keywordUpper = "TRUE";
            peeking = 5;
        } else if (c == 'f' || c == 'F') {
            keyword = "false";
            keywordUpper = "FALSE";
            peeking = 6;
        } else if (c == 'n' || c == 'N') {
            keyword = "null";
            keywordUpper = "NULL";
            peeking = 7;
        } else {
            return 0;
        }
        boolean allowsUpperCased = this.strictness != Strictness.STRICT;
        int length = keyword.length();
        for (int i = 0; i < length; ++i) {
            boolean matched;
            if (this.pos + i >= this.limit && !this.fillBuffer(i + 1)) {
                return 0;
            }
            c = this.buffer[this.pos + i];
            boolean bl = matched = c == keyword.charAt(i) || allowsUpperCased && c == keywordUpper.charAt(i);
            if (matched) continue;
            return 0;
        }
        if ((this.pos + length < this.limit || this.fillBuffer(length + 1)) && this.isLiteral(this.buffer[this.pos + length])) {
            return 0;
        }
        this.pos += length;
        this.peeked = peeking;
        return this.peeked;
    }

    private int peekNumber() throws IOException {
        char[] buffer = this.buffer;
        int p = this.pos;
        int l = this.limit;
        long value = 0L;
        boolean negative = false;
        boolean fitsInLong = true;
        int last = 0;
        int i = 0;
        block6: while (true) {
            if (p + i == l) {
                if (i == buffer.length) {
                    return 0;
                }
                if (!this.fillBuffer(i + 1)) break;
                p = this.pos;
                l = this.limit;
            }
            char c = buffer[p + i];
            switch (c) {
                case '-': {
                    if (last == 0) {
                        negative = true;
                        last = 1;
                        break;
                    }
                    if (last == 5) {
                        last = 6;
                        break;
                    }
                    return 0;
                }
                case '+': {
                    if (last == 5) {
                        last = 6;
                        break;
                    }
                    return 0;
                }
                case 'E': 
                case 'e': {
                    if (last == 2 || last == 4) {
                        last = 5;
                        break;
                    }
                    return 0;
                }
                case '.': {
                    if (last == 2) {
                        last = 3;
                        break;
                    }
                    return 0;
                }
                default: {
                    if (c < '0' || c > '9') {
                        if (!this.isLiteral(c)) break block6;
                        return 0;
                    }
                    if (last == 1 || last == 0) {
                        value = -(c - 48);
                        last = 2;
                        break;
                    }
                    if (last == 2) {
                        if (value == 0L) {
                            return 0;
                        }
                        long newValue = value * 10L - (long)(c - 48);
                        fitsInLong &= value > -922337203685477580L || value == -922337203685477580L && newValue < value;
                        value = newValue;
                        break;
                    }
                    if (last == 3) {
                        last = 4;
                        break;
                    }
                    if (last != 5 && last != 6) break;
                    last = 7;
                }
            }
            ++i;
        }
        if (!(last != 2 || !fitsInLong || value == Long.MIN_VALUE && !negative || value == 0L && negative)) {
            this.peekedLong = negative ? value : -value;
            this.pos += i;
            this.peeked = 15;
            return 15;
        }
        if (last == 2 || last == 4 || last == 7) {
            this.peekedNumberLength = i;
            this.peeked = 16;
            return 16;
        }
        return 0;
    }

    private boolean isLiteral(char c) throws IOException {
        switch (c) {
            case '#': 
            case '/': 
            case ';': 
            case '=': 
            case '\\': {
                this.checkLenient();
            }
            case '\t': 
            case '\n': 
            case '\f': 
            case '\r': 
            case ' ': 
            case ',': 
            case ':': 
            case '[': 
            case ']': 
            case '{': 
            case '}': {
                return false;
            }
        }
        return true;
    }

    public String nextName() throws IOException {
        String result;
        int p = this.peeked;
        if (p == 0) {
            p = this.doPeek();
        }
        if (p == 14) {
            result = this.nextUnquotedValue();
        } else if (p == 12) {
            result = this.nextQuotedValue('\'');
        } else if (p == 13) {
            result = this.nextQuotedValue('\"');
        } else {
            throw this.unexpectedTokenError("a name");
        }
        this.peeked = 0;
        this.pathNames[this.stackSize - 1] = result;
        return result;
    }

    public String nextString() throws IOException {
        String result;
        int p = this.peeked;
        if (p == 0) {
            p = this.doPeek();
        }
        if (p == 10) {
            result = this.nextUnquotedValue();
        } else if (p == 8) {
            result = this.nextQuotedValue('\'');
        } else if (p == 9) {
            result = this.nextQuotedValue('\"');
        } else if (p == 11) {
            result = this.peekedString;
            this.peekedString = null;
        } else if (p == 15) {
            result = Long.toString(this.peekedLong);
        } else if (p == 16) {
            result = new String(this.buffer, this.pos, this.peekedNumberLength);
            this.pos += this.peekedNumberLength;
        } else {
            throw this.unexpectedTokenError("a string");
        }
        this.peeked = 0;
        int n = this.stackSize - 1;
        this.pathIndices[n] = this.pathIndices[n] + 1;
        return result;
    }

    private String nextQuotedValue(char quote) throws IOException {
        char[] buffer = this.buffer;
        StringBuilder builder = null;
        do {
            int p = this.pos;
            int l = this.limit;
            int start = p;
            while (p < l) {
                int len;
                char c = buffer[p++];
                if (this.strictness == Strictness.STRICT && c < ' ') {
                    throw this.syntaxError("Unescaped control characters (\\u0000-\\u001F) are not allowed in strict mode");
                }
                if (c == quote) {
                    this.pos = p;
                    len = p - start - 1;
                    if (builder == null) {
                        return new String(buffer, start, len);
                    }
                    builder.append(buffer, start, len);
                    return builder.toString();
                }
                if (c == '\\') {
                    this.pos = p;
                    len = p - start - 1;
                    if (builder == null) {
                        int estimatedLength = (len + 1) * 2;
                        builder = new StringBuilder(Math.max(estimatedLength, 16));
                    }
                    builder.append(buffer, start, len);
                    builder.append(this.readEscapeCharacter());
                    p = this.pos;
                    l = this.limit;
                    start = p;
                    continue;
                }
                if (c != '\n') continue;
                ++this.lineNumber;
                this.lineStart = p;
            }
            if (builder == null) {
                int estimatedLength = (p - start) * 2;
                builder = new StringBuilder(Math.max(estimatedLength, 16));
            }
            builder.append(buffer, start, p - start);
            this.pos = p;
        } while (this.fillBuffer(1));
        throw this.syntaxError("Unterminated string");
    }

    private String nextUnquotedValue() throws IOException {
        StringBuilder builder = null;
        int i = 0;
        block4: while (true) {
            if (this.pos + i < this.limit) {
                switch (this.buffer[this.pos + i]) {
                    case '#': 
                    case '/': 
                    case ';': 
                    case '=': 
                    case '\\': {
                        this.checkLenient();
                    }
                    case '\t': 
                    case '\n': 
                    case '\f': 
                    case '\r': 
                    case ' ': 
                    case ',': 
                    case ':': 
                    case '[': 
                    case ']': 
                    case '{': 
                    case '}': {
                        break block4;
                    }
                    default: {
                        ++i;
                        break;
                    }
                }
                continue;
            }
            if (i < this.buffer.length) {
                if (!this.fillBuffer(i + 1)) break;
                continue;
            }
            if (builder == null) {
                builder = new StringBuilder(Math.max(i, 16));
            }
            builder.append(this.buffer, this.pos, i);
            this.pos += i;
            i = 0;
            if (!this.fillBuffer(1)) break;
        }
        String result = builder == null ? new String(this.buffer, this.pos, i) : builder.append(this.buffer, this.pos, i).toString();
        this.pos += i;
        return result;
    }

    private void skipQuotedValue(char quote) throws IOException {
        char[] buffer = this.buffer;
        do {
            int p = this.pos;
            int l = this.limit;
            while (p < l) {
                char c;
                if ((c = buffer[p++]) == quote) {
                    this.pos = p;
                    return;
                }
                if (c == '\\') {
                    this.pos = p;
                    char unused = this.readEscapeCharacter();
                    p = this.pos;
                    l = this.limit;
                    continue;
                }
                if (c != '\n') continue;
                ++this.lineNumber;
                this.lineStart = p;
            }
            this.pos = p;
        } while (this.fillBuffer(1));
        throw this.syntaxError("Unterminated string");
    }

    private void skipUnquotedValue() throws IOException {
        do {
            int i = 0;
            while (this.pos + i < this.limit) {
                switch (this.buffer[this.pos + i]) {
                    case '#': 
                    case '/': 
                    case ';': 
                    case '=': 
                    case '\\': {
                        this.checkLenient();
                    }
                    case '\t': 
                    case '\n': 
                    case '\f': 
                    case '\r': 
                    case ' ': 
                    case ',': 
                    case ':': 
                    case '[': 
                    case ']': 
                    case '{': 
                    case '}': {
                        this.pos += i;
                        return;
                    }
                }
                ++i;
            }
            this.pos += i;
        } while (this.fillBuffer(1));
    }

    @Override
    public void close() throws IOException {
        this.peeked = 0;
        this.stack[0] = 8;
        this.stackSize = 1;
        this.in.close();
    }

    public void skipValue() throws IOException {
        int count = 0;
        do {
            int p;
            if ((p = this.peeked) == 0) {
                p = this.doPeek();
            }
            switch (p) {
                case 3: {
                    this.push(1);
                    ++count;
                    break;
                }
                case 1: {
                    this.push(3);
                    ++count;
                    break;
                }
                case 4: {
                    --this.stackSize;
                    --count;
                    break;
                }
                case 2: {
                    if (count == 0) {
                        this.pathNames[this.stackSize - 1] = null;
                    }
                    --this.stackSize;
                    --count;
                    break;
                }
                case 10: {
                    this.skipUnquotedValue();
                    break;
                }
                case 8: {
                    this.skipQuotedValue('\'');
                    break;
                }
                case 9: {
                    this.skipQuotedValue('\"');
                    break;
                }
                case 14: {
                    this.skipUnquotedValue();
                    if (count != 0) break;
                    this.pathNames[this.stackSize - 1] = "<skipped>";
                    break;
                }
                case 12: {
                    this.skipQuotedValue('\'');
                    if (count != 0) break;
                    this.pathNames[this.stackSize - 1] = "<skipped>";
                    break;
                }
                case 13: {
                    this.skipQuotedValue('\"');
                    if (count != 0) break;
                    this.pathNames[this.stackSize - 1] = "<skipped>";
                    break;
                }
                case 16: {
                    this.pos += this.peekedNumberLength;
                    break;
                }
                case 17: {
                    return;
                }
            }
            this.peeked = 0;
        } while (count > 0);
        int n = this.stackSize - 1;
        this.pathIndices[n] = this.pathIndices[n] + 1;
    }

    private void push(int newTop) {
        if (this.stackSize == this.stack.length) {
            int newLength = this.stackSize * 2;
            this.stack = Arrays.copyOf(this.stack, newLength);
            this.pathIndices = Arrays.copyOf(this.pathIndices, newLength);
            this.pathNames = Arrays.copyOf(this.pathNames, newLength);
        }
        this.stack[this.stackSize++] = newTop;
    }

    private boolean fillBuffer(int minimum) throws IOException {
        int total;
        char[] buffer = this.buffer;
        this.lineStart -= this.pos;
        if (this.limit != this.pos) {
            this.limit -= this.pos;
            System.arraycopy(buffer, this.pos, buffer, 0, this.limit);
        } else {
            this.limit = 0;
        }
        this.pos = 0;
        while ((total = this.in.read(buffer, this.limit, buffer.length - this.limit)) != -1) {
            this.limit += total;
            if (this.lineNumber == 0 && this.lineStart == 0 && this.limit > 0 && buffer[0] == '\ufeff') {
                ++this.pos;
                ++this.lineStart;
                ++minimum;
            }
            if (this.limit < minimum) continue;
            return true;
        }
        return false;
    }

    private int nextNonWhitespace(boolean throwOnEof) throws IOException {
        block12: {
            char c;
            char[] buffer = this.buffer;
            int p = this.pos;
            int l = this.limit;
            block4: while (true) {
                if (p == l) {
                    this.pos = p;
                    if (!this.fillBuffer(1)) break block12;
                    p = this.pos;
                    l = this.limit;
                }
                if ((c = buffer[p++]) == '\n') {
                    ++this.lineNumber;
                    this.lineStart = p;
                    continue;
                }
                if (c == ' ' || c == '\r' || c == '\t') continue;
                if (c == '/') {
                    this.pos = p;
                    if (p == l) {
                        --this.pos;
                        boolean charsLoaded = this.fillBuffer(2);
                        ++this.pos;
                        if (!charsLoaded) {
                            return c;
                        }
                    }
                    this.checkLenient();
                    char peek = buffer[this.pos];
                    switch (peek) {
                        case '*': {
                            ++this.pos;
                            if (!this.skipTo("*/")) {
                                throw this.syntaxError("Unterminated comment");
                            }
                            p = this.pos + 2;
                            l = this.limit;
                            continue block4;
                        }
                        case '/': {
                            ++this.pos;
                            this.skipToEndOfLine();
                            p = this.pos;
                            l = this.limit;
                            continue block4;
                        }
                    }
                    return c;
                }
                if (c != '#') break;
                this.pos = p;
                this.checkLenient();
                this.skipToEndOfLine();
                p = this.pos;
                l = this.limit;
            }
            this.pos = p;
            return c;
        }
        if (throwOnEof) {
            throw new EOFException("End of input" + this.locationString());
        }
        return -1;
    }

    private void checkLenient() throws MalformedJsonException {
        if (this.strictness != Strictness.LENIENT) {
            throw this.syntaxError("Use JsonReader.setStrictness(Strictness.LENIENT) to accept malformed JSON");
        }
    }

    private void skipToEndOfLine() throws IOException {
        while (this.pos < this.limit || this.fillBuffer(1)) {
            char c;
            if ((c = this.buffer[this.pos++]) == '\n') {
                ++this.lineNumber;
                this.lineStart = this.pos;
                break;
            }
            if (c != '\r') continue;
            break;
        }
    }

    private boolean skipTo(String toFind) throws IOException {
        int length = toFind.length();
        while (this.pos + length <= this.limit || this.fillBuffer(length)) {
            block5: {
                if (this.buffer[this.pos] == '\n') {
                    ++this.lineNumber;
                    this.lineStart = this.pos + 1;
                } else {
                    for (int c = 0; c < length; ++c) {
                        if (this.buffer[this.pos + c] == toFind.charAt(c)) {
                            continue;
                        }
                        break block5;
                    }
                    return true;
                }
            }
            ++this.pos;
        }
        return false;
    }

    public String toString() {
        return this.getClass().getSimpleName() + this.locationString();
    }

    String locationString() {
        int line = this.lineNumber + 1;
        int column = this.pos - this.lineStart + 1;
        return " at line " + line + " column " + column + " path " + this.getPath();
    }

    private String getPath(boolean usePreviousPath) {
        StringBuilder result = new StringBuilder().append('$');
        block5: for (int i = 0; i < this.stackSize; ++i) {
            int scope = this.stack[i];
            switch (scope) {
                case 1: 
                case 2: {
                    int pathIndex = this.pathIndices[i];
                    if (usePreviousPath && pathIndex > 0 && i == this.stackSize - 1) {
                        --pathIndex;
                    }
                    result.append('[').append(pathIndex).append(']');
                    continue block5;
                }
                case 3: 
                case 4: 
                case 5: {
                    result.append('.');
                    if (this.pathNames[i] == null) continue block5;
                    result.append(this.pathNames[i]);
                    continue block5;
                }
                case 6: 
                case 7: 
                case 8: {
                    continue block5;
                }
                default: {
                    throw new AssertionError((Object)("Unknown scope value: " + scope));
                }
            }
        }
        return result.toString();
    }

    public String getPath() {
        return this.getPath(false);
    }

    private char readEscapeCharacter() throws IOException {
        if (this.pos == this.limit && !this.fillBuffer(1)) {
            throw this.syntaxError("Unterminated escape sequence");
        }
        char escaped = this.buffer[this.pos++];
        switch (escaped) {
            case 'u': {
                int i;
                if (this.pos + 4 > this.limit && !this.fillBuffer(4)) {
                    throw this.syntaxError("Unterminated escape sequence");
                }
                int result = 0;
                int end = i + 4;
                for (i = this.pos; i < end; ++i) {
                    char c = this.buffer[i];
                    result <<= 4;
                    if (c >= '0' && c <= '9') {
                        result += c - 48;
                        continue;
                    }
                    if (c >= 'a' && c <= 'f') {
                        result += c - 97 + 10;
                        continue;
                    }
                    if (c >= 'A' && c <= 'F') {
                        result += c - 65 + 10;
                        continue;
                    }
                    throw this.syntaxError("Malformed Unicode escape \\u" + new String(this.buffer, this.pos, 4));
                }
                this.pos += 4;
                return (char)result;
            }
            case 't': {
                return '\t';
            }
            case 'b': {
                return '\b';
            }
            case 'n': {
                return '\n';
            }
            case 'r': {
                return '\r';
            }
            case 'f': {
                return '\f';
            }
            case '\n': {
                if (this.strictness == Strictness.STRICT) {
                    throw this.syntaxError("Cannot escape a newline character in strict mode");
                }
                ++this.lineNumber;
                this.lineStart = this.pos;
            }
            case '\'': {
                if (this.strictness == Strictness.STRICT) {
                    throw this.syntaxError("Invalid escaped character \"'\" in strict mode");
                }
            }
            case '\"': 
            case '/': 
            case '\\': {
                return escaped;
            }
        }
        throw this.syntaxError("Invalid escape sequence");
    }

    private MalformedJsonException syntaxError(String message) throws MalformedJsonException {
        throw new MalformedJsonException(message + this.locationString() + "\nSee " + TroubleshootingGuide.createUrl("malformed-json"));
    }

    private IllegalStateException unexpectedTokenError(String expected) throws IOException {
        JsonToken peeked = this.peek();
        String troubleshootingId = peeked == JsonToken.NULL ? "adapter-not-null-safe" : "unexpected-json-structure";
        return new IllegalStateException("Expected " + expected + " but was " + (Object)((Object)this.peek()) + this.locationString() + "\nSee " + TroubleshootingGuide.createUrl(troubleshootingId));
    }

    private void consumeNonExecutePrefix() throws IOException {
        int unused = this.nextNonWhitespace(true);
        --this.pos;
        if (this.pos + 5 > this.limit && !this.fillBuffer(5)) {
            return;
        }
        char[] buf = this.buffer;
        int p = this.pos;
        if (buf[p] != ')' || buf[p + 1] != ']' || buf[p + 2] != '}' || buf[p + 3] != '\'' || buf[p + 4] != '\n') {
            return;
        }
        this.pos += 5;
    }

    static {
        JsonReaderInternalAccess.INSTANCE = new JsonReaderInternalAccess(){};
    }
}

