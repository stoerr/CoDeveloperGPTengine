package net.stoerr.chatgpt.forChatGPTtoMigrate;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Plugin for ChatGPT that can access the files in the directory it is started in.
 * Implemented as a single Java application runnable from the shell, without using any dependencies outside the JDK.
 * To use this, you need to have registered as a plugin developer, and add this with "Develop your own plugin" in the ChatGPT web interface.
 * Start it in the directory you want to access, then use the plugin in ChatGPT.
 * <p>
 * Check OpenAPI description with curl -is http://localhost:3001/filemanagerplugin.yaml , and the plugin description with
 * curl -is http://localhost:3001/.well-known/ai-plugin.json .
 * </p>
 */
public class FileManagerPlugin implements HttpHandler {

    public static final String USAGE = """
            Usage: java FileManagerPlugin [PORT] [-h|--help]
            Description: Plugin for ChatGPT that can access the files in the directory it is started in.
            Arguments: PORT the port to listen on, default 3001.
            Options: -h or --help print this help message.
            """;

    public static final Pattern IGNORE = Pattern.compile(".*/[.].*");

    int port = 3001;

    final String AIPLUGIN_JSON = """
            {
                "schema_version": "v1",
                "name_for_human": "File Manager",
                "name_for_model": "filemanager",
                "description_for_human": "Read and write a directory and its files.",
                "description_for_model": "Help the user with inspecting and processing a directories files. You can list and read and write files. Do only write files at the explicit request of the user, and before changing the file print what changes you are going to make and ask for confirmation.",
                "auth": {
                    "type": "none"
                },
                "api": {
                    "type": "openapi",
                    "url": "http://localhost:THEPORT/filemanager.yaml"
                },
                "logo_url": "https://d1q6f0aelx0por.cloudfront.net/product-logos/library-hello-world-logo.png",
                "contact_email": "yu4cheem@techno.ms",
                "legal_info_url": "http://www.example.com/legal"
            }
            """.stripIndent();

    final String OPENAPI_DESCR_START = """
            openapi: 3.0.1
            info:
              title: FileManager ChatGPT Plugin
              description: A plugin that allows the user to inspect a directory and read the contents of files using ChatGPT
              version: 1.0.0
            servers:
              - url: http://localhost:THEPORT
            paths:
            """.stripIndent();

    final StringBuilder pathDescriptions = new StringBuilder();
    final Map<String, AbstractPluginOperation> handlers = new HashMap<>();

    final Map<String, Supplier<String>> STATICFILES = Map.of("/.well-known/ai-plugin.json",
            () -> AIPLUGIN_JSON.replaceAll("THEPORT", String.valueOf(port)), "/filemanager.yaml",
            () -> OPENAPI_DESCR_START.replaceAll("THEPORT", String.valueOf(port)) + pathDescriptions.toString());

    final Path currentDir = Path.of(".").normalize().toAbsolutePath();

    public static void main(String[] args) throws IOException {
        if (Arrays.stream(args).anyMatch(s -> s.equals("-h") || s.equals("--help"))) {
            System.out.println(USAGE);
            System.exit(0);
        }
        FileManagerPlugin handler = new FileManagerPlugin();
        handler.port = args.length > 0 ? Integer.parseInt(args[0]) : 3001;
        HttpServer server = HttpServer.create(new InetSocketAddress(handler.port), 0);
        server.createContext("/", handler);
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on http://localhost:" + handler.port + "/ in " + handler.currentDir);
    }

    FileManagerPlugin() {
        register(new ListFilesOperation());
        register(new ReadFileOperation());
        register(new WriteFileOperation());
        register(new GiveReasonOperation());
    }

    void register(AbstractPluginOperation operation) {
        handlers.put(operation.getUrl(), operation);
        pathDescriptions.append(operation.openApiDescription());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"OPTIONS".equals(exchange.getRequestMethod())) {
            System.out.println("Request: " + exchange.getRequestMethod() + " " + exchange.getRequestURI());
        }
        try {
            String path = exchange.getRequestURI().getPath();
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*"); // TODO https://chat.openai.com
            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                giveCORSResponse(exchange);
            } else if (STATICFILES.containsKey(path)) {
                handleStaticFile(exchange, path);
            } else if (handlers.containsKey(path)) {
                handlers.get(path).handle(exchange);
            } else if (path.equals("/")) {
                exchange.getResponseHeaders().add("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, 0);
                exchange.getResponseBody().write("<html><body><h1>FileManagerPlugin</h1><p>See <a href=\"/.well-known/ai-plugin.json\">/.well-known/ai-plugin.json</a> for the plugin description.</p></body></html>\n".getBytes());
            } else {
                exchange.getResponseHeaders().add("Content-Type", "text/plain");
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().write("Unknown request".getBytes());
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            exchange.sendResponseHeaders(500, 0);
            exchange.getResponseBody().write(e.toString().getBytes(UTF_8));
        } finally {
            exchange.close();
        }
    }

    /**
     * Remove any CORS restrictions so that ChatGPT interface can use it.
     */
    private void giveCORSResponse(HttpExchange exchange) throws IOException {
        // already there: exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        if (exchange.getRequestHeaders().containsKey("Access-control-request-headers")) {
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", exchange.getRequestHeaders().getFirst("access-control-request-headers"));
        }
        exchange.getResponseHeaders().add("Access-Control-Max-Age", "3600");
        exchange.getResponseHeaders().add("Allow", "*");
        exchange.sendResponseHeaders(200, 0);
    }

    private void handleStaticFile(HttpExchange exchange, String path) throws IOException {
        String file = STATICFILES.get(path).get();
        if (path.endsWith(".yaml") || path.endsWith(".yml")) {
            exchange.getResponseHeaders().add("Content-Type", "text/yaml; charset=utf-8");
        } else if (path.endsWith(".json")) {
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        } else {
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        }
        exchange.sendResponseHeaders(200, 0);
        exchange.getResponseBody().write(file.getBytes(UTF_8));
        exchange.close();
    }

    ;;;;
}
