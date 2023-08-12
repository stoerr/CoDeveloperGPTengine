package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Node;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UrlAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/fetchUrlTextContent";
    }

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

        try {
            URL url = new URL(urlString);
            Document document = Jsoup.parse(url, 10000);
            // iterate through all tags in the document and collect text contents separated with \n
            // take care to only use the text of the current element and not of its children
            // but <p> hallo <b> you </b> here </p> should be "hallo\nyou\nhere"
            StringBuilder content = new StringBuilder();
            // iterate recursively over childNodes() and collect text of TextNodes into content
            appendChildText(document.body(), content);

            byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(bytes.length);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(bytes);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().write("Error fetching content from the URL: " + e.getMessage());
        }
    }

    private void appendChildText(Node node, StringBuilder content) {
        for (Node child : node.childNodes()) {
            if (child instanceof org.jsoup.nodes.TextNode) {
                content.append(child.toString()).append("\n");
            } else {
                appendChildText(child, content);
            }
        }
    }

}
