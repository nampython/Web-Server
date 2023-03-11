package org.nampython.core.engine;

import com.cyecize.ioc.annotations.Autowired;
import com.cyecize.ioc.annotations.Service;
import org.jetbrains.annotations.NotNull;
import org.nampython.base.api.*;
import org.nampython.config.ConfigCenter;
import org.nampython.config.ConfigValue;
import org.nampython.core.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *  This class is a part of {@link RequestHandler}. The responsible for this class is to listen for incoming connections on
 *  configured TCP ports on the server and forwards requests to the {@link ResourceHandler} for processing and returning a response to the requesting client.
 *  Each request comes from the client need to know the  attributes. Set theses to {@link BaseHttpRequest}
 *  <ul>
 *      <li><b>HTTP request methods:</b> (GET, POST, PUT, DELETE...)</li>
 *      <li><b>Request URL</b> is made by a client, to a named host, which is located on a server. The aim of the request is to access a resource on the server.</li>
 *      <li><b>Query parameters</b> are a defined set of parameters attached to the end of a url.
 *      They are extensions of the URL that are used to help define specific content or actions based on the data being passed</li>
 *      <li><b>An HTTP request</b> header is a component of a network packet sent by a browser or client to the server to request for a specific page or data on the Web server</li>
 *      <li><b>Content-Length</b> is a number that indicates the size of the data in the body of the request or response in bytes.</li>
 *  </ul>
 *
 */
@Service
public class RequestProcessor implements RequestHandler {
    public static final String CONTENT_LENGTH;
    public static final String CACHE_CONTROL_HEADER_NAME;
    public static final String COOKIE_HEADER_NAME;
    public static final String TEXT_PLAIN;
    public static final String MULTIPART_FORM_DATA;
    public static final String RAW_BODY_PARAM_NAME;
    private static final String REQUEST_TOO_BIG_MSG = "Request too big.";

    static {
        CONTENT_LENGTH = "Content-Length";
        CACHE_CONTROL_HEADER_NAME = "Cache-Control";
        COOKIE_HEADER_NAME = "Cookie";
        TEXT_PLAIN = "text/plain";
        MULTIPART_FORM_DATA = "multipart/form-data";
        RAW_BODY_PARAM_NAME = "rawBodyText";
    }

    private final Map<FormDataParserProvider, FormDataParser> instanceProviderMap = new HashMap<>();
    private final ConfigCenter configCenter;
    private final ErrorHandling errorHandling;
    private final FormDataParser defaultFormDataParser;
    private final FormDataParser multipartFormDataParser;
    private final int maxRequestSize;

    @Autowired
    public RequestProcessor(List<FormDataParser> formDataParsers, ConfigCenter configCenter, ErrorHandling errorHandling, FormDataParserDefault defaultFormDataParser, FormDataParserMultipart multipartFormDataParser) {
        this.configCenter = configCenter;
        this.maxRequestSize = configCenter.getConfigValue(ConfigValue.MAX_REQUEST_SIZE, int.class);
        this.errorHandling = errorHandling;
        this.defaultFormDataParser = defaultFormDataParser;
        this.multipartFormDataParser = multipartFormDataParser;
    }

    @Override
    public void init() {
    }

    /**
     * @param inputStream {@link InputStream}
     * @param outputStream @{@link OutputStream}
     * @param sharedData  {@link RequestHandlerShareData}
     * @return - False if parse successfully or else
     * @throws IOException @exception
     */
    @Override
    public boolean handleRequest(InputStream inputStream, OutputStream outputStream, RequestHandlerShareData sharedData) throws IOException {
        try {
            final BaseHttpRequest baseHttpRequest = this.parseHttpRequest(inputStream);
            final BaseHttpResponse baseHttpResponse = new BaseHttpResponseImpl();
            sharedData.addObject(RequestHandlerShareData.HTTP_REQUEST, baseHttpRequest);
            sharedData.addObject(RequestHandlerShareData.HTTP_RESPONSE, baseHttpResponse);
        } catch (RequestTooBigException ex) {
            this.disposeInputStream(ex.getContentLength(), inputStream);
            return this.errorHandling.handleRequestTooBig(outputStream, ex, new BaseHttpResponseImpl());
        } catch (Exception e) {
            return this.errorHandling.handleException(outputStream, e, new BaseHttpResponseImpl(), HttpStatus.BAD_REQUEST);
        }
        return false;
    }

    /**
     * The purpose of this method is to read the input stream before closing it
     * otherwise the TCP connection will not be closed properly.
     */
    private void disposeInputStream(int length, InputStream inputStream) throws IOException {
        byte[] buffer = new byte[0];
        int leftToRead = length;
        int bytesRead = Math.min(2048, inputStream.available());
        while (leftToRead > 0) {
            buffer = inputStream.readNBytes(bytesRead);
            leftToRead -= bytesRead;
            bytesRead = Math.min(2048, inputStream.available());
        }
        buffer = null;
    }

    /**
     * Parses the HTTP request produced by the given stream.
     *
     * @param inputStream producing a HTTP request
     * @return HttpRequest
     */
    private BaseHttpRequest parseHttpRequest(InputStream inputStream) {
        try {
            final BaseHttpRequest baseHttpRequest = new BaseHttpRequestImpl();
            final List<String> requestMetadata = this.parseMetadataLines(inputStream, false);
            this.handlerMethodAndURL(requestMetadata.get(0), baseHttpRequest);
            this.handlerParamQuery(requestMetadata.get(0), baseHttpRequest);
            this.handlerHeader(requestMetadata, baseHttpRequest);
            this.handlerCookies(baseHttpRequest);
            this.handlerContentLength(inputStream, baseHttpRequest);
            if (baseHttpRequest.getContentLength() > this.maxRequestSize) {
                throw new RequestTooBigException(REQUEST_TOO_BIG_MSG, baseHttpRequest.getContentLength());
            } else {
                final String contentType = baseHttpRequest.getContentType();
                if (contentType != null && contentType.startsWith(MULTIPART_FORM_DATA)) {
                    this.multipartFormDataParser.parseBodyParams(inputStream, baseHttpRequest);
                } else {
                    this.defaultFormDataParser.parseBodyParams(inputStream, baseHttpRequest);
                }
                this.trimRequestPath(baseHttpRequest);
                return baseHttpRequest;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param request
     */
    private void trimRequestPath(BaseHttpRequest request) {
        request.setRequestURL(
                request.getRequestURL().replaceAll("\\.{2,}\\/?", "")
        );
    }

    /**
     * Content-Length header is a number that indicates the size of the data in the body of the request or response in bytes.
     * The HTTP body begins immediately after the first blank line, after the initial line and headers.
     * The actual length of the content sent over the network may differ from the size of the data in the body
     * because servers can compress the data before sending it.
     *
     * @param inputStream {@link InputStream}
     * @param request {@link BaseHttpRequest}
     * @throws IOException – if an I/O error occurs.
     */
    private void handlerContentLength(InputStream inputStream, BaseHttpRequest request) throws IOException {
        if (request.getHeaders().get(CONTENT_LENGTH) != null) {
            request.setContentLength(Integer.parseInt(request.getHeaders().get(CONTENT_LENGTH)));
        } else {
            request.setContentLength(inputStream.available());
        }
    }

    /**
     * An HTTP cookie (web cookie, browser cookie) is a small piece of data that a server sends to a user's web browser.
     * The browser may store the cookie and send it back to the same server with later requests. Typically, an HTTP cookie is used to tell if
     * two requests come from the same browser—keeping a user logged in, for example.
     * It remembers stateful information for the stateless HTTP protocol.
     *
     * @param baseHttpRequest {@link BaseHttpRequest}
     */
    private void handlerCookies(BaseHttpRequest baseHttpRequest) {
        if (baseHttpRequest.getHeaders().get(COOKIE_HEADER_NAME) != null) {
            String[] allCookies = baseHttpRequest.getHeaders().get(COOKIE_HEADER_NAME).split(";\\s");
            for (String cookieStr : allCookies) {
                final String[] cookieKeyValuePair = cookieStr.split("=");
                final String keyName = decode(cookieKeyValuePair[0]);
                final String value = cookieKeyValuePair.length > 1 ? decode(cookieKeyValuePair[1]) : null;
                baseHttpRequest.getCookies().put(keyName, new HttpCookieImpl(keyName, value));
            }
        }
    }

    /**
     * HTTP header fields are a list of strings sent and received by both the client program and server on every HTTP request and response.
     * These headers are usually invisible to the end-user and are only processed or logged by the server and client applications.
     * They define how information sent/received through the connection are encoded (as in Content-Encoding),
     * the session verification and identification of the client (as in browser cookies, IP address, user-agent) or their anonymity
     * thereof (VPN or proxy masking, user-agent spoofing), how the server should handle data (as in Do-Not-Track),
     * the age (the time it has resided in a shared cache) of the document being downloaded, amongst others.
     *
     * @param requestMetadata List of String metadata
     * @param baseHttpRequest {@link BaseHttpRequest}
     */
    private void handlerHeader(List<String> requestMetadata, BaseHttpRequest baseHttpRequest) {
        for (int i = 1; i < requestMetadata.size(); i++) {
            final String[] headerKeyValuePair = requestMetadata.get(i).split(":\\s+");
            final String key = headerKeyValuePair[0];
            final String value = headerKeyValuePair[1];
            baseHttpRequest.addHeader(key, value);
        }
    }

    /**
     * @param requestFirstLine {@link String}
     * @param baseHttpRequest {@link BaseHttpRequest}est
     */
    private void handlerMethodAndURL(String requestFirstLine, BaseHttpRequest baseHttpRequest) {
        baseHttpRequest.setMethod(requestFirstLine.split("\\s")[0]);
        baseHttpRequest.setRequestURL(URLDecoder.decode(
                requestFirstLine.split("[\\s\\?]")[1],
                StandardCharsets.UTF_8
        ));
    }

    /**
     * GET parameters (also called URL parameters or query strings) are used when a client, such as a browser,
     * requests a particular resource from a web server using the HTTP protocol.These parameters are usually name-value pairs, separated by an equals sign =.
     * For Example: <a href="http://localhost:8080/index.html?name=A&age=2">http://localhost:8080/index.html?name=A&age=2</a> we can see that requestURL is index.html?name=1&age=2
     * with the query parameter pair name = 1 and age = 2. We have to detach key-value of parameters and set them to request object.
     *
     * @param requestFirstLine {@link String}
     * @param baseHttpRequest {@link BaseHttpRequest}
     */
    private void handlerParamQuery(@NotNull String requestFirstLine, BaseHttpRequest baseHttpRequest) {
        String fullRequestURL = requestFirstLine.split("\\s")[1];
        String[] urlQueryParamPair = fullRequestURL.split("\\?");
        if (urlQueryParamPair.length >= 2) {
            for (int i = 1; i < urlQueryParamPair.length; i++) {
                String[] queryParamPairs = urlQueryParamPair[i].split("&");
                for (String paramPair : queryParamPairs) {
                    final String[] queryParamPair = paramPair.split("=");
                    final String key = decode(queryParamPair[0]);
                    final String value = queryParamPair.length > 1 ? decode(queryParamPair[1]) : null;
                    baseHttpRequest.getQueryParameters().put(key, value);
                }
            }
        }
    }

    /**
     * @param str {@link String}
     * @return - the newly decoded String
     */
    private static String decode(String str) {
        return URLDecoder.decode(str, StandardCharsets.UTF_8);
    }


    /**
     * @param inputStream {@link InputStream}
     * @param allowNewLineWithoutReturn - True if allow new line or else
     * @return all meta data
     * @throws CannotParseRequestException - If cannot parse the request
     */
    private List<String> parseMetadataLines(InputStream inputStream, boolean allowNewLineWithoutReturn)
            throws CannotParseRequestException {
        try {
            final List<String> metadataLines = new ArrayList<>();

            StringBuilder metadataBuilder = new StringBuilder();
            boolean wasNewLine = true;
            int lineNumber = 1;
            int readBytesCount = 0;
            int b;

            while ((b = inputStream.read()) >= 0) {
                readBytesCount++;
                if (b == '\r') {
                    // expect new-line
                    int next = inputStream.read();
                    if (next < 0 || next == '\n') {
                        lineNumber++;
                        if (wasNewLine) {
                            break;
                        }
                        metadataLines.add(metadataBuilder.toString());
                        if (next < 0) break;
                        metadataBuilder = new StringBuilder();
                        wasNewLine = true;
                    } else {
                        inputStream.close();
                        throw new CannotParseRequestException(
                                String.format("Illegal character after return on line %d.", lineNumber)
                        );
                    }
                } else if (b == '\n') {
                    if (!allowNewLineWithoutReturn) {
                        throw new CannotParseRequestException(
                                String.format("Illegal new-line character without preceding return on line %d.", lineNumber)
                        );
                    }

                    // unexpected, but let's accept new-line without returns
                    lineNumber++;
                    if (wasNewLine) {
                        break;
                    }
                    metadataLines.add(metadataBuilder.toString());
                    metadataBuilder = new StringBuilder();
                    wasNewLine = true;
                } else {
                    metadataBuilder.append((char) b);
                    wasNewLine = false;
                }
            }

            if (metadataBuilder.length() > 0) {
                metadataLines.add(metadataBuilder.toString());
            }

            if (readBytesCount < 2) {
                throw new CannotParseRequestException("Request is empty");
            }

//            if (this.showRequestLog) {
//                this.loggingService.info(String.join("\n", metadataLines));
//            }
            return metadataLines;
        } catch (IOException ex) {
            throw new CannotParseRequestException(ex.getMessage(), ex);
        }
    }


    /**
     * Run {@link RequestProcessor} first
     * @return - The order of {@link RequestProcessor} in {@link RequestHandler} implements
     */
    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

    enum FormDataParserProvider {
        DEFAULT(TEXT_PLAIN, FormDataParserDefault.class),
        MULTIPART(MULTIPART_FORM_DATA, FormDataParserDefault.class);
        private final String contentType;
        private final Class<? extends FormDataParser> parserType;

        FormDataParserProvider(String contentType, Class<? extends FormDataParser> parserType) {
            this.contentType = contentType;
            this.parserType = parserType;
        }

        public static FormDataParserProvider findByContentType(String contentType) {
            if (contentType != null) {
                for (FormDataParserProvider value : values()) {
                    if (value.contentType.startsWith(contentType)) {
                        return value;
                    }
                }
            }
            return DEFAULT;
        }

        public Class<? extends FormDataParser> getParserType() {
            return parserType;
        }
    }
}
