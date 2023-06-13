package net.stoerr.chatgpt.devtoolbench;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.nio.file.Files;
import java.nio.file.Path;

public class ReadFileOperation extends AbstractPluginOperation {

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Path path = getPath(exchange);
        if (Files.exists(path)) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            byte[] bytes = Files.readAllBytes(path);
            exchange.getResponseSender().send(new String(bytes));
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("File not found");
        }
    }
}