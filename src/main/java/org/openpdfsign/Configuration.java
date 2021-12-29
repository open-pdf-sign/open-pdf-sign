package org.openpdfsign;


import lombok.Getter;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.convert.DefaultListDelimiterHandler;
import org.apache.commons.configuration2.ex.ConfigurationException;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

@Getter
public class Configuration {
    private static Configuration INSTANCE;

    private PropertiesConfiguration properties;
    private ResourceBundle resourceBundle;

    private Configuration(Locale locale) {
        //load from properties file
        Configurations configurations = new Configurations();
        try {
            this.properties = configurations.properties("application.properties");
            properties.setListDelimiterHandler(new DefaultListDelimiterHandler(','));

            //load resourceBundle, if any
            if (locale == null) {
                resourceBundle = ResourceBundle.getBundle("strings", Locale.US);
            }
            else {
                try {
                    resourceBundle = ResourceBundle.getBundle("strings", locale);
                } catch (MissingResourceException e) {
                    e.printStackTrace();
                    resourceBundle = ResourceBundle.getBundle("strings", Locale.US);
                }

            }


        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static Configuration getInstance(Locale locale) {
        if (INSTANCE == null) {
            INSTANCE = new Configuration(locale);
        }
        return INSTANCE;
    }

    public static Configuration getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Configuration(null);
        }
        return INSTANCE;
    }


}
