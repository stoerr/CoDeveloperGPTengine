package net.stoerr.chatgpt.devtoolbench;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class DevToolbench {

    public static void main(final String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 3001;
        Undertow server = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(new HttpHandler() {
                    @Override
                    public void handleRequest(final HttpServerExchange exchange) throws Exception {
                        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
                        exchange.getResponseSender().send("Hello World!");
                    }
                }).build();
        server.start();
    }
}