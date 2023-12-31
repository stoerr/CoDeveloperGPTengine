package net.stoerr.chatgpt.codevengine;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Range;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Catch-all class for various utilities not related to a class.
 */
public class TbUtils {
    static final Path requestLog = CoDeveloperEngine.currentDir.resolve(".cgptcodeveloper/.requestlog.txt");
    public static final PrintStream ERRLOG = System.err;
    public static final PrintStream LOG = System.out;

    static boolean isLoggingEnabled = true;

    /**
     * If there is a file named .cgptcodeveloper/.requestlog.txt, we append the request data to it.
     */
    protected static void logRequest(HttpServletRequest request) {
        if (isLoggingEnabled && Files.exists(requestLog)) {
            try {
                String isoDate = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
                String logmsg = isoDate + " ##### "
                        + request.getMethod() + " " + request.getRequestURI()
                        + (request.getQueryString() != null && !request.getQueryString().isEmpty() ? "?" + request.getQueryString() : "")
                        + "\n";
                Files.write(requestLog, logmsg.getBytes(), StandardOpenOption.APPEND);
            } catch (IOException e) { // not critical but strange - we'd want to know.
                logError("Could not write to request log " + requestLog + ": " + e);
            }
        }
    }

    protected static void logBody(String parameterName, String parameterValue) {
        if (isLoggingEnabled) {
            if (parameterValue != null && parameterValue.length() > 400) {
                parameterValue = parameterValue.substring(0, 200) + "\n... (part omitted)\n" + parameterValue.substring(parameterValue.length() - 200);
            }
            logInfo(parameterName + ": " + parameterValue + "\n");
            if (Files.exists(requestLog)) {
                try {
                    Files.write(requestLog, (parameterName + ": " + parameterValue + "\n").getBytes(), StandardOpenOption.APPEND);
                } catch (IOException e) { // not critical but strange - we'd want to know.
                    logError("Could not write to request log " + requestLog + ": " + e);
                }
            }
        }
    }

    static void logStacktrace(Exception e) {
        e.printStackTrace(ERRLOG);
    }

    static void logError(String msg) {
        ERRLOG.println(msg);
    }

    static void logInfo(String msg) {
        if (isLoggingEnabled) {
            LOG.println(msg);
        }
    }

    static void logVersion() {
        Properties properties = new Properties();
        try {
            InputStream gitPropertiesStream = CoDeveloperEngine.class.getResourceAsStream("/git.properties");
            if (gitPropertiesStream != null) {
                properties.load(gitPropertiesStream);
                String versionInfo = "CoDeveloperEngine version" + properties.getProperty("git.build.version") +
                        properties.getProperty("git.commit.id.describe") + " from " + properties.getProperty("git.build.time");
                logBody("\n\nversion", versionInfo);
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
            InputStream gitPropertiesStream = CoDeveloperEngine.class.getResourceAsStream("/git.properties");
            if (gitPropertiesStream != null) {
                properties.load(gitPropertiesStream);
                gitInfo = properties.getProperty("git.commit.id.describe");
            }
        } catch (IOException e) {
            logError("Could't read git.properties - " + e);
        }
        properties = new Properties();
        try {
            InputStream mainPropertiesStream = CoDeveloperEngine.class.getResourceAsStream("/main.properties");
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
    protected static String compileReplacement(HttpServletResponse response, String replacementWithGroupReferences) {
        Matcher invalidGroupMatcher = Pattern.compile("\\$[^0-9$]").matcher(replacementWithGroupReferences);
        if (invalidGroupMatcher.find()) {
            throw AbstractPluginAction.sendError(response, 400, "Invalid replacement pattern " + invalidGroupMatcher.group());
        }
        String compiled = replacementWithGroupReferences.replace("\\", "\\\\");
        compiled = compiled.replace("$$", "\\$");
        return compiled;
    }

    protected static void addShortContentReport(String content, StringBuilder output) {
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
    }

    protected static long lineNumberAfter(String contentpart) {
        return (contentpart + "x").split("\n").length;
    }

    static List<String> rangeDescription(List<Range<Long>> modifiedLineNumbers) {
        List<String> modifiedLineDescr = new ArrayList<>();
        Range<Long> lastRange = null;
        for (Range<Long> range : modifiedLineNumbers) {
            if (lastRange != null) {
                if (lastRange.upperEndpoint() >= range.lowerEndpoint() - 1) {
                    lastRange = lastRange.span(range);
                } else {
                    modifiedLineDescr.add(rangeDescription(lastRange));
                    lastRange = range;
                }
            } else {
                lastRange = range;
            }
        }
        if (lastRange != null) {
            modifiedLineDescr.add(rangeDescription(lastRange));
        }
        return modifiedLineDescr;
    }

    private static String rangeDescription(Range<Long> lastRange) {
        String rangeDescr;
        if (lastRange.lowerEndpoint().equals(lastRange.upperEndpoint())) {
            rangeDescr = String.valueOf(lastRange.lowerEndpoint());
        } else {
            rangeDescr = " " + lastRange.lowerEndpoint() + " - " + lastRange.upperEndpoint();
        }
        return rangeDescr;
    }
}
