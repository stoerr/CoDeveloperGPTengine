package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                      summary: Replaces occurrences of a regular expression in a file. You are a Java regular expression expert - there is \\A for start of file, \\z for end of file, they can match multiple lines or line fragments, there are reluctant quantifiers, you can use zero-width positive lookaheads / lookbehinds for inserting the replacement, and much more.
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
        String pattern = getBodyParameter(exchange, json, "pattern", true);
        String replacement = getBodyParameter(exchange, json, "replacement", true);
        boolean multiple = getBodyParameter(exchange, json, "multiple", false).equalsIgnoreCase("true");

        if (pattern == null || pattern.isEmpty() || replacement == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("searchString and replacement parameters are required");
            return;
        }

        Path filePath = DevToolbench.currentDir.resolve(path);
        try {
            String content = Files.readString(filePath, UTF_8);
            Matcher m = Pattern.compile(pattern).matcher(content);
            int replacementCount = 0;
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                m.appendReplacement(sb, "dog");
                replacementCount++;
            }
            m.appendTail(sb);
            if (!multiple && replacementCount != 1) {
                if (replacementCount == 0) {
                    throw sendError(exchange, 400, "Found no occurrences of pattern.");
                } else {
                    throw sendError(exchange, 400, "Found " + replacementCount + " occurrences, but expected exactly one because parameter multiple = false.");
                }
            }
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
