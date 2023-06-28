package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.AbstractPluginAction.sendError;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

public class DevToolBench {

    static Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    private static final Map<String, Supplier<String>> STATICFILES = new HashMap<>();
    private static final Map<String, AbstractPluginAction> HANDLERS = new HashMap<>();

    /**
     * Which files we always ignore.
     */
    public static final Pattern IGNORE = Pattern.compile(".*/[.].*|.*/target/.*|.*/(Hpsx|hpsx).*");

    /**
     * Exceptions overriding {@link #IGNORE}.
     */
    public static final Pattern OVERRIDE_IGNORE = Pattern.compile(".github/.*");

    private static int port;

    private static final String OPENAPI_DESCR_START = """
            openapi: 3.0.1
            info:
              title: Developers ToolBench ChatGPT Plugin
              version: THEVERSION
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
        addHandler(new ReplaceAction());

        STATICFILES.put("/.well-known/ai-plugin.json", () -> {
            try (InputStream in = DevToolBench.class.getResourceAsStream("/ai-plugin.json")) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8)
                        .replace("THEPORT", String.valueOf(port))
                        .replace("THEVERSION", TbUtils.getVersionString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        STATICFILES.put("/devtoolbench.yaml", () -> {
            StringBuilder pathDescriptions = new StringBuilder();
            HANDLERS.values().stream().sorted(Comparator.comparing(AbstractPluginAction::getUrl))
                    .forEach(handler -> pathDescriptions.append(handler.openApiDescription()));
            return OPENAPI_DESCR_START.replace("THEPORT", String.valueOf(port)) + pathDescriptions;
        });
        STATICFILES.put("/", () -> "<html><body><h1>Developers ToolBench ChatGPT Plugin</h1><p>See <a href=\"/.well-known/ai-plugin.json\">/.well-known/ai-plugin.json</a> for the plugin description.</p></body></html>\n");
    }

    public static void main(String[] args) {
        TbUtils.logVersion();

        parseOptions(args);
        server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(DevToolBench::handleRequest)
                .setIoThreads(10)
                .setWorkerThreads(10)
                .build();
        server.start();
        TbUtils.log("Started on http://localhost:" + port);
    }

    private static void parseOptions(String[] args) {
        Options options = new Options();

        options.addOption("p", "port", true, "Port number");
        options.addOption("w", "write", false, "Permit file writes");
        options.addOption("h", "help", false, "Display this help message");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("DevToolBench", options);
                System.exit(0);
            }

            if (cmd.hasOption("p")) {
                port = Integer.parseInt(cmd.getOptionValue("p"));
            } else {
                port = 3002;
            }

            if (!cmd.hasOption("w")) {
                HANDLERS.remove("/writeFile");
                HANDLERS.remove("/grep");
            }
        } catch (ParseException e) {
            System.out.println("Error parsing command line options: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void stop() {
        server.stop();
    }

    private static void handleRequest(HttpServerExchange exchange) {
        try {
            TbUtils.log(exchange.getRequestMethod() + " " + exchange.getRequestURI() +
                    (exchange.getQueryString() != null && !exchange.getQueryString().isEmpty() ? "?" + exchange.getQueryString() : ""));
            TbUtils.logRequest(exchange);
            String path = exchange.getRequestPath();
            exchange.getResponseHeaders().put(HttpString.tryFromString("Access-Control-Allow-Origin"), "https://chat.openai.com");
            if (exchange.getRequestMethod().equals(Methods.OPTIONS)) {
                giveCORSResponse(exchange);
            } else if (STATICFILES.containsKey(path)) {
                handleStaticFile(exchange, path);
            } else if (path.equals("/icon.png")) {
                byte[] bytes = DevToolBench.class.getResourceAsStream("/icon.png").readAllBytes();
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "image/png");
                exchange.setStatusCode(200);
                exchange.setResponseContentLength(bytes.length);
                exchange.getResponseSender().send(ByteBuffer.wrap(bytes));
            } else {
                HttpHandler handler = HANDLERS.get(path);
                if (handler != null) {
                    handler.handleRequest(exchange);
                    TbUtils.log("Response: " + exchange.getStatusCode() + " " + exchange.getResponseHeaders());
                } else {
                    throw sendError(exchange, 404, "Unknown request");
                }
            }
        } catch (ExecutionAbortedException e) {
            TbUtils.log("Aborted and problem reported to ChatGPT : " + e.getMessage());
        } catch (Exception e) {
            TbUtils.logError("Bug! Abort handling request " + exchange.getRequestURL());
            TbUtils.logStacktrace(e);
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
            if (content.contains("<html>")) {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
            } else {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
            }
            exchange.getResponseSender().send(content);
        } else {
            throw sendError(exchange, 404, "File not found");
        }
    }

}
