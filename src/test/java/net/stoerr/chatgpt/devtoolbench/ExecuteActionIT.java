package net.stoerr.chatgpt.devtoolbench;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

public class ExecuteActionIT extends AbstractActionIT {

    @Test
    public void testExecuteActionHelloWorld() throws Exception {
        TbUtils.logInfo("\nExecuteActionIT.testExecuteActionHelloWorld");
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"actionInput\":\"testinput\"}", 200, "action-helloworld.txt");
    }

    @Test
    public void testHelloWorldWithLargeFile() throws Exception {
        TbUtils.logInfo("\nExecuteActionIT.testHelloWorldWithLargeFile");
        String prefix = "Hello World! Your input was: ";
        StringBuilder testinput = new StringBuilder();
        testinput.append("test input".repeat(500));
        System.out.println(testinput.length());
        String actualResponse = checkResponse("/executeAction?actionName=helloworld", "POST", "{\"actionInput\":\"" + testinput + "\"}", 200, null);
        collector.checkThat(actualResponse.length(), is(prefix.length() + testinput.length() + 1)); // newliner
        collector.checkThat(actualResponse.replace(testinput, "(THETESTINPUT)"), is(prefix + "(THETESTINPUT)\n"));
    }

    @Test
    public void testHelloWorldWithLargeFileAndCut() throws Exception {
        TbUtils.logInfo("\nExecuteActionIT.testHelloWorldWithLargeFileAndCut");
        String prefix = "Hello World! Your input was: ";
        StringBuilder testinput = new StringBuilder();
        testinput.append("test input".repeat(3000));
        System.out.println(testinput.length());
        String actualResponse = checkResponse("/executeAction?actionName=helloworld", "POST", "{\"actionInput\":\"" + testinput + "\"}", 200, null);
        // check that it contains many "test input" and has the limiter and has < 2000 tokens.
        collector.checkThat(actualResponse.contains("test input".repeat(300)), is(true));
        collector.checkThat(actualResponse.replace(testinput, "(THETESTINPUT)"),
                containsString("middle removed because of length restrictions"));
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);
        int tokenCount = enc.countTokens(actualResponse);
        collector.checkThat(tokenCount < 2000, CoreMatchers.is(true));
        collector.checkThat(tokenCount > 1500, CoreMatchers.is(true));
        // System.out.println(actualResponse);
    }

    @Test
    public void testExecuteActionNotThere() throws Exception {
        TbUtils.logInfo("\nExecuteActionIT.testExecuteActionNotThere");
        checkResponse("/executeAction?actionName=notthere", "POST", "{\"actionInput\":\"testinput\"}", 400, "action-notthere.txt");
    }

    @Test
    public void testExecuteActionFail() throws Exception {
        TbUtils.logInfo("\nExecuteActionIT.testExecuteActionFail");
        checkResponse("/executeAction?actionName=fail", "POST", "{\"actionInput\":\"testinput\"}", 500, "action-fail.txt");
    }

    @Test
    public void testExecuteActionJsonError() throws Exception {
        TbUtils.logInfo("\nExecuteActionIT.testExecuteActionFail");
        checkResponse("/executeAction?actionName=fail", "POST", "{{", 400, "action-gsonerror.txt");
    }

}
