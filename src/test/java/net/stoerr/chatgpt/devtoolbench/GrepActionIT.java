package net.stoerr.chatgpt.devtoolbench;

import static org.hamcrest.CoreMatchers.is;

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

    @Test
    public void testWrongFilename() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testWrongFilename");
        String response = checkResponse("/grepFiles?path=secondddfile.md&grepRegex=Hello", "GET", null, 404, null);
        collector.checkThat(response, is("Path secondddfile.md does not exist! Try to list files with /listFiles to find the right path.\n\n" +
                "Did you mean one of these files?\n" +
                "secondfile.md\n" +
                "firstfile.txt\n" +
                "subdir/fileinsubdir.md\n" +
                "filewritten.txt\n\n" +
                "(suggestion list truncated - there are more files)."));
    }
}
