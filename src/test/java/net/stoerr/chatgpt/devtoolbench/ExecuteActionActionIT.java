package net.stoerr.chatgpt.devtoolbench;

import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

public class ExecuteActionActionIT extends AbstractActionIT {

    @Test
    public void testExecuteActionHelloWorld() throws Exception {
        TbUtils.log("\nExecuteActionActionIT.testExecuteActionHelloWorld");
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"actionInput\":\"testinput\"}", 200, "action-helloworld.txt");
    }

    @Test
    @Ignore("FIXME currently fails on Github Actions. Possibly refactoring to use Jetty necessary.")
    public void testHelloWorldWithLargeFile() throws Exception {
        TbUtils.log("\nExecuteActionActionIT.testHelloWorldWithLargeFile");
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
        TbUtils.log("\nExecuteActionActionIT.testExecuteActionNotThere");
        checkResponse("/executeAction?actionName=notthere", "POST", "{\"actionInput\":\"testinput\"}", 400, "action-notthere.txt");
    }

    @Test
    public void testExecuteActionFail() throws Exception {
        TbUtils.log("\nExecuteActionActionIT.testExecuteActionFail");
        checkResponse("/executeAction?actionName=fail", "POST", "{\"actionInput\":\"testinput\"}", 500, "action-fail.txt");
    }
}
