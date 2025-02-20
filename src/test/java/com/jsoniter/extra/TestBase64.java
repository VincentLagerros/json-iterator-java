package com.jsoniter.extra;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestBase64 {
    static {
        Base64Support.enable();
    }

    @Test
    public void test_encode() {
        assertEquals("\"YWJj\"", JsonStream.serialize("abc".getBytes()));
    }

    @Test
    public void test_decode() {
        assertEquals("abc", new String(JsonIterator.deserialize("\"YWJj\"", byte[].class)));
    }

    @Test
    public void test_decodeInvalid() {
        // test for both leading and trailing invalid data
        assertEquals("abc", new String(JsonIterator.deserialize("\"\0\0YWJj\"", byte[].class)));
        assertEquals("abc", new String(JsonIterator.deserialize("\"YWJj\0\0\"", byte[].class)));
    }

    @Test
    public void test_decodeEmpty() {
        // test when there is no data
        assertEquals("", new String(JsonIterator.deserialize("\"\"", byte[].class)));
    }

    @Test
    public void test_decodeLineSeperator() {
        // test for line seperator, this is a really weird support in the base64 decoder
        assertTrue((new String(JsonIterator.deserialize("\"YWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWFhYWF\rYWFh\"", byte[].class))).startsWith("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));
    }

    @Test
    public void test_lastEquals() {
        // test the trailing ==
        assertEquals("hello worldhello worldhello worldhello worldhello world", new String(JsonIterator.deserialize("\"aGVsbG8gd29ybGRoZWxsbyB3b3JsZGhlbGxvIHdvcmxkaGVsbG8gd29ybGRoZWxsbyB3b3JsZA==\r\n\"", byte[].class)));
    }

    @AfterClass
    public static void printCoverageReport() {
        Base64.printCoverage();
    }
}
