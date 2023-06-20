package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class ExecuteActionIT extends AbstractIT {

    @Test
    public void testExecuteAction() throws Exception {
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"content\":\"testinput\"}", 200, "helloworld.txt");
        checkResponse("/executeAction?actionName=notthere", "POST", "{\"content\":\"testinput\"}", 400, "notthere.txt");
        checkResponse("/executeAction?actionName=fail", "POST", "{\"content\":\"testinput\"}", 500, "fail.txt");
    }
}