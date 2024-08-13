package net.stoerr.chatgpt.codevengine;

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
                "filewritten.txt"));
    }

    @Test
    public void testNotExistingGrepRegex() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testNotExistingGrepRegex");
        String response = checkResponse("/grepFiles?path=firstfile.txt&grepRegex=ThisIsNowhere", "GET", null, 404, null);
        collector.checkThat(response, is("Found 1 files whose name is matching the filePathRegex but none of them contain a line matching the grepRegex."));
    }

    @Test
    public void testFileRegexNotMatching() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testFileRegexNotMatching");
        String response = checkResponse("/grepFiles?path=.&grepRegex=Hello&fileRegex=secondxfile", "GET", null, 404, null);
        collector.checkThat(response, is("No files found matching filePathRegex: secondxfile\n" +
                "\n" +
                "Did you mean one of these files?\n" +
                "secondfile.md\n" +
                "firstfile.txt\n" +
                "subdir/fileinsubdir.md\n" +
                "filewritten.txt"));
    }

    @Test
    public void testBrokenGrepRegex() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testWrongGrepRegex");
        String response = checkResponse("/grepFiles?path=firstfile.txt&grepRegex=Hello(", "GET", null, 400, null);
        collector.checkThat(response, is("Invalid grepRegex parameter: Hello(\n" +
                "Unclosed group near index 6\n" +
                "Hello("));
    }

    @Test
    public void testBrokenFilePathRegex() throws Exception {
        TbUtils.logInfo("\nGrepActionIT.testWrongFilePathRegex");
        String response = checkResponse("/grepFiles?path=firstfile.txt&grepRegex=Hello&fileRegex=Hello(", "GET", null, 400, null);
        collector.checkThat(response, is("Invalid fileRegex parameter: Hello(\n" +
                "Unclosed group near index 6\n" +
                "Hello("));
    }
}
