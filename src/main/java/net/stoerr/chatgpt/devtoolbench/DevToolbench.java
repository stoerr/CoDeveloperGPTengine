package net.stoerr.chatgpt.devtoolbench;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class DevToolbench {

    private static final Map<String, Supplier<String>> STATICFILES = new HashMap<>();
    private static final Map<String, HttpHandler> HANDLERS = new HashMap<>();

    private static final String OPENAPI_DESCR_START = """
            openapi: 3.0.1
            info:
              title: FileManager ChatGPT Plugin
              description: A plugin that allows the user to inspect a directory and read the contents of files using ChatGPT
              version: 1.0.0
            servers:
              - url: http://localhost:THEPORT
            paths:
            """.stripIndent();

    static {
        HANDLERS.put("/listFiles", new ListFilesOperation());
        HANDLERS.put("/readFile", new ReadFileOperation());
        HANDLERS.put("/writeFile", new WriteFileOperation());
        STATICFILES.put("/.well-known/ai-plugin.json", () -> {
            try {
                InputStream in = DevToolbench.class.getResourceAsStream("/ai-plugin.json");
                if (in == null) {
                    throw new RuntimeException("Could not find ai-plugin.json");
                }
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        STATICFILES.put("/devtoolbench.yaml", () -> {
            StringBuilder pathDescriptions = new StringBuilder();
            HANDLERS.values().forEach(handler -> pathDescriptions.append(((AbstractPluginOperation) handler).openApiDescription()));
            return OPENAPI_DESCR_START + pathDescriptions.toString();
        });
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 3001;
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(DevToolbench::handleRequest)
                .build();
        server.start();
    }

    private static void handleRequest(HttpServerExchange exchange) {
        try {
            String path = exchange.getRequestPath();
            if (STATICFILES.containsKey(path)) {
                handleStaticFile(exchange, path);
            } else {
                HttpHandler handler = HANDLERS.get(path);
                if (handler != null) {
                    handler.handleRequest(exchange);
                } else {
                    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                    exchange.getResponseSender().send("Unknown request");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleStaticFile(HttpServerExchange exchange, String path) {
        String content = STATICFILES.get(path).get();
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
        exchange.getResponseSender().send(content);
    }

    // ... rest of the class ...
}
