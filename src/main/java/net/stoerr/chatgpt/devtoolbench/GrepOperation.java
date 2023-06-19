package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import io.undertow.server.HttpServerExchange;

public class GrepOperation extends AbstractPluginOperation {

    @Override
    public String getUrl() {
        return "/grepFiles";
    }

    @Override
    public String openApiDescription() {
        return """
                  /grepFiles:
                    post:
                      operationId: grepAction
                      summary: Search for lines in files matching the given regex.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to directory. root directory = '.'
                          required: true
                          schema:
                            type: string
                        - name: fileRegex
                          in: query
                          description: optional regex to filter file names
                          required: false
                          schema:
                            type: string
                        - name: grepRegex
                          in: query
                          description: regex to filter lines in the files
                          required: true
                          schema:
                            type: string
                        - name: contextLines
                          in: query
                          description: number of context lines to include with each match (not yet used)
                          required: false
                          schema:
                            type: integer
                      responses:
                        '200':
                          description: Lines matching the regex
                          content:
                            text/plain:
                              schema:
                                type: string
                        '400':
                          description: Invalid parameter
                        '500':
                          description: Error reading files
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        try {
            String fileRegex = getQueryParam(exchange, "fileRegex");
            String grepRegex = getMandatoryQueryParam(exchange, "grepRegex");
            Pattern grepPattern = Pattern.compile(grepRegex);
            Pattern filePattern = fileRegex != null ? Pattern.compile(fileRegex) : Pattern.compile(".*");
            int contextLines = 0;
            String contextLinesParam = getQueryParam(exchange, "contextLines");
            if (contextLinesParam != null && !contextLinesParam.isBlank()) {
                try {
                    contextLines = Integer.parseInt(contextLinesParam);
                } catch (NumberFormatException e) {
                    sendError(exchange, 400, "Invalid contextLines parameter: " + contextLinesParam);
                }
            }

            // TODO: Implement the contextLines parameter
            List<String> result = findMatchingFiles(getPath(exchange), filePattern, grepPattern)
                    .map(p -> DevToolbench.currentDir.relativize(p).toString())
                    .toList();
            exchange.getResponseSender().send(result.stream().collect(Collectors.joining("\n")) + "\n");
        } catch (IOException e) {
            sendError(exchange, 500, "Error reading files: " + e.getMessage());
        }
    }
}
