package net.stoerr.chatgpt.codevengine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import net.stoerr.chatgpt.codevengine.AbstractPluginAction.GitIgnoreRules;

public class GitIgnoreRulesTest {

    private Path tempDirectory;
    private Path gitignoreFile;

    @Before
    public void setUp() throws IOException {
        tempDirectory = Files.createTempDirectory("testGitIgnoreRules");
        gitignoreFile = tempDirectory.resolve(".gitignore");
    }

    @After
    public void tearDown() throws IOException {
        Files.deleteIfExists(gitignoreFile);
        Files.deleteIfExists(tempDirectory);
    }

    @Test
    public void testNoGitignoreFile() {
        GitIgnoreRules gitIgnoreRules = new GitIgnoreRules(tempDirectory);
        assertFalse(gitIgnoreRules.isIgnored(tempDirectory.resolve("somefile.txt")));
    }

    @Test
    public void testEmptyGitignoreFile() throws IOException {
        Files.createFile(gitignoreFile);
        GitIgnoreRules gitIgnoreRules = new GitIgnoreRules(tempDirectory);
        assertFalse(gitIgnoreRules.isIgnored(tempDirectory.resolve("somefile.txt")));
    }

    @Test
    public void testValidGitignoreRules() throws IOException {
        Files.write(gitignoreFile, "ignored.txt\nfolder/\n*.log".getBytes());
        GitIgnoreRules gitIgnoreRules = new GitIgnoreRules(tempDirectory);

        assertTrue(gitIgnoreRules.isIgnored(tempDirectory.resolve("ignored.txt")));
        assertTrue(gitIgnoreRules.isIgnored(tempDirectory.resolve("folder/somefile.txt")));
        assertTrue(gitIgnoreRules.isIgnored(tempDirectory.resolve("error.log")));
        assertFalse(gitIgnoreRules.isIgnored(tempDirectory.resolve("notignored.txt")));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testNegatedGitignoreRuleThrowsException() throws IOException {
        Files.write(gitignoreFile, "!notignored.txt".getBytes());
        new GitIgnoreRules(tempDirectory);
    }

    @Test
    public void testIsIgnored() throws IOException {
        Files.write(gitignoreFile, "ignored.txt\n*.log".getBytes());
        GitIgnoreRules gitIgnoreRules = new GitIgnoreRules(tempDirectory);

        assertTrue(gitIgnoreRules.isIgnored(tempDirectory.resolve("ignored.txt")));
        assertTrue(gitIgnoreRules.isIgnored(tempDirectory.resolve("somefile.log")));
        assertFalse(gitIgnoreRules.isIgnored(tempDirectory.resolve("notignored.txt")));
    }

    @Test
    public void testIsNotIgnored() throws IOException {
        Files.write(gitignoreFile, "ignored.txt\n*.log".getBytes());
        GitIgnoreRules gitIgnoreRules = new GitIgnoreRules(tempDirectory);

        assertFalse(gitIgnoreRules.isIgnored(tempDirectory.resolve("notignored.txt")));
        assertFalse(gitIgnoreRules.isIgnored(tempDirectory.resolve("somefile.txt")));
    }
}
