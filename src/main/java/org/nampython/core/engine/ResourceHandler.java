package org.nampython.core.engine;

import org.ioc.stereotype.Autowired;
import org.ioc.stereotype.PostConstruct;
import org.ioc.stereotype.Service;
import org.nampython.base.api.BaseHttpRequest;
import org.nampython.base.api.BaseHttpResponse;
import org.nampython.base.api.HttpStatus;
import org.nampython.config.ConfigCenter;
import org.nampython.config.ConfigValue;
import org.nampython.core.*;
import org.nampython.support.PathUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 *  This class is a part of {@link RequestHandler}. The responsible for this class is to find suitable resource to back client.
 */
@Service
public class ResourceHandler implements RequestHandler {
    private static final String RESOURCE_NOT_FOUND_FORMAT = "Resource \"%s\" not found!";
    private final ConfigCenter configCenter;
    private final TikaBase tikaBase;
    private Map<String, String> mediaTypeCacheMap;
    private String pathToAssetsFormat;
    private String pathToWebappsFormat;
    private final List<String> appNames;

    @Autowired
    public ResourceHandler(ConfigCenter configCenter, TikaBase tikaBase) {
        this.configCenter = configCenter;
        this.tikaBase = tikaBase;
        this.appNames = new ArrayList<>();
    }

    @PostConstruct
    public void initialize() {
        this.appNames.addAll(List.of(this.configCenter.getConfigParamString(ConfigValue.MAIN_APP_JAR_NAME)));
        this.initDirectories();
    }

    /**
     *
     */
    @Override
    public void init() {
        this.mediaTypeCacheMap = CachingExpressingParser.parseExpression(this.configCenter.getConfigValue(ConfigValue.RESOURCE_CACHING_EXPRESSION));
    }

    /**
     * @param inputStream
     * @param outputStream
     * @param sharedData
     * @return
     * @throws IOException
     */
    @Override
    public boolean handleRequest(InputStream inputStream, OutputStream outputStream, RequestHandlerShareData sharedData) throws IOException {
        final BaseHttpRequest baseHttpRequest = sharedData.getObject(RequestHandlerShareData.HTTP_REQUEST, BaseHttpRequest.class);
        final BaseHttpResponse baseHttpResponse = sharedData.getObject(RequestHandlerShareData.HTTP_RESPONSE, BaseHttpResponse.class);
        try {
            final File resource = this.locateResource(baseHttpRequest.getRequestURL());
            try (final FileInputStream fileInputStream = new FileInputStream(resource)) {
                this.handleResourceFoundResponse(baseHttpRequest, baseHttpResponse, resource, fileInputStream.available());
                outputStream.write(baseHttpResponse.getBytes());
                this.transferStream(fileInputStream, outputStream);
            }
            return true;
        } catch (ResourceNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Looks for a resource in the webapps or in the assets directory.
     * @param requestURL - path to resource.
     * @return - file which name matches the request url.
     * @throws ResourceNotFoundException if the resource file cannot be found.
     */
    private File locateResource(String requestURL) throws ResourceNotFoundException {
        final String currentRequestAppName = this.getAppNameForRequest(requestURL);
        requestURL = requestURL.replaceFirst(Pattern.quote("/" + currentRequestAppName), "");
        File file = new File(this.createWebappsResourceDir(requestURL, currentRequestAppName));
        if (!file.exists() || file.isDirectory()) {
            file = new File(this.createAssetsResourceDir(requestURL, currentRequestAppName));
        }
        if (file.exists() && !file.isDirectory()) {
            return file;
        }
        throw new ResourceNotFoundException(String.format(RESOURCE_NOT_FOUND_FORMAT, requestURL));
    }

    /**
     *
     * @param requestURL
     * @param appName
     * @return
     */
    private String createWebappsResourceDir(String requestURL, String appName) {
        return String.format(this.pathToWebappsFormat, PathUtil.trimAllSlashes(appName), PathUtil.trimAllSlashes(requestURL)
        );
    }

    /**
     *
     * @param requestURL
     * @param appName
     * @return
     */
    private String createAssetsResourceDir(String requestURL, String appName) {
        return String.format(
                this.pathToAssetsFormat,
                PathUtil.trimAllSlashes(appName),
                PathUtil.trimAllSlashes(requestURL)
        );
    }

    /**
     * @param requestURL
     * @return
     */
    private String getAppNameForRequest(String requestURL) {
        Iterator<String> appNames = this.appNames.iterator();
        String appName;
        do {
            if (!appNames.hasNext()) {
                return this.configCenter.getConfigValue(ConfigValue.MAIN_APP_JAR_NAME);
            }
            appName = appNames.next();
        } while (!requestURL.startsWith("/" + appName));
        return appName;
//        for (String appName : this.appNames) {
//            if (requestURL.startsWith("/" + appName)) {
//                return appName;
//            }
//        }
//        return this.configCenter.getConfigValue(ConfigValue.MAIN_APP_JAR_NAME);
    }

    /**
     *
     */
    private void initDirectories() {
        final String workingDir = this.configCenter.getConfigValue(ConfigValue.JAVACHE_WORKING_DIRECTORY);
        this.getPathToAssets(workingDir);
        this.getPathToWebApps(workingDir);
    }

    /**
     *
     * @param workingDir
     */
    private void getPathToAssets(String workingDir) {
        String pathToAssets = PathUtil.appendPath(workingDir, this.configCenter.getConfigValue(ConfigValue.ASSETS_DIR_NAME)
        );
        pathToAssets = PathUtil.appendPath(pathToAssets, "%s");
        pathToAssets = PathUtil.appendPath(pathToAssets, "%s");
        this.pathToAssetsFormat = pathToAssets;
    }


    /**
     *
     * @param workingDir
     */
    private void getPathToWebApps(String workingDir) {
        String pathToWebApps = PathUtil.appendPath(workingDir, this.configCenter.getConfigValue(ConfigValue.WEB_APPS_DIR_NAME));
        pathToWebApps = PathUtil.appendPath(pathToWebApps, "%s");
        pathToWebApps = PathUtil.appendPath(pathToWebApps, this.configCenter.getConfigValue(ConfigValue.APP_COMPILE_OUTPUT_DIR_NAME));
        pathToWebApps = PathUtil.appendPath(pathToWebApps, this.configCenter.getConfigValue(ConfigValue.APP_RESOURCES_DIR_NAME));
        pathToWebApps = PathUtil.appendPath(pathToWebApps, "%s");
        this.pathToWebappsFormat = pathToWebApps;
    }

    /**
     * @param inputStream
     * @param outputStream
     * @throws IOException
     */
    private void transferStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        final byte[] buffer = new byte[2048];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
    }


    /**
     * @return
     */
    @Override
    public int order() {
        return this.configCenter.getConfigValue(ConfigValue.RESOURCE_HANDLER_ORDER.name(), int.class);
    }

    /**
     * Populates {@link BaseHttpResponse} with found resource.
     * Adds necessary headers that are required in order to transfer a resource using the HTTP protocol.
     * In this case I use Tika library that is used for document type detection and content extraction from various file formats.
     * To know more about Tika library, accesses via link <a href="https://tika.apache.org/">https://tika.apache.org/</a>
     * to research and know more about this library
     */
    private void handleResourceFoundResponse(BaseHttpRequest request, BaseHttpResponse response, File resourceFile, long fileSize) throws IOException {
        final String mediaType = this.tikaBase.detect(resourceFile);
        response.setStatusCode(HttpStatus.OK);
        response.addHeader("Content-Type", mediaType);
        response.addHeader("Content-Length", fileSize + "");
        response.addHeader("Content-Disposition", "inline");
        this.addCachingHeader(request, response, mediaType);
    }

    /**
     * Adds caching header to the given response.
     * <p>
     * If caching is not enabled or the caching header is already present, do nothing.
     * <p>
     * Uses Cache-Control header to set up caching options.
     * Cache-Control header value from the request is prioritized if present,
     * otherwise value from the config for that specific media type will be used (if present)
     *
     * @param request       - current request.
     * @param response      - current response.
     * @param fileMediaType - current file media type.
     */
    public void addCachingHeader(BaseHttpRequest request, BaseHttpResponse response, String fileMediaType) {
        if (!this.isCachingEnabled() || this.hasCacheHeader(response)) {
            return;
        }
        String responseCacheControl = request.getHeader(RequestProcessor.CACHE_CONTROL_HEADER_NAME);
        if (responseCacheControl == null && this.mediaTypeCacheMap.containsKey(fileMediaType)) {
            responseCacheControl = this.mediaTypeCacheMap.get(fileMediaType);
        }
        if (responseCacheControl != null) {
            response.addHeader(RequestProcessor.CACHE_CONTROL_HEADER_NAME, responseCacheControl);
        }
    }

    /**
     * @return
     */
    private boolean isCachingEnabled() {
        return this.configCenter.getConfigValue(ConfigValue.ENABLE_RESOURCE_CACHING, boolean.class);
    }

    /**
     * @param response
     * @return
     */
    private boolean hasCacheHeader(BaseHttpResponse response) {
        return response.getHeaders().containsKey(RequestProcessor.CACHE_CONTROL_HEADER_NAME);
    }


    /**
     * "image/png, image/gif, image/jpeg @ max-age=120 & text/css @ max-age=84600, public & application/javascript @ max-age=7200";
     * Cache-control is an HTTP header used to specify browser caching policies in both client requests and server responses.
     * Policies include how a resource is cached, where it’s cached and its maximum age before expiring
     */
    static class CachingExpressingParser {
        public static Map<String, String> parseExpression(String expressionString) {
            final Map<String, String> mediaTypeCacheMap = new HashMap<>();
            final String[] expressions = expressionString.split("\\s*&\\s*");
            try {
                for (String expression : expressions) {
                    final String[] tokens = expression.split("\\s*@\\s*");
                    final String headerValue = tokens[1].trim();
                    final String[] mediaTypes = tokens[0].split(",\\s*");
                    for (String mediaType : mediaTypes) {
                        mediaTypeCacheMap.put(mediaType.trim(), headerValue);
                    }
                }
            } catch (Exception ex) {
                throw new CannotParseExpressionException(
                        String.format("Cannot parse caching expression '%s', check the syntax.", expressionString),
                        ex
                );
            }
            return mediaTypeCacheMap;
        }
    }
}
