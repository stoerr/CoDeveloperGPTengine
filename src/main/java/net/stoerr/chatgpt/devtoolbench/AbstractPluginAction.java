package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.TbUtils.logBody;
import static net.stoerr.chatgpt.devtoolbench.TbUtils.logInfo;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractPluginAction extends HttpServlet {

    private final transient Gson gson = new Gson();

    /**
     * Logs an error and sends it to ChatGPT, always throws {@link ExecutionAbortedException}.
     * Use with pattern {@code throw sendError(...)} to let compiler know that.
     */
    protected static ExecutionAbortedException sendError(HttpServletResponse response, int statusCode, String error) throws ExecutionAbortedException {
        logInfo("Error " + statusCode + ": " + error);
        response.setStatus(statusCode);
        response.setContentType("text/plain;charset=UTF-8");
        try {
            response.getWriter().write(error);
        } catch (IOException e) {
            logInfo("Error writing error: " + e);
        }
        throw new ExecutionAbortedException();
    }

    protected static Stream<Path> findMatchingFiles(HttpServletResponse response, Path path, Pattern filePathPattern, Pattern grepPattern) {
        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (isIgnored(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult res = super.visitFile(file, attrs);
                    result.add(file);
                    return res;
                }
            });
        } catch (IOException e) {
            throw sendError(response, 500, "Error reading " + path + " : " + e);
        }

        return result.stream()
                .filter(Files::isRegularFile)
                .filter(p -> !isIgnored(p))
                .filter(p -> filePathPattern == null || filePathPattern.matcher(p.toString()).find())
                .filter(p -> {
                    if (grepPattern == null) {
                        return true;
                    } else {
                        try (Stream<String> lines = Files.lines(p)) {
                            return lines.anyMatch(line -> grepPattern.matcher(line).find());
                        } catch (Exception e) {
                            logInfo("Error reading " + p + " : " + e);
                            return false;
                        }
                    }
                }).sorted();
    }

    protected static boolean isIgnored(Path path) {
        return DevToolBench.IGNORE.matcher(path.toString()).matches()
                && !DevToolBench.OVERRIDE_IGNORE.matcher(path.toString()).matches();
    }

    /**
     * The URL it is deployed at, e.g. /listFiles.
     */
    public abstract String getUrl();

    /**
     * The OpenAPI description for this operation.
     */
    public abstract String openApiDescription();

    protected String getQueryParam(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    protected String getMandatoryQueryParam(HttpServletRequest request, HttpServletResponse response, String name) {
        String result = getQueryParam(request, name);
        if (null == result) {
            logInfo("Missing query parameter " + name + " in " + request.getRequestURI());
            throw sendError(response, 400, "Missing query parameter " + name);
        }
        return result;
    }

    protected Path getPath(HttpServletRequest request, HttpServletResponse response, boolean mustExist) {
        String path = getMandatoryQueryParam(request, response, "path");
        if (DevToolBench.IGNORE.matcher(path).matches() && !DevToolBench.OVERRIDE_IGNORE.matcher(path).matches()) {
            throw sendError(response, 400, "Access to path " + path + " is not allowed! (matches " + DevToolBench.IGNORE.pattern() + ")");
        }
        Path resolved = DevToolBench.currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(DevToolBench.currentDir)) {
            throw sendError(response, 400, "Path " + path + " is outside of current directory!");
        }
        if (mustExist && !Files.exists(resolved)) {
            String message = "Path " + path + " does not exist! Try to list files with /listFiles to find the right path.";
            String filename = resolved.getFileName().toString();
            List<Path> matchingFiles = findMatchingFiles(response, DevToolBench.currentDir, null, null).toList();
            List<String> files = matchingFiles.stream()
                    .map(p -> DevToolBench.currentDir.relativize(p).toString())
                    .map(p -> Pair.of(p, StringUtils.getFuzzyDistance(p, filename, Locale.getDefault())))
                    .map(p -> Pair.of(p.getLeft(), -p.getRight()))
                    .sorted(Comparator.comparingDouble(Pair::getRight))
                    .limit(10)
                    .map(Pair::getLeft)
                    .toList();
            if (!files.isEmpty()) {
                message += "\n\nDid you mean one of these files?\n" + String.join("\n", files);
                if (files.size() < matchingFiles.size()) {
                    message += "\n\n(suggestion list truncated - there are " + matchingFiles.size() + " files; use listFiles to find more files).";
                }
            }
            throw sendError(response, 404, message);
        }
        return resolved;
    }

    /**
     * Returns a parameter encoded in JSON the request body; returns "" if that parameter isn't there.
     */
    @Nullable
    protected String getBodyParameter(HttpServletResponse response, String json, String parameterName, boolean mandatory) {
        String parameterValue = null;
        if (!json.isEmpty() && !"{}".equals(json)) {
            try {
                Map<String, Object> decoded = gson.fromJson(json, Map.class);
                Object parameterObj = decoded.get(parameterName);
                parameterValue = parameterObj != null ? parameterObj.toString() : null;
                logBody(parameterName, parameterValue);
                if (mandatory && !decoded.containsKey(parameterName)) {
                    throw sendError(response, 400, "Missing parameter " + parameterName);
                }
            } catch (Exception e) {
                String error = "Parse error for content: " + e;
                throw sendError(response, 400, error);
            }
        }
        return parameterValue;
    }

    protected String mappedFilename(Path path) {
        return DevToolBench.currentDir.relativize(path).toString();
    }

    protected String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "...";
    }

    protected boolean isNotEmpty(String s) {
        return null != s && !s.trim().isEmpty();
    }

}
