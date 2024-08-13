package net.stoerr.chatgpt.codevengine;

import static org.hamcrest.CoreMatchers.is;

import org.junit.Test;

public class ListFilesActionIT extends AbstractActionIT {

    @Test
    public void testListFilesOperationRoot() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testListFilesOperationRoot");
        checkResponse("/listFiles?path=.", "GET", null, 200, "listFiles.txt");
    }

    @Test
    public void testListFilesOperationSubdir() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testListFilesOperationSubdir");
        checkResponse("/listFiles?path=subdir", "GET", null, 200, "listFilesSubdir.txt");
    }

    @Test
    public void testListFilesOperationFilePathRegex() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testListFilesOperationFilePathRegex");
        checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt", "GET", null, 200, "listFilesFilePathRegex.txt");
    }

    @Test
    public void testListFilesOperationGrepRegex() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testListFilesOperationGrepRegex");
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "GET", null, 200, "listFilesGrepRegex.txt");
    }

    @Test
    public void testListFilesOperationBothRegex() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testListFilesOperationBothRegex");
        checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt&grepRegex=testcontent", "GET", null, 200, "listFilesBothRegex.txt");
    }

    @Test
    public void testBrokenFileRegex() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testBrokenFileRegex");
        String response = checkResponse("/listFiles?path=.&filePathRegex=.*%5C.txt(", "GET", null, 400, null);
        collector.checkThat(response, is("Invalid filePathRegex: java.util.regex.PatternSyntaxException: Unclosed group near index 8\n" +
                ".*\\.txt("));
    }

    @Test
    public void testBrokenGrepRegex() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testBrokenGrepRegex");
        String response = checkResponse("/listFiles?path=.&grepRegex=.*%5C.txt(", "GET", null, 400, null);
        collector.checkThat(response, is("Invalid grepRegex: java.util.regex.PatternSyntaxException: Unclosed group near index 8\n" +
                ".*\\.txt("));
    }

    @Test
    public void testInvalidPath() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testInvalidPath");
        String response = checkResponse("/listFiles?path=notexisting", "GET", null, 404, null);
        collector.checkThat(response, is("Path notexisting does not exist! Try to list files with /listFiles to find the right path.\n" +
                "\n" +
                "Did you mean one of these files?\n" +
                "filewritten.txt\n" +
                "secondfile.md\n" +
                "subdir/fileinsubdir.md\n" +
                "firstfile.txt"));
    }

    @Test
    public void testFileRegexNotFound() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testFileRegexNotFound");
        String response = checkResponse("/listFiles?path=.&filePathRegex=secondxfile", "GET", null, 404, null);
        collector.checkThat(response, is("No files found matching filePathRegex: secondxfile\n" +
                "\n" +
                "Did you mean one of these files?\n" +
                "secondfile.md\n" +
                "firstfile.txt\n" +
                "subdir/fileinsubdir.md\n" +
                "filewritten.txt"));
    }

    @Test
    public void testGrepRegexNotFound() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testGrepRegexNotFound");
        String response = checkResponse("/listFiles?path=.&grepRegex=notexisting", "GET", null, 404, null);
        collector.checkThat(response, is("Found 4 files but none of them contain a line matching the grepRegex.\n" +
                "Did you really want to search for files containing 'notexisting' or for files named like that pattern? If so you have to repeat the search with filePathRegex set instead of grepRegex."));
    }

    @Test
    public void testDirectories() throws Exception {
        TbUtils.logInfo("\nListFilesActionIT.testDirectories");
        String directoriesResponse = checkResponse("/listFiles?path=.&listDirectories=true", "GET", null, 200, null);
        collector.checkThat(directoriesResponse, is("./\nsubdir/\n"));
    }

}
