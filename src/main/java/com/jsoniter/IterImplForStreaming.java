package com.jsoniter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

class IterImplForStreaming {

    private static Map<String, Boolean> branchCoverageOnReadStringSlowPath = new HashMap<>();

    static {
        branchCoverageOnReadStringSlowPath.put("0_EnterOuterLoop", false);
        branchCoverageOnReadStringSlowPath.put("1_ExitLoop", false);
        branchCoverageOnReadStringSlowPath.put("2_Backslash", false);
        branchCoverageOnReadStringSlowPath.put("3_Case_b", false);
        branchCoverageOnReadStringSlowPath.put("4_Case_t", false);
        branchCoverageOnReadStringSlowPath.put("5_Case_n", false);
        branchCoverageOnReadStringSlowPath.put("6_Case_f", false);
        branchCoverageOnReadStringSlowPath.put("7_Case_r", false);
        branchCoverageOnReadStringSlowPath.put("8_Case_Quote", false);
        branchCoverageOnReadStringSlowPath.put("9_Case_Slash", false);
        branchCoverageOnReadStringSlowPath.put("10_Case_Backslash", false);
        branchCoverageOnReadStringSlowPath.put("11_Unicode", false);
        branchCoverageOnReadStringSlowPath.put("12_HighSurrogateCheck", false);
        branchCoverageOnReadStringSlowPath.put("13_InvalidSurrogate", false);
        branchCoverageOnReadStringSlowPath.put("14_ValidHighSurrogate", false);
        branchCoverageOnReadStringSlowPath.put("15_LowSurrogateCheck", false);
        branchCoverageOnReadStringSlowPath.put("16_ValidLowSurrogate", false);
        branchCoverageOnReadStringSlowPath.put("17_InvalidSurrogate", false);
        branchCoverageOnReadStringSlowPath.put("18_ElseBranch", false);
        branchCoverageOnReadStringSlowPath.put("19_InvalidSurrogate", false);
        branchCoverageOnReadStringSlowPath.put("20_DefaultCase", false);
        branchCoverageOnReadStringSlowPath.put("21_MultiByteCheck", false);
        branchCoverageOnReadStringSlowPath.put("22_MultiByte_2Byte", false);
        branchCoverageOnReadStringSlowPath.put("23_Else_MultiByte_2Byte", false);           
        branchCoverageOnReadStringSlowPath.put("24_MultiByte_3Byte", false);
        branchCoverageOnReadStringSlowPath.put("25_Else_MultiByte_3Byte", false);
        branchCoverageOnReadStringSlowPath.put("26_MultiByte_4Byte", false);
        branchCoverageOnReadStringSlowPath.put("27_InvalidMultiByte", false);
        branchCoverageOnReadStringSlowPath.put("28_UnicodeCheck", false);
        branchCoverageOnReadStringSlowPath.put("29_InvalidUnicodeCheck", false);
        branchCoverageOnReadStringSlowPath.put("30_ExpandBuffer", false);
        branchCoverageOnReadStringSlowPath.put("31_ExpandBuffer", false);
        branchCoverageOnReadStringSlowPath.put("32_ExpandBuffer_Final", false);
    }

    public static void printCoverageForReadStringSlowPath() {
        System.out.println("Branch Coverage Report for readStringSlowPath():");
        for (Map.Entry<String, Boolean> entry : branchCoverageOnReadStringSlowPath.entrySet()) {
            System.out.println(entry.getKey() + ": " + (entry.getValue() ? "Hit" : "Not Hit"));
        }
    }

    public static Map<String, Boolean> getBranchCoverageMap() {
        return branchCoverageOnReadStringSlowPath;
    }

    public static final int readObjectFieldAsHash(JsonIterator iter) throws IOException {
        if (nextToken(iter) != '"') {
            throw iter.reportError("readObjectFieldAsHash", "expect \"");
        }
        long hash = 0x811c9dc5;
        for (; ; ) {
            byte c = 0;
            int i = iter.head;
            for (; i < iter.tail; i++) {
                c = iter.buf[i];
                if (c == '"') {
                    break;
                }
                hash ^= c;
                hash *= 0x1000193;
            }
            if (c == '"') {
                iter.head = i + 1;
                if (nextToken(iter) != ':') {
                    throw iter.reportError("readObjectFieldAsHash", "expect :");
                }
                return (int) hash;
            }
            if (!loadMore(iter)) {
                throw iter.reportError("readObjectFieldAsHash", "unmatched quote");
            }
        }
    }

    public static final Slice readObjectFieldAsSlice(JsonIterator iter) throws IOException {
        Slice field = readSlice(iter);
        boolean notCopied = field != null;
        if (CodegenAccess.skipWhitespacesWithoutLoadMore(iter)) {
            if (notCopied) {
                int len = field.tail() - field.head();
                byte[] newBuf = new byte[len];
                System.arraycopy(field.data(), field.head(), newBuf, 0, len);
                field.reset(newBuf, 0, newBuf.length);
            }
            if (!loadMore(iter)) {
                throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
            }
        }
        if (iter.buf[iter.head] != ':') {
            throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
        }
        iter.head++;
        return field;
    }

    final static void skipArray(JsonIterator iter) throws IOException {
        int level = 1;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                switch (iter.buf[i]) {
                    case '"': // If inside string, skip it
                        iter.head = i + 1;
                        skipString(iter);
                        i = iter.head - 1; // it will be i++ soon
                        break;
                    case '[': // If open symbol, increase level
                        level++;
                        break;
                    case ']': // If close symbol, decrease level
                        level--;

                        // If we have returned to the original level, we're done
                        if (level == 0) {
                            iter.head = i + 1;
                            return;
                        }
                        break;
                }
            }
            if (!loadMore(iter)) {
                return;
            }
        }
    }

    final static void skipObject(JsonIterator iter) throws IOException {
        int level = 1;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                switch (iter.buf[i]) {
                    case '"': // If inside string, skip it
                        iter.head = i + 1;
                        skipString(iter);
                        i = iter.head - 1; // it will be i++ soon
                        break;
                    case '{': // If open symbol, increase level
                        level++;
                        break;
                    case '}': // If close symbol, decrease level
                        level--;

                        // If we have returned to the original level, we're done
                        if (level == 0) {
                            iter.head = i + 1;
                            return;
                        }
                        break;
                }
            }
            if (!loadMore(iter)) {
                return;
            }
        }
    }

    final static void skipString(JsonIterator iter) throws IOException {
        for (; ; ) {
            int end = IterImplSkip.findStringEnd(iter);
            if (end == -1) {
                int j = iter.tail - 1;
                boolean escaped = true;
                // can not just look the last byte is \
                // because it could be \\ or \\\
                for (; ; ) {
                    // walk backward until head
                    if (j < iter.head || iter.buf[j] != '\\') {
                        // even number of backslashes
                        // either end of buffer, or " found
                        escaped = false;
                        break;
                    }
                    j--;
                    if (j < iter.head || iter.buf[j] != '\\') {
                        // odd number of backslashes
                        // it is \" or \\\"
                        break;
                    }
                    j--;

                }
                if (!loadMore(iter)) {
                    throw iter.reportError("skipString", "incomplete string");
                }
                if (escaped) {
                    // TODO add unit test to prove/verify bug
                    iter.head += 1; // skip the first char as last char is \
                }
            } else {
                iter.head = end;
                return;
            }
        }
    }

    final static void skipUntilBreak(JsonIterator iter) throws IOException {
        // true, false, null, number
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                byte c = iter.buf[i];
                if (IterImplSkip.breaks[c]) {
                    iter.head = i;
                    return;
                }
            }
            if (!loadMore(iter)) {
                iter.head = iter.tail;
                return;
            }
        }
    }

    final static boolean skipNumber(JsonIterator iter) throws IOException {
        // true, false, null, number
        boolean dotFound = false;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                byte c = iter.buf[i];
                if (c == '.' || c == 'e' || c == 'E') {
                    dotFound = true;
                    continue;
                }
                if (IterImplSkip.breaks[c]) {
                    iter.head = i;
                    return dotFound;
                }
            }
            if (!loadMore(iter)) {
                iter.head = iter.tail;
                return dotFound;
            }
        }
    }

    // read the bytes between " "
    final static Slice readSlice(JsonIterator iter) throws IOException {
        if (IterImpl.nextToken(iter) != '"') {
            throw iter.reportError("readSlice", "expect \" for string");
        }
        int end = IterImplString.findSliceEnd(iter);
        if (end != -1) {
            // reuse current buffer
            iter.reusableSlice.reset(iter.buf, iter.head, end - 1);
            iter.head = end;
            return iter.reusableSlice;
        }
        // TODO: avoid small memory allocation
        byte[] part1 = new byte[iter.tail - iter.head];
        System.arraycopy(iter.buf, iter.head, part1, 0, part1.length);
        for (; ; ) {
            if (!loadMore(iter)) {
                throw iter.reportError("readSlice", "unmatched quote");
            }
            end = IterImplString.findSliceEnd(iter);
            if (end == -1) {
                byte[] part2 = new byte[part1.length + iter.buf.length];
                System.arraycopy(part1, 0, part2, 0, part1.length);
                System.arraycopy(iter.buf, 0, part2, part1.length, iter.buf.length);
                part1 = part2;
            } else {
                byte[] part2 = new byte[part1.length + end - 1];
                System.arraycopy(part1, 0, part2, 0, part1.length);
                System.arraycopy(iter.buf, 0, part2, part1.length, end - 1);
                iter.head = end;
                iter.reusableSlice.reset(part2, 0, part2.length);
                return iter.reusableSlice;
            }
        }
    }

    final static byte nextToken(JsonIterator iter) throws IOException {
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                byte c = iter.buf[i];
                switch (c) {
                    case ' ':
                    case '\n':
                    case '\t':
                    case '\r':
                        continue;
                    default:
                        iter.head = i + 1;
                        return c;
                }
            }
            if (!loadMore(iter)) {
                return 0;
            }
        }
    }

    public final static boolean loadMore(JsonIterator iter) throws IOException {
        if (iter.in == null) {
            return false;
        }
        if (iter.skipStartedAt != -1) {
            return keepSkippedBytesThenRead(iter);
        }
        int n = iter.in.read(iter.buf);
        if (n < 1) {
            if (n == -1) {
                return false;
            } else {
                throw iter.reportError("loadMore", "read from input stream returned " + n);
            }
        } else {
            iter.head = 0;
            iter.tail = n;
        }
        return true;
    }

    private static boolean keepSkippedBytesThenRead(JsonIterator iter) throws IOException {
        int offset = iter.tail - iter.skipStartedAt;
        byte[] srcBuffer = iter.buf;
        // Check there is no unused buffer capacity
        if ((getUnusedBufferByteCount(iter)) == 0) {
          // If auto expand buffer enabled, then create larger buffer
          if (iter.autoExpandBufferStep > 0) {
            iter.buf = new byte[iter.buf.length + iter.autoExpandBufferStep];
          } else {
            throw iter.reportError("loadMore", String.format("buffer is full and autoexpansion is disabled. tail: [%s] skipStartedAt: [%s]", iter.tail, iter.skipStartedAt));
          }
        }
        System.arraycopy(srcBuffer, iter.skipStartedAt, iter.buf, 0, offset);
        int n = iter.in.read(iter.buf, offset, iter.buf.length - offset);
        iter.skipStartedAt = 0;
        if (n < 1) {
            if (n == -1) {
                return false;
            } else {
                throw iter.reportError("loadMore", "read from input stream returned " + n);
            }
        } else {
            iter.head = offset;
            iter.tail = offset + n;
        }
        return true;
    }

    private static int getUnusedBufferByteCount(JsonIterator iter) {
        // Get bytes from 0 to skipStart + from tail till end
        return iter.buf.length - iter.tail + iter.skipStartedAt;
    }

    final static byte readByte(JsonIterator iter) throws IOException {
        if (iter.head == iter.tail) {
            if (!loadMore(iter)) {
                throw iter.reportError("readByte", "no more to read");
            }
        }
        return iter.buf[iter.head++];
    }

    public static Any readAny(JsonIterator iter) throws IOException {
        // TODO: avoid small memory allocation
        iter.skipStartedAt = iter.head;
        byte c = nextToken(iter);
        switch (c) {
            case '"':
                skipString(iter);
                byte[] copied = copySkippedBytes(iter);
                return Any.lazyString(copied, 0, copied.length);
            case 't':
                skipFixedBytes(iter, 3);
                iter.skipStartedAt = -1;
                return Any.wrap(true);
            case 'f':
                skipFixedBytes(iter, 4);
                iter.skipStartedAt = -1;
                return Any.wrap(false);
            case 'n':
                skipFixedBytes(iter, 3);
                iter.skipStartedAt = -1;
                return Any.wrap((Object) null);
            case '[':
                skipArray(iter);
                copied = copySkippedBytes(iter);
                return Any.lazyArray(copied, 0, copied.length);
            case '{':
                skipObject(iter);
                copied = copySkippedBytes(iter);
                return Any.lazyObject(copied, 0, copied.length);
            default:
                if (skipNumber(iter)) {
                    copied = copySkippedBytes(iter);
                    return Any.lazyDouble(copied, 0, copied.length);
                } else {
                    copied = copySkippedBytes(iter);
                    return Any.lazyLong(copied, 0, copied.length);
                }
        }
    }

    private static byte[] copySkippedBytes(JsonIterator iter) {
        int start = iter.skipStartedAt;
        iter.skipStartedAt = -1;
        int end = iter.head;
        byte[] bytes = new byte[end - start];
        System.arraycopy(iter.buf, start, bytes, 0, bytes.length);
        return bytes;
    }

    public static void skipFixedBytes(JsonIterator iter, int n) throws IOException {
        iter.head += n;
        if (iter.head >= iter.tail) {
            int more = iter.head - iter.tail;
            if (!loadMore(iter)) {
                if (more == 0) {
                    iter.head = iter.tail;
                    return;
                }
                throw iter.reportError("skipFixedBytes", "unexpected end");
            }
            iter.head += more;
        }
    }

    public static int updateStringCopyBound(final JsonIterator iter, final int bound) {
        if (bound > iter.tail - iter.head) {
            return iter.tail - iter.head;
        } else {
            return bound;
        }
    }

    /**
     * Requirements documented:
     * 1. When a simple string is encountered, the function should append all characters into iter.reusableChars and stop at the closing quote.
     * Making branch 0 and 1 hit
     *
     * 2. If the reusableChars array is full before appending a character,
     *    the function should expand the buffer making branch 32 hit. 
     */
    public final static int readStringSlowPath(JsonIterator iter, int j) throws IOException {
        boolean isExpectingLowSurrogate = false;
        for (;;) {
            branchCoverageOnReadStringSlowPath.put("0_EnterOuterLoop", true);
            int bc = readByte(iter);
            if (bc == '"') {
                branchCoverageOnReadStringSlowPath.put("1_ExitLoop", true);
                return j;
            }
            if (bc == '\\') {
                branchCoverageOnReadStringSlowPath.put("2_Backslash", true);
                bc = readByte(iter);
                switch (bc) {
                    case 'b':
                        branchCoverageOnReadStringSlowPath.put("3_Case_b", true);
                        bc = '\b';
                        break;
                    case 't':
                        branchCoverageOnReadStringSlowPath.put("4_Case_t", true);
                        bc = '\t';
                        break;
                    case 'n':
                        branchCoverageOnReadStringSlowPath.put("5_Case_n", true);
                        bc = '\n';
                        break;
                    case 'f':
                        branchCoverageOnReadStringSlowPath.put("6_Case_f", true);
                        bc = '\f';
                        break;
                    case 'r':
                        branchCoverageOnReadStringSlowPath.put("7_Case_r", true);
                        bc = '\r';
                        break;
                    case '"':
                        branchCoverageOnReadStringSlowPath.put("8_Case_Quote", true);
                    case '/':
                        branchCoverageOnReadStringSlowPath.put("9_Case_Slash", true);
                    case '\\':
                        branchCoverageOnReadStringSlowPath.put("10_Case_Backslash", true);
                        break;
                    case 'u':
                        branchCoverageOnReadStringSlowPath.put("11_Unicode", true);
                        bc = (IterImplString.translateHex(readByte(iter)) << 12) +
                                (IterImplString.translateHex(readByte(iter)) << 8) +
                                (IterImplString.translateHex(readByte(iter)) << 4) +
                                IterImplString.translateHex(readByte(iter));
                        if (Character.isHighSurrogate((char) bc)) {
                            branchCoverageOnReadStringSlowPath.put("12_HighSurrogateCheck", true);
                            if (isExpectingLowSurrogate) {
                                branchCoverageOnReadStringSlowPath.put("13_InvalidSurrogate", true);
                                throw new JsonException("invalid surrogate");
                            } else {
                                branchCoverageOnReadStringSlowPath.put("14_ValidHighSurrogate", true);
                                isExpectingLowSurrogate = true;
                            }
                        } else if (Character.isLowSurrogate((char) bc)) {
                            branchCoverageOnReadStringSlowPath.put("15_LowSurrogateCheck", true);
                            if (isExpectingLowSurrogate) {
                                branchCoverageOnReadStringSlowPath.put("16_ValidLowSurrogate", true);
                                isExpectingLowSurrogate = false;
                            } else {
                                branchCoverageOnReadStringSlowPath.put("17_InvalidSurrogate", true);
                                throw new JsonException("invalid surrogate");
                            }
                        } else {
                            branchCoverageOnReadStringSlowPath.put("18_ElseBranch", true);
                            if (isExpectingLowSurrogate) {
                                branchCoverageOnReadStringSlowPath.put("19_InvalidSurrogate", true);
                                throw new JsonException("invalid surrogate");
                            }
                        }
                        break;

                    default:
                        branchCoverageOnReadStringSlowPath.put("20_DefaultCase", true);
                        throw iter.reportError("readStringSlowPath", "invalid escape character: " + bc);
                }
            } else if ((bc & 0x80) != 0) {
                branchCoverageOnReadStringSlowPath.put("21_MultiByteCheck", true);
                final int u2 = readByte(iter);
                if ((bc & 0xE0) == 0xC0) {
                    branchCoverageOnReadStringSlowPath.put("22_MultiByte_2Byte", true);
                    bc = ((bc & 0x1F) << 6) + (u2 & 0x3F);
                } else {
                    branchCoverageOnReadStringSlowPath.put("23_Else_MultiByte_2Byte", true);
                    final int u3 = readByte(iter);
                    if ((bc & 0xF0) == 0xE0) {
                        branchCoverageOnReadStringSlowPath.put("24_MultiByte_3Byte", true);
                        bc = ((bc & 0x0F) << 12) + ((u2 & 0x3F) << 6) + (u3 & 0x3F);
                    } else {
                        branchCoverageOnReadStringSlowPath.put("25_Else_MultiByte_3Byte", true);
                        final int u4 = readByte(iter);
                        if ((bc & 0xF8) == 0xF0) {
                            branchCoverageOnReadStringSlowPath.put("26_MultiByte_4Byte", true);
                            bc = ((bc & 0x07) << 18) + ((u2 & 0x3F) << 12) + ((u3 & 0x3F) << 6) + (u4 & 0x3F);
                        } else {
                            branchCoverageOnReadStringSlowPath.put("27_InvalidMultiByte", true);
                            throw iter.reportError("readStringSlowPath", "invalid unicode character");
                        }

                        if (bc >= 0x10000) {
                            branchCoverageOnReadStringSlowPath.put("28_UnicodeCheck", true);
                            // check if valid unicode
                            if (bc >= 0x110000) {
                                branchCoverageOnReadStringSlowPath.put("29_InvalidUnicodeCheck", true);
                                throw iter.reportError("readStringSlowPath", "invalid unicode character");
                            }
                            // split surrogates
                            final int sup = bc - 0x10000;
                            if (iter.reusableChars.length == j) {
                                branchCoverageOnReadStringSlowPath.put("30_ExpandBuffer", true);
                                char[] newBuf = new char[iter.reusableChars.length * 2];
                                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                                iter.reusableChars = newBuf;
                            }
                            iter.reusableChars[j++] = (char) ((sup >>> 10) + 0xd800);
                            if (iter.reusableChars.length == j) {
                                branchCoverageOnReadStringSlowPath.put("31_ExpandBuffer", true);
                                char[] newBuf = new char[iter.reusableChars.length * 2];
                                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                                iter.reusableChars = newBuf;
                            }
                            iter.reusableChars[j++] = (char) ((sup & 0x3ff) + 0xdc00);
                            continue;
                        }
                    }
                }
            }
            if (iter.reusableChars.length == j) {
                branchCoverageOnReadStringSlowPath.put("32_ExpandBuffer_Final", true);
                char[] newBuf = new char[iter.reusableChars.length * 2];
                System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                iter.reusableChars = newBuf;
            }
            iter.reusableChars[j++] = (char) bc;
        }
    }

    static long readLongSlowPath(final JsonIterator iter, long value) throws IOException {
        value = -value; // add negatives to avoid redundant checks for Long.MIN_VALUE on each iteration
        long multmin = -922337203685477580L; // limit / 10
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                int ind = IterImplNumber.intDigits[iter.buf[i]];
                if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                    iter.head = i;
                    return value;
                }
                if (value < multmin) {
                    throw iter.reportError("readLongSlowPath", "value is too large for long");
                }
                value = (value << 3) + (value << 1) - ind;
                if (value >= 0) {
                    throw iter.reportError("readLongSlowPath", "value is too large for long");
                }
            }
            if (!IterImpl.loadMore(iter)) {
                iter.head = iter.tail;
                return value;
            }
        }
    }

    static int readIntSlowPath(final JsonIterator iter, int value) throws IOException {
        value = -value; // add negatives to avoid redundant checks for Integer.MIN_VALUE on each iteration
        int multmin = -214748364; // limit / 10
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                int ind = IterImplNumber.intDigits[iter.buf[i]];
                if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                    iter.head = i;
                    return value;
                }
                if (value < multmin) {
                    throw iter.reportError("readIntSlowPath", "value is too large for int");
                }
                value = (value << 3) + (value << 1) - ind;
                if (value >= 0) {
                    throw iter.reportError("readIntSlowPath", "value is too large for int");
                }
            }
            if (!IterImpl.loadMore(iter)) {
                iter.head = iter.tail;
                return value;
            }
        }
    }

    public static final double readDoubleSlowPath(final JsonIterator iter) throws IOException {
        try {
            numberChars numberChars = readNumber(iter);
            if (numberChars.charsLength == 0 && iter.whatIsNext() == ValueType.STRING) {
                String possibleInf = iter.readString();
                if ("infinity".equals(possibleInf)) {
                    return Double.POSITIVE_INFINITY;
                }
                if ("-infinity".equals(possibleInf)) {
                    return Double.NEGATIVE_INFINITY;
                }
                throw iter.reportError("readDoubleSlowPath", "expect number but found string: " + possibleInf);
            }
            return Double.valueOf(new String(numberChars.chars, 0, numberChars.charsLength));
        } catch (NumberFormatException e) {
            throw iter.reportError("readDoubleSlowPath", e.toString());
        }
    }

    static class numberChars {
        char[] chars;
        int charsLength;
        boolean dotFound;
    }

    public static final numberChars readNumber(final JsonIterator iter) throws IOException {
        int j = 0;
        boolean dotFound = false;
        for (; ; ) {
            for (int i = iter.head; i < iter.tail; i++) {
                if (j == iter.reusableChars.length) {
                    char[] newBuf = new char[iter.reusableChars.length * 2];
                    System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                    iter.reusableChars = newBuf;
                }
                byte c = iter.buf[i];
                switch (c) {
                    case '.':
                    case 'e':
                    case 'E':
                        dotFound = true;
                        // fallthrough
                    case '-':
                    case '+':
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        iter.reusableChars[j++] = (char) c;
                        break;
                    default:
                        iter.head = i;
                        numberChars numberChars = new numberChars();
                        numberChars.chars = iter.reusableChars;
                        numberChars.charsLength = j;
                        numberChars.dotFound = dotFound;
                        return numberChars;
                }
            }
            if (!IterImpl.loadMore(iter)) {
                iter.head = iter.tail;
                numberChars numberChars = new numberChars();
                numberChars.chars = iter.reusableChars;
                numberChars.charsLength = j;
                numberChars.dotFound = dotFound;
                return numberChars;
            }
        }
    }

    static final double readDouble(final JsonIterator iter) throws IOException {
        return readDoubleSlowPath(iter);
    }

    static final long readLong(final JsonIterator iter, final byte c) throws IOException {
        long ind = IterImplNumber.intDigits[c];
        if (ind == 0) {
            assertNotLeadingZero(iter);
            return 0;
        }
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readLong", "expect 0~9");
        }
        return IterImplForStreaming.readLongSlowPath(iter, ind);
    }

    static final int readInt(final JsonIterator iter, final byte c) throws IOException {
        int ind = IterImplNumber.intDigits[c];
        if (ind == 0) {
            assertNotLeadingZero(iter);
            return 0;
        }
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readInt", "expect 0~9");
        }
        return IterImplForStreaming.readIntSlowPath(iter, ind);
    }

    static void assertNotLeadingZero(JsonIterator iter) throws IOException {
        try {
            byte nextByte = iter.buf[iter.head];
            int ind2 = IterImplNumber.intDigits[nextByte];
            if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                return;
            }
            throw iter.reportError("assertNotLeadingZero", "leading zero is invalid");
        } catch (ArrayIndexOutOfBoundsException e) {
            iter.head = iter.tail;
            return;
        }
    }
}
