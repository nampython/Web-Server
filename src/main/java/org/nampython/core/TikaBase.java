package org.nampython.core;

import org.apache.tika.Tika;
import org.ioc.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TikaBase extends Tika {
    @Override
    public String detect(File file) throws IOException {
        return super.detect(file);
    }
}
