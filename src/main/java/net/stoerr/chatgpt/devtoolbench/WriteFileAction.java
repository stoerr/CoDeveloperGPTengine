package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.TbUtils.addShortContentReport;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * an operation that writes the message into the file at path.
 */
// curl -is http://localhost:3001/writeFile?path=testfile -d '{"content":"testcontent line one\nline two\n"}'
public class WriteFileAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/writeFile";
    }

    @Override
    public String openApiDescription() {
        return """
                  /writeFile:
                    post:
                      operationId: writeFile
                      summary: Overwrite a small file with the complete content given in one step. You cannot append to a file or write parts or write parts - use the replaceAction for inserting parts.
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
                                content:
                                  type: string
                      responses:
                        '200':
                          description: File overwritten
                        '400':
                          description: Invalid parameter
                """.stripIndent();
    }
    // We remove the append parameter for now, because it is not transactional and the file might be half overwritten
    // when ChatGPT aborts because of length constraints or other issues.
    //                         - name: append
    //                          in: query
    //                          description: If true, append to the very end of the file instead of overwriting. If false, you need to give the whole content at once.
    //                          required: false
    //                          schema:
    //                            type: boolean

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedReader reader = req.getReader();
        String body = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        handleBody(req, resp, body.getBytes());
    }

    private void handleBody(HttpServletRequest req, HttpServletResponse resp, byte[] bytes) throws IOException {
        String json = new String(bytes, StandardCharsets.UTF_8);
        try {
            String appendParam = getQueryParam(req, "append");
            boolean append = appendParam != null && appendParam.toLowerCase().contains("true");
            String content = getBodyParameter(resp, json, "content", true);
            Path path = getPath(req, resp);
            if (!Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (append && !Files.exists(path)) {
                throw sendError(resp, 400, "File " + path + " does not exist, cannot append to it.");
            }
            StandardOpenOption appendOption = append ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING;
            Files.write(path, content.getBytes(), java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.WRITE, appendOption);
            resp.setStatus(HttpServletResponse.SC_OK);
            StringBuilder output = new StringBuilder();
            output.append("File completely overwritten with: ");
            output.append(addShortContentReport(content, output));
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(output.toString());
        } catch (IOException e) {
            throw sendError(resp, 500, "Error writing file: " + e);
        }
    }

}
