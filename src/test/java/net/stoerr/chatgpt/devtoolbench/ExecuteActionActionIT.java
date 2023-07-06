package net.stoerr.chatgpt.devtoolbench;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ExecuteActionActionIT extends AbstractActionIT {

    @Test
    public void testExecuteActionHelloWorld() throws Exception {
        TbUtils.logInfo("\nExecuteActionActionIT.testExecuteActionHelloWorld");
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"actionInput\":\"testinput\"}", 200, "action-helloworld.txt");
    }

    @Test
    public void testHelloWorldWithLargeFile() throws Exception {
        TbUtils.logInfo("\nExecuteActionActionIT.testHelloWorldWithLargeFile");
        String prefix = "Hello World! Your input was: ";
        StringBuilder testinput = new StringBuilder();
        testinput.append("test input".repeat(3000));
        System.out.println(testinput.length());
        String actualResponse = checkResponse("/executeAction?actionName=helloworld", "POST", "{\"actionInput\":\"" + testinput + "\"}", 200, null);
        collector.checkThat(actualResponse.length(), CoreMatchers.is(prefix.length() + testinput.length() + 1)); // newliner
        collector.checkThat(actualResponse.replace(testinput, "(THETESTINPUT)"), CoreMatchers.is(prefix + "(THETESTINPUT)\n"));
    }

    @Test
    public void testExecuteActionNotThere() throws Exception {
        TbUtils.logInfo("\nExecuteActionActionIT.testExecuteActionNotThere");
        checkResponse("/executeAction?actionName=notthere", "POST", "{\"actionInput\":\"testinput\"}", 400, "action-notthere.txt");
    }

    @Test
    public void testExecuteActionFail() throws Exception {
        TbUtils.logInfo("\nExecuteActionActionIT.testExecuteActionFail");
        checkResponse("/executeAction?actionName=fail", "POST", "{\"actionInput\":\"testinput\"}", 500, "action-fail.txt");
    }

    @Test
    public void testExecuteActionJsonError() throws Exception {
        TbUtils.logInfo("\nExecuteActionActionIT.testExecuteActionFail");
        checkResponse("/executeAction?actionName=fail", "POST", "{{", 400, "action-gsonerror.txt");
    }

}
