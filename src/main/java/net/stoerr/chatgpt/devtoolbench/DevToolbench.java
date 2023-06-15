package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.AbstractPluginOperation.sendError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class DevToolbench {

    static Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    private static final Map<String, Supplier<String>> STATICFILES = new HashMap<>();
    private static final Map<String, AbstractPluginOperation> HANDLERS = new HashMap<>();

    /**
     * Which files we always ignore.
     */
    public static final Pattern IGNORE = Pattern.compile(".*/[.].*|.*/target/.*");

    private static int port;

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

    private static Undertow server;

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
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).replaceAll("THEPORT", "" + port);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        STATICFILES.put("/devtoolbench.yaml", () -> {
            StringBuilder pathDescriptions = new StringBuilder();
            HANDLERS.values().stream().sorted(Comparator.comparing(AbstractPluginOperation::getUrl))
                    .forEach(handler -> pathDescriptions.append(((AbstractPluginOperation) handler).openApiDescription()));
            return OPENAPI_DESCR_START.replaceAll("THEPORT", "" + port) + pathDescriptions.toString();
        });
        STATICFILES.put("/", () -> "<html><body><h1>FileManagerPlugin</h1><p>See <a href=\"/.well-known/ai-plugin.json\">/.well-known/ai-plugin.json</a> for the plugin description.</p></body></html>\n");
    }

    public static void main(String[] args) {
        port = args.length > 0 ? Integer.parseInt(args[0]) : 3002;
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(DevToolbench::handleRequest)
                .build();
        server.start();
    }

    public static void stop() {
        server.stop();
    }

    private static void handleRequest(HttpServerExchange exchange) {
        try {
            System.out.println("Request: " + exchange.getRequestURI());
            String path = exchange.getRequestPath();
            if (STATICFILES.containsKey(path)) {
                handleStaticFile(exchange, path);
            } else {
                HttpHandler handler = HANDLERS.get(path);
                if (handler != null) {
                    handler.handleRequest(exchange);
                    System.out.println("Response: " + exchange.getStatusCode() + " " + exchange.getResponseHeaders());
                } else {
                    sendError(exchange, 404, "Unknown request");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void handleStaticFile(HttpServerExchange exchange, String path) {
        String content = STATICFILES.get(path).get();
        if (content != null && !content.isBlank()) {
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send(content);
        } else {
            sendError(exchange, 404, "File not found");
        }
    }
}
