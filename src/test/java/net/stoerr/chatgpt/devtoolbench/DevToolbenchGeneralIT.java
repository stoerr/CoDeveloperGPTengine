package net.stoerr.chatgpt.devtoolbench;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class DevToolbenchGeneralIT extends AbstractActionIT {

    @Test
    public void testGeneralOperations() throws IOException {
        checkResponse("/.well-known/ai-plugin.json", "GET", null, 200, "ai-plugin.json");
        checkResponse("/", "GET", null, 200, "index.html");
    }

    @Test
    public void testDevToolbenchYaml() throws IOException {
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
