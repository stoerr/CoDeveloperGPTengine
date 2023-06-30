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

import com.google.common.collect.Range;

import io.undertow.io.Receiver;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

public class ReplaceAction extends AbstractPluginAction {

    public static final String ERRORMESSAGE_PATTERNNOTFOUND = """
            Found no occurrences of pattern. Re-read the file - it might be different than you think. Think out of the box and use a completely different pattern, match something else or use a different way to reach your goal. You are a Java regular expression expert, so you can use all advanced regex features to create a short but precise pattern.
            Common errors:
             - (.*) does not match newlines - ((?s).*?) does.
             - replaceWithGroupReferences might have already broken something because of backslash escaping rules - think of how Matcher.appendReplacement works.
            Some ideas for advanced constructs for the pattern:
                - \\A matches the start of the file
                - \\z matches the end of the file
                - (?<=something) matches the point after 'something', without matching 'something' itself - good for adding after a certain string
                - (?=something) is the opposite of that, good for inserting before a certain string
            """;

    @Override
    public String getUrl() {
        return "/replaceInFile";
    }

    @Override
    public String openApiDescription() {
        return """
                  /replaceInFile:
                    post:
                      operationId: replaceInFile
                      summary: Replaces the single occurrence of a regular expression in a file. You are a Java regular expression expert and can use all advanced regex features. The whole file is matched, not line by line. Use exactly one of literalReplacement and replacementWithGroupReferences.
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
                                pattern:
                                  type: string
                                  description: "java.util.regex.Pattern to be replaced. Examples: \\\\z is end of file, ((?s).*?) matches any characters including line breaks non-greedily."
                                literalReplacement:
                                  type: string
                                  description: searches for the given Java pattern in the file content and replaces it with the literalReplacement as it is.
                                replacementWithGroupReferences:
                                  type: string
                                  description: replaces the finding of the pattern with the replacement and replaces group references $0, $1, ..., $9 with the corresponding groups from the match. A literal $ must be given as $$.
                              required:
                                - pattern
                      responses:
                        '200':
                          description: File updated successfully
                        '400':
                          description: Invalid parameter
                        '500':
                          description: Server error
                """.stripIndent();
        // Take out multiple for now, as it has been used wrongly several times
        //                                 multiple:
        //                                  type: boolean
        //                                  description: If multiple is true, replace all occurrences, otherwise exactly one occurrence - it would be an error if there is no occurrence or several occurrences. Default is false. Use true with care and only if there is a good reason to do so.
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        Receiver requestReceiver = exchange.getRequestReceiver();
        requestReceiver.setMaxBufferSize(100000);
        requestReceiver.receiveFullBytes(this::handleBody, this::handleRequestBodyError);
    }

    // FIXME response cites actual content, token abbreviated
    // Implement own placeholder mechanism

    private void handleBody(HttpServerExchange exchange, byte[] bytes) {
        String json = new String(bytes, UTF_8);
        String path = exchange.getQueryParameters().get("path").getFirst();
        String pattern = getBodyParameter(exchange, json, "pattern", true);
        String literalReplacement = getBodyParameter(exchange, json, "literalReplacement", false);
        String replacementWithGroupReferences = getBodyParameter(exchange, json, "replacementWithGroupReferences", false);
        boolean multiple = getBodyParameter(exchange, json, "multiple", false).equalsIgnoreCase("true");

        if (!isNotEmpty(literalReplacement) && !isNotEmpty(replacementWithGroupReferences)) {
            throw sendError(exchange, 400, "Either literalReplacement or replacementWithGroupReferences must be given.");
        }
        if (isNotEmpty(literalReplacement) && isNotEmpty(replacementWithGroupReferences)) {
            throw sendError(exchange, 400, "Either literalReplacement or replacementWithGroupReferences must be given, but not both.");
        }
        if (isNotEmpty(replacementWithGroupReferences) && !replacementWithGroupReferences.contains("$")) {
            throw sendError(exchange, 400, "don't use replacementWithGroupReferences if there are no group references.");
        }

        Path filePath = DevToolBench.currentDir.resolve(path);
        try {
            String content = Files.readString(filePath, UTF_8);
            Matcher m = Pattern.compile(pattern).matcher(content);
            List<Range<Long>> modifiedLineNumbers = new ArrayList<>();
            int replacementCount = 0;

            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                long startLine = lineNumberAfter(content.substring(0, m.start()));
                long endLine = lineNumberAfter(content.substring(0, m.end()));
                modifiedLineNumbers.add(Range.closed(startLine, endLine));
                if (isNotEmpty(literalReplacement)) {
                    m.appendReplacement(sb, Matcher.quoteReplacement(literalReplacement));
                } else {
                    m.appendReplacement(sb, TbUtils.compileReplacement(exchange, replacementWithGroupReferences));
                }
                replacementCount++;
            }
            m.appendTail(sb);

            if (!multiple && replacementCount != 1) {
                if (replacementCount == 0) {
                    throw sendError(exchange, 400, ERRORMESSAGE_PATTERNNOTFOUND);
                } else {
                    throw sendError(exchange, 400, "Found " + replacementCount + " occurrences, but expected exactly one. Please make the pattern more specific so that it matches only one occurrence.");
                }
            }

            List<String> modifiedLineDescr = rangeDescription(modifiedLineNumbers);

            Files.writeString(filePath, sb.toString(), UTF_8);
            exchange.setStatusCode(200);
            exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain; charset=UTF-8");
            exchange.getResponseSender().send("Replaced " + replacementCount + " occurrences of pattern; modified lines "
                    + String.join(", ", modifiedLineDescr));
        } catch (NoSuchFileException e) {
            throw sendError(exchange, 404, "File not found: " + DevToolBench.currentDir.relativize(filePath));
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
