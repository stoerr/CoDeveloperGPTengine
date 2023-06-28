package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class DevToolBenchGeneralIT extends AbstractActionIT {

    @Test
    public void testRoot() throws IOException {
        checkResponse("/", "GET", null, 200, "index.html");
    }

    @Test
    public void testAiPluginJson() throws IOException {
        String response = checkResponse("/.well-known/ai-plugin.json", "GET", null, 200, null);
        String expected = readFile("/../../../src/main/resources/ai-plugin.json")
                .replace("THEPORT", "" + port)
                .replace("THEVERSION", TbUtils.getVersionString());
        collector.checkThat(response, CoreMatchers.is(expected));
    }

    @Test
    public void testDevToolBenchYaml() throws IOException {
        checkResponse("/devtoolbench.yaml", "GET", null, 200, "devtoolbench.yaml");
        // read target/test-actual/devtoolbench.yaml and compare to src/test/resources/test-expected/devtoolbench.yaml
        // and overwrite that file if it is different
        String expected = readFile("/test-expected/devtoolbench.yaml");
        String actual = Files.readString(Paths.get("target/test-actual/devtoolbench.yaml"), UTF_8);
        collector.checkThat(actual, CoreMatchers.is(expected));
        if (!expected.equals(actual) && actual.contains("requestBody:")) {
            Files.writeString(Paths.get("src/test/resources/test-expected/devtoolbench.yaml"), actual, UTF_8);
        }
    }

    @Test
    public void testUnknownRequest() throws Exception {
        checkResponse("/nothing", "GET", null, 404, "unknown");
    }
}
