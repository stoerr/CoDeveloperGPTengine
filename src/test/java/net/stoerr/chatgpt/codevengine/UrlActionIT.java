package net.stoerr.chatgpt.codevengine;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class UrlActionIT extends AbstractActionIT {

    @Test
    public void testValidUrl() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testValidUrl");
        String validUrl = "https://www.example.com";
        String response = checkResponse("/fetchUrlTextContent?url=" + validUrl, "GET", null, 200, null);
        collector.checkThat(response, CoreMatchers.containsString("Example Domain"));
        collector.checkThat(response, is("markdown for text/html content of https://www.example.com\n" +
                "\n" +
                "Example Domain      \n" +
                "\n" +
                "# Example Domain\n" +
                "\n" +
                "This domain is for use in illustrative examples in documents. You may use this domain in literature without prior coordination or asking for permission.\n" +
                "\n" +
                "[More information...](https://www.iana.org/domains/example)"));
    }

    // test as testValidUrl but with parameter raw=true
    @Test
    public void testValidUrlRaw() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testValidUrlRaw");
        String validUrl = "https://www.example.com";
        String response = checkResponse("/fetchUrlTextContent?url=" + validUrl + "&raw=true", "GET", null, 200, null);
        collector.checkThat(response, containsString("<h1>Example Domain</h1>"));
    }


    @Test
    public void testUrlWithoutProtocol() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testUrlWithoutProtocol");
        String urlWithoutProtocol = "www.example.com";
        String response = checkResponse("/fetchUrlTextContent?url=" + urlWithoutProtocol, "GET", null, 200, null);
        collector.checkThat(response, CoreMatchers.containsString("Example Domain"));
    }

    @Test
    public void testPdfUrl() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testPdfUrl");
        String pdfUrl = "https://datatracker.ietf.org/doc/pdf/rfc791.pdf";
        String response = checkResponse("/fetchUrlTextContent?url=" + pdfUrl, "GET", null, 200, null);
        collector.checkThat(response, CoreMatchers.containsString("markdown for text/html content of "));
        collector.checkThat(response, CoreMatchers.containsString("INTERNET PROTOCOL"));
        collector.checkThat(response, CoreMatchers.containsString("September 1981"));
    }

    @Test
    public void testInvalidUrl() throws Exception {
        TbUtils.logInfo("\nUrlActionIT.testInvalidUrl");
        String invalidUrl = "https://invalid.url.that.does.not.exist";
        String response = checkResponse("/fetchUrlTextContent?url=" + invalidUrl, "GET", null, 400, null);
        System.out.printf("Response: %s\n", response);
    }
}
