package org.aksw.jena.rdfpatch.stream.cmd;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.commons.io.output.CloseShieldOutputStream;

/* package */ class StdIoUtils {
    public static OutputStream openStdOutWithCloseShield() { return CloseShieldOutputStream.wrap(openStdOut()); }
    public static OutputStream openStdOut() { return new FileOutputStream(FileDescriptor.out); }
}
