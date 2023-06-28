package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class GrepActionIT extends AbstractActionIT {

    @Test
    public void testGrepOperationFirstFile() throws Exception {
        checkResponse("/grepFiles?path=firstfile.txt&grepRegex=Hello", "GET", null, 200, "grepFirst.txt");
    }

    @Test
    public void testGrepOperationContext() throws Exception {
        checkResponse("/grepFiles?path=.&grepRegex=duck&fileRegex=md&contextLines=1", "GET", null, 200, "grepContext.txt");
    }
}