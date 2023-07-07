package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// curl -is http://localhost:3001/listFiles?path=.
public class ListFilesAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/listFiles";
    }

    @Override
    public String openApiDescription() {
        return """
                  /listFiles:
                    get:
                      operationId: listFiles
                      summary: Recursively lists files in a directory. Optionally filters by filename and content.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to directory to list. root directory = '.'
                          required: true
                          schema:
                            type: string
                        - name: filePathRegex
                          in: query
                          description: regex to filter file paths
                          required: false
                          schema:
                            type: string
                        - name: grepRegex
                          in: query
                          description: an optional regex that lists only files with matching content
                          required: false
                          schema:
                            type: string
                      responses:
                        '200':
                          description: List of relative paths of the files
                          content:
                            text/plain:
                              schema:
                                type: string
                """.stripIndent();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path path = getPath(req, resp);
        String filePathRegex = getQueryParam(req, "filePathRegex");
        String grepRegex = getQueryParam(req, "grepRegex");
        RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, path, filePathRegex, grepRegex);
        Pattern filePathPattern;
        try {
            filePathPattern = filePathRegex != null ? Pattern.compile(filePathRegex) : null;
        } catch (Exception e) {
            throw sendError(resp, 400, "Invalid filePathRegex: " + e);
        }
        Pattern grepPattern;
        try {
            grepPattern = grepRegex != null ? Pattern.compile(grepRegex) : null;
        } catch (Exception e) {
            throw sendError(resp, 400, "Invalid grepRegex: " + e);
        }

        if (Files.isDirectory(path)) {
            resp.setContentType("text/plain;charset=UTF-8");
            List<String> files = findMatchingFiles(resp, path, filePathPattern, grepPattern)
                    .map(this::mappedFilename)
                    .toList();
            if (files.isEmpty()) {
                throw sendError(resp, 404, "No files found");
            }
            byte[] response = (String.join("\n", files) + "\n").getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(response.length);
            resp.getOutputStream().write(response);
        } else {
            throw sendError(resp, 404, "Directory not found");
        }
    }

}
