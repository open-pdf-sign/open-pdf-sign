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
        String keyPath = ServerConfigHolder.getInstance().getKeystores().keySet().stream().findFirst().get();

        HashSet<String> headers = new HashSet<>(Collections.list(req.getHeaderNames()));
        Path path;
        if (headers.contains("X-Open-Pdf-Sign-File")) {
            path = Paths.get(req.getHeader("X-Open-Pdf-Sign-File"));
        }
        else {
            path = Paths.get(req.getRequestURI());
        }



        Signer s = new Signer();
        res.setStatus(HttpServletResponse.SC_OK);
        res.setHeader("Content-Disposition", "attachment; filename=\"" + path.getFileName().toString() + "\"");
        s.signPdf(path, null, ServerConfigHolder.getInstance().getKeystores().get(keyPath), ServerConfigHolder.getInstance().getKeystorePassphrase(), res.getOutputStream(), ServerConfigHolder.getInstance().getParams());
        log.debug("signed " + path + " with " + keyPath);
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
