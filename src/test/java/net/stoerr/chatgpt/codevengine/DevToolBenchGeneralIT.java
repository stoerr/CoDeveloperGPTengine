package net.stoerr.chatgpt.codevengine;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class DevToolBenchGeneralIT extends AbstractActionIT {

    @Test
    public void testRoot() throws IOException {
        TbUtils.logInfo("\nDevToolBenchGeneralIT.testRoot");
        String response = checkResponse("/", "GET", null, 200, null);
        String expected = readFile("/../../../src/main/resources/static/index.html");
        collector.checkThat(response, CoreMatchers.is(expected));
    }

    @Test
    public void testAiPluginJson() throws IOException {
        TbUtils.logInfo("\nDevToolBenchGeneralIT.testAiPluginJson");
        String response = checkResponse("/.well-known/ai-plugin.json", "GET", null, 200, null);
        String expected = readFile("/../../../src/main/resources/ai-plugin.json")
                .replace("THEURL", "http://localhost:7364")
                .replace("THEOPENAITOKEN", "")
                .replace("THEVERSION", TbUtils.getVersionString());
        collector.checkThat(response, CoreMatchers.is(expected));
    }

    @Test
    public void testDevToolBenchYaml() throws IOException {
        TbUtils.logInfo("\nDevToolBenchGeneralIT.testDevToolBenchYaml");
        checkResponse("/codeveloperengine.yaml", "GET", null, 200, "codeveloperengine.yaml");
        // read target/test-actual/codeveloperengine.yaml and compare to src/test/resources/test-expected/codeveloperengine.yaml
        // and overwrite that file if it is different
        String expected = readFile("/test-expected/codeveloperengine.yaml");
        String actual = new String(Files.readAllBytes(Paths.get("target/test-actual/codeveloperengine.yaml")), UTF_8);
        collector.checkThat(actual, CoreMatchers.is(expected));
        if (!expected.equals(actual) && actual.contains("requestBody:")) {
            Files.write(Paths.get("src/test/resources/test-expected/codeveloperengine.yaml"), actual.getBytes( UTF_8));
        }
    }

    @Test
    public void testUnknownRequest() throws Exception {
        TbUtils.logInfo("\nDevToolBenchGeneralIT.testUnknownRequest");
        String response = checkResponse("/nothing", "GET", null, 404, null);
        collector.checkThat(response, CoreMatchers.containsString("Error 404 Not Found"));
    }
}
