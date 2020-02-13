package org.exoplatform.wiki.utils;

import junit.framework.TestCase;

public class TestWikiNameValidator extends TestCase {
    public void testWikiValidateFileName() {
        String text="exo.platform";
        try {
          WikiNameValidator.validateFileName(text);
        } catch(Exception e) {
          fail();
        }
        assertEquals("exo.platform","exo.platform");
    }
}
