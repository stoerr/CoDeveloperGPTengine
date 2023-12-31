package net.stoerr.chatgpt.codevengine;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ReplaceActionIT extends AbstractActionIT {

    @Test
    public void testReplaceOperation() throws Exception {
        TbUtils.logInfo("\nReplaceActionIT.testReplaceOperation");
        try {
            String content = new String(Files.readAllBytes(Paths.get("src/test/resources/testdir/firstfile.txt")), UTF_8);
            Files.write(Paths.get("src/test/resources/testdir/replace3.txt"), content.getBytes( UTF_8));
            String response = checkResponse("/replaceInFile?path=replace3.txt", "POST",
                    "{\"replacements\":[{\"search\":\"test\",\"replace\":\"dingding\"}]}"
                    , 200, null);
            collector.checkThat(response, is("1 replacement; modified line(s) 2"));
            response = checkResponse("/readFile?path=replace3.txt", "GET", null, 200, null);
            collector.checkThat(response, is("Hello!\nJust a dingding.\n"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace3.txt"));
        }
    }

    @Test
    public void testComplainAboutMultiplesSinceNoMatch() throws Exception {
        TbUtils.logInfo("\nReplaceActionIT.testComplainAboutMultiplesSinceNoMatch");
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"replacements\":[{\"search\":\"neverthereinthefile\",\"replace\":\"goose\"}]}"
                , 400, null);
        collector.checkThat(response, containsString("not found"));
    }

    @Test
    public void testComplainAboutMultiplesSinceManyMatches() throws Exception {
        TbUtils.logInfo("\nReplaceActionIT.testComplainAboutMultiplesSinceManyMatches");
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"replacements\":[{\"search\":\"duck\",\"replace\":\"goose\"}]}"
                , 400, null);
        collector.checkThat(response, containsString(", but expected exactly one."));
    }

    @Test
    public void testReplaceOperationFileNotFound() throws Exception {
        TbUtils.logInfo("\nReplaceActionIT.testReplaceOperationFileNotFound");
        checkResponse("/replaceInFile?path=fileinsubdir", "POST",
                "{\"replacements\":[{\"search\":\"duck\",\"replace\":\"goose\"}]}", 404, "notfound.txt");
    }

    @Test
    public void testMultipleReplacementsInOneRequest() throws Exception {
        TbUtils.logInfo("\nReplaceActionIT.testMultipleReplacementsInOneRequest");
        try {
            String content = new String(Files.readAllBytes(Paths.get("src/test/resources/testdir/firstfile.txt")), UTF_8);
            Files.write(Paths.get("src/test/resources/testdir/replace4.txt"), content.getBytes( UTF_8));
            String response = checkResponse("/replaceInFile?path=replace4.txt", "POST",
                    "{\"replacements\":[{\"search\":\"test\",\"replace\":\"dingding\"}, {\"search\":\"Hello\",\"replace\":\"Hi\"}]}",
                    200, null);
            collector.checkThat(response, is("2 replacement; modified line(s)  1 - 2"));
            response = checkResponse("/readFile?path=replace4.txt", "GET", null, 200, null);
            collector.checkThat(response, is("Hi!\nJust a dingding.\n"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace4.txt"));
        }
    }

}
