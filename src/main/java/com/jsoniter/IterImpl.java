package com.jsoniter;

import com.jsoniter.any.Any;
import com.jsoniter.spi.JsonException;
import com.jsoniter.spi.Slice;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;

class IterImpl {

    private static BigInteger maxLong = BigInteger.valueOf(Long.MAX_VALUE);
    private static BigInteger minLong = BigInteger.valueOf(Long.MIN_VALUE);
    private static BigInteger maxInt = BigInteger.valueOf(Integer.MAX_VALUE);
    private static BigInteger minInt = BigInteger.valueOf(Integer.MIN_VALUE);

    public static final int readObjectFieldAsHash(JsonIterator iter) throws IOException {
        if (readByte(iter) != '"') {
            if (nextToken(iter) != '"') {
                throw iter.reportError("readObjectFieldAsHash", "expect \"");
            }
        }
        long hash = 0x811c9dc5;
        int i = iter.head;
        for (; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (c == '"') {
                break;
            }
            hash ^= c;
            hash *= 0x1000193;
        }
        iter.head = i + 1;
        if (readByte(iter) != ':') {
            if (nextToken(iter) != ':') {
                throw iter.reportError("readObjectFieldAsHash", "expect :");
            }
        }
        return (int) hash;
    }

    public static final Slice readObjectFieldAsSlice(JsonIterator iter) throws IOException {
        Slice field = readSlice(iter);
        if (nextToken(iter) != ':') {
            throw iter.reportError("readObjectFieldAsSlice", "expect : after object field");
        }
        return field;
    }

    final static void skipArray(JsonIterator iter) throws IOException {
        int level = 1;
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
        throw iter.reportError("skipArray", "incomplete array");
    }

    final static void skipObject(JsonIterator iter) throws IOException {
        int level = 1;
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
        throw iter.reportError("skipObject", "incomplete object");
    }

    final static void skipString(JsonIterator iter) throws IOException {
        int end = IterImplSkip.findStringEnd(iter);
        if (end == -1) {
            throw iter.reportError("skipString", "incomplete string");
        } else {
            iter.head = end;
        }
    }

    final static void skipUntilBreak(JsonIterator iter) throws IOException {
        // true, false, null, number
        for (int i = iter.head; i < iter.tail; i++) {
            byte c = iter.buf[i];
            if (IterImplSkip.breaks[c]) {
                iter.head = i;
                return;
            }
        }
        iter.head = iter.tail;
    }

    final static boolean skipNumber(JsonIterator iter) throws IOException {
        // true, false, null, number
        boolean dotFound = false;
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
        iter.head = iter.tail;
        return dotFound;
    }

    // read the bytes between " "
    public final static Slice readSlice(JsonIterator iter) throws IOException {
        if (IterImpl.nextToken(iter) != '"') {
            throw iter.reportError("readSlice", "expect \" for string");
        }
        int end = IterImplString.findSliceEnd(iter);
        if (end == -1) {
            throw iter.reportError("readSlice", "incomplete string");
        } else {
            // reuse current buffer
            iter.reusableSlice.reset(iter.buf, iter.head, end - 1);
            iter.head = end;
            return iter.reusableSlice;
        }
    }

    final static byte nextToken(final JsonIterator iter) throws IOException {
        int i = iter.head;
        for (; ; ) {
            byte c = iter.buf[i++];
            switch (c) {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    continue;
                default:
                    iter.head = i;
                    return c;
            }
        }
    }

    final static byte readByte(JsonIterator iter) throws IOException {
        return iter.buf[iter.head++];
    }

    public static Any readAny(JsonIterator iter) throws IOException {
        int start = iter.head;
        byte c = nextToken(iter);
        switch (c) {
            case '"':
                skipString(iter);
                return Any.lazyString(iter.buf, start, iter.head);
            case 't':
                skipFixedBytes(iter, 3);
                return Any.wrap(true);
            case 'f':
                skipFixedBytes(iter, 4);
                return Any.wrap(false);
            case 'n':
                skipFixedBytes(iter, 3);
                return Any.wrap((Object) null);
            case '[':
                skipArray(iter);
                return Any.lazyArray(iter.buf, start, iter.head);
            case '{':
                skipObject(iter);
                return Any.lazyObject(iter.buf, start, iter.head);
            default:
                if (skipNumber(iter)) {
                    return Any.lazyDouble(iter.buf, start, iter.head);
                } else {
                    return Any.lazyLong(iter.buf, start, iter.head);
                }
        }
    }

    public static void skipFixedBytes(JsonIterator iter, int n) throws IOException {
        iter.head += n;
    }

    public final static boolean loadMore(JsonIterator iter) throws IOException {
        return false;
    }

    // funtions for DIY CC testing----------------
    private static boolean[] branches = new boolean[50];

    private static void setBanchEntry(int id){
        branches[id] = true;
    }

    public static void writeCCToFile(){
        File f = new File("ccResult.txt");
        try{
            FileWriter w = new FileWriter(f);
            StringBuilder s = new StringBuilder();
            for(int i= 0;i<branches.length;i++){
                if(branches[i]){
                    s.append("Branch "+i+": ++\n");
                }
                else{
                    s.append("Branch "+i+": -\n");
                }
            }
            w.write(s.toString());
            w.close();
        }catch(Exception e){}
    }
    //--------------------------------------------

    public final static int readStringSlowPath(JsonIterator iter, int j) throws IOException {
        setBanchEntry(0); // Id 0
        try {
            boolean isExpectingLowSurrogate = false;
            for (int i = iter.head; i < iter.tail; ) {
                setBanchEntry(1); // Id 1
                int bc = iter.buf[i++];
                if (bc == '"') {
                    setBanchEntry(2); // Id 2
                    iter.head = i;
                    return j;
                }
                setBanchEntry(3); // Id 3
                if (bc == '\\') {
                    setBanchEntry(4); // Id 4
                    bc = iter.buf[i++];
                    switch (bc) {
                        case 'b':
                        setBanchEntry(5); // Id 5
                            bc = '\b';
                            break;
                        case 't':
                        setBanchEntry(6); // Id 6
                            bc = '\t';
                            break;
                        case 'n':
                        setBanchEntry(7); // Id 7
                            bc = '\n';
                            break;
                        case 'f':
                        setBanchEntry(8); // Id 8
                            bc = '\f';
                            break;
                        case 'r':
                        setBanchEntry(9);// Id 9
                            bc = '\r';
                            break;
                        case '"':
                        setBanchEntry(10);// Id 10
                        case '/':
                        setBanchEntry(11);// Id 11
                        case '\\':
                        setBanchEntry(12);// Id 12
                            break;
                        case 'u':
                        setBanchEntry(13);// Id 13
                            bc = (IterImplString.translateHex(iter.buf[i++]) << 12) +
                                    (IterImplString.translateHex(iter.buf[i++]) << 8) +
                                    (IterImplString.translateHex(iter.buf[i++]) << 4) +
                                    IterImplString.translateHex(iter.buf[i++]);
                            if (Character.isHighSurrogate((char) bc)) {
                                setBanchEntry(14);// Id 14
                                if (isExpectingLowSurrogate) {
                                    setBanchEntry(15);// Id 15
                                    throw new JsonException("invalid surrogate");
                                } else {
                                    setBanchEntry(16);// Id 16
                                    isExpectingLowSurrogate = true;
                                }
                            } else if (Character.isLowSurrogate((char) bc)) {
                                setBanchEntry(17);// Id 17
                                if (isExpectingLowSurrogate) {
                                    setBanchEntry(18);// Id 18
                                    isExpectingLowSurrogate = false;
                                } else {
                                    setBanchEntry(19);// Id 19
                                    throw new JsonException("invalid surrogate");
                                }
                            } else {
                                setBanchEntry(20);// Id 20
                                if (isExpectingLowSurrogate) {
                                    setBanchEntry(21);// Id 21
                                    throw new JsonException("invalid surrogate");
                                }
                                setBanchEntry(21);// Id 22
                            }
                            break;

                        default:
                        setBanchEntry(23);// Id 23
                            throw iter.reportError("readStringSlowPath", "invalid escape character: " + bc);
                    }

                } else if ((bc & 0x80) != 0) {
                    setBanchEntry(24);// Id 24
                    final int u2 = iter.buf[i++];
                    if ((bc & 0xE0) == 0xC0) {
                        setBanchEntry(25);// Id 25
                        bc = ((bc & 0x1F) << 6) + (u2 & 0x3F);
                    } else {
                        setBanchEntry(26);// Id 26
                        final int u3 = iter.buf[i++];
                        if ((bc & 0xF0) == 0xE0) {
                            setBanchEntry(27);// Id 27
                            bc = ((bc & 0x0F) << 12) + ((u2 & 0x3F) << 6) + (u3 & 0x3F);
                        } else {
                            setBanchEntry(28);// Id 28
                            final int u4 = iter.buf[i++];
                            if ((bc & 0xF8) == 0xF0) {
                                setBanchEntry(29);// Id 29
                                bc = ((bc & 0x07) << 18) + ((u2 & 0x3F) << 12) + ((u3 & 0x3F) << 6) + (u4 & 0x3F);
                            } else {
                                setBanchEntry(30);// Id 30
                                throw iter.reportError("readStringSlowPath", "invalid unicode character");
                            }

                            if (bc >= 0x10000) {
                                setBanchEntry(31);// Id 31
                                // check if valid unicode
                                if (bc >= 0x110000){
                                    setBanchEntry(32);// Id 32
                                    throw iter.reportError("readStringSlowPath", "invalid unicode character");
                                }

                                setBanchEntry(33);// Id 33
                                // split surrogates
                                final int sup = bc - 0x10000;
                                if (iter.reusableChars.length == j) {
                                    setBanchEntry(34);// Id 34
                                    char[] newBuf = new char[iter.reusableChars.length * 2];
                                    System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                                    iter.reusableChars = newBuf;
                                }
                                setBanchEntry(35);// Id 35
                                iter.reusableChars[j++] = (char) ((sup >>> 10) + 0xd800);
                                if (iter.reusableChars.length == j) {
                                    setBanchEntry(36);// Id 36
                                    char[] newBuf = new char[iter.reusableChars.length * 2];
                                    System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                                    iter.reusableChars = newBuf;
                                }
                                setBanchEntry(37);// Id 37
                                iter.reusableChars[j++] = (char) ((sup & 0x3ff) + 0xdc00);
                                continue;
                            }
                            setBanchEntry(38);// Id 38
                        }
                    }
                }
                setBanchEntry(39);// Id 39
                if (iter.reusableChars.length == j) {
                    setBanchEntry(40);// Id 40
                    char[] newBuf = new char[iter.reusableChars.length * 2];
                    System.arraycopy(iter.reusableChars, 0, newBuf, 0, iter.reusableChars.length);
                    iter.reusableChars = newBuf;
                }
                setBanchEntry(41);// Id 41
                iter.reusableChars[j++] = (char) bc;
            }
            throw iter.reportError("readStringSlowPath", "incomplete string");
        } catch (IndexOutOfBoundsException e) {
            setBanchEntry(42);// Id 42
            throw iter.reportError("readString", "incomplete string");
        }
    }

    public static int updateStringCopyBound(final JsonIterator iter, final int bound) {
        return bound;
    }

    static final int readInt(final JsonIterator iter, final byte c) throws IOException {
        int ind = IterImplNumber.intDigits[c];
        if (ind == 0) {
            IterImplForStreaming.assertNotLeadingZero(iter);
            return 0;
        }
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readInt", "expect 0~9");
        }
        if (iter.tail - iter.head > 9) {
            int i = iter.head;
            int ind2 = IterImplNumber.intDigits[iter.buf[i]];
            if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                return -ind;
            }
            int ind3 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind3 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 10 + ind2;
                return -ind;
            }
            int ind4 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind4 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 100 + ind2 * 10 + ind3;
                return -ind;
            }
            int ind5 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind5 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 1000 + ind2 * 100 + ind3 * 10 + ind4;
                return -ind;
            }
            int ind6 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind6 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 10000 + ind2 * 1000 + ind3 * 100 + ind4 * 10 + ind5;
                return -ind;
            }
            int ind7 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind7 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 100000 + ind2 * 10000 + ind3 * 1000 + ind4 * 100 + ind5 * 10 + ind6;
                return -ind;
            }
            int ind8 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind8 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 1000000 + ind2 * 100000 + ind3 * 10000 + ind4 * 1000 + ind5 * 100 + ind6 * 10 + ind7;
                return -ind;
            }
            int ind9 = IterImplNumber.intDigits[iter.buf[++i]];
            ind = ind * 10000000 + ind2 * 1000000 + ind3 * 100000 + ind4 * 10000 + ind5 * 1000 + ind6 * 100 + ind7 * 10 + ind8;
            iter.head = i;
            if (ind9 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                return -ind;
            }
        }
        return IterImplForStreaming.readIntSlowPath(iter, ind);
    }

    static final long readLong(final JsonIterator iter, final byte c) throws IOException {
        long ind = IterImplNumber.intDigits[c];
        if (ind == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
            throw iter.reportError("readLong", "expect 0~9");
        }
        if (iter.tail - iter.head > 9) {
            int i = iter.head;
            int ind2 = IterImplNumber.intDigits[iter.buf[i]];
            if (ind2 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                return -ind;
            }
            int ind3 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind3 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 10 + ind2;
                return -ind;
            }
            int ind4 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind4 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 100 + ind2 * 10 + ind3;
                return -ind;
            }
            int ind5 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind5 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 1000 + ind2 * 100 + ind3 * 10 + ind4;
                return -ind;
            }
            int ind6 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind6 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 10000 + ind2 * 1000 + ind3 * 100 + ind4 * 10 + ind5;
                return -ind;
            }
            int ind7 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind7 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 100000 + ind2 * 10000 + ind3 * 1000 + ind4 * 100 + ind5 * 10 + ind6;
                return -ind;
            }
            int ind8 = IterImplNumber.intDigits[iter.buf[++i]];
            if (ind8 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                iter.head = i;
                ind = ind * 1000000 + ind2 * 100000 + ind3 * 10000 + ind4 * 1000 + ind5 * 100 + ind6 * 10 + ind7;
                return -ind;
            }
            int ind9 = IterImplNumber.intDigits[iter.buf[++i]];
            ind = ind * 10000000 + ind2 * 1000000 + ind3 * 100000 + ind4 * 10000 + ind5 * 1000 + ind6 * 100 + ind7 * 10 + ind8;
            iter.head = i;
            if (ind9 == IterImplNumber.INVALID_CHAR_FOR_NUMBER) {
                return -ind;
            }
        }
        return IterImplForStreaming.readLongSlowPath(iter, ind);
    }

    static final double readDouble(final JsonIterator iter) throws IOException {
        int oldHead = iter.head;
        try {
            try {
                long value = IterImplNumber.readLong(iter); // without the dot & sign
                if (iter.head == iter.tail) {
                    return value;
                }
                byte c = iter.buf[iter.head];
                if (c == '.') {
                    iter.head++;
                    int start = iter.head;
                    c = iter.buf[iter.head++];
                    long decimalPart = readLong(iter, c);
                    if (decimalPart == Long.MIN_VALUE) {
                        return IterImplForStreaming.readDoubleSlowPath(iter);
                    }
                    decimalPart = -decimalPart;
                    int decimalPlaces = iter.head - start;
                    if (decimalPlaces > 0 && decimalPlaces < IterImplNumber.POW10.length && (iter.head - oldHead) < 10) {
                        return value + (decimalPart / (double) IterImplNumber.POW10[decimalPlaces]);
                    } else {
                        iter.head = oldHead;
                        return IterImplForStreaming.readDoubleSlowPath(iter);
                    }
                } else {
                    return value;
                }
            } finally {
                if (iter.head < iter.tail && (iter.buf[iter.head] == 'e' || iter.buf[iter.head] == 'E')) {
                    iter.head = oldHead;
                    return IterImplForStreaming.readDoubleSlowPath(iter);
                }
            }
        } catch (JsonException e) {
            iter.head = oldHead;
            return IterImplForStreaming.readDoubleSlowPath(iter);
        }
    }
}
