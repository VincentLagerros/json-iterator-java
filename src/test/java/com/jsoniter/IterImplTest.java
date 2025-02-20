package com.jsoniter;

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


}
