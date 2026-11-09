package com.shopping.app;

import org.junit.Test;
import static org.junit.Assert.*;

public class VersionsTest {
    @Test public void stripsReleasePrefix() {
        assertEquals(0, Versions.compare("v1.11.0", "1.11.0"));
        assertEquals(0, Versions.compare("V1.11.0", "v1.11.0"));
    }

    @Test public void comparesMinorNumberNumericNotLexical() {
        assertTrue(Versions.compare("v1.11.0", "1.9.9") > 0);
        assertTrue(Versions.compare("v1.2.0", "1.10.0") < 0);
    }

    @Test public void comparesPatchAndMissingSegments() {
        assertTrue(Versions.compare("v1.0.10", "1.0.9") > 0);
        assertEquals(0, Versions.compare("1.11", "1.11.0"));
        assertTrue(Versions.compare("2.0", "1.99.99") > 0);
    }

    @Test public void ignoresPrereleaseSuffixWhenNumbersMatch() {
        assertEquals(0, Versions.compare("v1.11.0-beta1", "1.11.0"));
    }

    @Test public void handlesEmptyAndMalformedInput() {
        assertEquals(0, Versions.compare("", null));
        assertEquals(0, Versions.compare("v", ""));
        assertTrue(Versions.compare("v1", "v0") > 0);
    }
}
