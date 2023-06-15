package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class DevToolbenchIT {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    int port = 7364;

    @Before
    public void setUp() {
        DevToolbench.currentDir = Paths.get(".").resolve("src/test/resources/testdir").normalize()
                .toAbsolutePath();
        DevToolbench.main(new String[]{String.valueOf(port)});
    }

    @After
    public void tearDown() {
        DevToolbench.stop();
    }

    @Test
    public void testServer() throws Exception {
        checkResponse("/.well-known/ai-plugin.json", "ai-plugin.json");
        checkResponse("/devtoolbench.yaml", "devtoolbench.yaml");
        checkResponse("/listFiles?path=.", "listFiles.txt");
        checkResponse("/listFiles?path=subdir", "listFilesSubdir.txt");
        checkResponse("/readFile?path=firstfile.txt", "getFirstfile.txt");
        checkResponse("/", "index.html");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt", "listFilesFilenameRegex.txt");
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "listFilesGrepRegex.txt");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt&grepRegex=testcontent",
                "listFilesBothRegex.txt");
    }

    @Test(expected = IOException.class)
    public void testUnknownRequest() throws Exception {
        checkResponse("/nothing", "unknown");
    }

    @Test
    public void testWrite() throws IOException {
        // curl -is $baseurl/writeFile?path=filewritten.txt -d '{"content":"testcontent line one\nline two \\\n with quoted backslashing \n"}'
        // perform a POST to url
        URL url = new URL("http://localhost:" + port + "/writeFile?path=filewritten.txt");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.getOutputStream().write("{\"content\":\"testcontent line one\\nline two \\\\\\n with quoted backslashing \\n\"}".getBytes(UTF_8));
        conn.getOutputStream().close();
        collector.checkThat(conn.getResponseCode(), CoreMatchers.is(204));
        collector.checkThat(conn.getResponseMessage(), CoreMatchers.is("No Content"));
        // check that the file was written
        String expected = readFile("/test-expected/filewritten.txt");
        String actual = readFile("/testdir/filewritten.txt");
        collector.checkThat(actual, CoreMatchers.is(expected));
    }

    private void checkResponse(String path, String expectFile) throws IOException {
        String expectedResponse = readFile("/test-expected/" + expectFile);
        String actual = executeGet(path);
        Files.createDirectories(Paths.get("target/test-actual"));
        Files.writeString(Paths.get("target/test-actual/" + expectFile), actual, UTF_8);
        collector.checkThat(actual, CoreMatchers.is(expectedResponse));
    }

    private String readFile(String filepath) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(filepath);
        collector.checkThat(filepath, resourceAsStream, CoreMatchers.notNullValue());
        if (resourceAsStream == null) throw new RuntimeException("Could not find " + filepath);
        return new String(resourceAsStream.readAllBytes(), UTF_8);
    }

    private String executeGet(String path) throws IOException {
        URL url = new URL("http://localhost:" + port + path);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        try (InputStream inputStream = conn.getInputStream()) {
            return new String(inputStream.readAllBytes(), UTF_8);
        }
    }

}
