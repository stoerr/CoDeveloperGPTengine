package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class DevToolBench {

    public static final Filter CORSFILTER = (rawRequest, rawResponse, chain) -> {
        // if it's an OPTIONS request, we need to give a CORS response like method giveCORSResponse below
        if (rawRequest instanceof HttpServletRequest request && rawResponse instanceof HttpServletResponse response) {
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.addHeader("Access-Control-Allow-Origin", "https://chat.openai.com");
                response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
                if (request.getHeader("Access-Control-Request-Headers") != null) {
                    response.addHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
                }
                response.addHeader("Access-Control-Max-Age", "3600");
                response.addHeader("Allow", "*");
                response.setStatus(200);
            } else {
                response.addHeader("Access-Control-Allow-Origin", "https://chat.openai.com");
                chain.doFilter(rawRequest, rawResponse);
            }
            return;
        }
        chain.doFilter(rawRequest, rawResponse);
    };
    static Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    public static final Pattern IGNORE = Pattern.compile(".*/[.].*|.*/target/.*|.*/(Hpsx|hpsx).*|.*/node_modules/.*");

    /**
     * Exceptions overriding {@link #IGNORE}.
     */
    public static final Pattern OVERRIDE_IGNORE = Pattern.compile(".*/.github/.*|.*/.content.xml");

    private static int port;

    private static final Map<String, AbstractPluginAction> HANDLERS = new LinkedHashMap<>();

    private static final String OPENAPI_DESCR_START = """
            openapi: 3.0.1
            info:
              title: Developers ToolBench ChatGPT Plugin
              version: THEVERSION
            servers:
              - url: http://localhost:THEPORT
            paths:
            """.stripIndent();

    private static Server server;
    private static ServletContextHandler context;
    private static boolean writingEnabled;

    private static void addHandler(AbstractPluginAction handler) {
        HANDLERS.put(handler.getUrl(), handler);
        context.addServlet(new ServletHolder(handler), handler.getUrl());
    }

    protected static void initServlets() {
        ResourceHandler resourceHandler = new ResourceHandler();
        context.insertHandler(resourceHandler);
        Resource baseResource = Resource.newResource(DevToolBench.class.getResource("/static"));
        resourceHandler.setBaseResource(baseResource);

        addHandler(new ListFilesAction());
        addHandler(new ReadFileAction());
        addHandler(new GrepAction());
        if (writingEnabled) {
            addHandler(new WriteFileAction());
            ExecuteAction executeAction = new ExecuteAction();
            if (executeAction.hasActions()) { // not quite clear whether that is writing...
                addHandler(executeAction);
            }
            // addHandler(new ReplaceRegexAction()); // too many mistakes when using that, look for alternatives
            addHandler(new ReplaceAction());
        addHandler(new UrlAction());
        }

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                try (InputStream in = DevToolBench.class.getResourceAsStream("/ai-plugin.json")) {
                    resp.getWriter().write(new String(in.readAllBytes(), StandardCharsets.UTF_8)
                            .replace("THEPORT", String.valueOf(port))
                            .replace("THEVERSION", TbUtils.getVersionString()));
                }
            }
        }), "/.well-known/ai-plugin.json");

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/yaml");
                resp.setCharacterEncoding("UTF-8");
                StringBuilder pathDescriptions = new StringBuilder();
                HANDLERS.values().stream().sorted(Comparator.comparing(AbstractPluginAction::getUrl))
                        .forEach(handler -> pathDescriptions.append(handler.openApiDescription()));
                resp.getWriter().write(OPENAPI_DESCR_START.replace("THEPORT", String.valueOf(port)) + pathDescriptions);
            }
        }), "/devtoolbench.yaml");

        context.addFilter(new FilterHolder(CORSFILTER), "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addFilter(new FilterHolder((ServletRequest request, ServletResponse response, FilterChain chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            TbUtils.logInfo(req.getMethod() + " " + req.getRequestURI() + (req.getQueryString() != null && !req.getQueryString().isEmpty() ? "?" + req.getQueryString() : ""));
            try {
                chain.doFilter(request, response);
            } catch (ExecutionAbortedException e) {
                TbUtils.logInfo("Aborted and problem reported to ChatGPT : " + e.getMessage());
            } catch (Exception e) {
                TbUtils.logError("Bug! Abort handling request " + ((HttpServletRequest) request).getRequestURI());
                TbUtils.logStacktrace(e);
                throw e;
            }
        }), "/*", EnumSet.of(DispatcherType.REQUEST));
    }

    public static void main(String[] args) throws Exception {
        TbUtils.logVersion();

        parseOptions(args);
        server = new Server(port);
        context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        server.insertHandler(context);
        initServlets();
        server.start();
        // server.join();
        TbUtils.logInfo("Started on http://localhost:" + port + " in directory " + currentDir);
    }

    private static void parseOptions(String[] args) {
        Options options = new Options();

        options.addOption("p", "port", true, "Port number");
        options.addOption("w", "write", false, "Permit file writes");
        // ChatGPTTask: make sure that --help also prints the help message and that it's displayed when there is an exception in parsing options.
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

            writingEnabled = cmd.hasOption("w");
            if (!cmd.hasOption("w")) {
                TbUtils.logInfo("No -w option present - writing and executing actions disabled!");
            }
        } catch (ParseException e) {
            TbUtils.logError("Error parsing command line options: " + e.getMessage());
            System.exit(1);
        }
    }

    public static void stop() throws Exception {
        server.stop();
    }

    public static void execute(Runnable runnable) {
        server.getThreadPool().execute(runnable);
    }

}
