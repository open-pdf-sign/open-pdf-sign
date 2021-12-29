package org.openpdfsign;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    @Test
    public void testConfigLoaded() {
        Configuration instance = Configuration.getInstance();

    }

    @Test
    public void testConfigLoadedLocale() {
        Locale locale = new Locale("de","AT");
        Configuration instance = Configuration.getInstance(locale);
        String[] tsp_sources = instance.getProperties().getStringArray("tsp_sources");
        assertNotNull(tsp_sources);
        assertTrue(tsp_sources.length > 1);
    }
}