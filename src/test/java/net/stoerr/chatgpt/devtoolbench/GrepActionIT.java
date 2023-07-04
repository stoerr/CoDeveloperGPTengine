package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class GrepActionIT extends AbstractActionIT {

    @Test
    public void testGrepOperationFirstFile() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testGrepOperationFirstFile");
        checkResponse("/grepFiles?path=firstfile.txt&grepRegex=Hello", "GET", null, 200, "grepFirst.txt");
    }

    @Test
    public void testGrepOperationContext() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testGrepOperationContext");
        checkResponse("/grepFiles?path=.&grepRegex=duck&fileRegex=md&contextLines=1", "GET", null, 200, "grepContext.txt");
    }
}
