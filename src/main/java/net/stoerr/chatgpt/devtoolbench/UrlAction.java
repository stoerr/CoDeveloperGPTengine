package net.stoerr.chatgpt.devtoolbench;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

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
        return "" +
                "  /fetchUrlTextContent:\n" +
                "    get:\n" +
                "      operationId: fetchUrlTextContent\n" +
                "      summary: Fetch text content from a given URL.\n" +
                "      parameters:\n" +
                "        - name: url\n" +
                "          in: query\n" +
                "          required: true\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: raw\n" +
                "          description: return raw html or pdf content without converting to markdown\n" +
                "          in: query\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: boolean\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: fetch successful; content returned\n" +
                "          content:\n" +
                "            text/plain:\n" +
                "              schema:\n" +
                "                type: string\n";
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
            Pair<String, byte[]> contentinfo;
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

            String contentString = new String(contentinfo.getRight(), StandardCharsets.UTF_8);
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
                if (contentType.equals("application/pdf") && !raw) {
                    contentString = "text content of " + contentType + " content of " + urlString + "\n\n"
                            + convertPdfToText(contentinfo.getRight());
                } else {
                    contentString = contentType + " content of URL " + urlString + "\n\n" + contentString;
                }
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

    private String convertPdfToText(byte[] pdfContent) throws IOException {
        try (PDDocument document = PDDocument.load(pdfContent)) {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper.getText(document);
        } catch (Exception e) {
            throw new IOException("Error processing PDF content: " + e.getMessage(), e);
        }
    }

    private Pair<String, byte[]> fetchContentFromUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        ByteArrayOutputStream contentOut = new ByteArrayOutputStream();
        URLConnection connection = url.openConnection();
        try (InputStream inputStream = connection.getInputStream()) {
            IOUtils.copy(inputStream, contentOut);
            String contentType = connection.getContentType();
            return Pair.of(contentType, contentOut.toByteArray());
        }
    }

}
