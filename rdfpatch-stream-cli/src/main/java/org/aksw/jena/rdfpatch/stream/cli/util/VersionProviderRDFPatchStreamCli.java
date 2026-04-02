package org.aksw.jena.rdfpatch.stream.cli.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

public class VersionProviderRDFPatchStreamCli
    extends VersionProviderFromClasspathProperties
{
    protected final String PREFIX = "rdfpatch-stream-cli";
    @Override public String getResourceName() { return PREFIX + ".properties"; }
    @Override public Collection<String> getStrings(Properties p) {
        return Arrays.asList(p.get(PREFIX + ".version") + " built at " + p.get(PREFIX + ".build.timestamp"));
    }
}
