package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import net.stoerr.chatgpt.forChatGPTtoMigrate.FileManagerPlugin;

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
                      summary: Recursively lists files in a directory.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to directory
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
                          description: q
                          required: false
                          schema:
                            type: string
                      responses:
                        '200':
                          description: List of relative paths of the files
                          content:
                            application/json:
                              schema:
                                type: array
                                items:
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
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json; charset=utf-8");
            List<String> files = Files.walk(path)
                    .filter(Files::isRegularFile)
                    .filter(p -> !FileManagerPlugin.IGNORE.matcher(p.toString()).matches())
                    .filter(p -> filenamePattern == null || filenamePattern.matcher(p.getFileName().toString()).matches())
                    // FIXME implement grepregex
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
                    .map(p -> currentDir.relativize(p).toString())
                    .collect(Collectors.toList());
            String response = files.stream().collect(Collectors.joining("\n")) + "\n";
            exchange.getResponseSender().send(response);
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Directory not found");
        }
    }
}
