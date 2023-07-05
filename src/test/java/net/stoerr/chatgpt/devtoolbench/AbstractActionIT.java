package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.nullValue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.ErrorCollector;

public abstract class AbstractActionIT {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    static final int port = 7364;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Files.createDirectories(Paths.get("target/test-actual"));
        DevToolBench.currentDir = Paths.get(".").resolve("src/test/resources/testdir").normalize()
                .toAbsolutePath();
        DevToolBench.main(new String[]{"-p", String.valueOf(port), "-w"});
        Thread.sleep(20);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        Thread.sleep(10);
        DevToolBench.stop();
        Thread.sleep(20);
    }

    @Before
    public void setUp() throws InterruptedException {
        Thread.sleep(100);
        System.out.flush();
        System.err.flush();
    }

    @After
    public void tearDown() throws InterruptedException {
        Thread.sleep(100);
        System.out.flush();
        System.err.flush();
    }

    /**
     * Executes a call and does checks on it; if you want to compare the response yourself just set expectFile=null.
     */
    protected String checkResponse(String path, String method, String requestBody, int expectedStatusCode, String expectFile) throws IOException {
        String result;
        String url = "http://localhost:" + port + path;

        try (CloseableHttpClient client = HttpClients.createDefault()) {
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

            result = response.getEntity() != null ? EntityUtils.toString(response.getEntity(), UTF_8) : null;

            collector.checkThat(result, response.getStatusLine().getStatusCode(), CoreMatchers.is(expectedStatusCode));

            if (expectedStatusCode == 204) {
                collector.checkThat(result, response.getEntity(), nullValue());
                collector.checkThat(expectFile, nullValue());
                if (response.getEntity() != null) {
                    collector.checkThat(result, result, nullValue());
                }
                return null;
            }

            if (expectFile != null) {
                writeActualAndCompareExpected(response, expectFile, result);
            }
            return result;
        }
    }

    protected void writeActualAndCompareExpected(HttpResponse response, String expectFilename, String result) throws IOException {
        // our IDE adds a \n to each file, which is a often desirable convention
        result = result.stripTrailing() + "\n";

        Header contentTypeHeader = response.getFirstHeader("Content-Type");

        Files.writeString(Paths.get("target/test-actual/" + Path.of(expectFilename).getFileName()), result, UTF_8);
        String expectedResponse = readFile("/test-expected/" + expectFilename);
        collector.checkThat(expectFilename, result, CoreMatchers.is(expectedResponse));

        String expectedContentType = expectedResponse.contains("<html>") ? "text/html; charset=UTF-8" : "text/plain; charset=UTF-8";
        collector.checkThat(contentTypeHeader != null ? contentTypeHeader.getValue() : null, CoreMatchers.is(expectedContentType));
    }

    protected String readFile(String filepath) throws IOException {
        Path path = Paths.get("src/test/resources" + filepath);
        collector.checkThat("Does not exist yet: " + path, Files.exists(path), CoreMatchers.is(true));
        if (!Files.exists(path)) {
            return ""; // make sure the following code runs, so that any files are created
        }
        return Files.readString(path, UTF_8);
    }
}
