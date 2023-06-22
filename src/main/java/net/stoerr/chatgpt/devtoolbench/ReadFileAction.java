package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Files;
import java.nio.file.Path;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

// curl -is http://localhost:3001/readFile?path=somefile.txt
public class ReadFileAction extends AbstractPluginAction {

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
            byte[] bytes = Files.readAllBytes(path);
            exchange.setResponseContentLength(bytes.length);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
            exchange.setStatusCode(200);
            exchange.getResponseSender().send(new String(bytes));
        } else {
            throw sendError(exchange, 404, "File not found. Try to list files with /listFiles to find the right path.");
        }
    }
}
