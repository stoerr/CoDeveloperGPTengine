package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Deque;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import io.undertow.server.HttpServerExchange;

/**
 * an operation that writes the message into the file at path.
 */
// curl -is http://localhost:3001/writeFile?path=testfile -d '{"content":"testcontent line one\nline two\n"}'
public class WriteFileOperation extends AbstractPluginOperation {

    private final Gson gson = new Gson();

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
        Map<String, String> decoded;
        if (json == null || json.isEmpty() || "{}".equals(json)) {
            sendeError(exchange, "Missing content parameter");
            return;
        }
        try {
            decoded = gson.fromJson(json, Map.class);
        } catch (Exception e) {
            String error = "Parse error for content: " + e.getMessage();
            sendeError(exchange, error);
            return;
        }
        String content = decoded.get("content");
        if (content == null) {
            sendeError(exchange, "Missing content parameter");
            return;
        }
        Path path = getPath(exchange);
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Files.write(path, content.getBytes(), java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.WRITE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }

}
