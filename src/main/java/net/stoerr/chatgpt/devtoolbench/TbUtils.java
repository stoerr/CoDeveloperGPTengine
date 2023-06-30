package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.undertow.server.HttpServerExchange;

/**
 * Catch-all class for various utilities not related to a class.
 */
public class TbUtils {
    static final Path requestLog = DevToolBench.currentDir.resolve(".cgptdevbench/.requestlog.txt");
    public static final PrintStream ERRLOG = System.err;
    public static final PrintStream LOG = System.out;

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
        e.printStackTrace(ERRLOG);
    }

    static void logError(String msg) {
        ERRLOG.println(msg);
    }

    static void log(String msg) {
        LOG.println(msg);
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

    /**
     * Transforms our simpler specification of replacement patterns
     * (group references $0, $1, ..., $9 with the corresponding groups from the match; a literal $ must be given as $$)
     * to what {@link Matcher#appendReplacement(StringBuffer, String)} expects. We want to simplify the backslash handling,
     * as backslashes are pretty common in Java source code, while $ is not.
     */
    protected static String compileReplacement(HttpServerExchange exchange, String replacementWithGroupReferences) {
        Matcher invalidGroupMatcher = Pattern.compile("\\$[^0-9$]").matcher(replacementWithGroupReferences);
        if (invalidGroupMatcher.find()) {
            throw AbstractPluginAction.sendError(exchange, 400, "Invalid replacement pattern " + invalidGroupMatcher.group());
        }
        String compiled = replacementWithGroupReferences.replace("\\", "\\\\");
        compiled = compiled.replace("$$", "\\$");
        return compiled;
    }

    protected static String addShortContentReport(String content, StringBuilder output) {
        String[] lines = content.split("\n");
        if (lines.length > 5) {
            output.append("(abbreviated):\n");
            output.append(lines[0]).append("\n").append(lines[1]).append("\n");
            output.append("\n...\n");
            output.append(lines[lines.length - 2]).append("\n").append(lines[lines.length - 1]).append("\n");
        } else {
            output.append(":\n");
            output.append(content);
        }
        return output.toString();
    }

}
