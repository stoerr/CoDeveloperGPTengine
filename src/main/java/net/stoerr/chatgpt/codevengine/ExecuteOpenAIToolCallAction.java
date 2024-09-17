package net.stoerr.chatgpt.codevengine;

import static net.stoerr.chatgpt.codevengine.TbUtils.logInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Executes an OpenAI tool call coming in as JSON - for usage outside ChatGPT.
 */
public class ExecuteOpenAIToolCallAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/executetool";
    }

    /**
     * This is not registered in the yaml description since it's not a normal action, but rather
     * distributes to actions.
     */
    @Override
    public String openApiDescription() {
        return "";
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        logInfo("Received tool call:\n" + json);
        String name = getBodyParameter(resp, json, "name", true);
        String arguments = getBodyParameter(resp, json, "arguments", true);
        Map<String, String> parsedArguments = gson.fromJson(arguments, Map.class);
        logInfo("Executing tool call: " + name + " " + parsedArguments);
        String body = gson.toJson(parsedArguments.get("requestBody"));
        logInfo("Body: " + body);

    }

}
