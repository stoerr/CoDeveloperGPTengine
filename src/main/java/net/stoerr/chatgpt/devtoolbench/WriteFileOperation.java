package net.stoerr.chatgpt.devtoolbench;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * an operation that writes the message into the file at path.
 */
// curl -is http://localhost:3001/writeFile?path=testfile -d '{"content":"testcontent line one\nline two\n"}'
public class WriteFileOperation extends AbstractPluginOperation {

    static final Pattern CONTENT_PATTERN = Pattern.compile("\\s*\\{\\s*\"content\"\\s*:\\s*\"(.*)\"\\s*\\}\\s*");

    @Override
    public String getUrl() {
        return "/writeFile";
    }

    @Override
    public String openApiDescription() {
        return """
                  /writeFile:
                    post:
                      operationId: writeFile
                      summary: Write a file.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to directory for the created file
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
                                content:
                                  type: string
                      responses:
                        '204':
                          description: File written
                        '422':
                          description: The request body was not a valid JSON object with a content property
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Deque<String> jsonDeque = exchange.getQueryParameters().get("content");
        String json = jsonDeque != null ? jsonDeque.peekFirst() : null;
        if (json == null) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Missing content parameter");
            return;
        }
        Matcher matcher = CONTENT_PATTERN.matcher(json);
        if (!matcher.matches()) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("The request body was not a valid JSON object with a content property");
            return;
        }
        String content = matcher.group(1);
        content = content.replaceAll("\\\\n", "\n");
        content = content.replaceAll("\\\\t", "\t");
        content = content.replaceAll("\\\\\"", "\"");
        content = content.replaceAll("\\\\\\\\", "\\\\");
        Path path = getPath(exchange);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }
}
