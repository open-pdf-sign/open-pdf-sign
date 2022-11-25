package org.openpdfsign;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SignerServlet extends HttpServlet {
    ObjectMapper mapper = new ObjectMapper();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {

        /**
         * proxy_set_header Host $http_host;
         * proxy_set_header X-Open-Pdf-Sign-Nginx-Version 1.0.0;
         * proxy_set_header X-Open-Pdf-Sign-File: $document_root$uri
         */
        String keyName = null;

        HashSet<String> headers = new HashSet<>(Collections.list(req.getHeaderNames()));
        Path path;
        if (!headers.contains("X-Open-Pdf-Sign-Nginx-Version")) {
            res.setStatus(400);
            res.getOutputStream().println("X-Open-Pdf-Sign-Nginx-Version header missing");
            res.getOutputStream().flush();
            log.debug("received request with missing X-Open-Pdf-Sign-Nginx-Version header");
            return;
        }
        if (!headers.contains("X-Open-Pdf-Sign-File")) {
            res.setStatus(400);
            res.getOutputStream().println("X-Open-Pdf-Sign-File header missing");
            res.getOutputStream().flush();
            log.debug("received request without filename");
            return;
        }
        path = Paths.get(req.getHeader("X-Open-Pdf-Sign-File"));

        if (headers.contains("Host")) {
            //try to find matching key, or default
            String hostname = req.getHeader("Host");
            if (ServerConfigHolder.getInstance().getKeystores().containsKey(hostname)) {
                keyName = hostname;
            }
            else if (ServerConfigHolder.getInstance().getKeystores().containsKey("_")) {
                keyName = "_";
            }
            else {
                //key not found, exception
                res.setStatus(400);
                res.getOutputStream().println("no key loaded for host");
                res.getOutputStream().flush();
                log.debug("received request with invalid host header, no default key: ", hostname);
                return;
            }
        }

        if (!path.toFile().exists()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getOutputStream().flush();
            return;
        }

        Signer s = new Signer();
        res.setStatus(HttpServletResponse.SC_OK);
        res.setHeader("Content-Disposition", "attachment; filename=\"" + path.getFileName().toString() + "\"");
        s.signPdf(path, null, ServerConfigHolder.getInstance().getKeystores().get(keyName), ServerConfigHolder.getInstance().getKeystorePassphrase(), res.getOutputStream(), ServerConfigHolder.getInstance().getParams());
        log.debug("signed " + path + " with " + keyName);
        res.getOutputStream().flush();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        //get path
        Path path;
        String keyPath;
        Map<String, String> errorMap = new HashMap<>();
        if (req.getContentType().equals("application/json")) {
            String requestAsJson = req.getReader().lines().collect(Collectors.joining());
            try {
                CommandLineArguments args = mapper.reader().readValue(requestAsJson, CommandLineArguments.class);
                path = Paths.get(args.getInputFile());
                keyPath = args.getKeyFile();
            }
            catch(RuntimeException e) {
                errorMap.put("error","invalid json arguments");
                res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                res.getOutputStream().print(mapper.writeValueAsString(errorMap));
                return;
            }
        } else {
            errorMap.put("error","invalid json");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getOutputStream().print(mapper.writeValueAsString(errorMap));
            return;
        }

        if (keyPath != null) {
            if (ServerConfigHolder.getInstance().getKeystores().containsKey(keyPath)) {
                //key matches
            }
            else if (ServerConfigHolder.getInstance().getKeystores().containsKey("_")) {
                keyPath = "_";
            }
            else {
                //key not found, exception
                res.setStatus(400);
                res.getOutputStream().println("no key loaded for host");
                res.getOutputStream().flush();
                log.debug("received request with invalid host header, no default key: ", keyPath);
                return;
            }
        }

        if (!path.toFile().exists()) {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        //key needs to be loaded OR not given
        if (keyPath == null) {
            keyPath = ServerConfigHolder.getInstance().getKeystores().keySet().stream().findFirst().get();
        } else if (!ServerConfigHolder.getInstance().getKeystores().containsKey(keyPath)) {
            errorMap.put("error","keyfile not loaded on server startup");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            res.getOutputStream().print(mapper.writeValueAsString(errorMap));
            return;
        }

        //sign pdf
        Signer s = new Signer();
        res.setStatus(HttpServletResponse.SC_OK);
        res.setHeader("Content-Disposition", "attachment; filename=\"" + path.getFileName().toString() + "\"");
        s.signPdf(path, null, ServerConfigHolder.getInstance().getKeystores().get(keyPath), ServerConfigHolder.getInstance().getKeystorePassphrase(), res.getOutputStream(), ServerConfigHolder.getInstance().getParams());
        log.debug("signed " + path + " with " + keyPath);
        res.getOutputStream().flush();
    }
}
