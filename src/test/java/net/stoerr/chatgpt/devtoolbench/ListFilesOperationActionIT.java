package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class ListFilesOperationActionIT extends AbstractActionIT {

    @Test
    public void testListFilesOperation() throws Exception {
        checkResponse("/listFiles?path=.", "GET", null, 200, "listFiles.txt");
        checkResponse("/listFiles?path=subdir", "GET", null, 200, "listFilesSubdir.txt");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt", "GET", null, 200, "listFilesFilenameRegex.txt");
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "GET", null, 200, "listFilesGrepRegex.txt");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt&grepRegex=testcontent",
                "GET", null, 200, "listFilesBothRegex.txt");
    }
}
