package org.aksw.jena.rdfpatch.stream.cmd;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.aksw.jena.rdfpatch.stream.PatchIter;
import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWriter;
import org.apache.jena.sparql.core.Quad;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "apply", description = "Apply a list of RDFStream patch files to base data.")
public class CmdRDFPatchStreamApplyOld
    implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Parameters(arity = "1..*", description = "Input files. The first one is the base data, all further files are considered patches.")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        IteratorCloseable<Quad> it = PatchIter.applyPatch(nonOptionArgs.get(0), nonOptionArgs.subList(1, nonOptionArgs.size()));
        try {
            try (OutputStream out = StdIoUtils.openStdOutWithCloseShield()) {
                StreamRDF sink = StreamRDFWriter.getWriterStream(out, Lang.NQUADS);
                sink.start();
                it.forEachRemaining(sink::quad);
                sink.finish();
            }
        } finally {
            it.close();
        }
        return 0;
    }
}
