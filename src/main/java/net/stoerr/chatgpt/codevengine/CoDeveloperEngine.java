package net.stoerr.chatgpt.codevengine;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Response;
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

public class CoDeveloperEngine {

    public static final String PATH_AI_PLUGIN_JSON = "/.well-known/ai-plugin.json";
    public static final String PATH_SPEC = "/codeveloperengine.yaml";
    public static final List<String> UNPROTECTED_PATHS = Arrays.asList(PATH_AI_PLUGIN_JSON, PATH_SPEC, "/favicon.ico",
            "debugging/setauthcookie.html");
    public static final String LOCAL_CONFIG_DIR = ".cgptcodeveloper";

    /** Files that are inaccessible to the program. */
    public static final Pattern IGNORE_FILES_PATTERN = Pattern.compile(".*/[.].*|.*/target|.*/target/.*|.*/(Hpsx|hpsx).*|.*/node_modules/.*");

    /**
     * Exceptions overriding {@link #IGNORE_FILES_PATTERN}.
     */
    public static final Pattern OVERRIDE_IGNORE_PATTERN = Pattern.compile(".*/.github/.*|.*/.content.xml|(.*/)?\\.chatgpt.*.md|.*\\.htaccess");

    // private static final Gson GSON = new Gson();

    static Path currentDir = Paths.get(".").normalize().toAbsolutePath();

    private static int port;

    private static final Map<String, AbstractPluginAction> HANDLERS = new LinkedHashMap<>();

    private static final String OPENAPI_DESCR_START = "# THESPECURL\n\n" +
            "openapi: 3.0.1\n" +
            "info:\n" +
            "  title: Co-Developer GPT Engine\n" +
            "  version: THEVERSION\n" +
            "servers:\n" +
            "  - url: THEURL\n" +
            "paths:\n";

    private static Server server;
    private static ServletContextHandler context;
    private static boolean writingEnabled;
    private static String mainUrl;
    private static String localUrl;

    static boolean ignoreGlobalConfig;
    private static String userGlobalConfigDir;
    private static UserGlobalConfig userconfig;

    private static final Filter CORSFILTER = (rawRequest, rawResponse, chain) -> {
        // if it's an OPTIONS request, we need to give a CORS response like method giveCORSResponse below
        if (rawRequest instanceof HttpServletRequest && rawResponse instanceof HttpServletResponse) {
            HttpServletResponse response = (HttpServletResponse) rawResponse;
            HttpServletRequest request = (HttpServletRequest) rawRequest;
            String origin = request.getHeader("Origin");
            if (origin == null) {
                origin = "https://chat.openai.com";
            }
            if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                response.addHeader("Access-Control-Allow-Origin", origin);
                response.addHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
                if (request.getHeader("Access-Control-Request-Headers") != null) {
                    response.addHeader("Access-Control-Allow-Headers", request.getHeader("Access-Control-Request-Headers"));
                }
                response.addHeader("Access-Control-Max-Age", "3600");
                response.addHeader("Allow", "*");
                response.setStatus(200);
            } else {
                response.addHeader("Access-Control-Allow-Origin", origin);
                chain.doFilter(rawRequest, rawResponse);
            }
            return;
        }
        TbUtils.logError("Unknown request type: " + rawRequest.getClass());
        chain.doFilter(rawRequest, rawResponse);
    };

    private static final RequestLog requestlog = new RequestLog() {
        @Override
        public void log(Request request, Response response) {
            TbUtils.logInfo("Remote address: " + request.getRemoteAddr());
            TbUtils.logInfo("Remote host: " + request.getRemoteHost());
            TbUtils.logInfo("Remote port: " + request.getRemotePort());
            TbUtils.logInfo("Requestlog: " + request.getMethod() + " " + request.getRequestURL() + (request.getQueryString() != null && !request.getQueryString().isEmpty() ? "?" + request.getQueryString() : "") + " " + response.getStatus());
            // list all request headers
            for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements(); ) {
                String header = e.nextElement();
                TbUtils.logInfo("Request header: " + header + ": " + request.getHeader(header));
            }
            // list all response headers
            for (String header : response.getHeaderNames()) {
                TbUtils.logInfo("Response header: " + header + ": " + response.getHeader(header));
            }
            TbUtils.logInfo("");
        }
    };

    private static void addHandler(AbstractPluginAction handler) {
        HANDLERS.put(handler.getUrl(), handler);
        ServletHolder servlet = new ServletHolder(handler);
        context.addServlet(servlet, handler.getUrl());
    }

    protected static void initServlets() {
        ResourceHandler resourceHandler = new ResourceHandler();
        context.insertHandler(resourceHandler);
        Resource baseResource = Resource.newResource(CoDeveloperEngine.class.getResource("/static"));
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
                try (InputStream in = CoDeveloperEngine.class.getResourceAsStream("/ai-plugin.json")) {
                    String json = new String(IOUtils.toByteArray(in), StandardCharsets.UTF_8)
                            .replace("THEURL", getMainUrl(req))
                            .replace("THEOPENAITOKEN", userconfig.getOpenaiToken())
                            .replace("THEVERSION", TbUtils.getVersionString());
                    // disabled for now since plugin development is broken with ChatGPT for a while as of 2023-12-14
                    //                    if (isLocal(req)) { // as local plugin development ChatGPT doesn't use authorization.
                    //                        Map<String, Object> map = GSON.fromJson(json, new TypeToken<Map<String, Object>>() {
                    //                        }.getType());
                    //                        map.put("auth", Map.of("type", "none", "comment", "authorization removed for localhost"));
                    //                        json = GSON.toJson(map);
                    //                    }
                    resp.getWriter().write(json);
                }
            }
        }), PATH_AI_PLUGIN_JSON);

        context.addServlet(new ServletHolder(new HttpServlet() {
            @Override
            protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
                resp.setContentType("text/yaml");
                resp.setCharacterEncoding("UTF-8");
                StringBuilder pathDescriptions = new StringBuilder();
                HANDLERS.values().stream().sorted(Comparator.comparing(AbstractPluginAction::getUrl))
                        .forEach(handler -> pathDescriptions.append(handler.openApiDescription()));
                resp.getWriter().write(OPENAPI_DESCR_START.replace("THEURL", getMainUrl(req))
                        .replace("THESPECURL", getMainUrl(req) + PATH_SPEC)
                        + pathDescriptions);
            }
        }), PATH_SPEC);

        context.addFilter(new FilterHolder(CORSFILTER), "/*", EnumSet.of(DispatcherType.REQUEST));

        context.addFilter(new FilterHolder((ServletRequest request, ServletResponse response, FilterChain chain) -> {
            HttpServletRequest req = (HttpServletRequest) request;
            TbUtils.logRequest(req);
            TbUtils.logInfo("\n\n" + req.getMethod() + " " + req.getRequestURL() + (req.getQueryString() != null && !req.getQueryString().isEmpty() ? "?" + req.getQueryString() : ""));
            try {
                chain.doFilter(request, response);
            } catch (ExecutionAbortedException e) {
                TbUtils.logInfo("Aborted and problem reported to ChatGPT : " + e);
            } catch (Exception e) {
                TbUtils.logError("Bug! Abort handling request " + ((HttpServletRequest) request).getRequestURI());
                TbUtils.logStacktrace(e);
                throw e;
            }
        }), "/*", EnumSet.of(DispatcherType.REQUEST));
    }

    private static String getMainUrl(HttpServletRequest request) {
        // cut off path part of request url
        StringBuffer url = request.getRequestURL();
        int pathpos = url.indexOf(request.getServletPath());
        if (pathpos > 0) {
            url.setLength(pathpos);
        }
        String protocol = request.getHeader("X-Forwarded-Proto");
        if (protocol != null) { // replace protocol if we are behind a proxy
            url.replace(0, url.indexOf(":"), protocol);
        }
        return url.toString();
    }

    public static void main(String[] args) throws Exception {
        TbUtils.logVersion();

        parseOptions(args);
        server = new Server(new InetSocketAddress("127.0.0.1", port));
        context = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        context.setContextPath("/");
        server.insertHandler(context);

        mainUrl = "http://localhost:" + port;
        localUrl = mainUrl;
        userconfig = new UserGlobalConfig();
        if (!ignoreGlobalConfig && userconfig.readAndCheckConfiguration(userGlobalConfigDir)) {
            userconfig.addHttpsConnector(server);
            mainUrl = userconfig.getExternUrl();
        }
        context.addFilter(new FilterHolder(userconfig.getSecretFilter()), "/*", EnumSet.of(DispatcherType.REQUEST));

        initServlets();
        // for debugging: server.setRequestLog(requestlog);
        server.start();
        // server.join();
        TbUtils.logInfo("Started on http://localhost:" + port + " in directory " + currentDir);
        TbUtils.logInfo("OpenAPI: " + StringUtils.defaultString(mainUrl, localUrl) + PATH_SPEC);
    }

    private static void parseOptions(String[] args) {
        Options options = new Options();

        options.addOption("p", "port", true, "Port number, default 3002");
        options.addOption("w", "write", false, "Permit file writes and action executions");
        options.addOption("h", "help", false, "Display this help message");
        options.addOption("g", "globalconfigdir", true, "Directory for global configuration (default: ~/.cgptcodeveloperglobal/");
        options.addOption("l", "local", false, "Only use local configuration via options - ignore any global configuration");

        CommandLineParser parser = new DefaultParser();

        try {
            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("options are", options);
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

            if (cmd.hasOption("g")) {
                userGlobalConfigDir = cmd.getOptionValue("g");
            }

            if (cmd.hasOption("l")) {
                userGlobalConfigDir = null;
                ignoreGlobalConfig = true;
            }
        } catch (ParseException e) {
            TbUtils.logError("Error parsing command line options: " + e);
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
