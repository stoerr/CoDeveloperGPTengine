package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.gson.Gson;

import io.undertow.server.HttpServerExchange;

/**
 * an operation that writes the message into the file at path.
 */
// curl -is http://localhost:3001/writeFile?path=testfile -d '{"content":"testcontent line one\nline two\n"}'
public class WriteFileAction extends AbstractPluginAction {

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
                      summary: Overwrite a file.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to file
                          required: true
                          schema:
                            type: string
                        - name: append
                          in: query
                          description: If true, append to the file instead of overwriting. If false, you need to give the whole content at once.
                          required: false
                          schema:
                            type: boolean
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
                        '400':
                          description: Invalid parameter
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullString(this::handleBody, this::handleRequestBodyError);
    }

    private void handleBody(HttpServerExchange exchange, String json) {
        try {
            String content = getMandatoryContentFromBody(exchange, json);
            String appendParam = getQueryParam(exchange, "append");
            boolean append = appendParam != null && appendParam.toLowerCase().contains("true");
            Path path = getPath(exchange);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (append && !Files.exists(path)) {
                throw sendError(exchange, 400, "File " + path + " does not exist, cannot append to it.");
            }
            StandardOpenOption appendOption = append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
            Files.write(path, content.getBytes(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE, appendOption);
            exchange.setStatusCode(204);
        } catch (IOException e) {
            throw sendError(exchange, 500, "Error writing file: " + e);
        }
    }

}
