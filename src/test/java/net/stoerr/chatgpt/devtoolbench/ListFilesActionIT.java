package net.stoerr.chatgpt.devtoolbench;

import org.junit.Test;

public class ListFilesActionIT extends AbstractActionIT {

    @Test
    public void testListFilesOperationRoot() throws Exception {
        TbUtils.log("\nListFilesActionIT.testListFilesOperationRoot");
        checkResponse("/listFiles?path=.", "GET", null, 200, "listFiles.txt");
    }

    @Test
    public void testListFilesOperationSubdir() throws Exception {
        TbUtils.log("\nListFilesActionIT.testListFilesOperationSubdir");
        checkResponse("/listFiles?path=subdir", "GET", null, 200, "listFilesSubdir.txt");
    }

    @Test
    public void testListFilesOperationFilePathRegex() throws Exception {
        TbUtils.log("\nListFilesActionIT.testListFilesOperationFilePathRegex");
        checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt", "GET", null, 200, "listFilesFilePathRegex.txt");
    }

    @Test
    public void testListFilesOperationGrepRegex() throws Exception {
        TbUtils.log("\nListFilesActionIT.testListFilesOperationGrepRegex");
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "GET", null, 200, "listFilesGrepRegex.txt");
    }

    @Test
    public void testListFilesOperationBothRegex() throws Exception {
        TbUtils.log("\nListFilesActionIT.testListFilesOperationBothRegex");
        checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt&grepRegex=testcontent", "GET", null, 200, "listFilesBothRegex.txt");
    }
}
