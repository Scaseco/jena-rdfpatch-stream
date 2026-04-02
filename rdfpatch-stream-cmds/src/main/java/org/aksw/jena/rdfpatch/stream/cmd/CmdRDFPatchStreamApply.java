package org.aksw.jena.rdfpatch.stream.cmd;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.aksw.jena.rdfpatch.stream.RawIter;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.iterator.IteratorCloseable;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "apply", description = "Apply a list of RDFStream patch files to base data.")
public class CmdRDFPatchStreamApply
    implements Callable<Integer> {

    @Option(names = { "-h", "--help" }, usageHelp = true)
    public boolean help = false;

    @Option(names = { "--check" }, description = "Don't produce output, just validate.")
    public boolean check = false;

    @Option(names = { "--quiet" }, description = "Suppress informational messages (implies --check).")
    public boolean quiet = false;

    @Parameters(arity = "1..*", description = "Input files. The first one is the base data, all further files are considered patches.")
    public List<String> nonOptionArgs = new ArrayList<>();

    @Override
    public Integer call() throws Exception {
        try {
            return callActual();
        } catch (RuntimeIOException e) {
            String msg = Optional.ofNullable(e.getCause()).map(Throwable::getMessage).orElse("").toLowerCase();
            if (msg.contains("broken pipe")) {
                // ignore
            } else {
                throw new IOException(e);
            }
        }
        return 1;
    }

    protected Integer callActual() throws Exception {
        IteratorCloseable<ByteBuffer> it = RawIter.applyPatch(nonOptionArgs.get(0), nonOptionArgs.subList(1, nonOptionArgs.size()));
        long counter[] = {0};
        if (check || quiet) {
            it.forEachRemaining(item -> { ++counter[0]; });
        } else {

            try {
                ByteBuffer nlBuf = ByteBuffer.wrap(new byte[] {'\n'});
                try (OutputStream out = StdIoUtils.openStdOutWithCloseShield();
                    WritableByteChannel channel = Channels.newChannel(out)) {
                    it.forEachRemaining(t -> {
                        ++counter[0];
                        try {
                            channel.write(t);
                            channel.write(nlBuf.duplicate());
                        } catch (IOException e) {
                            throw new RuntimeIOException(e);
                        }
                    });
                    out.flush(); // byte channel doesn't have flush.
                }
            } finally {
                it.close();
            }
        }

        if (!quiet) {
            System.err.println(counter[0]);
        }

        return 0;
    }
}
