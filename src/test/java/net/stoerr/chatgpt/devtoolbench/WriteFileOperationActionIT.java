package net.stoerr.chatgpt.devtoolbench;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class WriteFileOperationActionIT extends AbstractActionIT {

    @Test
    public void testWriteFileOperation() throws Exception {
        checkResponse("/writeFile?path=filewritten.txt", "POST", "{\"content\":\"testcontent line one\\nline two \\\\\\n with quoted backslashing \\n\"}", 204, null);
        String expected = readFile("/test-expected/filewritten.txt");
        String actual = readFile("/testdir/filewritten.txt");
        collector.checkThat(actual, CoreMatchers.is(expected));
    }

    @Test
    public void testWriteLargeFile() throws Exception {
        try {
            StringBuilder testinput = new StringBuilder();
            for (int i = 0; i < 20000; ++i) {
                testinput.append("test input");
            }
            testinput.append("\n");
            checkResponse("/writeFile?path=largefile.txt", "POST", "{\"content\":\"" + testinput + "\"}", 204, null);
            String actual = readFile("/testdir/largefile.txt");
            collector.checkThat(actual.replace(testinput, "(TESTINPUT)"), CoreMatchers.is("(TESTINPUT)"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/largefile.txt"));
        }
    }
}