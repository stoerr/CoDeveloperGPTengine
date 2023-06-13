package net.stoerr.chatgpt.forChatGPTtoMigrate;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.net.httpserver.HttpExchange;

/**
 * an operation that writes the message into the file at path.
 */
// curl -is http://localhost:3001/writeFile?path=testfile -d '{"content":"testcontent line one\nline two\n"}'
class WriteFileOperation extends AbstractPluginOperation {

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

    /**
     * Content JSON is  e.g. {"content":"Sunlight warms the day,\nClouds dance in the azure sky,\nWeather's gentle play."}
     * We transform to real text by matching that with regex. The regex should match the whole thing including braces, possibly whitespace before and after and at places where it doesn't hurt the JSON
     */
    static final Pattern CONTENT_PATTERN = Pattern.compile("\\s*\\{\\s*\"content\"\\s*:\\s*\"(.*)\"\\s*\\}\\s*");

    public void handle(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            String json = new String(is.readAllBytes(), UTF_8);
            Matcher matcher = CONTENT_PATTERN.matcher(json);
            if (!matcher.matches()) { // send 422
                System.out.println("Broken JSON: " + json);
                exchange.sendResponseHeaders(422, 0);
                exchange.getResponseBody().write("The request body was not a valid JSON object with a content property".getBytes());
                return;
            }
            exchange.sendResponseHeaders(204, -1);
            String content = matcher.group(1);
            // unquote quoted characters \n, \t, \", \\, \b, \f, \r in content
            content = content.replaceAll("\\\\n", "\n");
            content = content.replaceAll("\\\\t", "\t");
            content = content.replaceAll("\\\\\"", "\"");
            content = content.replaceAll("\\\\\\\\", "\\\\");
            content = content.replaceAll("\\\\b", "\b");
            content = content.replaceAll("\\\\f", "\f");
            Path path = getPath(exchange);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            Files.write(path, content.getBytes(UTF_8), CREATE, WRITE, TRUNCATE_EXISTING);
            System.out.println("Wrote file " + path);
        }
    }
}
