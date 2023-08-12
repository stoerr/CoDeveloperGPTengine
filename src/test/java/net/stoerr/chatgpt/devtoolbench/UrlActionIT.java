package net.stoerr.chatgpt.devtoolbench;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class UrlActionIT extends AbstractActionIT {

    @Test
    public void testValidUrl() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testValidUrl");
        String validUrl = "https://www.example.com";
        String response = checkResponse("/fetchUrlTextContent?url=" + validUrl, "GET", null, 200, null);
        collector.checkThat(response, CoreMatchers.containsString("Example Domain"));
    }

    @Test
    public void testUrlWithoutProtocol() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testUrlWithoutProtocol");
        String urlWithoutProtocol = "www.example.com";
        String response = checkResponse("/fetchUrlTextContent?url=" + urlWithoutProtocol, "GET", null, 200, null);
        collector.checkThat(response, CoreMatchers.containsString("Example Domain"));
    }

    @Test
    public void testInvalidUrl() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testInvalidUrl");
        String invalidUrl = "https://invalid.url.that.does.not.exist";
        String response = checkResponse("/fetchUrlTextContent?url=" + invalidUrl, "GET", null, 400, null);
        System.out.printf("Response: %s\n", response);
    }
}
