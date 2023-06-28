package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class ListFilesActionIT extends AbstractActionIT {

    @Test
    public void testListFilesOperationRoot() throws Exception {
        checkResponse("/listFiles?path=.", "GET", null, 200, "listFiles.txt");
    }

    @Test
    public void testListFilesOperationSubdir() throws Exception {
        checkResponse("/listFiles?path=subdir", "GET", null, 200, "listFilesSubdir.txt");
    }

    @Test
    public void testListFilesOperationFilePathRegex() throws Exception {
        checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt", "GET", null, 200, "listFilesFilePathRegex.txt");
    }

    @Test
    public void testListFilesOperationGrepRegex() throws Exception {
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "GET", null, 200, "listFilesGrepRegex.txt");
    }

    @Test
    public void testListFilesOperationBothRegex() throws Exception {
        checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt&grepRegex=testcontent", "GET", null, 200, "listFilesBothRegex.txt");
    }
}
