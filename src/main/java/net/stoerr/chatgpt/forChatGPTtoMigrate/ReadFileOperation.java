package net.stoerr.chatgpt.forChatGPTtoMigrate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.sun.net.httpserver.HttpExchange;

// curl -is http://localhost:3001/readFile?path=somefile.txt
class ReadFileOperation extends AbstractPluginOperation {

    @Override
    public String getUrl() {
        return "/readFile";
    }

    @Override
    public String openApiDescription() {
        return """
                  /readFile:
                    get:
                      operationId: readFile
                      summary: Read a files content.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to file
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: Content of the file
                          content:
                            text/plain:
                              schema:
                                type: string
                        '404':
                          description: File not found
                """.stripIndent();
    }

    public void handle(HttpExchange exchange) throws IOException {
        Path path = getPath(exchange);
        if (Files.exists(path)) {
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            byte[] bytes = Files.readAllBytes(path);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
        } else {
            exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(404, 0);
            exchange.getResponseBody().write("File not found".getBytes());
        }
    }
}
