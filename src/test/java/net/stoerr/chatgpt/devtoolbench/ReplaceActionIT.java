package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ReplaceActionIT extends AbstractActionIT {

    @Test
    public void testReplaceOperation() throws Exception {
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace.txt"), content, UTF_8);
            checkResponse("/replaceInFile?path=replace.txt", "POST",
                    "{\"pattern\":\"duck\",\"replacement\":\"goose\",\"multiple\":\"true\"}"
                    , 200, "replace-successfulmulti.txt");
            checkResponse("/readFile?path=replace.txt", "GET", null, 200, "replace-successfulmulti-replaced.txt");
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace.txt"));
        }
    }

    @Test
    public void testComplainAboutMultiplesSinceNoMatch() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"neverthereinthefile\",\"replacement\":\"goose\"}"
                , 400, "replace-multiplenotrequested1.txt");
    }

    @Test
    public void testComplainAboutMultiplesSinceManyMatches() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"replacement\":\"goose\"}"
                , 400, "replace-multiplenotrequested2.txt");
    }

    @Test
    public void testReplaceOperationFileNotFound() throws Exception {
        checkResponse("/replaceInFile?path=notfound.txt", "POST",
                "{\"pattern\":\"duck\",\"replacement\":\"goose\"}", 404, "notfound.txt");
    }

}
