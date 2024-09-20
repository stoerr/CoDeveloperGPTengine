package net.stoerr.chatgpt.codevengine;

import static net.stoerr.chatgpt.codevengine.TbUtils.logInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Executes an OpenAI tool call coming in as JSON - for usage outside ChatGPT.
 */
public class ExecuteOpenAIToolCallAction extends AbstractPluginAction {

    private final Map<String, AbstractPluginAction> handlers;

    public ExecuteOpenAIToolCallAction(Map<String, AbstractPluginAction> handlers) {
        this.handlers = handlers;
    }

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
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        BufferedReader reader = req.getReader();
        String json = reader.lines().collect(Collectors.joining(System.lineSeparator()));
        logInfo("Received tool call:\n" + json);
        String name = getBodyParameter(resp, json, "name", true);
        String arguments = getBodyParameter(resp, json, "arguments", true);
        Map<String, Object> parsedArguments = gson.fromJson(arguments, Map.class);
        logInfo("Executing tool call: " + name + " " + parsedArguments);
        Object requestBody = parsedArguments.get("requestBody");
        String body = requestBody != null ? gson.toJson(requestBody) : null;
        if (StringUtils.isNotBlank(body)) logInfo("Body: " + body);
        AbstractPluginAction handler = handlers.get("/" + name);
        if (null == handler) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "No handler for tool call: " + name);
        }
        // call handler with a request that has parsedArguments as parameters and body as request body (JSON request)
        HttpServletRequest requestWrapper = new HttpServletRequestWrapper(req) {
            @Override
            public String getParameter(String name) {
                Object value = parsedArguments.get(name);
                return value != null ? value.toString() : null;
            }

            @Override
            public BufferedReader getReader() throws IOException {
                return body != null ? new BufferedReader(new StringReader(body)) : null;
            }

            @Override
            public String getMethod() {
                return requestBody != null ? "POST" : "GET";
            }
        };
        handler.service(requestWrapper, resp);
    }

}
