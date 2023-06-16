package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public abstract class AbstractPluginOperation implements HttpHandler {

    protected static void sendError(HttpServerExchange exchange, int statusCode, String error) {
        System.out.println("Error " + statusCode + ": " + error);
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
            sendError(exchange, 400, "Access to path " + path + " is not allowed! (matches " + DevToolbench.IGNORE.pattern() + ")");
            throw new ExecutionAbortedException("Path " + path + " is not allowed");
        }
        Path resolved = DevToolbench.currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(DevToolbench.currentDir)) {
            sendError(exchange, 400, "Path " + path + " is outside of current directory!");
            throw new ExecutionAbortedException("Path " + path + " is not in current directory " + DevToolbench.currentDir);
        }
        return resolved;
    }

}
