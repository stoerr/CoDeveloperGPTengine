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
import org.junit.rules.ErrorCollector;

public abstract class AbstractIT {

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

    protected void checkResponse(String path, String method, String requestBody, int expectedStatusCode, String expectFile) throws IOException {
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

    protected String readFile(String filepath) throws IOException {
        InputStream resourceAsStream = getClass().getResourceAsStream(filepath);
        collector.checkThat(filepath, resourceAsStream, CoreMatchers.notNullValue());
        if (resourceAsStream == null) throw new RuntimeException("Could not find " + filepath);
        return new String(resourceAsStream.readAllBytes(), UTF_8);
    }
}