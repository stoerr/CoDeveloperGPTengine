package net.stoerr.chatgpt.devtoolbench;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

// curl -is http://localhost:3001/listFiles?path=.
public class ListFilesOperation extends AbstractPluginOperation {

    @Override
    public String getUrl() {
        return "/listFiles";
    }

    @Override
    public String openApiDescription() {
        return """
                  /listFiles:
                    get:
                      operationId: listFiles
                      summary: Recursively lists files in a directory. Optionally filters by filename and content.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to directory. root directory = '.'
                          required: true
                          schema:
                            type: string
                        - name: filenameRegex
                          in: query
                          description: regex to filter file names
                          required: false
                          schema:
                            type: string
                        - name: grepRegex
                          in: query
                          description: an optional regex that lists only files with matching content
                          required: false
                          schema:
                            type: string
                      responses:
                        '200':
                          description: List of relative paths of the files
                          content:
                            text/plain:
                              schema:
                                type: string
                        '404':
                          description: Directory not found
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        Path path = getPath(exchange);
        String filenameRegex = getQueryParam(exchange, "filenameRegex");
        String grepRegex = getQueryParam(exchange, "grepRegex");
        Pattern filenamePattern;
        try {
            filenamePattern = filenameRegex != null ? Pattern.compile(filenameRegex) : null;
        } catch (Exception e) {
            throw sendError(exchange, 400, "Invalid filenameRegex: " + e.getMessage());
        }
        Pattern grepPattern;
        try {
            grepPattern = grepRegex != null ? Pattern.compile(grepRegex) : null;
        } catch (Exception e) {
            throw sendError(exchange, 400, "Invalid grepRegex: " + e.getMessage());
        }

        if (Files.isDirectory(path)) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            List<String> files = findMatchingFiles(path, filenamePattern, grepPattern)
                    .map(p -> DevToolbench.currentDir.relativize(p).toString())
                    .toList();
            if (files.isEmpty()) {
                throw sendError(exchange, 404, "No files found");
            }
            byte[] response = (String.join("\n", files) + "\n").getBytes(StandardCharsets.UTF_8);
            exchange.setStatusCode(200);
            exchange.setResponseContentLength(response.length);
            exchange.getResponseSender().send(ByteBuffer.wrap(response));
        } else {
            throw sendError(exchange, 404, "Directory not found");
        }
    }

}
