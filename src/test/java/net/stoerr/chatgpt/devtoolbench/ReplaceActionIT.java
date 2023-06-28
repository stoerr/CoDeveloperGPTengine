package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ReplaceActionIT extends AbstractActionIT {

    @Test
    public void testLiteralReplaceOperation() throws Exception {
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace.txt"), content, UTF_8);
            checkResponse("/replaceInFile?path=replace.txt", "POST",
                    "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\",\"multiple\":true}"
                    , 200, "replace-successfulmulti.txt");
            checkResponse("/readFile?path=replace.txt", "GET", null, 200, "replace-successfulmulti-replaced.txt");
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace.txt"));
        }
    }

    @Test
    public void testComplainAboutMultiplesSinceNoMatch() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"neverthereinthefile\",\"literalReplacement\":\"goose\"}"
                , 400, "replace-multiplenotrequested1.txt");
    }

    @Test
    public void testComplainAboutMultiplesSinceManyMatches() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}"
                , 400, "replace-multiplenotrequested2.txt");
    }

    @Test
    public void testLiteralReplaceOperationFileNotFound() throws Exception {
        checkResponse("/replaceInFile?path=notfound.txt", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}", 404, "notfound.txt");
    }


    @Test
    public void testBothReplacementsGiven() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                , 400, "replace-both-replacements-given.txt");
    }

    @Test
    public void testNoReplacementsGiven() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"multiple\":true}"
                , 400, "replace-no-replacements-given.txt");
    }

    @Test
    public void testReplacementWithGroupReferencesNoGroup() throws Exception {
        checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                , 400, "replace-replacement-with-group-references-no-group.txt");
    }

    @Test
    public void testReplacementWithGroupReferencesSuccessful() throws Exception {
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace.txt"), content, UTF_8);
            checkResponse("/replaceInFile?path=replace.txt", "POST",
                    "{\"pattern\":\"duck\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                    , 200, "replace-successfulmulti.txt");
            checkResponse("/readFile?path=replace.txt", "GET", null, 200, "replace-successfulmulti-replaced.txt");
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace.txt"));
        }
    }

}
