package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class ExecuteActionActionIT extends AbstractActionIT {

    @Test
    public void testExecuteAction() throws Exception {
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"content\":\"testinput\"}", 200, "action-helloworld.txt");
        checkResponse("/executeAction?actionName=notthere", "POST", "{\"content\":\"testinput\"}", 400, "action-notthere.txt");
        checkResponse("/executeAction?actionName=fail", "POST", "{\"content\":\"testinput\"}", 500, "action-fail.txt");
    }
}
