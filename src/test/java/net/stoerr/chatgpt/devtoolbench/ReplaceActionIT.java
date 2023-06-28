package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class ReplaceActionIT extends AbstractActionIT {

    @Test
    public void testLiteralReplaceOperation() throws Exception {
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace.txt"), content, UTF_8);
            String response = checkResponse("/replaceInFile?path=replace.txt", "POST",
                    "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\",\"multiple\":true}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 12 occurrences of pattern; modified lines  1 - 3,  11 - 13, 16"));
            checkResponse("/readFile?path=replace.txt", "GET", null, 200, "replace-successfulmulti-replaced.txt");
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace.txt"));
        }
    }

    @Test
    public void testComplainAboutMultiplesSinceNoMatch() throws Exception {
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"neverthereinthefile\",\"literalReplacement\":\"goose\"}"
                , 400, null);
        collector.checkThat(response, is("Found no occurrences of pattern."));
    }

    @Test
    public void testComplainAboutMultiplesSinceManyMatches() throws Exception {
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}"
                , 400, null);
        collector.checkThat(response, is("Found 12 occurrences, but expected exactly one because parameter multiple = false."));
    }

    @Test
    public void testLiteralReplaceOperationFileNotFound() throws Exception {
        checkResponse("/replaceInFile?path=notfound.txt", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}", 404, "notfound.txt");
    }


    @Test
    public void testBothReplacementsGiven() throws Exception {
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                , 400, null);
        collector.checkThat(response, is("Either literalReplacement or replacementWithGroupReferences must be given, but not both."));
    }

    @Test
    public void testNoReplacementsGiven() throws Exception {
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"multiple\":true}"
                , 400, null);
        collector.checkThat(response, is("Either literalReplacement or replacementWithGroupReferences must be given."));
    }

    @Test
    public void testReplacementWithGroupReferencesNoGroup() throws Exception {
        String response = checkResponse("/replaceInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                , 400, null);
        collector.checkThat(response, is("don't use replacementWithGroupReferences if there are no group references."));
    }

    @Test
    public void testReplacementWithGroupReferencesSuccessful() throws Exception {
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace2.txt"), content, UTF_8);
            String response = checkResponse("/replaceInFile?path=replace2.txt", "POST",
                    "{\"pattern\":\"(duck)\",\"replacementWithGroupReferences\":\"goose$1\",\"multiple\":true}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 12 occurrences of pattern; modified lines  1 - 3,  11 - 13, 16"));
            checkResponse("/readFile?path=replace2.txt", "GET", null, 200, "replace-successfulmulti-replaced2.txt");
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace2.txt"));
        }
    }

}
