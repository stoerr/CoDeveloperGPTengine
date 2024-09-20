package net.stoerr.chatgpt.codevengine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// curl -is http://localhost:3001/listFiles?path=.
public class ListFilesAction extends AbstractPluginAction {

    @Override
    public String getUrl() {
        return "/listFiles";
    }

    @Override
    public String openApiDescription() {
        return "  /listFiles:\n" +
                "    get:\n" +
                "      operationId: listFiles\n" +
                "      x-openai-isConsequential: false\n" +
                "      summary: Recursively lists files in a directory. Optionally filters by filename and content.\n" +
                "      parameters:\n" +
                "        - name: path\n" +
                "          in: query\n" +
                "          description: relative path to directory to list. default is the root directory = '.'\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: recursive\n" +
                "          in: query\n" +
                "          description: if true (default) lists files recursively, else only in that directory. " +
                "                       In that case we will also list directories.\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: boolean\n" +
                "            default: true\n" +
                "        - name: filePathRegex\n" +
                "          in: query\n" +
                "          description: regex to filter file paths - use for search by file name\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: grepRegex\n" +
                "          in: query\n" +
                "          description: an optional regex that lists only files that contain a line matching this pattern\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: string\n" +
                "        - name: listDirectories\n" +
                "          in: query\n" +
                "          description: if true, lists directories instead of files\n" +
                "          required: false\n" +
                "          schema:\n" +
                "            type: boolean\n" +
                "      responses:\n" +
                "        '200':\n" +
                "          description: List of relative paths of the files\n" +
                "          content:\n" +
                "            text/plain:\n" +
                "              schema:\n" +
                "                type: string\n";
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Path path = getPath(req, resp, true, true);
        String filePathRegex = getQueryParam(req, "filePathRegex");
        String grepRegex = getQueryParam(req, "grepRegex");
        String listDirectoriesRaw = getQueryParam(req, "listDirectories");
        boolean listDirectories = Boolean.parseBoolean(listDirectoriesRaw);
        String recursiveRaw = getQueryParam(req, "recursive");
        boolean recursive = recursiveRaw == null || Boolean.parseBoolean(recursiveRaw);
        RepeatedRequestChecker.CHECKER.checkRequestRepetition(resp, this, path, filePathRegex, grepRegex, recursiveRaw, listDirectories);
        Pattern filePathPattern;
        try {
            filePathPattern = filePathRegex != null ? Pattern.compile(filePathRegex) : null;
        } catch (Exception e) {
            throw sendError(resp, 400, "Invalid filePathRegex: " + e);
        }
        Pattern grepPattern;
        try {
            grepPattern = grepRegex != null ? Pattern.compile(grepRegex) : null;
        } catch (Exception e) {
            throw sendError(resp, 400, "Invalid grepRegex: " + e);
        }

        if (Files.isDirectory(path)) {
            resp.setContentType("text/plain;charset=UTF-8");
            List<Path> paths = findMatchingFiles(false, resp, path, filePathPattern, grepPattern, recursive, listDirectories)
                    .collect(Collectors.toList());
            List<String> files = paths.stream()
                    .map(path1 -> CoDeveloperEngine.canonicalName(path1))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (files.isEmpty()) {
                if (grepPattern != null) {
                    long filePathFileCount = findMatchingFiles(false, resp, path, filePathPattern, null, recursive, listDirectories).count();
                    if (filePathFileCount > 0)
                        throw sendError(resp, 404, "Found " + filePathFileCount + " files whose name is matching the filePathRegex but none of them contain a line matching the grepRegex.");
                }
                if (Files.newDirectoryStream(path).iterator().hasNext()) {
                    String similarFilesMessage = getSimilarFilesMessage(resp, path, filePathPattern != null ? filePathPattern.toString() : "", listDirectories);
                    throw sendError(resp, 404, "No files found matching filePathRegex: " + filePathRegex + "\n\n" + similarFilesMessage);
                } else {
                    throw sendError(resp, 404, "No files found in directory: " + path);
                }
            } else if (files.size() > 100) {
                long directoryCount = paths.stream().map(Path::getParent).distinct().count();
                throw sendError(resp, 404, "Found " + files.size() + " files in " + directoryCount + " directories - please use a more specific path or filePathRegex, or use listDirectories instead to get an overview and then list specific directories you're interested in.");
            }
            byte[] response = (String.join("\n", files) + "\n").getBytes(StandardCharsets.UTF_8);
            resp.setContentLength(response.length);
            resp.getOutputStream().write(response);
        } else {
            throw sendError(resp, 404, "Is not a directory: " + path);
        }
    }

}
