package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;

/**
 * an operation that writes the message into the file at path.
 */
// curl -is http://localhost:3001/writeFile?path=testfile -d '{"content":"testcontent line one\nline two\n"}'
public class WriteFileAction extends AbstractPluginAction {

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
                      summary: Overwrite a small file with the complete content given in one step. You cannot append to a file or write parts or write parts - use the replaceAction for inserting parts.
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
                                content:
                                  type: string
                      responses:
                        '204':
                          description: File written
                        '400':
                          description: Invalid parameter
                """.stripIndent();
    }
    // We remove the append parameter for now, because it is not transactional and the file might be half overwritten
    // when ChatGPT aborts because of length constraints or other issues.
    //                         - name: append
    //                          in: query
    //                          description: If true, append to the very end of the file instead of overwriting. If false, you need to give the whole content at once.
    //                          required: false
    //                          schema:
    //                            type: boolean

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Receiver requestReceiver = exchange.getRequestReceiver();
        requestReceiver.setMaxBufferSize(100000);
        requestReceiver.receiveFullBytes(this::handleBody, this::handleRequestBodyError);
    }

    private void handleBody(HttpServerExchange exchange, byte[] bytes) {
        String json = new String(bytes, StandardCharsets.UTF_8);
        try {
            String appendParam = getQueryParam(exchange, "append");
            boolean append = appendParam != null && appendParam.toLowerCase().contains("true");
            String content = getBodyParameter(exchange, json, "content", true);
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
