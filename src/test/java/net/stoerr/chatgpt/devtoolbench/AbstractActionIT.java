package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

import io.undertow.util.Headers;

public abstract class AbstractActionIT {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    static final int port = 7364;

    @BeforeClass
    public static void setUp() {
        DevToolbench.currentDir = Paths.get(".").resolve("src/test/resources/testdir").normalize()
                .toAbsolutePath();
        DevToolbench.main(new String[]{String.valueOf(port)});
    }

    @AfterClass
    public static void tearDown() {
        DevToolbench.stop();
    }

    protected String checkResponse(String path, String method, String requestBody, int expectedStatusCode, String expectFile) throws IOException {
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
            collector.checkThat(expectFile, CoreMatchers.nullValue());
            return null;
        }

        collector.checkThat(response.getFirstHeader(Headers.CONTENT_TYPE.toString()).getValue(), CoreMatchers.is("text/plain; charset=UTF-8"));

        result = EntityUtils.toString(response.getEntity(), UTF_8);

        if (expectFile != null) {
            Files.createDirectories(Paths.get("target/test-actual"));
            Files.writeString(Paths.get("target/test-actual/" + expectFile), result, UTF_8);
            String expectedResponse = readFile("/test-expected/" + expectFile);
            collector.checkThat(result, CoreMatchers.is(expectedResponse));
        }
        return result;
    }

    protected String readFile(String filepath) throws IOException {
        Path path = Paths.get("src/test/resources" + filepath);
        if (!Files.exists(path)) throw new RuntimeException("Could not find " + path);
        return Files.readString(path, UTF_8);
    }
}