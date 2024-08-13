package net.stoerr.chatgpt.codevengine;

import static net.stoerr.chatgpt.codevengine.CoDeveloperEngine.LOCAL_CONFIG_DIR;
import static net.stoerr.chatgpt.codevengine.CoDeveloperEngine.currentDir;
import static net.stoerr.chatgpt.codevengine.TbUtils.logInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.io.IOUtils;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ExecuteAction extends AbstractPluginAction {

    /**
     * The maximum number of ChatGPT tokes we output if there is no maxLines parameter given
     */
    private static final int MAXTOKENS_NOLIMIT = 2000;

    protected final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();

    /**
     * Tokenizer used for GPT-4o*
     */
    protected final Encoding enc = registry.getEncoding(EncodingType.O200K_BASE); // GPT-4o*

    @Override
    public String getUrl() {
        return "/executeAction";
    }

    @Override
    public String openApiDescription() {
        return "  /executeAction:\n" +
                "    post:\n" +
                "      operationId: executeAction\n" +
                "      x-openai-isConsequential: false\n" +
                "      summary: Execute an action with some arguments and the given actionInput content as standard input. Only on explicit user request.\n" +
                "      parameters:\n" +
                "        - name: actionName\n" +
                "          in: query\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: arguments\n" +
                "          in: query\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: string\n" +
                "      requestBody:\n" +
                "        required: true\n" +
                "        content:\n" +
                "          application/json:\n" +
                "            schema:\n" +
                "              type: object\n" +
                "              properties:\n" +
                "                actionInput:\n" +
                "                  type: string\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: Action executed successfully, output returned\n" +
                "          content:\n" +
                "            text/plain:\n" +
                "              schema:\n" +
                "                type: string\n";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        Process process = null;
        try {
            String content = StringUtils.defaultString(getBodyParameter(resp, json, "actionInput", false));
            String actionName = getMandatoryQueryParam(req, resp, "actionName");
            String args = getQueryParam(req, "arguments");
            RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, content, actionName, args);
            Path path = currentDir.resolve(LOCAL_CONFIG_DIR).resolve(actionName + ".sh");

            if (!Files.exists(path)) {
                throw sendError(resp, 400, "Action " + actionName + " not found");
            }

            List<String> cmdline = new ArrayList<>();
            cmdline.add("/bin/sh");
            cmdline.add(path.toString());
            if (args != null && !args.trim().isEmpty()) {
                cmdline.addAll(Arrays.asList(args.trim().split("\\s+")));
            }
            ProcessBuilder pb = new ProcessBuilder(cmdline);
            pb.redirectErrorStream(true);
            logInfo("Starting process: " + pb.command() + " with content: " + abbreviate(content, 40));
            process = pb.start();

            OutputStream out = process.getOutputStream();
            CoDeveloperEngine.execute(() -> {
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
                output = new String(IOUtils.toByteArray(inputStream), StandardCharsets.UTF_8);
                if (!process.waitFor(1, TimeUnit.MINUTES)) {
                    throw sendError(resp, 500, "Process did not finish within one minute");
                }
            }
            int exitCode = process.exitValue();
            logInfo("Process finished with exit code " + exitCode + ": " + abbreviate(output, 200));
            output = output.replaceAll(Pattern.quote(currentDir + "/"), "");
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
        IntArrayList tokens = enc.encode(output);
        if (tokens.size() < maxTokens) {
            return output;
        }
        int startLimit = maxTokens / 2 - 10;
        int endLimit = tokens.size() - maxTokens / 2 + 10;
        return enc.decode(intArrayListFromList(tokens.boxed().subList(0, startLimit))) +
                MIDDLE_MARKER +
                enc.decode(intArrayListFromList(tokens.boxed().subList(endLimit, tokens.size())));
    }

    protected IntArrayList intArrayListFromList(List<Integer> tokens) {
        IntArrayList result = new IntArrayList();
        tokens.forEach(result::add);
        return result;
    }

    public boolean hasActions() {
        // check whether there are any files that would be returned for CoDeveloperEngine.currentDir.resolve(".cgptcodeveloper/" + actionName + ".sh")
        // check whether there are actually *.sh files there
        Path dir = currentDir.resolve(LOCAL_CONFIG_DIR);
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
