package net.stoerr.chatgpt.codevengine;

import static java.util.stream.Collectors.toList;
import static net.stoerr.chatgpt.codevengine.TbUtils.logBody;
import static net.stoerr.chatgpt.codevengine.TbUtils.logError;
import static net.stoerr.chatgpt.codevengine.TbUtils.logInfo;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.gson.Gson;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class AbstractPluginAction extends HttpServlet {

    /**
     * A pattern for filenames of binary files where grep would not work.
     */
    public static final Pattern BINARY_FILES_PATTERN = Pattern.compile("(?i).*\\.(gif|png|mov|jpg|jpeg|mp4|mp3|pdf|zip|gz|tgz|tar|jar|class|war|ear|exe|dll|so|o|a|lib|bin|dat|dmg|iso)");

    private final transient Gson gson = new Gson();

    /**
     * Logs an error and sends it to ChatGPT, always throws {@link ExecutionAbortedException}.
     * Use with pattern {@code throw sendError(...)} to let compiler know that.
     */
    protected static ExecutionAbortedException sendError(HttpServletResponse response, int statusCode, String error) throws ExecutionAbortedException {
        logInfo("Error " + statusCode + ": " + error);
        response.setStatus(statusCode);
        response.setContentType("text/plain;charset=UTF-8");
        try {
            response.getWriter().write(error);
        } catch (IOException e) {
            logInfo("Error writing error: " + e);
        }
        throw new ExecutionAbortedException();
    }

    protected static Stream<Path> findMatchingFiles(HttpServletResponse response, Path path, Pattern filePathPattern, Pattern grepPattern) {
        boolean haveFilePathPattern = filePathPattern != null && !filePathPattern.pattern().isEmpty();
        List<Path> result = new ArrayList<>();
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    if (isIgnored(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return super.preVisitDirectory(dir, attrs);
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    FileVisitResult res = super.visitFile(file, attrs);
                    result.add(file);
                    return res;
                }
            });
        } catch (IOException e) {
            throw sendError(response, 500, "Error reading " + path + " : " + e);
        }

        List<Path> matchingFiles = result.stream()
                .filter(Files::isRegularFile)
                .filter(p -> !isIgnored(p))
                .filter(p -> !haveFilePathPattern || filePathPattern.matcher(p.toString()).find())
                .collect(toList());
        if (matchingFiles.isEmpty()) {
            throw sendError(response, 404, "No files found matching filePathRegex: " + filePathPattern);
        }
        Collections.sort(matchingFiles); // make it deterministic.

        List<Path> grepMatched = matchingFiles;
        if (grepPattern != null && !grepPattern.pattern().isEmpty()) {
            grepMatched = matchingFiles.stream()
                    .filter(f -> !BINARY_FILES_PATTERN.matcher(f.toString()).find())
                    .filter(p -> {
                        try (Stream<String> lines = Files.lines(p)) {
                            return lines.anyMatch(line -> grepPattern.matcher(line).find());
                        } catch (Exception e) {
                            logInfo("Error reading " + p + " : " + e);
                            return false;
                        }
                    })
                    .sorted().collect(toList());
        }

        if (grepMatched.isEmpty()) {
            String foundFilesStatement = haveFilePathPattern ?
                    "Found " + matchingFiles.size() + " files whose name is matching the filePathRegex" :
                    "Found " + matchingFiles.size() + " files";
            String fixingStatement = haveFilePathPattern ?
                    "" :
                    "\nDid you really want to search for files containing '" + grepPattern + "' or for files named like that pattern? If so you have to repeat the search with filePathRegex set instead of grepRegex.";
            throw sendError(response, 404, foundFilesStatement + " but none of them contain a line matching the grepRegex." + fixingStatement);
        }
        return grepMatched.stream();
    }

    protected static boolean isIgnored(Path path) {
        if (CoDeveloperEngine.IGNORE_FILES_PATTERN.matcher(path.toString()).matches()
                && !CoDeveloperEngine.OVERRIDE_IGNORE_PATTERN.matcher(path.toString()).matches()) {
            return true;
        }
        return gitIgnored(path);
    }

    /**
     * The URL it is deployed at, e.g. /listFiles.
     */
    public abstract String getUrl();

    /**
     * The OpenAPI description for this operation.
     */
    public abstract String openApiDescription();

    protected String getQueryParam(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    protected String getMandatoryQueryParam(HttpServletRequest request, HttpServletResponse response, String name) {
        String result = getQueryParam(request, name);
        if (null == result) {
            logInfo("Missing query parameter " + name + " in " + request.getRequestURI());
            throw sendError(response, 400, "Missing query parameter " + name);
        }
        return result;
    }

    /**
     * Returns the path parameter from the request, checks if it is within the current directory.
     */
    protected Path getPath(HttpServletRequest request, HttpServletResponse response, boolean mustExist) {
        String path = getMandatoryQueryParam(request, response, "path");
        if (CoDeveloperEngine.IGNORE_FILES_PATTERN.matcher(path).matches() && !CoDeveloperEngine.OVERRIDE_IGNORE_PATTERN.matcher(path).matches()) {
            throw sendError(response, 400, "Access to path " + path + " is not allowed! (matches " + CoDeveloperEngine.IGNORE_FILES_PATTERN.pattern() + ")");
        }
        Path resolved = CoDeveloperEngine.currentDir.resolve(path).normalize().toAbsolutePath();
        if (!resolved.startsWith(CoDeveloperEngine.currentDir)) {
            throw sendError(response, 400, "Path " + path + " is outside of current directory!");
        }
        if (mustExist && !Files.exists(resolved)) {
            String message = "Path " + path + " does not exist! Try to list files with /listFiles to find the right path.";
            String filename = resolved.getFileName().toString();
            List<Path> matchingFiles = findMatchingFiles(response, CoDeveloperEngine.currentDir, null, null)
                    .collect(toList());
            List<String> files = matchingFiles.stream()
                    .map(p -> CoDeveloperEngine.currentDir.relativize(p).toString())
                    .map(p -> Pair.of(p, StringUtils.getFuzzyDistance(p, filename, Locale.getDefault())))
                    .map(p -> Pair.of(p.getLeft(), -p.getRight()))
                    .sorted(Comparator.comparingDouble(Pair::getRight))
                    .limit(10)
                    .map(Pair::getLeft)
                    .collect(toList());
            if (!files.isEmpty()) {
                message += "\n\nDid you mean one of these files?\n" + String.join("\n", files);
                if (files.size() < matchingFiles.size()) {
                    message += "\n\n(suggestion list truncated - there are " + matchingFiles.size() + " files; use listFiles to find more files).";
                }
            }
            throw sendError(response, 404, message);
        }
        return resolved;
    }

    /**
     * Returns a parameter encoded in JSON the request body; returns "" if that parameter isn't there.
     */
    @Nullable
    protected String getBodyParameter(HttpServletResponse response, String json, String parameterName, boolean mandatory) {
        String parameterValue = null;
        if (!json.isEmpty() && !"{}".equals(json)) {
            try {
                Map<String, Object> decoded = gson.fromJson(json, Map.class);
                Object parameterObj = decoded.get(parameterName);
                parameterValue = parameterObj != null ? parameterObj.toString() : null;
                logBody(parameterName, parameterValue);
                if (mandatory && !decoded.containsKey(parameterName)) {
                    throw sendError(response, 400, "Missing parameter " + parameterName);
                }
            } catch (Exception e) {
                String error = "Parse error for content: " + e;
                throw sendError(response, 400, error);
            }
        }
        return parameterValue;
    }

    protected String mappedFilename(Path path) {
        return CoDeveloperEngine.currentDir.relativize(path).toString();
    }

    protected String abbreviate(String s, int max) {
        if (s == null || s.length() <= max) {
            return s;
        }
        return s.substring(0, max - 3) + "...";
    }

    protected boolean isNotEmpty(String s) {
        return null != s && !s.trim().isEmpty();
    }

    protected static Map<Path, GitIgnoreRules> gitIgnoreRules = new HashMap<>();

    protected static boolean gitIgnored(Path path) {
        Path parent = path.getParent();
        while (parent != null) {
            GitIgnoreRules gitIgnoreFile = gitIgnoreRules.get(parent);
            if (gitIgnoreFile == null) {
                gitIgnoreFile = new GitIgnoreRules(parent);
                gitIgnoreRules.put(parent, gitIgnoreFile);
            }
            if (gitIgnoreFile.isIgnored(path)) {
                return true;
            }
            parent = parent.getParent();
        }
        return false;
    }

    /**
     * Rules from the .gitignore file in the given directory - might be nothing if there is no .gitignore there.
     * Not quite complete, but should match the most common cases.
     */
    static class GitIgnoreRules {

        private final List<PathMatcher> rules = new ArrayList<>();
        private final Path directory;

        public GitIgnoreRules(Path directory) {
            this.directory = directory;
            Path gitignore = directory.resolve(".gitignore");
            if (Files.exists(gitignore)) {
                try {
                    List<String> lines = Files.readAllLines(gitignore);
                    for (String line : lines) {
                        if (line.startsWith("#")) {
                            continue;
                        }
                        if (line.isEmpty()) {
                            continue;
                        }
                        if (line.startsWith("!")) {
                            logError("Negated gitignore rules are not supported: " + line);
                            continue;
                        }
                        if (line.endsWith("/")) {
                            line += "**";
                        }
                        // that's a rough approximation, but should match most of the common cases. Sorry.
                        rules.add(directory.getFileSystem().getPathMatcher("glob:" + line));
                    }
                } catch (IOException e) {
                    throw new IllegalStateException("Error reading " + gitignore + " : " + e);
                }
            }
        }

        public boolean isIgnored(Path path) {
            Path relativePath = directory.relativize(path);
            return rules.stream().anyMatch(r -> r.matches(relativePath));
        }
    }
}
