package net.stoerr.chatgpt.devtoolbench;

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
            String content = Files.readString(Paths.get("src/test/resources/testdir/firstfile.txt"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace3.txt"), content, UTF_8);
            String response = checkResponse("/replaceInFile?path=replace3.txt", "POST",
                    "{\"replacements\":[{\"search\":\"test\",\"replace\":\"dingding\"}]}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 1 occurrences of pattern; modified lines 2"));
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
        checkResponse("/replaceInFile?path=notfound.txt", "POST",
                "{\"replacements\":[{\"search\":\"duck\",\"replace\":\"goose\"}]}", 404, "notfound.txt");
    }

}
