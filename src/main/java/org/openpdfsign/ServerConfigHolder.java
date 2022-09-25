package org.openpdfsign;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class ServerConfigHolder {
    private static ServerConfigHolder INSTANCE = new ServerConfigHolder();

    private CommandLineArguments params;
    private Map<String, byte[]> keystores = new HashMap<>();
    private char[] keystorePassphrase;

    public static ServerConfigHolder getInstance() {
        return INSTANCE;
    }


}