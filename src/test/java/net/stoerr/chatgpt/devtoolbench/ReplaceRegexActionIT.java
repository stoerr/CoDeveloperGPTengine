package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("Disabled since ReplaceRegexAction isn't active because of too many errors.")
public class ReplaceRegexActionIT extends AbstractActionIT {

    @Test
    public void testLiteralReplaceOperation() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testLiteralReplaceOperation");
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/firstfile.txt"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace3.txt"), content, UTF_8);
            String response = checkResponse("/replaceRegexInFile?path=replace3.txt", "POST",
                    "{\"pattern\":\"test\",\"literalReplacement\":\"dingding\"}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 1 occurrences of pattern; modified lines 2"));
            response = checkResponse("/readFile?path=replace3.txt", "GET", null, 200, null);
            collector.checkThat(response, is("Hello!\nJust a dingding.\n"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace3.txt"));
        }
    }

    @Deprecated
    @Test
    public void testLiteralReplaceOperationMulti() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testLiteralReplaceOperationMulti");
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace.txt"), content, UTF_8);
            String response = checkResponse("/replaceRegexInFile?path=replace.txt", "POST",
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
        TbUtils.logInfo("\nReplaceRegexActionIT.testComplainAboutMultiplesSinceNoMatch");
        String response = checkResponse("/replaceRegexInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"neverthereinthefile\",\"literalReplacement\":\"goose\"}"
                , 400, null);
        collector.checkThat(response, containsString("Found no occurrences of pattern."));
    }

    @Test
    public void testComplainAboutMultiplesSinceManyMatches() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testComplainAboutMultiplesSinceManyMatches");
        String response = checkResponse("/replaceRegexInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}"
                , 400, null);
        collector.checkThat(response, containsString("Found 12 occurrences, but expected exactly one."));
    }

    @Test
    public void testLiteralReplaceOperationFileNotFound() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testLiteralReplaceOperationFileNotFound");
        checkResponse("/replaceRegexInFile?path=notfound.txt", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}", 404, "notfound.txt");
    }


    @Test
    public void testBothReplacementsGiven() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testBothReplacementsGiven");
        String response = checkResponse("/replaceRegexInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"literalReplacement\":\"goose\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                , 400, null);
        collector.checkThat(response, is("Either literalReplacement or replacementWithGroupReferences must be given, but not both."));
    }

    @Test
    public void testNoReplacementsGiven() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testNoReplacementsGiven");
        String response = checkResponse("/replaceRegexInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"multiple\":true}"
                , 400, null);
        collector.checkThat(response, is("Either literalReplacement or replacementWithGroupReferences must be given."));
    }

    @Test
    public void testReplacementWithGroupReferencesNoGroup() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testReplacementWithGroupReferencesNoGroup");
        String response = checkResponse("/replaceRegexInFile?path=secondfile.md", "POST",
                "{\"pattern\":\"duck\",\"replacementWithGroupReferences\":\"goose\",\"multiple\":true}"
                , 400, null);
        collector.checkThat(response, is("don't use replacementWithGroupReferences if there are no group references."));
    }

    @Test
    public void testReplacementWithGroupReferencesSuccessful() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testReplacementWithGroupReferencesSuccessful");
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/firstfile.txt"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace4.txt"), content, UTF_8);
            String response = checkResponse("/replaceRegexInFile?path=replace4.txt", "POST",
                    "{\"pattern\":\"(test)\",\"replacementWithGroupReferences\":\"repl$1\"}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 1 occurrences of pattern; modified lines 2"));
            response = checkResponse("/readFile?path=replace4.txt", "GET", null, 200, null);
            collector.checkThat(response, is("Hello!\nJust a repltest.\n"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace4.txt"));
        }
    }

    @Deprecated
    @Test
    public void testReplacementWithGroupReferencesSuccessfulWithMulti() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testReplacementWithGroupReferencesSuccessfulWithMulti");
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/secondfile.md"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replace2.txt"), content, UTF_8);
            String response = checkResponse("/replaceRegexInFile?path=replace2.txt", "POST",
                    "{\"pattern\":\"(duck)\",\"replacementWithGroupReferences\":\"goose$1\",\"multiple\":true}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 12 occurrences of pattern; modified lines  1 - 3,  11 - 13, 16"));
            checkResponse("/readFile?path=replace2.txt", "GET", null, 200, "replace-successfulmulti-replaced2.txt");
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replace2.txt"));
        }
    }

    @Test
    public void testLiteralSearchStringReplaceOperation() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testLiteralSearchStringReplaceOperation");
        try {
            String content = Files.readString(Paths.get("src/test/resources/testdir/firstfile.txt"), UTF_8);
            Files.writeString(Paths.get("src/test/resources/testdir/replaceLiteral.txt"), content, UTF_8);
            String response = checkResponse("/replaceRegexInFile?path=replaceLiteral.txt", "POST",
                    "{\"literalSearchString\":\"test\",\"literalReplacement\":\"dingding\"}"
                    , 200, null);
            collector.checkThat(response, is("Replaced 1 occurrences of pattern; modified lines 2"));
            response = checkResponse("/readFile?path=replaceLiteral.txt", "GET", null, 200, null);
            collector.checkThat(response, is("Hello!\nJust a dingding.\n"));
        } finally {
            Files.deleteIfExists(Paths.get("src/test/resources/testdir/replaceLiteral.txt"));
        }
    }

    @Test
    public void testBothLiteralSearchStringAndPatternGiven() throws Exception {
        TbUtils.logInfo("\nReplaceRegexActionIT.testBothLiteralSearchStringAndPatternGiven");
        String response = checkResponse("/replaceRegexInFile?path=secondfile.md", "POST",
                "{\"literalSearchString\":\"duck\",\"pattern\":\"duck\",\"literalReplacement\":\"goose\"}"
                , 400, null);
        collector.checkThat(response, is("Either literalSearchString or pattern must be given, but not both."));
    }

}
