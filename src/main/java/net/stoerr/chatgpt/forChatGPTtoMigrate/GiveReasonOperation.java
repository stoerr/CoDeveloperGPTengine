package net.stoerr.chatgpt.forChatGPTtoMigrate;

import java.io.IOException;
import java.io.InputStream;

import com.sun.net.httpserver.HttpExchange;

/**
 * An operation /reason that might or might not be good to introduce a REACT like pattern - it just gets a text on stdin and writes that to stdout.
 */
// curl -is http://localhost:3001/reason -d "{\"reason\": \"testreason\"}"
class GiveReasonOperation extends AbstractPluginOperation {

    @Override
    public String getUrl() {
        return "/reason";
    }

    @Override
    public String openApiDescription() {
        return """
                  /reason:
                    post:
                      operationId: reason
                      summary: Provide a reason for the next operation on the filemanager plugin.
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
                          description: Reason accepted
                """.stripIndent();
    }

    public void handle(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(204, -1);
        try (InputStream is = exchange.getRequestBody()) {
            System.out.print("Reason: ");
            is.transferTo(System.out);
            System.out.println();
        }
    }
}
