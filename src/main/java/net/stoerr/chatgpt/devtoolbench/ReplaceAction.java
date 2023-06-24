package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.undertow.server.HttpServerExchange;

public class ReplaceAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/replace";
    }

    @Override
    public String openApiDescription() {
        return """
                  /replaceInFile:
                    post:
                      operationId: replaceInFile
                      summary: Replaces occurrences of a regular expression in a file. Remember the full power of regular expressions - there are some for start of input (the file), end of input (file), they can match multiple lines, there are reluctant quantifiers, zero with lookaheads / lookbehinds for insertions, ...
                      parameters:
                        - name: path
                          in: query
                          description: relative path to file
                          required: true
                          schema:
                            type: string
                      requestBody:
                        required: true
                        content:
                          application/json:
                            schema:
                              type: object
                              properties:
                                multiple:
                                  type: boolean
                                  description: if true, replace all occurrences, otherwise exactly one occurrence - it would be an error if there is no occurrence or several occurrences
                                pattern:
                                  required: true
                                  type: string
                                  description: java Pattern to be replaced
                                replacement:
                                  required: true
                                  type: string
                                  description: will replace the regex; can contain group references as in Java Matcher.appendReplacement
                      responses:
                        '200':
                          description: File updated successfully
                        '400':
                          description: Invalid parameter
                        '500':
                          description: Server error
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullString(this::handleBody, this::handleRequestBodyError);
    }

    private void handleBody(HttpServerExchange exchange, String json) {
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
            String content = Files.readString(filePath, UTF_8);
            content = content.replace(searchString, replacement);
            Files.writeString(filePath, content, UTF_8);
            exchange.setStatusCode(204);
        } catch (NoSuchFileException e) {
            throw sendError(exchange, 404, "File not found: " + filePath);
        } catch (IOException e) {
            throw sendError(exchange, 500, "Error reading or writing file : " + e);
        }
    }

    @Override
    protected Path getPath(HttpServerExchange exchange) {
        String path = exchange.getQueryParameters().get("path").getFirst();
        return Paths.get(path);
    }

}
