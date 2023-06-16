package net.stoerr.chatgpt.devtoolbench;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.gson.Gson;

import io.undertow.server.HttpServerExchange;

public class ExecutePluginAction extends AbstractPluginOperation {

    private final Gson gson = new Gson();

    @Override
    public String getUrl() {
        return "/executePluginAction";
    }

    @Override
    public String openApiDescription() {
        return """
                  /executePluginAction:
                    post:
                      operationId: executePluginAction
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
        exchange.getRequestReceiver().receiveFullString(this::handleBody, this::handleError);
    }

    private void handleBody(HttpServerExchange exchange, String json) {
        try {
            Map<String, String> decoded;
            if (json.isEmpty() || "{}".equals(json)) {
                sendError(exchange, 422, "Missing content parameter");
                return;
            }
            try {
                decoded = gson.fromJson(json, Map.class);
            } catch (Exception e) {
                String error = "Parse error for content: " + e.getMessage();
                sendError(exchange, 422, error);
                return;
            }
            String content = decoded.get("content");
            if (content == null || content.isBlank()) {
                sendError(exchange, 422, "Missing content parameter or empty content");
                return;
            }
            String actionName = getQueryParams(exchange).get("actionName");
            Path path = DevToolbench.currentDir.resolve(".cgptfmgr/" + actionName + ".sh");

            if (!Files.exists(path)) {
                sendError(exchange, 400, "Action " + actionName + " not found");
                return;
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
            output = output.replaceAll(Pattern.quote(DevToolbench.currentDir.toString()), "");

            if (exitCode == 0) {
                exchange.setStatusCode(200);
                exchange.getResponseSender().send(output);
            } else {
                String response = "Execution failed with exit code " + exitCode + ": " + output;
                sendError(exchange, 500, response);
            }
        } catch (IOException | InterruptedException e) {
            sendError(exchange, 422, "Error executing action: " + e.getMessage());
        }
    }

    private void handleError(HttpServerExchange httpServerExchange, IOException e) {
        sendError(httpServerExchange, 422, "Error reading request body: " + e.getMessage());
    }
}
