package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.google.common.collect.Range;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ReplaceAction extends AbstractPluginAction {

    public static final String ERRORMESSAGE_PATTERNNOTFOUND = """
            Found no occurrences of pattern.
            Possible actions to fix the problem:
             - Re-read the file - it might be different than you think. 
             - Use literalSearchString instead of specifying a pattern. That is less error prone.
             - Think out of the box and use a completely different pattern, match something else or use a different way to reach your goal. You can use all advanced regex features to create a short but precise pattern.
            Common errors:
             - (.*) does not match newlines - ((?s).*?) does.
             - replaceWithGroupReferences might have already broken something because of backslash escaping rules - think of how Matcher.appendReplacement works.
            Some ideas for advanced constructs for the pattern:
                - \\A matches the start of the file
                - \\z matches the end of the file
                - you can match backslashes with . in the pattern, to avoid potential errors due to missed backslash escaping
                - (?<=something) matches the point after 'something', without matching 'something' itself - good for adding after a certain string
                - (?=something) matches the point before 'something', good for inserting before a certain string
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
                      summary: Replaces the single occurrence of a regular expression or a string in a file. You can use all advanced regex features. The whole file is matched, not line by line. Use exactly one of literalReplacement and replacementWithGroupReferences.
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
                                literalSearchString:
                                  type: string
                                  description: "The string to be replaced - can contain many lines, but please take care to find a small number of lines to replace. Prefer this to pattern for simplicity."
                                pattern:
                                  type: string
                                  description: "java.util.regex.Pattern to be replaced. Examples: \\\\z is end of file, ((?s).*?) matches any characters including line breaks non-greedily."
                                literalReplacement:
                                  type: string
                                  description: "searches for the given Java pattern in the file content and replaces it with the literalReplacement as it is."
                                replacementWithGroupReferences:
                                  type: string
                                  description: "replaces the finding of the pattern with the replacement and replaces group references $0, $1, ..., $9 with the corresponding groups from the match. A literal $ must be given as $$."
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

    // FIXME response cites should cite actual content, token abbreviated

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        Path path = getPath(req, resp);
        String pattern = getBodyParameter(resp, json, "pattern", false);
        String literalSearchString = getBodyParameter(resp, json, "literalSearchString", false);
        String literalReplacement = getBodyParameter(resp, json, "literalReplacement", false);
        String replacementWithGroupReferences = getBodyParameter(resp, json, "replacementWithGroupReferences", false);
        boolean multiple = "true".equalsIgnoreCase(getBodyParameter(resp, json, "multiple", false));

        if (isNotEmpty(literalSearchString) && isNotEmpty(pattern)) {
            throw sendError(resp, 400, "Either literalSearchString or pattern must be given, but not both.");
        }
        if (!isNotEmpty(literalSearchString) && !isNotEmpty(pattern)) {
            throw sendError(resp, 400, "One of literalSearchString or pattern must be given.");
        }

        if (literalReplacement == null && replacementWithGroupReferences == null) {
            throw sendError(resp, 400, "Either literalReplacement or replacementWithGroupReferences must be given.");
        }
        if (literalReplacement != null && replacementWithGroupReferences != null) {
            throw sendError(resp, 400, "Either literalReplacement or replacementWithGroupReferences must be given, but not both.");
        }
        if (isNotEmpty(replacementWithGroupReferences) && !replacementWithGroupReferences.contains("$")) {
            throw sendError(resp, 400, "don't use replacementWithGroupReferences if there are no group references.");
        }
        if (isNotEmpty(literalSearchString) && isNotEmpty(replacementWithGroupReferences)) {
            throw sendError(resp, 400, "literalSearchString doesn't make sense with replacementWithGroupReferences.");
        }

        if (isNotEmpty(literalSearchString)) {
            pattern = Pattern.quote(literalSearchString);
        }

        try {
            String content = Files.readString(path, UTF_8);
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
                    m.appendReplacement(sb, TbUtils.compileReplacement(resp, replacementWithGroupReferences));
                }
                replacementCount++;
            }
            m.appendTail(sb);

            if (!multiple && replacementCount != 1) {
                if (replacementCount == 0) {
                    throw sendError(resp, 400, ERRORMESSAGE_PATTERNNOTFOUND);
                } else {
                    throw sendError(resp, 400, "Found " + replacementCount + " occurrences, but expected exactly one. Please make the pattern more specific so that it matches only one occurrence.");
                }
            }

            List<String> modifiedLineDescr = rangeDescription(modifiedLineNumbers);

            Files.writeString(path, sb.toString(), UTF_8);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Replaced " + replacementCount + " occurrences of pattern; modified lines "
                    + String.join(", ", modifiedLineDescr));
        } catch (NoSuchFileException e) {
            throw sendError(resp, 404, "File not found: " + DevToolBench.currentDir.relativize(path));
        } catch (IOException e) {
            throw sendError(resp, 500, "Error reading or writing file : " + e);
        } catch (PatternSyntaxException e) {
            if (e.getMessage().contains("Unclosed character class") && pattern.contains("[^]")) {
                throw sendError(resp, 400, "Invalid pattern; [^] is invalid in Java regular expressions. To match characters including newline you can use ((?s).*?)\n" +
                        "The error was: " + e.getMessage());
            } else {
                throw sendError(resp, 400, "Invalid pattern. You are a Javascript expert, analyze the following problem with the regular expression you used: " + e.getMessage());
            }
        } catch (IllegalArgumentException e) {
            throw sendError(resp, 400, "Invalid replacement: " + e.getMessage());
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

}
