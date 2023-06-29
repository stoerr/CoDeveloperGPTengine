package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class WriteFileActionIT extends AbstractActionIT {

    @Test
    public void testWriteFileOperation() throws Exception {
        TbUtils.log("\nWriteFileActionIT.testWriteFileOperation");
        checkResponse("/writeFile?path=filewritten.txt", "POST", "{\"content\":\"testcontent line one\\nline two \\\\\\n with quoted backslashing \\n\"}", 204, null);
        String expected = readFile("/test-expected/filewritten.txt");
        String actual = readFile("/testdir/filewritten.txt");
        collector.checkThat(actual, CoreMatchers.is(expected));
    }

    @Test
    public void testWriteLargeFile() throws Exception {
        TbUtils.log("\nWriteFileActionIT.testWriteLargeFile");
        try {
            StringBuilder testinput = new StringBuilder();
            testinput.append("test input".repeat(3000));
            testinput.append("\n");
            checkResponse("/writeFile?path=largefile.txt", "POST", "{\"content\":\"" + testinput + "\"}", 204, null);
            String actual = readFile("/testdir/largefile.txt");
            collector.checkThat(actual.replace(testinput, "(TESTINPUT)"), CoreMatchers.is("(TESTINPUT)"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/largefile.txt"));
        }
    }

    @Test
    public void testAppendToFile() throws Exception {
        TbUtils.log("\nWriteFileActionIT.testAppendToFile");
        String initialContent = "Initial content\n";
        String appendedContent = "Appended content\n";
        String expectedContent = initialContent + appendedContent;

        checkResponse("/writeFile?path=appendtest.txt", "POST", "{\"content\":\"" + initialContent + "\"}", 204, null);
        checkResponse("/writeFile?path=appendtest.txt&append=true", "POST", "{\"content\":\"" + appendedContent + "\"}", 204, null);

        String actualContent = readFile("/testdir/appendtest.txt");
        collector.checkThat(actualContent, CoreMatchers.is(expectedContent));

        Files.deleteIfExists(Paths.get("src/test/resources/testdir/appendtest.txt"));
    }
}
