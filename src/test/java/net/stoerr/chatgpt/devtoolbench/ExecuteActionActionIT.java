package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class ExecuteActionActionIT extends AbstractActionIT {

    @Test
    public void testExecuteActionHelloWorld() throws Exception {
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"content\":\"testinput\"}", 200, "action-helloworld.txt");
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