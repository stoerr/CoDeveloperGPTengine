package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletResponse;

/**
 * It happens sometimes that ChatGPT goes into a loop, repeating the same request again and again, which destroys the current 3h quota. We block the 3rd try.
 */
public class RepeatedRequestChecker {

    public static final String ERRORMSG = "ERROR: you are repeating the same request again and again, and you will get the same result. Stop what you are doing and consult the user immediately!";

    public static final RepeatedRequestChecker CHECKER = new RepeatedRequestChecker();
    public static final int MAX_REPETITIONS = 3;

    private List<String> lastRequest = Collections.emptyList();
    private int repetitionCount = 0;

    public void checkRequestRepetition(HttpServletResponse response, HttpServlet servlet, Object... parameters) throws ExecutionAbortedException {
        List<String> key = new ArrayList<>();
        key.add(servlet.getClass().getSimpleName());
        for (Object parameter : parameters) {
            if (parameter instanceof String) {
                key.add((String) parameter);
            } else if (parameter instanceof Path) {
                key.add(parameter.toString());
            } else if (parameter instanceof Boolean) {
                key.add(parameter.toString());
            } else if (parameter == null) {
                key.add("<null parameter>");
            } else {
                throw new IllegalArgumentException("BUG: unknown parameter type " + parameter.getClass());
            }
        }
        TbUtils.logInfo("Repetition key: " + key);
        if (lastRequest.equals(key)) {
            repetitionCount++;
            if (repetitionCount >= MAX_REPETITIONS) {
                TbUtils.logError("REPEATED REQUEST: " + key);
                AbstractPluginAction.sendError(response, 400, ERRORMSG);
                throw new ExecutionAbortedException();
            }
        }
        lastRequest = key;
        repetitionCount = 0;
    }

}
