package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
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
                          description: Invalid regex
                        '500':
                          description: Error reading files
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        try {
            Map<String, String> queryParams = getQueryParams(exchange);
            String fileRegex = queryParams.get("fileRegex");
            String grepRegex = queryParams.get("grepRegex");
            if (grepRegex == null || grepRegex.isBlank()) {
                sendError(exchange, 422, "Missing grepRegex parameters");
                return;
            }
            Pattern grepPattern = Pattern.compile(grepRegex);
            Pattern filePattern = fileRegex != null ? Pattern.compile(fileRegex) : Pattern.compile(".*");
            // TODO: Implement the contextLines parameter
            String result = Files.walk(getPath(exchange))
                    .filter(Files::isRegularFile)
                    .filter(path -> filePattern != null ? filePattern.matcher(path.getFileName().toString()).find() : true)
                    .flatMap(p -> {
                        try {
                            return Files.lines(p).filter(line -> grepPattern.matcher(line).find());
                        } catch (IOException e) {
                            System.out.println("Error reading " + p + " : " + e.getMessage());
                            return "".lines();
                        }
                    })
                    .collect(Collectors.joining("\n"));
            exchange.getResponseSender().send(result);
        } catch (IOException e) {
            sendError(exchange, 500, "Error reading files: " + e.getMessage());
        }
    }
}
