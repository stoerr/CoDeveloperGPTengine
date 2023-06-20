package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

public abstract class AbstractActionIT {

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
        String url = "http://localhost:" + port + path;

        CloseableHttpClient client = HttpClients.createDefault();
        HttpResponse response;

        if ("GET".equals(method)) {
            HttpGet request = new HttpGet(url);
            response = client.execute(request);
        } else if ("POST".equals(method)) {
            HttpPost request = new HttpPost(url);
            if (requestBody != null) {
                request.setEntity(new StringEntity(requestBody, UTF_8));
            }
            response = client.execute(request);
        } else {
            throw new IllegalArgumentException("Unsupported method: " + method);
        }

        collector.checkThat(response.getStatusLine().getStatusCode(), CoreMatchers.is(expectedStatusCode));

        if (expectedStatusCode == 204) {
            collector.checkThat(response.getEntity(), CoreMatchers.nullValue());
            return;
        }
        result = EntityUtils.toString(response.getEntity(), UTF_8);

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
