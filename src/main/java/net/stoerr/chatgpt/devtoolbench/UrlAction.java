package net.stoerr.chatgpt.devtoolbench;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class UrlAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/fetchUrlContent";
    }

    @Override
    public String openApiDescription() {
        return """
                  /fetchUrlContent:
                    get:
                      operationId: fetchUrlContent
                      summary: Fetch content from a given URL.
                      parameters:
                        - name: url
                          in: query
                          description: The URL to fetch content from
                          required: true
                          schema:
                            type: string
                      responses:
                        '200':
                          description: Content of the fetched URL
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
            String content = org.jsoup.Jsoup.parse(url, 10000).text();

            byte[] bytes = content.toString().getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(bytes.length);
            resp.setContentType("text/plain;charset=UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getOutputStream().write(bytes);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.getWriter().write("Error fetching content from the URL: " + e.getMessage());
        }
    }
}
