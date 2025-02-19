package net.stoerr.chatgpt.codevengine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// curl -is http://localhost:3001/readFile?path=somefile.txt
public class ReadFileAction extends AbstractPluginAction {

    /**
     * The maximum number of ChatGPT tokes we output if there is no maxLines parameter given
     */
    private static final int MAXTOKENS_NOLIMIT = 10000;

    protected final transient EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * Tokenizer used for GPT-3.5 and GPT-4.
     */
    protected final transient Encoding enc = registry.getEncoding(EncodingType.O200K_BASE); // GPT-4o*

    @Override
    public String getUrl() {
        return "/readFile";
    }

    @Override
    public String openApiDescription() {
        return "  /readFile:\n" +
                "    get:\n" +
                "      operationId: readFile\n" +
                "      x-openai-isConsequential: false\n" +
                "      summary: Read a files content.\n" +
                "      parameters:\n" +
                "        - name: path\n" +
                "          in: query\n" +
                "          description: relative path to file\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: maxLines\n" +
                "          in: query\n" +
                "          description: maximum number of lines to read from the file, e.g. 500\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: integer\n" +
                "        - name: startLine\n" +
                "          in: query\n" +
                "          description: line number to start reading from; 1 is the first line\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: integer\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Content of the file\n" +
                "          content:\n" +
                "            text/plain:\n" +
                "              schema:\n" +
                "                type: string\n";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path path = getPath(req, resp, true, false);
        int maxLines = req.getParameter("maxLines") != null ? Integer.parseInt(req.getParameter("maxLines")) : Integer.MAX_VALUE;
        int startLine = req.getParameter("startLine") != null ? Integer.parseInt(req.getParameter("startLine")) : 1;
        RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, path);
        if (Files.exists(path)) {
            if (Files.isDirectory(path)) {
                throw sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Path is a directory - please use listFiles for listing directory contents: " + path);
            }
            List<String> lines;
            int fulllinecount = (int) Files.lines(path).count();
            if (startLine > fulllinecount) {
                throw sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "startLine is beyond the end of the file: " + startLine + " > " + fulllinecount);
            }
            try (Stream<String> linesStream = Files.lines(path)) {
                lines = linesStream
                        .skip(Math.max(startLine - 1L, 0L))
                        .limit(maxLines)
                        .collect(Collectors.toList());
            }
            String content = String.join("\n", lines) + "\n";
            int dropped = 0;
            if (maxLines == Integer.MAX_VALUE) {
                int numtokens = enc.encode(content).size();
                if (numtokens > MAXTOKENS_NOLIMIT) {
                    do {
                        // drop the 10% last lines of lines and try again
                        int numlinesToDrop = (int) (lines.size() * 0.1) + 1;
                        dropped += numlinesToDrop;
                        lines = lines.subList(0, lines.size() - numlinesToDrop);
                        content = String.join("\n", lines) + "\n";
                    } while (enc.encode(content).size() > MAXTOKENS_NOLIMIT);
                    log("Dropped " + dropped + " lines to reduce token count from " + numtokens + " to " + enc.encode(content).size() +
                            " . To get more of the file content repeat the read request with startLine=" + (startLine + lines.size()) +
                            " , or use the grepAction with enough contextLines if you are searching for something specific.");
                }
            }
            if (maxLines <= fulllinecount || startLine > 1 || dropped != 0) {
                content = "CAUTION: Lines " + startLine + " to " + (startLine + lines.size() - 1) + " of " + fulllinecount +
                        " lines of file " + CoDeveloperEngine.canonicalName(path) + " start now. " +
                        "To get more of the file content repeat read request with startLine=" + (startLine + lines.size()) +
                        " , or use the grepAction with enough contextLines if you are searching for something specific.\n\n"
                        + content + "\n\n" +
                        "CAUTION: file is truncated. To read more of the file see instructions at the beginning of the answer.";
            }
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(bytes.length);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(bytes);
        }
    }
}
