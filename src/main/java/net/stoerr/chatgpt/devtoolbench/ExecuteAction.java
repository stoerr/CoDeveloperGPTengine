package net.stoerr.chatgpt.devtoolbench;

import static net.stoerr.chatgpt.devtoolbench.TbUtils.logInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ExecuteAction extends AbstractPluginAction {

    /**
     * The maximum number of ChatGPT tokes we output if there is no maxLines parameter given
     */
    private static final int MAXTOKENS_NOLIMIT = 2000;

    protected final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * Tokenizer used for GPT-3.5 and GPT-4.
     */
    protected final Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);

    @Override
    public String getUrl() {
        return "/executeAction";
    }

    @Override
    public String openApiDescription() {
        return """
                  /executeAction:
                    post:
                      operationId: executeAction
                      summary: Execute an action with given content as standard input. Only on explicit user request.
                      parameters:
                        - name: actionName
                          in: query
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
                                actionInput:
                                  type: string
                      responses:
                        '200':
                          description: Action executed successfully, output returned
                          content:
                            text/plain:
                              schema:
                                type: string
                """.stripIndent();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        Process process = null;
        try {
            String content = StringUtils.defaultString(getBodyParameter(resp, json, "actionInput", false));
            String actionName = getMandatoryQueryParam(req, resp, "actionName");
            RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, content, actionName);
            Path path = DevToolBench.currentDir.resolve(".cgptdevbench/" + actionName + ".sh");

            if (!Files.exists(path)) {
                throw sendError(resp, 400, "Action " + actionName + " not found");
            }

            ProcessBuilder pb = new ProcessBuilder("/bin/sh", path.toString());
            pb.redirectErrorStream(true);
            logInfo("Starting process: " + pb.command() + " with content: " + abbreviate(content, 40));
            process = pb.start();

            OutputStream out = process.getOutputStream();
            DevToolBench.execute(() -> {
                try {
                    try {
                        out.write(content.getBytes(StandardCharsets.UTF_8));
                    } finally {
                        out.close();
                    }
                } catch (IOException e) {
                    logInfo("Error writing to process: " + e);
                }
            });

            String output;
            try (InputStream inputStream = process.getInputStream()) {
                output = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                if (!process.waitFor(1, TimeUnit.MINUTES)) {
                    throw sendError(resp, 500, "Process did not finish within one minute");
                }
            }
            int exitCode = process.exitValue();
            logInfo("Process finished with exit code " + exitCode + ": " + abbreviate(output, 200));
            output = output.replaceAll(Pattern.quote(DevToolBench.currentDir + "/"), "");
            output = limitOutput(output, 2000);

            if (exitCode == 0) {
                resp.setStatus(HttpServletResponse.SC_OK);
                resp.setContentType("text/plain;charset=UTF-8");
                resp.getWriter().write(output);
            } else {
                String response = "Execution failed with exit code " + exitCode + ": " + output;
                throw sendError(resp, 500, response);
            }
        } catch (IOException e) {
            throw sendError(resp, 500, "Error executing action: " + e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw sendError(resp, 500, "Error executing action: " + e);
        } finally {
            if (process != null) {
                process.destroy();
            }
        }

    }

    protected final String MIDDLE_MARKER = "\n\n[... middle removed because of length restrictions ...]\n\n";

    /**
     * Turns output to tokens and replaces the middle by  if that's more than maxTokens.
     */
    protected String limitOutput(String output, int maxTokens) {
        if (output.length() < maxTokens) { // tokens are longer than one char, no need to decode.
            return output;
        }
        List<Integer> tokens = enc.encode(output);
        if (tokens.size() < maxTokens) {
            return output;
        }
        int startLimit = maxTokens / 2 - 10;
        int endLimit = tokens.size() - maxTokens / 2 + 10;
        return enc.decode(tokens.subList(0, startLimit)) + MIDDLE_MARKER + enc.decode(tokens.subList(endLimit, tokens.size()));
    }

    public boolean hasActions() {
        // check whether there are any files that would be returned for DevToolBench.currentDir.resolve(".cgptdevbench/" + actionName + ".sh")
        // check whether there are actually *.sh files there
        Path dir = DevToolBench.currentDir.resolve(".cgptdevbench");
        if (!Files.exists(dir)) {
            return false;
        }
        try (Stream<Path> list = Files.list(dir)) {
            return list.anyMatch(p -> p.toString().endsWith(".sh"));
        } catch (IOException e) {
            logInfo("Error checking for actions: " + e);
            return false;
        }
    }
}
