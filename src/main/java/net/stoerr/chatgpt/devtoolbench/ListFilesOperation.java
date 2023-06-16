package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
        Map<String, String> queryParams = getQueryParams(exchange);
        Path path = getPath(exchange);
        String filenameRegex = queryParams.get("filenameRegex");
        String grepRegex = queryParams.get("grepRegex");
        Pattern filenamePattern = filenameRegex != null ? Pattern.compile(filenameRegex) : null;
        Pattern grepPattern = grepRegex != null ? Pattern.compile(grepRegex) : null;

        if (Files.isDirectory(path)) {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=utf-8");
            List<String> files = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> !DevToolbench.IGNORE.matcher(p.toString()).matches())
                    .filter(p -> filenamePattern == null || filenamePattern.matcher(p.getFileName().toString()).matches())
                    .filter(p -> {
                        if (grepPattern == null) {
                            return true;
                        } else {
                            try (Stream<String> lines = Files.lines(p)) {
                                return lines.anyMatch(line -> grepPattern.matcher(line).find());
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    })
                    .map(p -> DevToolbench.currentDir.relativize(p).toString())
                    .toList();
            byte[] response = (String.join("\n", files) + "\n").getBytes(StandardCharsets.UTF_8);
            exchange.setStatusCode(200);
            exchange.setResponseContentLength(response.length);
            exchange.getResponseSender().send(ByteBuffer.wrap(response));
        } else {
            sendError(exchange, 404, "Directory not found");
        }
    }
}
