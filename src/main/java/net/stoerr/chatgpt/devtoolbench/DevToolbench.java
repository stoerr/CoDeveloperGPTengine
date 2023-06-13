package net.stoerr.chatgpt.devtoolbench;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.HashMap;
import java.util.Map;

public class DevToolbench {

    private static final Map<String, HttpHandler> handlers = new HashMap<>();

    public static void main(final String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 3001;
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(exchange -> {
                    String path = exchange.getRequestPath();
                    if (handlers.containsKey(path)) {
                        try {
                            handlers.get(path).handleRequest(exchange);
                        } catch (Exception e) {
                            e.printStackTrace();
                            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                            exchange.getResponseSender().send("Internal server error: " + e.getMessage());
                        }
                    } else {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("Unknown request");
                    }
                })
                .build();
        server.start();
    }

    static {
        register("/listFiles", new ListFilesOperation());
        register("/readFile", new ReadFileOperation());
        register("/writeFile", new WriteFileOperation());
        // TODO: Register other handlers here
    }

    static void register(String path, HttpHandler handler) {
        handlers.put(path, handler);
    }
}