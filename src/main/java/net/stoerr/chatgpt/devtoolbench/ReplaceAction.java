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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.google.common.collect.Range;
import com.google.gson.Gson;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ReplaceAction extends AbstractPluginAction {

    private final Gson gson = new Gson();

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
                      summary: Replaces the single occurrence of a string in a file. The whole file is matched, not line by line.
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
                                replacements:
                                  type: array
                                  items:
                                    type: object
                                    properties:
                                      search:
                                        type: string
                                        description: The string to be replaced - can contain many lines, but please take care to find a small number of lines to replace. Everything that is replaced must be here. Prefer to match the whole line / several whole lines.
                                      replace:
                                        type: string
                                        description: Replacement, can contain several lines. Please observe the correct indentation.
                      responses:
                        '200':
                          description: File updated successfully
                """.stripIndent();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        Path path = getPath(req, resp, true);
        ReplaceInFileRequest replacementRequest = gson.fromJson(json, ReplaceInFileRequest.class);
        if (replacementRequest.getReplacements().isEmpty()) {
            throw sendError(resp, 400, "No replacements given.");
        }

        try {
            String content = Files.readString(path, UTF_8);
            int totalReplacementCount = 0;
            List<Range<Long>> totalModifiedLineNumbers = new ArrayList<>();
            int replacementNo = 0;

            for (Replacement replacement : replacementRequest.getReplacements()) {
                replacementNo++;
                StringBuilder sb = new StringBuilder();
                String pattern = Pattern.quote(replacement.getSearch());
                String compiledReplacement = Matcher.quoteReplacement(replacement.getReplace());
                Matcher m = Pattern.compile(pattern).matcher(content);

                TbUtils.logInfo("<<<<<<<<< ORIGINAL");
                TbUtils.logInfo(replacement.getSearch());
                TbUtils.logInfo("=======");
                TbUtils.logInfo(replacement.getReplace());
                TbUtils.logInfo(">>>>>>> UPDATED");

                List<Range<Long>> modifiedLineNumbers = new ArrayList<>();
                int replacementCount = 0;

                while (m.find()) {
                    long startLine = lineNumberAfter(content.substring(0, m.start()));
                    long endLine = lineNumberAfter(content.substring(0, m.end()));
                    // That's not correct after the first replacement, but we don't care that much.
                    modifiedLineNumbers.add(Range.closed(startLine, endLine));
                    m.appendReplacement(sb, compiledReplacement);
                    replacementCount++;
                }
                m.appendTail(sb);

                if (replacementCount != 1) {
                    if (replacementCount == 0) {
                        throw sendError(resp, 400, "Search string " + replacementNo + " not found. You might want to re-read the file to find out whether something is different from what you expected, or use grep with enough context lines if the file is long.");
                    } else {
                        throw sendError(resp, 400, "Found " + replacementCount + " occurrences of search string " +
                                replacementNo +
                                ", but expected exactly one. You can e.g. add the previous or following line to the search string and pattern.");
                    }
                }

                totalModifiedLineNumbers.addAll(modifiedLineNumbers);
                totalReplacementCount += replacementCount;
                content = sb.toString();
            }

            List<String> modifiedLineDescr = rangeDescription(totalModifiedLineNumbers);

            Files.writeString(path, content, UTF_8);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(totalReplacementCount + " replacement; modified line(s) "
                    + String.join(", ", modifiedLineDescr));
        } catch (NoSuchFileException e) {
            throw sendError(resp, 404, "File not found: " + DevToolBench.currentDir.relativize(path));
        } catch (IOException e) {
            throw sendError(resp, 500, "Error reading or writing file : " + e);
        } catch (IllegalArgumentException e) {
            throw sendError(resp, 400, "Invalid replacement: " + e.getMessage());
        }
    }


    public static class ReplaceInFileRequest {

        private List<Replacement> replacements;

        public List<Replacement> getReplacements() {
            return replacements;
        }

        public void setReplacements(List<Replacement> replacements) {
            this.replacements = replacements;
        }

    }

    public static class Replacement {

        private String search;

        private String replace;

        public String getSearch() {
            return search;
        }

        public void setSearch(String search) {
            this.search = search;
        }

        public String getReplace() {
            return replace;
        }

        public void setReplace(String replace) {
            this.replace = replace;
        }
    }


}
