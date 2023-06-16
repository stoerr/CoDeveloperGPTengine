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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

@Ignore
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
        checkResponse("/.well-known/ai-plugin.json", "GET", null, 200, "ai-plugin.json");
        checkResponse("/devtoolbench.yaml", "GET", null, 200, "devtoolbench.yaml");
        checkResponse("/listFiles?path=.", "GET", null, 200, "listFiles.txt");
        checkResponse("/listFiles?path=subdir", "GET", null, 200, "listFilesSubdir.txt");
        checkResponse("/readFile?path=firstfile.txt", "GET", null, 200, "getFirstfile.txt");
        checkResponse("/", "GET", null, 200, "index.html");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt", "GET", null, 200, "listFilesFilenameRegex.txt");
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "GET", null, 200, "listFilesGrepRegex.txt");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt&grepRegex=testcontent",
                "GET", null, 200, "listFilesBothRegex.txt");
    }

    @Test(expected = IOException.class)
    public void testUnknownRequest() throws Exception {
        checkResponse("/nothing", "GET", null, 404, "unknown");
    }

    @Test
    public void testWrite() throws IOException {
        // curl -is $baseurl/writeFile?path=filewritten.txt -d '{"content":"testcontent line one\nline two \\\n with quoted backslashing \n"}'
        checkResponse("/writeFile?path=filewritten.txt", "POST", "{\"content\":\"testcontent line one\\nline two \\\\\\n with quoted backslashing \\n\"}", 204, "writeFile.txt");
        // check that the file was written
        String expected = readFile("/test-expected/filewritten.txt");
        String actual = readFile("/testdir/filewritten.txt");
        collector.checkThat(actual, CoreMatchers.is(expected));
    }

    @Test
    public void testActions() throws IOException {
        checkResponse("/executeAction?actionName=helloworld", "POST", "{\"content\":\"testinput\"}", 200, "helloworld.txt");
        collector.checkThrows(IOException.class, () -> {
            checkResponse("/executeAction?actionName=notthere", "POST", "{\"content\":\"testinput\"}", 400, "notthere.txt");
        });
        collector.checkThrows(IOException.class, () -> {
            checkResponse("/executeAction?actionName=fail", "POST", "{\"content\":\"testinput\"}", 500, "fail.txt");
        });
    }

    private void checkResponse(String path, String method, String requestBody, int expectedStatusCode, String expectFile) throws IOException {
        String result;
        URL url = new URL("http://localhost:" + port + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        if (requestBody != null) {
            conn.setDoOutput(true);
            conn.getOutputStream().write(requestBody.getBytes(UTF_8));
            conn.getOutputStream().close();
        }
        collector.checkThat(conn.getResponseCode(), CoreMatchers.is(expectedStatusCode));
        try (InputStream inputStream = conn.getInputStream()) {
            result = new String(inputStream.readAllBytes(), UTF_8);
        }
        Files.createDirectories(Paths.get("target/test-actual"));
        Files.writeString(Paths.get("target/test-actual/" + expectFile), result, UTF_8);
        String expectedResponse = readFile("/test-expected/" + expectFile);
        collector.checkThat(result, CoreMatchers.is(expectedResponse));
    }

    private String readFile(String filepath) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(filepath);
        collector.checkThat(filepath, resourceAsStream, CoreMatchers.notNullValue());
        if (resourceAsStream == null) throw new RuntimeException("Could not find " + filepath);
        return new String(resourceAsStream.readAllBytes(), UTF_8);
    }

}
