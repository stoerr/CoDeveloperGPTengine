package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

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
        Process process = null;
        try {
            String content = getMandatoryContentFromBody(exchange, json);
            String actionName = getMandatoryQueryParam(exchange, "actionName");
            Path path = DevToolbench.currentDir.resolve(".cgptdevbench/" + actionName + ".sh");

            if (!Files.exists(path)) {
                sendError(exchange, 400, "Action " + actionName + " not found");
            }

            ProcessBuilder pb = new ProcessBuilder("/bin/sh", path.toString());
            pb.redirectErrorStream(true);
            System.out.println("Starting process: " + pb.command() + " with content: " + abbreviate(content, 40));
            process = pb.start();

            OutputStream out = process.getOutputStream();
            exchange.getConnection().getWorker().execute(() -> {
                try {
                    try {
                        out.write(content.getBytes(StandardCharsets.UTF_8));
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            InputStream inputStream = process.getInputStream();
            String output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(1, TimeUnit.MINUTES)) {
                sendError(exchange, 500, "Process did not finish within one minute");
            }
            int exitCode = process.exitValue();
            System.out.println("Process finished with exit code " + exitCode + ": " + abbreviate(output, 200));
            output = output.replaceAll(Pattern.quote(DevToolbench.currentDir.toString() + "/"), "");

            if (exitCode == 0) {
                exchange.setStatusCode(200);
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
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
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

    }

}
