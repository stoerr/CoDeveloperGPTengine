package net.stoerr.chatgpt.codevengine;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.stoerr.chatgpt.codevengine.TbUtils.lineNumberAfter;
import static net.stoerr.chatgpt.codevengine.TbUtils.rangeDescription;

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
        return "  /replaceInFile:\n" +
                "    post:\n" +
                "      operationId: replaceInFile\n" +
                "      x-openai-isConsequential: false\n" +
                "      summary: Replaces the single occurrence of one or more literal strings in a file. The whole file content is matched, not line by line.\n" +
                "      parameters:\n" +
                "        - name: path\n" +
                "          in: query\n" +
                "          description: relative path to file\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      requestBody:\n" +
                "        required: true\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                replacements:\n" +
                "                  type: array\n" +
                "                  items:\n" +
                "                    type: object\n" +
                "                    properties:\n" +
                "                      search:\n" +
                "                        type: string\n" +
                "                        description: The literal string to be replaced - can contain many lines, but please take care to find a small number of lines to replace. Everything that is replaced must be here. Prefer to match the whole line / several whole lines.\n" +
                "                      replace:\n" +
                "                        type: string\n" +
                "                        description: Literal replacement, can contain several lines. Please observe the correct indentation.\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: File updated successfully\n";
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        TbUtils.logBody("body", json);
        Path path = getPath(req, resp, true, false);
        ReplaceInFileRequest replacementRequest = gson.fromJson(json, ReplaceInFileRequest.class);
        if (replacementRequest.getReplacements().isEmpty()) {
            throw sendError(resp, 400, "No replacements given.");
        }

        try {
            String content = new String(Files.readAllBytes(path), UTF_8);
            int totalReplacementCount = 0;
            List<Range<Long>> totalModifiedLineNumbers = new ArrayList<>();
            int replacementNo = 0;

            for (Replacement replacement : replacementRequest.getReplacements()) {
                replacementNo++;
                StringBuffer sb = new StringBuffer();
                if (replacement.getSearch() == null || replacement.getSearch().isEmpty()) {
                    throw sendError(resp, 400, "Search string in replacement number " + replacementNo + " is empty.");
                }
                if (replacement.getReplace() == null) {
                    throw sendError(resp, 400, "Replacement string in replacement number " + replacementNo + " is null.");
                }
                String pattern = Pattern.quote(replacement.getSearch());
                String compiledReplacement = Matcher.quoteReplacement(replacement.getReplace());
                if (pattern.contains("\\n") && !Pattern.compile(pattern).matcher(content).find()) {
                    // try a workaround for weird ChatGPT behaviour: as of 24/1/4 it sometimes transmitted \\n
                    // instead of \n for newlines.
                    String fixedPattern = Pattern.quote(replacement.getSearch().replaceAll("\\\\n", "\n"));
                    if (Pattern.compile(fixedPattern).matcher(content).find()) {
                        TbUtils.logInfo("Applied \\\\n workaround to pattern");
                        pattern = fixedPattern;
                    }
                }

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
                        String newlinecomplaint = "";
                        if (replacement.getSearch().contains("\\n")) {
                            newlinecomplaint = "\nIf you wanted to search for an actual newline character, you need to use `\\n`, not `\\\\n` .";
                        }
                        throw sendError(resp, 400, "Search string " + replacementNo + " not found. " +
                                "You might want to re-read the file to find out whether something is different from what you expected, or use grep with enough context lines if the file is long. " +
                                "The search string is a literal string, not a regular expression." + newlinecomplaint +
                                "\nNo changes have been made because of this error" +
                                (replacementRequest.getReplacements().size() > 1 ? " - the other replacements also have not been performed" : "") + "." +
                                "The search string that was not found is:\n" + replacement.getSearch()
                        );
                    } else {
                        throw sendError(resp, 400, "Found " + replacementCount + " occurrences of search string " +
                                replacementNo +
                                ", but expected exactly one. You must change the pattern - you can e.g. add the previous or following line to the search string to make it match only one place in the file.");
                    }
                }

                totalModifiedLineNumbers.addAll(modifiedLineNumbers);
                totalReplacementCount += replacementCount;
                content = sb.toString();
            }

            List<String> modifiedLineDescr = rangeDescription(totalModifiedLineNumbers);

            Files.write(path, content.getBytes(UTF_8));
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(totalReplacementCount + " replacement; modified line(s) "
                    + String.join(", ", modifiedLineDescr));
        } catch (NoSuchFileException e) {
            throw sendError(resp, 404, "File not found: " + CoDeveloperEngine.canonicalName(path));
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
