package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class ReplaceAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/replace";
    }

    @Override
    public String openApiDescription() {
        return "/replace:\n" +
                "    post:\n" +
                "      operationId: replace\n" +
                "      summary: Replace occurrences of a string in a file.\n" +
                "      parameters:\n" +
                "        - name: path\n" +
                "          in: query\n" +
                "          description: Path of the file\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: searchString\n" +
                "          in: query\n" +
                "          description: String to be replaced\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: replacement\n" +
                "          in: query\n" +
                "          description: Replacement string\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      requestBody:\n" +
                "        required: false\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                content:\n" +
                "                  type: string\n" +
                "      responses:\n" +
                "        ''204'':\n" +
                "          description: File updated successfully\n" +
                "        '404':\n" +
                "          description: Invalid parameter\n" +
                "        '500':\n" +
                "          description: Server error";
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullString(this::handleBody, this::handleRequestBodyError);
    }

    private void handleBody(HttpServerExchange exchange, String json) {
        exchange.startBlocking();
        String path = exchange.getQueryParameters().get("path").getFirst();
        String searchString = exchange.getQueryParameters().get("searchString").getFirst();
        String replacement = exchange.getQueryParameters().get("replacement").getFirst();

        if (searchString == null || replacement == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("searchString and replacement parameters are required");
            return;
        }

        Path filePath = DevToolbench.currentDir.resolve(path);
        try {
            String content = new String(Files.readAllBytes(filePath), UTF_8);
            content = content.replace(searchString, replacement);
            Files.write(filePath, content.getBytes(UTF_8));

            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
            exchange.getResponseSender().send("File updated successfully");
        } catch (NoSuchFileException e) {
            sendError(exchange, 404, "File not found");
        } catch (IOException e) {
            sendError(exchange, 500, "Error reading or writing file : " + e.toString());
        }
    }

    @Override
    protected Path getPath(HttpServerExchange exchange) {
        String path = exchange.getQueryParameters().get("path").getFirst();
        return Paths.get(path);
    }

}
