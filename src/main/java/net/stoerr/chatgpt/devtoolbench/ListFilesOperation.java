package net.stoerr.chatgpt.devtoolbench;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ListFilesOperation extends AbstractPluginOperation {

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
                    .map(p -> currentDir.relativize(p).toString())
                    .collect(Collectors.toList());
            String response = "[\n" + files.stream().map(this::jsonRep).collect(Collectors.joining(",\n")) + "\n]\n";
            exchange.getResponseSender().send(response);
        } else {
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
            exchange.getResponseSender().send("Directory not found");
        }
    }
}
