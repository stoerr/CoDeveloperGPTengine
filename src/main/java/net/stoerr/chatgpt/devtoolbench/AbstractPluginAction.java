package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.TbUtils.log;
import static net.stoerr.chatgpt.devtoolbench.TbUtils.logBody;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.wildfly.common.annotation.NotNull;

import com.google.gson.Gson;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public abstract class AbstractPluginAction implements HttpHandler {

    private final Gson gson = new Gson();

    /**
     * Logs an error and sends it to ChatGPT, always throws {@link ExecutionAbortedException}.
     * Use with pattern {@code throw sendError(...)} to let compiler know that.
     */
    protected static ExecutionAbortedException sendError(HttpServerExchange exchange, int statusCode, String error) throws ExecutionAbortedException {
        log("Error " + statusCode + ": " + error);
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
        exchange.getResponseSender().send(error);
        throw new ExecutionAbortedException(error);
    }

    protected static Stream<Path> findMatchingFiles(HttpServerExchange exchange, Path path, Pattern filePathPattern, Pattern grepPattern) {
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
            throw sendError(exchange, 500, "Error reading " + path + " : " + e);
        }

        return result.stream()
                .filter(Files::isRegularFile)
                .filter(p -> !DevToolBench.IGNORE.matcher(p.toString()).matches())
                .filter(p -> filePathPattern == null || filePathPattern.matcher(p.toString()).find())
                .filter(p -> {
                    if (grepPattern == null) {
                        return true;
                    } else {
                        try (Stream<String> lines = Files.lines(p)) {
                            return lines.anyMatch(line -> grepPattern.matcher(line).find());
                        } catch (Exception e) {
                            log("Error reading " + p + " : " + e);
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

    protected String getQueryParam(HttpServerExchange exchange, String name) {
        Deque<String> paramDeque = exchange.getQueryParameters().get(name);
        return paramDeque != null ? paramDeque.peekFirst() : null;
    }

    protected String getMandatoryQueryParam(HttpServerExchange exchange, String name) {
        String result = getQueryParam(exchange, name);
        if (null == result) {
            log("Missing query parameter " + name + " in " + exchange.getRequestURI());
            throw sendError(exchange, 400, "Missing query parameter " + name);
        }
        return result;
    }

    protected Path getPath(HttpServerExchange exchange) {
        String path = getMandatoryQueryParam(exchange, "path");
        if (DevToolBench.IGNORE.matcher(path).matches()) {
            throw sendError(exchange, 400, "Access to path " + path + " is not allowed! (matches " + DevToolBench.IGNORE.pattern() + ")");
        }
        Path resolved = DevToolBench.currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(DevToolBench.currentDir)) {
            throw sendError(exchange, 400, "Path " + path + " is outside of current directory!");
        }
        return resolved;
    }

    /**
     * Returns a parameter encoded in JSON the request body; returns "" if that parameter isn't there.
     */
    @NotNull
    protected String getBodyParameter(HttpServerExchange exchange, String json, String parameterName, boolean mandatory) {
        String parameterValue = "";
        if (!json.isEmpty() && !"{}".equals(json)) {
            try {
                Map<String, Object> decoded = gson.fromJson(json, Map.class);
                parameterValue = String.valueOf(decoded.getOrDefault(parameterName, ""));
                logBody(parameterName, parameterValue);
                if (mandatory && !decoded.containsKey(parameterName)) {
                    throw sendError(exchange, 400, "Missing parameter " + parameterName);
                }
            } catch (Exception e) {
                String error = "Parse error for content: " + e;
                throw sendError(exchange, 400, error);
            }
        }
        return parameterValue;
    }

    protected void handleRequestBodyError(HttpServerExchange httpServerExchange, IOException e) {
        throw sendError(httpServerExchange, 400, "Error reading request body: " + e);
    }

    protected String mappedFilename(Path path) {
        return DevToolBench.currentDir.relativize(path).toString();
    }

    protected String abbreviate(String s, int max) {
        if (s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "...";
    }

    protected boolean isNotEmpty(String s) {
        return null != s && !s.trim().isEmpty();
    }

}
