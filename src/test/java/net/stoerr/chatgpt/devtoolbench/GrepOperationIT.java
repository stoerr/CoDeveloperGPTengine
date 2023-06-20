package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class GrepOperationIT extends AbstractIT {

    @Test
    public void testGrepOperation() throws Exception {
        checkResponse("/grepFiles?path=firstfile.txt&grepRegex=Hello", "GET", null, 200, "grepFirst.txt");
        checkResponse("/grepFiles?path=.&grepRegex=duck&fileRegex=md&contextLines=1", "GET", null, 200, "grepContext.txt");
    }
}