package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Random;

import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

public class DevToolbenchIT {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    int port = 7364;

    @Before
    public void setUp() throws Exception {
        DevToolbench.currentDir = Paths.get(".").resolve("src/test/resources/testdir").normalize()
                .toAbsolutePath();
        DevToolbench.main(new String[]{String.valueOf(port)});
    }

    @After
    public void tearDown() throws Exception {
        DevToolbench.stop();
    }

    @Test
    public void testServer() throws Exception {
        checkResponse("/.well-known/ai-plugin.json", "/test-expected/ai-plugin.json");
        checkResponse("/devtoolbench.yaml", "/test-expected/devtoolbench.yaml");
        checkResponse("/listFiles?path=.", "/test-expected/listFiles.txt");
        checkResponse("/listFiles?path=subdir", "/test-expected/listFilesSubdir.txt");
        checkResponse("/readFile?path=firstfile.txt", "/test-expected/getFirstfile.txt");
        checkResponse("/", "/test-expected/index.html");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt", "/test-expected/listFilesFilenameRegex.txt");
        checkResponse("/listFiles?path=.&grepRegex=testcontent", "/test-expected/listFilesGrepRegex.txt");
        checkResponse("/listFiles?path=.&filenameRegex=.*%5C.txt&grepRegex=testcontent",
                "/test-expected/listFilesBothRegex.txt");
    }

    @Ignore
    @Test(expected = IOException.class)
    public void testUnknownRequest() throws Exception {
        checkResponse("/nothing", "/test-expected/unknown");
    }

    private void checkResponse(String path, String expectFile) throws IOException {
        String expectedResponse = readFile(expectFile);
        String actual = executeGet(path);
        collector.checkThat(actual, CoreMatchers.is(expectedResponse));
    }

    private String readFile(String filepath) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(filepath);
        collector.checkThat(filepath, resourceAsStream, CoreMatchers.notNullValue());
        if (resourceAsStream == null) throw new RuntimeException("Could not find " + filepath);
        String fileContent = new String(resourceAsStream.readAllBytes(), UTF_8);
        return fileContent;
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
