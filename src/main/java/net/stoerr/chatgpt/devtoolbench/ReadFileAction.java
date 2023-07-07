package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, path);
        if (Files.exists(path)) {
            byte[] bytes = Files.readAllBytes(path);
            resp.setContentLength(bytes.length);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(bytes);
        }
    }
}
