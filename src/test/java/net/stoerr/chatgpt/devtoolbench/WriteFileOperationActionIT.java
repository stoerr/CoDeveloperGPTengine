package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;
import org.hamcrest.CoreMatchers;

public class WriteFileOperationActionIT extends AbstractActionIT {

    @Test
    public void testWriteFileOperation() throws Exception {
        checkResponse("/writeFile?path=filewritten.txt", "POST", "{\"content\":\"testcontent line one\\nline two \\\\\\n with quoted backslashing \\n\"}", 204, "writeFile.txt");
        String expected = readFile("/test-expected/filewritten.txt");
        String actual = readFile("/testdir/filewritten.txt");
        collector.checkThat(actual, CoreMatchers.is(expected));
    }
}
