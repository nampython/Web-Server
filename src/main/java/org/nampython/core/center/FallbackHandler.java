package org.nampython.core.center;

import com.cyecize.ioc.annotations.Service;
import org.nampython.base.api.BaseHttpResponse;
import org.nampython.base.api.HttpStatus;
import org.nampython.core.RequestHandler;
import org.nampython.core.RequestHandlerShareData;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@Service
public class FallbackHandler implements RequestHandler {

    @Override
    public void init() {

    }

    @Override
    public boolean handleRequest(InputStream inputStream, OutputStream outputStream, RequestHandlerShareData sharedData) throws IOException {
        final BaseHttpResponse response = sharedData.getObject(RequestHandlerShareData.HTTP_RESPONSE, BaseHttpResponse.class);
        response.setStatusCode(HttpStatus.NOT_FOUND);
        response.setContent("The resource you are looking for could not be found!");
        outputStream.write(response.getBytes());
        return true;
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }
}
