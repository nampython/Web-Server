package org.nampython.base.api;

import java.io.InputStream;

public interface MultipartFile {

    long getFileLength();

    String getContentType();

    String getFileName();

    String getFieldName();

    InputStream getInputStream();

    byte[] getBytes();
}
