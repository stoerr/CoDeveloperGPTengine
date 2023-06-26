package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Range;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class ReplaceAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/replace";
    }

    @Override
    public String openApiDescription() {
        return """
                  /replaceInFile:
                    post:
                      operationId: replaceInFile
                      summary: Replaces occurrences of a regular expression in a file. You are a Java regular expression expert - there is \\A for start of file, \\z for end of file, they can match multiple lines or line fragments, there are reluctant quantifiers, you can use zero-width positive lookaheads / lookbehinds for inserting the replacement, and much more.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to file
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
                                multiple:
                                  type: boolean
                                  description: if true, replace all occurrences, otherwise exactly one occurrence - it would be an error if there is no occurrence or several occurrences
                                pattern:
                                  required: true
                                  type: string
                                  description: java Pattern to be replaced
                                replacement:
                                  required: true
                                  type: string
                                  description: will replace the regex; can contain group references as in Java Matcher.appendReplacement
                      responses:
                        '200':
                          description: File updated successfully
                        '400':
                          description: Invalid parameter
                        '500':
                          description: Server error
                """.stripIndent();
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.getRequestReceiver().receiveFullString(this::handleBody, this::handleRequestBodyError);
    }

    private void handleBody(HttpServerExchange exchange, String json) {
        String path = exchange.getQueryParameters().get("path").getFirst();
        String pattern = getBodyParameter(exchange, json, "pattern", true);
        String replacement = getBodyParameter(exchange, json, "replacement", true);
        boolean multiple = getBodyParameter(exchange, json, "multiple", false).equalsIgnoreCase("true");

        if (pattern == null || pattern.isEmpty() || replacement == null) {
            exchange.setStatusCode(400);
            exchange.getResponseSender().send("searchString and replacement parameters are required");
            return;
        }

        Path filePath = DevToolbench.currentDir.resolve(path);
        try {
            String content = Files.readString(filePath, UTF_8);
            Matcher m = Pattern.compile(pattern).matcher(content);
            List<Range<Long>> modifiedLineNumbers = new ArrayList<>();
            int replacementCount = 0;

            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                long startLine = lineNumberAfter(content.substring(0, m.start()));
                long endLine = lineNumberAfter(content.substring(0, m.end()));
                modifiedLineNumbers.add(Range.closed(startLine, endLine));
                m.appendReplacement(sb, replacement);
                replacementCount++;
            }
            m.appendTail(sb);

            if (!multiple && replacementCount != 1) {
                if (replacementCount == 0) {
                    throw sendError(exchange, 400, "Found no occurrences of pattern.");
                } else {
                    throw sendError(exchange, 400, "Found " + replacementCount + " occurrences, but expected exactly one because parameter multiple = false.");
                }
            }

            List<String> modifiedLineDescr = rangeDescription(modifiedLineNumbers);

            Files.writeString(filePath, sb.toString(), UTF_8);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
            exchange.getResponseSender().send("Replaced " + replacementCount + " occurrences of pattern; modified lines "
                    + modifiedLineDescr.stream().collect(Collectors.joining(", ")));
        } catch (NoSuchFileException e) {
            throw sendError(exchange, 404, "File not found: " + DevToolbench.currentDir.relativize(filePath));
        } catch (IOException e) {
            throw sendError(exchange, 500, "Error reading or writing file : " + e);
        }
    }

    protected long lineNumberAfter(String contentpart) {
        return (contentpart + "x").lines().count();
    }

    private static List<String> rangeDescription(List<Range<Long>> modifiedLineNumbers) {
        List<String> modifiedLineDescr = new ArrayList<>();
        Range<Long> lastRange = null;
        for (Range<Long> range : modifiedLineNumbers) {
            if (lastRange != null) {
                if (lastRange.upperEndpoint() >= range.lowerEndpoint() - 1) {
                    lastRange = lastRange.span(range);
                } else {
                    modifiedLineDescr.add(rangeDescription(lastRange));
                    lastRange = range;
                }
            } else {
                lastRange = range;
            }
        }
        if (lastRange != null) {
            modifiedLineDescr.add(rangeDescription(lastRange));
        }
        return modifiedLineDescr;
    }

    private static String rangeDescription(Range<Long> lastRange) {
        String rangeDescr;
        if (lastRange.lowerEndpoint().equals(lastRange.upperEndpoint())) {
            rangeDescr = String.valueOf(lastRange.lowerEndpoint());
        } else {
            rangeDescr = " " + lastRange.lowerEndpoint() + " - " + lastRange.upperEndpoint();
        }
        return rangeDescr;
    }

    @Override
    protected Path getPath(HttpServerExchange exchange) {
        String path = exchange.getQueryParameters().get("path").getFirst();
        return Paths.get(path);
    }

}
