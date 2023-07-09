package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.stoerr.chatgpt.devtoolbench.TbUtils.lineNumberAfter;
import static net.stoerr.chatgpt.devtoolbench.TbUtils.rangeDescription;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.google.common.collect.Range;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ReplaceAction extends AbstractPluginAction {

    // FIXME not quite appropriate here
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
                              type: array
                              items:
                                type: object
                                properties:
                                  search:
                                    type: string
                                    description: The string to be replaced - can contain many lines, but please take care to find a small number of lines to replace. Prefer this to pattern for simplicity.
                                  replace:
                                    type: string
                                    description: replaces the finding of the pattern with the replacement and replaces group references $0, $1, ..., $9 with the corresponding groups from the match. A literal $ must be given as $$.
                      responses:
                        '200':
                          description: File updated successfully
                """.stripIndent();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        Path path = getPath(req, resp, true);
        JsonArray jsonArray = new JsonParser().parse(json).getAsJsonArray();
        List<Map<String, String>> replacements = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            JsonObject jsonObject = element.getAsJsonObject();
            String search = jsonObject.get("search").getAsString();
            String replace = jsonObject.get("replace").getAsString();
            Map<String, String> replacement = new HashMap<>();
            replacement.put("search", search);
            replacement.put("replace", replace);
            replacements.add(replacement);
        }


        try {
            String content = Files.readString(path, UTF_8);
            StringBuilder sb = new StringBuilder(content);
            int totalReplacementCount = 0;
            List<Range<Long>> totalModifiedLineNumbers = new ArrayList<>();

            for (Map<String, String> replacement : replacements) {
                String pattern = Pattern.quote(replacement.get("search"));
                String compiledReplacement = Matcher.quoteReplacement(replacement.get("replace"));
                Matcher m = Pattern.compile(pattern).matcher(sb);
                List<Range<Long>> modifiedLineNumbers = new ArrayList<>();
                int replacementCount = 0;

                while (m.find()) {
                    long startLine = lineNumberAfter(sb.substring(0, m.start()));
                    long endLine = lineNumberAfter(sb.substring(0, m.end()));
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

                totalModifiedLineNumbers.addAll(modifiedLineNumbers);
                totalReplacementCount += replacementCount;
            }

            List<String> modifiedLineDescr = rangeDescription(totalModifiedLineNumbers);

            Files.writeString(path, sb.toString(), UTF_8);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write("Replaced " + totalReplacementCount + " occurrences of pattern; modified lines "
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
