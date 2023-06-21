package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.AbstractPluginAction.sendError;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
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
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public class DevToolbench {

    static Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    private static final Map<String, Supplier<String>> STATICFILES = new HashMap<>();
    private static final Map<String, AbstractPluginAction> HANDLERS = new HashMap<>();

    /**
     * Which files we always ignore.
     */
    public static final Pattern IGNORE = Pattern.compile(".*/[.].*|.*/target/.*|.*/(Hpsx|hpsx).*");

    private static int port;

    private static final String OPENAPI_DESCR_START = """
            openapi: 3.0.1
            info:
              title: FileManager ChatGPT Plugin
              description: A plugin that allows the user to inspect a directory and read the contents of files using ChatGPT. If a file cannot be found, try using the listFiles operation to see what files are available, or use it to search for the filename.
              version: 1.0.0
            servers:
              - url: http://localhost:THEPORT
            paths:
            """.stripIndent();

    private static Undertow server;

    private static void addHandler(AbstractPluginAction handler) {
        HANDLERS.put(handler.getUrl(), handler);
    }

    static {
        addHandler(new ListFilesAction());
        addHandler(new ReadFileAction());
        addHandler(new WriteFileAction());
        addHandler(new ExecuteAction());
        addHandler(new GrepAction());

        STATICFILES.put("/.well-known/ai-plugin.json", () -> {
            try (InputStream in = DevToolbench.class.getResourceAsStream("/ai-plugin.json")) {
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
            HANDLERS.values().stream().sorted(Comparator.comparing(AbstractPluginAction::getUrl))
                    .forEach(handler -> pathDescriptions.append(handler.openApiDescription()));
            return OPENAPI_DESCR_START.replace("THEPORT", "" + port) + pathDescriptions;
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
        System.out.println("Started on http://localhost:" + port);
    }

    public static void stop() {
        server.stop();
    }

    private static void handleRequest(HttpServerExchange exchange) {
        try {
            System.out.println("Request: " + exchange.getRequestURL());
            String path = exchange.getRequestPath();
            exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Origin"), "*"); // TODO https://chat.openai.com
            if (exchange.getRequestMethod().equals(Methods.OPTIONS)) {
                giveCORSResponse(exchange);
            } else if (STATICFILES.containsKey(path)) {
                handleStaticFile(exchange, path);
            } else if (path.equals("/icon.png")) {
                byte[] bytes = DevToolbench.class.getResourceAsStream("/icon.png").readAllBytes();
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "image/png");
                exchange.setStatusCode(200);
                exchange.setResponseContentLength(bytes.length);
                exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
            } else {
                HttpHandler handler = HANDLERS.get(path);
                if (handler != null) {
                    handler.handleRequest(exchange);
                    System.out.println("Response: " + exchange.getStatusCode() + " " + exchange.getResponseHeaders());
                } else {
                    sendError(exchange, 404, "Unknown request");
                }
            }
        } catch (ExecutionAbortedException e) {
            System.out.println("Aborted and problem reported to ChatGPT: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Bug! Abort handling request " + exchange.getRequestURL());
            e.printStackTrace(System.err);
        } finally {
            exchange.endExchange();
        }
    }

    private static void giveCORSResponse(HttpServerExchange exchange) {
        // already there: exchange.getResponseHeaders().add(HttpString.tryFromString("Access-Control-Allow-Origin"), "*");
        exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Methods"), "GET, POST, PUT, DELETE");
        if (exchange.getRequestHeaders().contains("Access-control-request-headers")) {
            exchange.getResponseHeaders().add(HttpString.tryFromString("Access-Control-Allow-Headers"), exchange.getRequestHeaders().getFirst("access-control-request-headers"));
        }
        exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Max-Age"), "3600");
        exchange.getResponseHeaders().put(HttpString.tryFromString("Allow"), "*");
        exchange.setStatusCode(200);
    }

    private static void handleStaticFile(HttpServerExchange exchange, String path) {
        String content = STATICFILES.get(path).get();
        if (content != null && !content.isBlank()) {
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
            exchange.getResponseSender().send(content);
        } else {
            sendError(exchange, 404, "File not found");
        }
    }


}
