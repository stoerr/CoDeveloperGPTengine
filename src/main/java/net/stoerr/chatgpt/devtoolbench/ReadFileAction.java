package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// curl -is http://localhost:3001/readFile?path=somefile.txt
public class ReadFileAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/readFile";
    }

    @Override
    public String openApiDescription() {
        return """
                  /readFile:
                    get:
                      operationId: readFile
                      summary: Read a files content.
                      parameters:
                        - name: path
                          in: query
                          description: relative path to file
                          required: true
                          schema:
                            type: string
                        - name: maxLines
                          in: query
                          description: maximum number of lines to read from the file
                          required: false
                          schema:
                            type: integer
                        - name: startLine
                          in: query
                          description: line number to start reading from
                          required: false
                          schema:
                            type: integer
                      responses:
                        '200':
                          description: Content of the file
                          content:
                            text/plain:
                              schema:
                                type: string
                """.stripIndent();
    }


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path path = getPath(req, resp, true);
        int maxLines = req.getParameter("maxLines") != null ? Integer.parseInt(req.getParameter("maxLines")) : Integer.MAX_VALUE;
        int startLine = req.getParameter("startLine") != null ? Integer.parseInt(req.getParameter("startLine")) : 1;
        RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, path);
        if (maxLines != Integer.MAX_VALUE || startLine != 1) {
            resp.getWriter().write("Reading from line " + startLine + " to line " + (startLine + maxLines - 1) + "\n");
        }
        if (Files.exists(path)) {
            List<String> lines = Files.lines(path)
                    .skip(startLine - 1)
                    .limit(maxLines)
                    .collect(Collectors.toList());
            String content = String.join("\n", lines);
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(bytes.length);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(bytes);
        }
    }
}
