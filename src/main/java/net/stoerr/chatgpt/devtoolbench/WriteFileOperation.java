package net.stoerr.chatgpt.devtoolbench;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WriteFileOperation extends AbstractPluginOperation {

    static final Pattern CONTENT_PATTERN = Pattern.compile("\\s*\\{\\s*\"content\"\\s*:\\s*\"(.*)\"\\s*\\}\\s*");

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Deque<String> jsonDeque = exchange.getQueryParameters().get("content");
        String json = jsonDeque != null ? jsonDeque.peekFirst() : null;
        Matcher matcher = CONTENT_PATTERN.matcher(json);
        if (!matcher.matches()) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("The request body was not a valid JSON object with a content property");
            return;
        }
        String content = matcher.group(1);
        // unquote quoted characters \n, \t, \", \\, \b, \f, \r in content
        content = content.replaceAll("\\\\n", "\n");
        content = content.replaceAll("\\\\t", "\t");
        content = content.replaceAll("\\\\\"", "\"");
        content = content.replaceAll("\\\\\\\\", "\\\\");
        content = content.replaceAll("\\\\b", "\b");
        content = content.replaceAll("\\\\f", "\f");
        Path path = getPath(exchange);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }
}
