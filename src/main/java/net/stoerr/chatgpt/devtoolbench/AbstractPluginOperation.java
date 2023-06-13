package net.stoerr.chatgpt.devtoolbench;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.PathTemplateMatch;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractPluginOperation implements HttpHandler {

    protected final Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    @Override
    public abstract void handleRequest(HttpServerExchange exchange) throws Exception;

    protected Map<String, String> getQueryParams(HttpServerExchange exchange) {
        return exchange.getQueryParameters().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().peekFirst()));
    }

    protected Path getPath(HttpServerExchange exchange) {
        PathTemplateMatch pathMatch = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        String path = pathMatch.getParameters().get("path");
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
