package net.stoerr.chatgpt.devtoolbench;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import io.undertow.server.HttpServerExchange;

public class ExecuteAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/executeAction";
    }

    @Override
    public String openApiDescription() {
        return """
                  /executeAction:
                    post:
                      operationId: executeAction
                      summary: Execute an action with given content as standard input. Only on explicit user request.
                      parameters:
                        - name: actionName
                          in: query
                          description: name of the action to be executed
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
                        '200':
                          description: Action executed successfully, output returned
                          content:
                            text/plain:
                              schema:
                                type: string
                        '400':
                          description: Action not found
                        '500':
                          description: Action execution failed, output returned
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullString(this::handleBody, this::handleRequestBodyError);
    }

    private void handleBody(HttpServerExchange exchange, String json) {
        try {
            String content = getMandatoryContentFromBody(exchange, json);
            String actionName = getMandatoryQueryParam(exchange, "actionName");
            Path path = DevToolbench.currentDir.resolve(".cgptdevbench/" + actionName + ".sh");

            if (!Files.exists(path)) {
                sendError(exchange, 400, "Action " + actionName + " not found");
            }

            ProcessBuilder pb = new ProcessBuilder("/bin/sh", path.toString());
            pb.redirectErrorStream(true);
            System.out.println("Starting process: " + pb.command() + " with content: " + content);
            Process process = pb.start();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            writer.write(content);
            writer.close();

            InputStream inputStream = process.getInputStream();
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            System.out.println("Process finished with exit code " + exitCode + ": " + output);
            output = output.replaceAll(Pattern.quote(DevToolbench.currentDir.toString() + "/"), "");

            if (exitCode == 0) {
                exchange.setStatusCode(200);
                exchange.getResponseSender().send(output);
            } else {
                String response = "Execution failed with exit code " + exitCode + ": " + output;
                sendError(exchange, 500, response);
            }
        } catch (InterruptedException e) {
            sendError(exchange, 500, "Error executing action: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            sendError(exchange, 500, "Error executing action: " + e.getMessage());
        }

    }

}
