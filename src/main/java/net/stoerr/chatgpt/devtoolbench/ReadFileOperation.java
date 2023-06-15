package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Files;
import java.nio.file.Path;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

// curl -is http://localhost:3001/readFile?path=somefile.txt
public class ReadFileOperation extends AbstractPluginOperation {

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

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Path path = getPath(exchange);
        if (Files.exists(path)) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            byte[] bytes = Files.readAllBytes(path);
            exchange.getResponseSender().send(new String(bytes));
        } else {
            sendError(exchange, 404, "File not found");
        }
    }
}
