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
                      summary: Replaces the single occurrence of a string in a file. Use exactly one of literalReplacement.
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
                                  description: The string to be replaced - can contain many lines, but please take care to find a small number of lines to replace.
                                literalReplacement:
                                  type: string
                                  description: searches for the given Java pattern in the file content and replaces it with the literalReplacement as it is.
                      responses:
                        '200':
                          description: File updated successfully
                """.stripIndent();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        Path path = getPath(req, resp, true);
        String literalSearchString = getBodyParameter(resp, json, "literalSearchString", false);
        String literalReplacement = getBodyParameter(resp, json, "literalReplacement", false);

        if (!isNotEmpty(literalSearchString)) {
            throw sendError(resp, 400, "literalSearchString must be given.");
        }

        if (literalReplacement == null) {
            throw sendError(resp, 400, "literalReplacement must be given.");
        }

        String pattern = Pattern.quote(literalSearchString);
        String compiledReplacement = Matcher.quoteReplacement(literalReplacement);

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
                m.appendReplacement(sb, compiledReplacement);
                replacementCount++;
            }
            m.appendTail(sb);

            if (replacementCount != 1) {
                if (replacementCount == 0) {
                    throw sendError(resp, 400, ERRORMESSAGE_PATTERNNOTFOUND);
                } else {
                    throw sendError(resp, 400, "Found " + replacementCount + " occurrences, but expected exactly one. Please make the pattern more specific so that it matches only one occurrence. You can e.g. add the previous or following line to the search pattern and the replacement. ");
                }
            }

            List<String> modifiedLineDescr = rangeDescription(modifiedLineNumbers);

            Files.writeString(path, sb.toString(), UTF_8);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("Replaced " + replacementCount + " occurrences of pattern; modified lines "
                    + String.join(", ", modifiedLineDescr));
        } catch (NoSuchFileException e) {
            throw sendError(resp, 404, "File not found: " + DevToolBench.currentDir.relativize(path));
        } catch (IOException e) {
            throw sendError(resp, 500, "Error reading or writing file : " + e);
        } catch (PatternSyntaxException e) {
            throw sendError(resp, 400, "Invalid pattern. You are a Javascript expert, analyze the following problem with the regular expression you used: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw sendError(resp, 400, "Invalid replacement: " + e.getMessage());
        }
    }
}