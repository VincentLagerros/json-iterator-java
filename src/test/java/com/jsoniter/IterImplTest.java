package com.jsoniter;

import java.io.IOException;

import com.jsoniter.spi.JsonException;

import junit.framework.TestCase;

public class IterImplTest extends TestCase{
    
    public void testReadSampleString() throws Exception {
		String sample = "stringfortesting";
		JsonIterator iter = JsonIterator.parse("stringfortesting\"");
		int endPos = IterImpl.readStringSlowPath(iter, 0);
		String res = new String(iter.reusableChars, 0, endPos);
		IterImpl.writeCCToFile();
		assertEquals(sample, res);
	}

	public void testReadEscapedCharString() throws Exception {
		String sample = "\b\t\n\f\r/\\";
		JsonIterator iter = JsonIterator.parse("\\b\\t\\n\\f\\r\\/\\\\\"");
		int endPos = IterImpl.readStringSlowPath(iter, 0);
		String res = new String(iter.reusableChars, 0, endPos);
		IterImpl.writeCCToFile();
		assertEquals(sample, res);
	}

	public void testDynamicBufResize() throws Exception {
		String sample = "longStringLongStringLongStringLongStringLongStringLongStringLongStringLongString";
		JsonIterator iter = JsonIterator.parse("longStringLongStringLongStringLongStringLongStringLongStringLongStringLongString\"");
		int endPos = IterImpl.readStringSlowPath(iter, 0);
		String res = new String(iter.reusableChars, 0, endPos);
		IterImpl.writeCCToFile();
		assertEquals(sample, res);
	}

	public void testIncompleteString() throws Exception {
		JsonIterator iter = JsonIterator.parse("incomplete");
		boolean flag = true;
		try{
			IterImpl.readStringSlowPath(iter, 0);
		}catch(Exception e){
			assertTrue(e.getMessage().contains("incomplete string"));
			flag = false;
		}
		if(flag) 
			fail();
	}
}
