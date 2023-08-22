package net.stoerr.chatgpt.devtoolbench;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.tuple.Pair;

import io.github.furstenheim.CodeBlockStyle;
import io.github.furstenheim.CopyDown;
import io.github.furstenheim.HeadingStyle;
import io.github.furstenheim.Options;
import io.github.furstenheim.OptionsBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UrlAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/fetchUrlTextContent";
    }

    // also add an optional parameter raw that returns the raw content of the URL without converting to markdown
    @Override
    public String openApiDescription() {
        return """
                  /fetchUrlTextContent:
                    get:
                      operationId: fetchUrlTextContent
                      summary: Fetch text content from a given URL.
                      parameters:
                        - name: url
                          in: query
                          required: true
                          schema:
                            type: string
                        - name: raw
                          description: return raw html content without converting to markdown
                          in: query
                          required: false
                          schema:
                            type: boolean
                      responses:
                        '200':
                          content:
                            text/plain:
                              schema:
                                type: string
                """.stripIndent();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String urlString = req.getParameter("url");
        if (urlString == null || urlString.isEmpty()) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("URL parameter is missing or empty.");
            return;
        }
        boolean raw = Boolean.TRUE.toString().equalsIgnoreCase(req.getParameter("raw"));

        try {
            Pair<String, String> contentinfo;
            try {
                contentinfo = fetchContentFromUrl(urlString);
            } catch (MalformedURLException e) {
                if (!urlString.startsWith("https://") && !urlString.startsWith("http://")) {
                    try {
                        contentinfo = fetchContentFromUrl("https://" + urlString);
                    } catch (SSLHandshakeException e1) {
                        contentinfo = fetchContentFromUrl("http://" + urlString);
                    }
                } else {
                    throw e;
                }
            }

            String contentString = contentinfo.getRight();
            String contentType = contentinfo.getLeft();
            // remove trailing ;charset=... from content type
            int charsetIndex = contentType.indexOf(";");
            if (charsetIndex > 0) {
                contentType = contentType.substring(0, charsetIndex);
            }

            if (contentType.startsWith("text/html") && !raw) {
                contentString = htmlToMarkdown(contentString);
                contentString = "markdown for " + contentType + " content of " + urlString + "\n\n" + contentString;
            } else {
                contentString = contentType + " content of URL " + urlString + "\n\n" + contentString;
            }

            byte[] bytes = contentString.getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(bytes.length);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(bytes);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error fetching content from the URL: " + e.getMessage());
        }
    }

    private static String htmlToMarkdown(String contentString) {
        OptionsBuilder optionsBuilder = OptionsBuilder.anOptions();
        Options options = optionsBuilder.withHeadingStyle(HeadingStyle.ATX)
                .withCodeBlockStyle(CodeBlockStyle.FENCED)
                .withBulletListMaker("-")
                .withEmDelimiter("*")
                .withStrongDelimiter("**")
                .withHr("---")
                .build();
        CopyDown converter = new CopyDown(options);
        contentString = converter.convert(contentString);
        return contentString;
    }

    private Pair<String, String> fetchContentFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            String contentType = connection.getContentType();
            return Pair.of(contentType, reader.lines().collect(Collectors.joining("\n")));
        }
    }

}
