package net.stoerr.chatgpt.devtoolbench;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractPluginOperation implements HttpHandler {

    protected final Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    protected static void sendError(HttpServerExchange exchange, int statusCode, String error) {
        exchange.setStatusCode(statusCode);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(error);
    }

    @Override
    public abstract void handleRequest(HttpServerExchange exchange) throws Exception;

    /**
     * The URL it is deployed at, e.g. /listFiles.
     */
    public abstract String getUrl();

    /**
     * The OpenAPI description for this operation.
     */
    public abstract String openApiDescription();

    protected Map<String, String> getQueryParams(HttpServerExchange exchange) {
        return exchange.getQueryParameters().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().peekFirst()));
    }

    protected Path getPath(HttpServerExchange exchange) {
        String path = getQueryParams(exchange).get("path");
        if (DevToolbench.IGNORE.matcher(path).matches()) {
            throw new IllegalArgumentException("Path " + path + " is not allowed");
        }
        Path resolved = currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(currentDir)) {
            throw new IllegalArgumentException("Path " + path + " is not in current directory " + currentDir);
        }
        return resolved;
    }

}
