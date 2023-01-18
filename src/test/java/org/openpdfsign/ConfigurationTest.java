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
        Locale locale = new Locale("de", "AT");
        Configuration.resetTestingInstance();
        Configuration instance = Configuration.getInstance(locale);
        String[] tsp_sources = instance.getProperties().getStringArray("tsp_sources");
        assertNotNull(tsp_sources);
        assertTrue(tsp_sources.length > 1);
        assertEquals("Zeitpunkt", instance.getResourceBundle().getString("timestamp"));
    }


    @Test
    public void testConfigLoadedDefault() {
        Locale locale = new Locale("est", "EE");
        Configuration.resetTestingInstance();
        Configuration instance = Configuration.getInstance(locale);
        assertEquals("Timestamp", instance.getResourceBundle().getString("timestamp"));
    }
}