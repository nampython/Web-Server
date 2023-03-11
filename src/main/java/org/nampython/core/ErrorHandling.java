package org.nampython.core;

import com.cyecize.ioc.annotations.Autowired;
import com.cyecize.ioc.annotations.Service;
import org.nampython.base.api.BaseHttpResponse;
import org.nampython.base.api.HttpStatus;
import org.nampython.config.ConfigCenter;
import org.nampython.config.ConfigValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Service for displaying exception messages to the browser.
 */
@Service
public class ErrorHandling{

    private boolean printStackTrace;

    @Autowired
    public ErrorHandling(ConfigCenter configCenter) {
        this.printStackTrace = configCenter.getConfigValue(ConfigValue.PRINT_EXCEPTIONS, boolean.class);
    }

    /**
     *
     * @param outputStream
     * @param ex
     * @param response
     * @return
     * @throws IOException
     */
    public boolean handleRequestTooBig(OutputStream outputStream, RequestTooBigException ex, BaseHttpResponse response)
            throws IOException {
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        this.writeException(outputStream, ex, response);

        return true;
    }

    /**
     *
     * @param outputStream
     * @param throwable
     * @param response
     * @return
     * @throws IOException
     */
    public boolean handleException(OutputStream outputStream, Throwable throwable, BaseHttpResponse response)
            throws IOException {
        return this.handleException(outputStream, throwable, response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     *
     * @param outputStream
     * @param throwable
     * @param response
     * @param status
     * @return
     * @throws IOException
     */
    public boolean handleException(OutputStream outputStream, Throwable throwable,
                                   BaseHttpResponse response, HttpStatus status) throws IOException {
        if (!this.printStackTrace) {
            return false;
        }

        response.setStatusCode(status);
        this.writeException(outputStream, throwable, response);

        return true;
    }

    /**
     *
     * @param outputStream
     * @param throwable
     * @param response
     * @throws IOException
     */
    private void writeException(OutputStream outputStream,
                                Throwable throwable, BaseHttpResponse response) throws IOException {
        final ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        throwable.printStackTrace(new PrintStream(byteOutputStream));
        response.setContent(byteOutputStream.toByteArray());
        outputStream.write(response.getBytes());
    }
}
