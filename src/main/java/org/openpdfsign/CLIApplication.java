package org.openpdfsign;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Strings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ListIterator;
import java.util.Locale;

@Slf4j
public class CLIApplication {

    public static void main(String[] args) throws Exception {
        log.debug("Starting open-pdf-sign");
        CommandLineArguments cla = parseArguments(args);

        if (cla == null) {
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
        char[] keystorePassphrase = null;
        if (!Strings.isStringEmpty(cla.getKeyPassphrase())) {
            keystorePassphrase = cla.getKeyPassphrase().toCharArray();
        } else {
            keystorePassphrase = "123456789".toCharArray();
        }
        if (!Strings.isStringEmpty(cla.getCertificateFile()) &&
            !Strings.isStringEmpty(cla.getKeyFile())) {
            keystore = KeyStoreLoader.loadKeyStoreFromKeys(
                    Paths.get(cla.getCertificateFile()),
                    Paths.get(cla.getKeyFile()),
                    (cla.getKeyPassphrase() == null) ? null : cla.getKeyPassphrase().toCharArray(),
                    keystorePassphrase
            );
            log.debug("Key and Certificate loaded");

        }
        else if (!Strings.isStringEmpty(cla.getKeyFile()) &&
                !Strings.isStringEmpty(cla.getKeyPassphrase())) {
            keystore = Files.readAllBytes(Paths.get(cla.getKeyFile()));
        }

        if (cla.getPort() > 0 || cla.getHostname() != null) {
            //set args + keys for later use
            ServerConfigHolder.getInstance().setParams(cla);
            ServerConfigHolder.getInstance().getKeystores().put(cla.getKeyFile(), keystore);
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

            //if config is passed, may use this
            if (!Strings.isStringEmpty(cla.getConfigFile())) {
                //try to load and parse config
                try {
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

                    return yamlCommandlineArgs;
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

            //either binary or output file has to be set
            if (!cla.isBinaryOutput() && cla.getOutputFile() == null && cla.getHostname() == null && cla.getPort() <= 0) {
                System.out.println("Either binary output or output file has to be set");
                parser.usage();
                return null;
            }

        }
        catch(ParameterException ex) {
            ex.printStackTrace();
            parser.usage();
            return null;
        }

        return cla;
    }
}
