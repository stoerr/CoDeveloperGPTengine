package net.stoerr.chatgpt.forChatGPTtoMigrate;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;

abstract class AbstractPluginOperation {
    protected final Path currentDir = Path.of(".").normalize().toAbsolutePath();

    public abstract void handle(HttpExchange exchange) throws IOException;

    /**
     * The URL it is deployed at, e.g. /listFiles.
     */
    public abstract String getUrl();

    /**
     * The OpenAPI description for this operation.
     */
    public abstract String openApiDescription();

    protected Map<String, String> getQueryParams(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return Collections.emptyMap();
        }
        return Arrays.stream(query.split("&")).map(s -> s.split("=")).collect(Collectors.toMap(a -> a[0], a -> URLDecoder.decode(a[1], UTF_8)));
    }

    protected Path getPath(HttpExchange exchange) {
        String path = getQueryParams(exchange).get("path");
        Path resolved = currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(currentDir)) {
            throw new IllegalArgumentException("Path " + path + " is not in current directory " + currentDir);
        }
        return resolved;
    }

    protected String jsonRep(String string) {
        string = string == null ? "" : string;
        string = string.replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + string.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }
}
