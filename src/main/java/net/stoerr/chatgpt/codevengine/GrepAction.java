package net.stoerr.chatgpt.codevengine;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GrepAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/grepFiles";
    }

    @Override
    public String openApiDescription() {
        return "  /grepFiles:\n" +
                "    get:\n" +
                "      operationId: grepAction\n" +
                "      x-openai-isConsequential: false\n" +
                "      summary: Search for lines in text files matching the given regex.\n" +
                "      parameters:\n" +
                "        - name: path\n" +
                "          in: query\n" +
                "          description: relative path to the directory to search in or the file to search. root directory = '.'\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: fileRegex\n" +
                "          in: query\n" +
                "          description: optional regex to filter file names\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: grepRegex\n" +
                "          in: query\n" +
                "          description: regex to filter lines in the files\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: contextLines\n" +
                "          in: query\n" +
                "          description: number of context lines to include with each match (not yet used)\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: integer\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Lines matching the regex\n" +
                "          content:\n" +
                "            text/plain:\n" +
                "              schema:\n" +
                "                type: string\n";
    }

    // output format:
    // ======================== <filename> line uvw until xyz
    // matching lines with context lines
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path startPath = getPath(req, resp, true);
        String fileRegex = getQueryParam(req, "fileRegex");
        String grepRegex = getMandatoryQueryParam(req, resp, "grepRegex");
        String contextLinesParam = getQueryParam(req, "contextLines");
        RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, startPath, fileRegex, grepRegex, contextLinesParam);
        Pattern grepPattern;
        try {
            grepPattern = Pattern.compile(grepRegex);
        } catch (PatternSyntaxException e) {
            throw sendError(resp, 400, "Invalid grepRegex parameter: " + grepRegex + "\n" + e.getMessage());
        }
        Pattern filePattern;
        try {
            filePattern = fileRegex != null ? Pattern.compile(fileRegex) : Pattern.compile(".*");
        } catch (PatternSyntaxException e) {
            throw sendError(resp, 400, "Invalid fileRegex parameter: " + fileRegex + "\n" + e.getMessage());
        }
        int contextLinesRaw = 0;
        if (contextLinesParam != null && !contextLinesParam.trim().isEmpty()) {
            try {
                contextLinesRaw = Integer.parseInt(contextLinesParam);
            } catch (NumberFormatException e) {
                throw sendError(resp, 400, "Invalid contextLines parameter: " + contextLinesParam);
            }
        }
        final int contextLines = contextLinesRaw;

        if (!Files.exists(startPath)) {
            throw sendError(resp, 404, "Path does not exist: " + startPath);
        } else if (!Files.isReadable(startPath)) {
            throw sendError(resp, 404, "Path is not readable: " + startPath);
        }

        List<Path> matchingFiles = findMatchingFiles(resp, startPath, filePattern, grepPattern)
                .collect(Collectors.toList());
        if (!matchingFiles.isEmpty()) {
            StringBuilder buf = new StringBuilder();
            matchingFiles.stream()
                    .filter(f -> !BINARY_FILES_PATTERN.matcher(f.toString()).find())
                    .forEachOrdered(path -> {
                        try {
                            List<String> lines = Files.readAllLines(path);
                            int lastEndLine = -1; // last match end line number
                            int blockStart = -1;  // start of current block of context lines
                            for (int i = 0; i < lines.size(); i++) {
                                String line = lines.get(i);
                                boolean isMatch = grepPattern.matcher(line).find();

                                if (isMatch) {
                                    if (blockStart == -1) {  // start of a new block
                                        blockStart = Math.max(lastEndLine + 1, i - contextLines);
                                    }
                                    lastEndLine = Math.min(i + contextLines, lines.size() - 1);
                                }

                                // If we're beyond the context of the last match or at the end of the file, append the block.
                                if ((i > lastEndLine && blockStart != -1) || (isMatch && i == lines.size() - 1)) {
                                    appendBlock(lines, buf, path, blockStart, lastEndLine + 1);  // +1 to include the last line of context
                                    blockStart = -1;  // reset block start
                                }
                            }
                            if (blockStart != -1) {  // append the last block
                                appendBlock(lines, buf, path, blockStart, lastEndLine + 1);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
            resp.setContentType("text/plain;charset=UTF-8");
            resp.getWriter().write(buf.toString());
        } else {
            long filePathFileCount = findMatchingFiles(resp, startPath, filePattern, null).count();
            if (filePathFileCount > 0)
                throw sendError(resp, 404, "Found " + filePathFileCount + " files whose name is matching the filePathRegex but none of them contain a line matching the grepRegex.");
            else if (Files.isDirectory(startPath)) {
                if (Files.newDirectoryStream(startPath).iterator().hasNext()) {
                    throw sendError(resp, 404, "No files found matching filePathRegex: " + fileRegex);
                } else {
                    throw sendError(resp, 404, "No files found in directory: " + startPath);
                }
            }
        }
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
