package net.stoerr.chatgpt.devtoolbench;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class ExecuteActionActionIT extends AbstractActionIT {

    @Test
    public void testExecuteActionHelloWorld() throws Exception {
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"content\":\"testinput\"}", 200, "action-helloworld.txt");
    }

    @Test
    public void testHelloWorldWithLargeFile() throws Exception {
        String prefix = "Hello World! Your input was: ";
        StringBuilder testinput = new StringBuilder();
        for (int i = 0; i < 20000; ++i) {
            testinput.append("test input");
        }
        System.out.println(testinput.length());
        String actualResponse = checkResponse("/executeAction?actionName=helloworld", "POST", "{\"content\":\"" + testinput.toString() + "\"}", 200, null);
        collector.checkThat(actualResponse.length(), CoreMatchers.is(prefix.length() + testinput.length() + 1)); // newliner
        collector.checkThat(actualResponse.replace(testinput, "(THETESTINPUT)"), CoreMatchers.is(prefix + "(THETESTINPUT)\n"));
    }

    @Test
    public void testExecuteActionNotThere() throws Exception {
        checkResponse("/executeAction?actionName=notthere", "POST", "{\"content\":\"testinput\"}", 400, "action-notthere.txt");
    }

    @Test
    public void testExecuteActionFail() throws Exception {
        checkResponse("/executeAction?actionName=fail", "POST", "{\"content\":\"testinput\"}", 500, "action-fail.txt");
    }
}
