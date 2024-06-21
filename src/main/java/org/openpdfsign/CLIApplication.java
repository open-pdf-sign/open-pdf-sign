package org.openpdfsign;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Strings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Scanner;

@Slf4j
public class CLIApplication {

    public static void main(String[] args) throws Exception {
        log.debug("Starting open-pdf-sign");
        CommandLineArguments cla = parseArguments(args);

        if (cla == null) {
            System.out.println("Try '--help' or '-h' for more information.");
            System.exit(1);
            return;
        }

        log.debug("Running with parameters: " + cla);

        //load locale, if any
        if (!Strings.isStringEmpty(cla.getLocale())) {
            Configuration.getInstance(Locale.forLanguageTag(cla.getLocale()));
        }

        //convert to keystore, if not already given
        byte[] keystore = null;
        char[] keystorePassphrase;
        if (!Strings.isStringEmpty(cla.getKeyPassphrase())) {
            keystorePassphrase = cla.getKeyPassphrase().toCharArray();
        } else {
            keystorePassphrase = "123456789".toCharArray();
        }
        boolean correctPassphraseAvailable = false;
        char[] passphrase = (cla.getKeyPassphrase() == null) ? null : cla.getKeyPassphrase().toCharArray();
        while (!correctPassphraseAvailable) {
            try {
                if (!Strings.isStringEmpty(cla.getCertificateFile()) &&
                        !Strings.isStringEmpty(cla.getKeyFile())) {
                    //chain and key were provided (e.g. as PEM files)
                    keystore = KeyStoreLoader.loadKeyStoreFromKeys(
                            Paths.get(cla.getCertificateFile()),
                            Paths.get(cla.getKeyFile()),
                            passphrase,
                            keystorePassphrase
                    );
                    log.debug("Key and Certificate loaded");

                } else if (!Strings.isStringEmpty(cla.getKeyFile())) {
                    //a keystore (.jks or .pfx) was provided
                    keystore = KeyStoreLoader.loadFromKeystore(Paths.get(cla.getKeyFile()), passphrase);
                    keystorePassphrase = passphrase;
                }

                correctPassphraseAvailable = true;
            } catch (KeyStoreLoader.KeyIsNeededException e) {
                //load key from stdin if not provided
                Console console = System.console();
                if (console == null) {
                    log.error("No console available. Falling back to Scanner.");
                    System.out.print("Please provide passphrase for private key file> ");
                    Scanner in = new Scanner(System.in);
                    String userPassphrase = in.nextLine();
                    passphrase = userPassphrase.toCharArray();
                } else {
                    passphrase = console.readPassword("Please provide passphrase for private key file> ");
                }
                if (passphrase == null || passphrase.length == 0) {
                    System.out.println("Passphrase not provided, quitting.");
                    System.exit(1);
                    return;
                }
            }
        }

        if (cla.getPort() > 0 || cla.getHostname() != null) {
            //set args + keys for later use
            ServerConfigHolder.getInstance().setParams(cla);

            if (cla.getCertificates() != null && !cla.getCertificates().isEmpty()) {
                //load all the keys
                char[] staticPassphrase = keystorePassphrase;
                cla.getCertificates().stream().forEach(cp -> {
                    try {
                        byte[] lKeystore = KeyStoreLoader.loadKeyStoreFromKeys(
                                Paths.get(cp.getCertificateFile()),
                                Paths.get(cp.getKeyFile()),
                                (cla.getKeyPassphrase() == null) ? null : cla.getKeyPassphrase().toCharArray(),
                                staticPassphrase
                        );
                        ServerConfigHolder.getInstance().getKeystores().put(cp.getHost(), lKeystore);
                    } catch (Exception e) {
                        log.error("could not load key from " + cp.getKeyFile() + " / " + cp.getCertificateFile());
                    }
                });
            }
            else {
                ServerConfigHolder.getInstance().getKeystores().put("_", keystore);
            }
            ServerConfigHolder.getInstance().setKeystorePassphrase(keystorePassphrase);

            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            ServletHandler servletHandler = new ServletHandler();
            server.setHandler(servletHandler);
            servletHandler.addServletWithMapping(SignerServlet.class,"/*");
            connector.setPort(cla.getPort() > 0 ? cla.getPort() : 8090);
            connector.setHost(cla.getHostname() != null ? cla.getHostname() : "localhost");
            server.setConnectors(new Connector[] {connector});
            server.start();
            log.info("Server launched " + connector.getHost() + ":" + connector.getPort());
            return;
        }
        else {
            Path pdfFile = Paths.get(cla.getInputFile());
            Path outputFile = cla.getOutputFile() == null ? null : Paths.get(cla.getOutputFile());

            Signer s = new Signer();
            s.signPdf(pdfFile, outputFile, keystore, keystorePassphrase, cla.isBinaryOutput() ? System.out : null, cla);
        }
    }

    public static CommandLineArguments parseArguments(String[] args) {
        CommandLineArguments cla = new CommandLineArguments();
        JCommander parser = JCommander.newBuilder()
                .addObject(cla)
                .build();


        try {
            parser.parse(args);

            // show help page
            if (cla.isHelp()) {
                parser.usage();
                System.exit(0);
            }

            //show version
            if (cla.isVersion()) {
                System.out.println("v" + CLIApplication.class.getPackage().getImplementationVersion());
                System.exit(0);
            }

            // if config is passed, may use this
            if (!Strings.isStringEmpty(cla.getConfigFile())) {
                //try to load and parse config
                try {
                    log.debug("loading config file from " + cla.getConfigFile());
                    String yamlSource = FileUtils.readFileToString(new File(cla.getConfigFile()), "utf-8");
                    ObjectMapper mapper = new YAMLMapper();
                    CommandLineArguments yamlCommandlineArgs = mapper.readValue(yamlSource, CommandLineArguments.class);

                    //in case of space-separated hosts, split
                    if (yamlCommandlineArgs.getCertificates() != null &&
                            !yamlCommandlineArgs.getCertificates().isEmpty()) {
                        ListIterator<CommandLineArguments.HostKeyCertificatePair> iterator = yamlCommandlineArgs.getCertificates().listIterator();
                        while (iterator.hasNext()) {
                            //split if space-separated
                            CommandLineArguments.HostKeyCertificatePair pair = iterator.next();
                            if (pair.getHost().contains(" ")) {
                                CommandLineArguments.HostKeyCertificatePair newPair = new CommandLineArguments.HostKeyCertificatePair();
                                newPair.setCertificateFile(pair.getCertificateFile());
                                newPair.setKeyFile(pair.getKeyFile());
                                newPair.setHost(pair.getHost().split(" ",2)[1]);
                                iterator.add(newPair);
                                iterator.previous();
                            }
                            pair.setHost(pair.getHost().split(" ")[0]);
                        }
                    }

                    //combine, command line overrides yaml
                    try {
                        CommandLineArguments defaults = new CommandLineArguments();

                        PropertyUtils.describe(cla).entrySet().stream()
                                .filter(e -> {
                                    //only override if not null and not default
                                    try {
                                        if (e.getValue() == null ||
                                                e.getValue() == PropertyUtils.getProperty(defaults, e.getKey()) ||
                                                e.getValue().equals(PropertyUtils.getProperty(defaults, e.getKey()))) {
                                            return false;
                                        }
                                        return true;

                                    } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                    }
                                })
                                .filter(e -> !e.getKey().equals("class"))
                                .forEach(e -> {
                                    try {
                                        PropertyUtils.setProperty(yamlCommandlineArgs, e.getKey(), e.getValue());
                                    } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                    }
                                });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    cla = yamlCommandlineArgs;

                } catch(MismatchedInputException e) {
                    System.out.println("Error parsing configuration file");
                    System.out.println(e.getMessage());
                    return null;
                }
                catch (IOException e) {
                    System.out.println("provided config file could not be found");
                    return null;
                }
            }

            if (cla.getHostname() != null && cla.getPort() > 0) {
                //server mode
                if ((cla.getCertificates() == null || cla.getCertificates().isEmpty()) &&
                        cla.getKeyFile() == null) {
                    System.out.println("no key file provided for server mode");
                    return null;
                }
            }
            else {
                //input file needs to be given
                if (cla.getInputFile() == null || cla.getInputFile().isEmpty()) {
                    System.out.println("input file missing");
                    return null;
                }

                //key needs to be given
                if (cla.getKeyFile() == null || cla.getKeyFile().isEmpty()) {
                    System.out.println("key file needs to be provided");
                    return null;
                }

                //either binary or output file has to be set
                if (!cla.isBinaryOutput() && cla.getOutputFile() == null) {
                    System.out.println("Either binary output or output file has to be set");
                    return null;
                }
            }

        }
        catch(ParameterException ex) {
            ex.printStackTrace();
            return null;
        }

        return cla;
    }
}
