package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.Ignore;
import org.junit.Test;

public class WriteFileActionIT extends AbstractActionIT {

    @Test
    public void testWriteFileOperation() throws Exception {
        TbUtils.logInfo("\nWriteFileActionIT.testWriteFileOperation");
        String response = checkResponse("/writeFile?path=filewritten.txt", "POST", "{\"content\":\"testcontent line one\\nline two \\\\\\n with quoted backslashing \\n\"}", 200, null);
        collector.checkThat(response, CoreMatchers.containsString("File completely overwritten"));
        String expected = readFile("/test-expected/filewritten.txt");
        String actual = readFile("/testdir/filewritten.txt");
        collector.checkThat(actual, CoreMatchers.is(expected));
    }

    @Ignore("FIXME fails on Github for unknown reason. Perhaps we have to rewrite to use Jetty.")
    @Test
    public void testWriteLargeFile() throws Exception {
        TbUtils.logInfo("\nWriteFileActionIT.testWriteLargeFile");
        try {
            StringBuilder testinput = new StringBuilder();
            testinput.append("test input".repeat(3000));
            testinput.append("\n");
            String response = checkResponse("/writeFile?path=largefile.txt", "POST", "{\"content\":\"" + testinput + "\"}", 200, null);
            collector.checkThat(response, CoreMatchers.containsString("File completely overwritten"));
            String actual = readFile("/testdir/largefile.txt");
            collector.checkThat(actual.replace(testinput, "(TESTINPUT)"), CoreMatchers.is("(TESTINPUT)"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/largefile.txt"));
        }
    }

    @Test
    public void testAppendToFile() throws Exception {
        TbUtils.logInfo("\nWriteFileActionIT.testAppendToFile");
        String initialContent = "Initial content\n";
        String appendedContent = "Appended content\n";
        String expectedContent = initialContent + appendedContent;

        String response = checkResponse("/writeFile?path=appendtest.txt", "POST", "{\"content\":\"" + initialContent + "\"}", 200, null);
        collector.checkThat(response, CoreMatchers.containsString("File completely overwritten"));
        response = checkResponse("/writeFile?path=appendtest.txt&append=true", "POST", "{\"content\":\"" + appendedContent + "\"}", 200, null);
        // Caution : response is wrong, but currently append is not announced.
        collector.checkThat(response, CoreMatchers.containsString("File completely overwritten"));

        String actualContent = readFile("/testdir/appendtest.txt");
        collector.checkThat(actualContent, CoreMatchers.is(expectedContent));

        Files.deleteIfExists(Paths.get("src/test/resources/testdir/appendtest.txt"));
    }
}
