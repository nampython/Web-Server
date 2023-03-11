package org.nampython.config;


import org.nampython.StartUp;
import org.nampython.support.PathUtil;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.nampython.support.PathUtil.*;

/**
 *
 */
public class ConstantsPool {
    /**
     *
     */
    public static final int DEFAULT_SERVER_PORT = 8000;
    /**
     *
     */
    public static final int EMPTY_PORT = -1;
    /**
     *
     */
    public static final String WORKING_DIRECTORY;
    /**
     *
     */
    public static final String DEFAULT_CACHING_EXPRESSION = "image/png, image/gif, image/jpeg @ max-age=120 " +
            "& text/css @ max-age=84600, public " +
            "& application/javascript @ max-age=7200";


    static {
        try {
            final URI uri = StartUp.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            final Path path = Path.of(appendPath(Path.of(uri).toString(), "../")).normalize();
            WORKING_DIRECTORY = appendPath(path.toString(), "");
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

}
