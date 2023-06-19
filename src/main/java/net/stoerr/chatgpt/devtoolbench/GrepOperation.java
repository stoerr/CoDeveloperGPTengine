package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

    // output format:
    // ======================== <filename> line uvw until xyz
    // matching lines with context lines
    @Override
    public void handleRequest(HttpServerExchange exchange) {
        String fileRegex = getQueryParam(exchange, "fileRegex");
        String grepRegex = getMandatoryQueryParam(exchange, "grepRegex");
        Pattern grepPattern = Pattern.compile(grepRegex);
        Pattern filePattern = fileRegex != null ? Pattern.compile(fileRegex) : Pattern.compile(".*");
        int contextLinesRaw = 0;
        String contextLinesParam = getQueryParam(exchange, "contextLines");
        if (contextLinesParam != null && !contextLinesParam.isBlank()) {
            try {
                contextLinesRaw = Integer.parseInt(contextLinesParam);
            } catch (NumberFormatException e) {
                sendError(exchange, 400, "Invalid contextLines parameter: " + contextLinesParam);
            }
        }
        final int contextLines = contextLinesRaw;

        Stream<Path> matchingFiles = findMatchingFiles(exchange, getPath(exchange), filePattern, grepPattern);
        StringBuilder buf = new StringBuilder();
        matchingFiles.forEachOrdered(path -> {
            try {
                List<String> lines = Files.readAllLines(path);
                int lastEndLine = -1; // last match end line number
                int blockStart = -1;  // start of current block of context lines
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (grepPattern.matcher(line).find()) {
                        if (blockStart == -1) {  // start of a new block
                            blockStart = Math.max(lastEndLine + 1, i - contextLines);
                        }
                        lastEndLine = Math.min(i + contextLines + 1, lines.size());
                    } else if (blockStart != -1) {  // end of a block
                        appendBlock(lines, buf, path, blockStart, lastEndLine);
                        blockStart = -1;
                    }
                }
                if (blockStart != -1) {  // append the last block
                    appendBlock(lines, buf, path, blockStart, lastEndLine);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        exchange.getResponseSender().send(buf.toString());
    }

    private void appendBlock(List<String> lines, StringBuilder buf, Path path, int start, int end) {
        if (start == end - 1) {
            buf.append("======================== ").append(mappedFilename(path)).append(" line ").append(start + 1).append('\n');
        } else {
            buf.append("======================== ").append(mappedFilename(path)).append(" lines ").append(start + 1).append(" to ").append(end).append('\n');
        }
        for (int j = start; j < end; j++) {
            buf.append(lines.get(j)).append('\n');
        }
    }

}
