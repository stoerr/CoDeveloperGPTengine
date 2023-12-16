package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ContextedRuntimeException;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import jakarta.servlet.Filter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Reads the global configuration from the configuration directory in the users homedir, which is used everywhere, no matter where the plugin is started.
 */
public class UserGlobalConfig {

    /**
     * The users global configuration directory at ~/.cgptdevbenchglobal .
     */
    Path configDir = Paths.get(System.getProperty("user.home"), ".cgptdevbenchglobal");
    Path configFile = configDir.resolve("https.properties");

    /**
     * The port to use for https.
     */
    Integer httpsPort;
    /**
     * The path to the keystore file.
     */
    String keystorePath;
    /**
     * The password for the keystore.
     */
    String keystorePassword;

    /**
     * The extern domain through which we are reachable.
     */
    String domain;
    /**
     * The port through which we are reachable.
     */
    int externport;
    /**
     * If given, a secret the GPT has to share to access the devtoolbench as action.
     */
    String gptSecret;
    /**
     * For working as a plugin we need the token OpenAI gives us back after we put in the gptSecret on "develop your own plugin".
     *
     * @see "https://www.hackwithgpt.com/blog/chatgpt-plugin-authentication-guide/"
     */
    String openaitoken;

    private Properties config;

    /**
     * Reads the httpsConfigFile if it exists. Return true if configuration could be read completely.
     */
    public boolean readAndCheckConfiguration(@Nullable String globalConfigDir) throws IOException {
        configDir = globalConfigDir != null ? Paths.get(globalConfigDir) :
                Paths.get(System.getProperty("user.home"), ".cgptdevbenchglobal");
        configFile = configDir.resolve("config.properties");
        if (!configFile.toFile().exists()) {
            TbUtils.logError("Could not find global configuration file " + configFile + " - https disabled.");
            return false;
        }
        config = new Properties();
        try (InputStream is = Files.newInputStream(configFile)) {
            config.load(is);
        } catch (IOException e) {
            throw new ContextedRuntimeException(e);
        }

        gptSecret = config.getProperty("gptsecret");
        if (null == gptSecret || gptSecret.trim().length() < 8) {
            TbUtils.logError("gptsecret property in " + configFile + " is missing or less than 8 chars - https disabled.");
            return false;
        }
        gptSecret = gptSecret.trim();

        openaitoken = config.getProperty("openaitoken");
        openaitoken = openaitoken != null ? openaitoken.trim() : null;

        httpsPort = config.getProperty("httpsport") != null ? Integer.parseInt(config.getProperty("httpsport")) : null;
        keystorePath = config.getProperty("keystorepath");
        String keystorePasswordPath = config.getProperty("keystorepasswordpath");
        domain = config.getProperty("domain");
        externport = Integer.parseInt(config.getProperty("externport", "443"));

        if (httpsPort == null || httpsPort <= 0) {
            TbUtils.logError("httpsport property in " + configFile + " is not set - our own https is disabled, relying on a tunnel.");
            // that is OK if we use a tunnel instead of doing https ourselves
            return true;
        } else {
            if (null == keystorePath) {
                TbUtils.logError("keystorepath property in " + configFile + " is missing - https disabled.");
                return false;
            }
            if (null == keystorePasswordPath) {
                TbUtils.logError("keystorepasswordpath property in " + configFile + " is missing - https disabled.");
                return false;
            }
            keystorePassword = new String(Files.readAllBytes(configDir.resolve(keystorePasswordPath)), StandardCharsets.UTF_8);
            if (null == keystorePassword || keystorePassword.isEmpty()) {
                TbUtils.logError("Could not read keystore password from " + keystorePasswordPath + " - https disabled.");
                return false;
            }
            keystorePassword = keystorePassword.trim();

            if (null == domain) {
                TbUtils.logError("domain property in " + configFile + " is missing - https disabled.");
                return false;
            }
        }

        return true;
    }

    public Filter getSecretFilter() {
        return (rawRequest, rawResponse, chain) -> {
            HttpServletRequest request = (HttpServletRequest) rawRequest;
            HttpServletResponse response = (HttpServletResponse) rawResponse;

            boolean requestislocal = request.getHeader("X-Forwarded-Proto") == null && !request.isSecure()
                    || request.getRequestURL().toString().startsWith("http://localhost");
            if (!requestislocal && StringUtils.isBlank(gptSecret)) {
                TbUtils.logError("No gptsecret configured in " + configFile + " and request is remote - refusing to serve for security reasons.");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().println("No gptsecret configured in " + configFile + " and request is remote - refusing to serve for security reasons.");
                return;
            }

            String secret = request.getHeader("Authorization");
            boolean isPublicRequest = DevToolBench.UNPROTECTED_PATHS.contains(request.getRequestURI());
            if (!requestislocal && !isPublicRequest && (secret == null || !secret.contains(gptSecret))) {
                TbUtils.logError("service access token missing. Request was " + request.getRequestURI() + " from " + request.getRemoteAddr() + " - wrong Authorization header " + secret);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().println("service access token missing in Authorization header for " + request.getRequestURI());
                return;
            }

            chain.doFilter(rawRequest, rawResponse);
        };
    }

    public void addHttpsConnector(Server server) throws IOException {
        if (httpsPort != null) {
            SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
            sslContextFactory.setKeyStorePath(configDir.resolve(keystorePath).toString());
            sslContextFactory.setKeyStorePassword(keystorePassword);

            ServerConnector httpsConnector = new ServerConnector(server,
                    new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                    new HttpConnectionFactory());
            httpsConnector.setHost("0.0.0.0");
            httpsConnector.setPort(httpsPort);

            server.addConnector(httpsConnector);
            TbUtils.logInfo("Starting with https locally at https://localhost:" + httpsPort + "/ for " + getExternUrl());
            TbUtils.logInfo("(We assume there is a tunnel from " + domain + ":" + externport + " to localhost:" + httpsPort + ".)");
        }
    }

    public String getExternUrl() {
        if (httpsPort != null) {
            if (externport != 443) {
                return "https://" + domain + ":" + externport;
            } else {
                return "https://" + domain;
            }
        }
        return null;
    }

    public String getOpenaiToken() {
        return openaitoken != null ? openaitoken : "";
    }
}
