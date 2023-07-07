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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractPluginAction extends HttpServlet {

    private final Gson gson = new Gson();

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
                    if (DevToolBench.IGNORE.matcher(dir.toString()).matches()) {
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
                .filter(p -> !DevToolBench.IGNORE.matcher(p.toString()).matches()
                        || DevToolBench.OVERRIDE_IGNORE.matcher(p.toString()).matches())
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

    protected Path getPath(HttpServletRequest request, HttpServletResponse response) {
        String path = getMandatoryQueryParam(request, response, "path");
        if (DevToolBench.IGNORE.matcher(path).matches() && !DevToolBench.OVERRIDE_IGNORE.matcher(path).matches()) {
            throw sendError(response, 400, "Access to path " + path + " is not allowed! (matches " + DevToolBench.IGNORE.pattern() + ")");
        }
        Path resolved = DevToolBench.currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(DevToolBench.currentDir)) {
            throw sendError(response, 400, "Path " + path + " is outside of current directory!");
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
                e.printStackTrace();
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
