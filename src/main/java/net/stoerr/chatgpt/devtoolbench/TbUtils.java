package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import io.undertow.server.HttpServerExchange;

/**
 * Catch-all class for various utilities not related to a class.
 */
public class TbUtils {
    static final Path requestLog = DevToolBench.currentDir.resolve(".cgptdevbench/.requestlog.txt");

    /**
     * If there is a file named .cgptdevbench/.requestlog.txt, we append the request data to it.
     */
    protected static void logRequest(HttpServerExchange exchange) {
        if (Files.exists(requestLog)) {
            try {
                String isoDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                Files.writeString(requestLog, isoDate + " ##### "
                        + exchange.getRequestMethod() + " " + exchange.getRequestURI()
                        + (exchange.getQueryString() != null && !exchange.getQueryString().isEmpty() ? "?" + exchange.getQueryString() : "")
                        + "\n", StandardOpenOption.APPEND);
            } catch (IOException e) { // not critical but strange - we'd want to know.
                logError("Could not write to request log " + requestLog + ": " + e.getMessage());
            }
        }
    }

    protected static void logBody(String parameterName, String parameterValue) {
        if (Files.exists(requestLog)) {
            if (parameterValue.length() > 400) {
                parameterValue = parameterValue.substring(0, 200) + "\n... (part omitted)\n" + parameterValue.substring(parameterValue.length() - 200);
            }
            try {
                Files.writeString(requestLog, parameterName + ": " + parameterValue + "\n\n", StandardOpenOption.APPEND);
                log(parameterName + ": " + parameterValue + "\n");
            } catch (IOException e) { // not critical but strange - we'd want to know.
                logError("Could not write to request log " + requestLog + ": " + e.getMessage());
            }
        }
    }

    static void logStacktrace(Exception e) {
        e.printStackTrace(System.err);
    }

    static void logError(String msg) {
        System.err.println(msg);
    }

    static void log(String msg) {
        System.out.println(msg);
    }

    static void logVersion() {
        Properties properties = new Properties();
        try {
            InputStream gitPropertiesStream = DevToolBench.class.getResourceAsStream("/git.properties");
            if (gitPropertiesStream != null) {
                properties.load(gitPropertiesStream);
                String versionInfo = "DevToolBench version: " + properties.getProperty("git.build.version") +
                        properties.getProperty("git.commit.id.describe") + " from " + properties.getProperty("git.build.time");
                logBody("\n\nversion: ", versionInfo);
            }
        } catch (IOException e) {
            logError("Version: unknown");
        }
    }

    /**
     * Reads property pomversion from /main.properties and git.commit.id.describe from /git.properties and returns them concatenated.
     */
    static String getVersionString() {
        String gitInfo = "";
        String mainInfo = "0.0.1";
        Properties properties = new Properties();
        try {
            InputStream gitPropertiesStream = DevToolBench.class.getResourceAsStream("/git.properties");
            if (gitPropertiesStream != null) {
                properties.load(gitPropertiesStream);
                gitInfo = properties.getProperty("git.commit.id.describe");
            }
        } catch (IOException e) {
            logError("Could't read git.properties - " + e);
        }
        properties = new Properties();
        try {
            InputStream mainPropertiesStream = DevToolBench.class.getResourceAsStream("/main.properties");
            if (mainPropertiesStream != null) {
                properties.load(mainPropertiesStream);
                mainInfo = properties.getProperty("pomversion");
            }
        } catch (IOException e) {
            logError("Couldnot read main.properties - " + e);
        }
        return mainInfo + gitInfo;
    }
}
