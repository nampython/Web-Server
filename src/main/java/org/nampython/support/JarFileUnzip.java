package org.nampython.support;

import java.io.File;
import java.io.IOException;

public interface JarFileUnzip {
    void unzipJar(File var1) throws IOException;
    void unzipJar(File var1, boolean var2) throws IOException;
    void unzipJar(File var1, boolean var2, String var3) throws IOException;
}
