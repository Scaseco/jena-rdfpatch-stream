package org.aksw.jena.rdfpatch.stream;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.riot.system.streammgr.StreamManager;

public class RawIter {

    public static void main(String[] args) throws IOException {
        // IteratorCloseable<ByteBuffer> it = LineBytesIterator.ofByteBuffers(Path.of("/data/home/raven/Lab/rdfpatch-stream/wikidata-20250723-to-20250918-truthy-BETA.sorted.rdfp"));
        IteratorCloseable<PatchRecord<ByteBuffer>> it = patchIter(Path.of("/data/home/raven/Lab/rdfpatch-stream/wikidata-20250723-to-20250918-truthy-BETA.sorted.rdfp"));
        try {
            while (it.hasNext()) {
                ByteBuffer bb = it.next().value();
                String str = toString(bb, StandardCharsets.UTF_8);
                System.out.println(str);
            }
        } finally {
            it.close();
        }
    }

    public static IteratorCloseable<PatchRecord<ByteBuffer>> patchIter(Path path) throws IOException {
        IteratorCloseable<ByteBuffer> it = LineBytesIterator.ofByteBuffers(path);
        IteratorCloseable<PatchRecord<ByteBuffer>> result = (IteratorCloseable<PatchRecord<ByteBuffer>>)
            Iter.filter(
                Iter.map(it, line -> toRecord(line)),
                Objects::nonNull);
        return result;
    }

    public static IteratorCloseable<PatchRecord<ByteBuffer>> patchIter(InputStream in) throws IOException {
        IteratorCloseable<ByteBuffer> it = LineBytesIterator.ofByteBuffers(in);
        IteratorCloseable<PatchRecord<ByteBuffer>> result = (IteratorCloseable<PatchRecord<ByteBuffer>>)
            Iter.filter(
                Iter.map(it, line -> toRecord(line)),
                Objects::nonNull);
        return result;
    }

    public static int compareBytes(ByteBuffer a, ByteBuffer b) {
        int lenA = a.remaining(), lenB = b.remaining();
        int min = Math.min(lenA, lenB);
        int posA = a.position();
        int posB = b.position();
        for (int i = 0; i < min; i++) {
            int cmp = Integer.compare(Byte.toUnsignedInt(a.get(posA + i)),
                                   Byte.toUnsignedInt(b.get(posB + i)));
            if (cmp != 0) return cmp;
        }
        return Integer.compare(lenA, lenB);
    }

    public static String toString(ByteBuffer buf, Charset charset) {
        byte[] bytes = toByteArray(buf);
        String result = new String(bytes, charset);
        return result;
    }

    public static byte[] toByteArray(ByteBuffer buf) {
        byte[] result;

        // Save state (position, limit)
        int pos = buf.position();
        int limit = buf.limit();

        try {
            if (buf.hasArray()) {
                // Heap buffer: efficient — no copy (if you use slice semantics)
                int offset = buf.arrayOffset() + pos;
                result = Arrays.copyOfRange(buf.array(), offset, offset + (limit - pos));
            } else {
                // Direct buffer: must copy
                byte[] bytes = new byte[buf.remaining()];
                buf.get(bytes);
                result = bytes;
            }
        } finally {
            // Restore state
            buf.position(pos);
            buf.limit(limit);
        }
        return result;
    }

    public static PatchRecord<ByteBuffer> toRecord(ByteBuffer line) {
        PatchRecord<ByteBuffer> result;
        int n = line.remaining();
        if (n == 0) {
            result = null;
        } else if (n < 2) {
            throw new RuntimeException("At least to bytes expected per line - got: [" + toString(line, StandardCharsets.UTF_8) + "]");
        } else {
            int pos = line.position();
            byte a = line.get(pos);
            byte b = line.get(pos + 1);
            if (b == ' ') {
                if (a == 'A') {
                    ByteBuffer r = line.slice(2, line.remaining() - 2);
                    result = PatchRecord.add(r);
                } else if (a == 'D') {
                    ByteBuffer r = line.slice(2, line.remaining() - 2);
                    result = PatchRecord.delete(r);
                } else {
                    throw new RuntimeException("Unexpected line start: [" + toString(line, StandardCharsets.UTF_8) + "]");
                }
            } else {
                throw new RuntimeException("Unexpected line start: [" + toString(line, StandardCharsets.UTF_8) + "]");
            }
        }
        return result;
    }

    /**
     * Applies a list of patches to a base RDF file.
     *
     * @param baseFilenameOrUri The filename or URI of the base RDF file
     * @param patchFilenameOrURIs List of patch file paths
     * @return An iterator over the patched quads
     * @throws IOException
     */
    public static IteratorCloseable<ByteBuffer> applyPatch(String baseFilenameOrUri, List<String> patchFilenameOrURIs) throws IOException {
        StreamManager streamMgr = StreamManager.get();
        List<Iterator<PatchRecord<ByteBuffer>>> patchIts = new ArrayList<>(patchFilenameOrURIs.size());
        IteratorCloseable<ByteBuffer> baseIt = null;
        try {
            InputStream baseIn = streamMgr.open(baseFilenameOrUri);
            baseIt = LineBytesIterator.ofByteBuffers(baseIn);

            for (String f : patchFilenameOrURIs) {
                InputStream patchIn = streamMgr.open(f);
                Iterator<PatchRecord<ByteBuffer>> it = patchIter(patchIn);
                patchIts.add(it);
            }
            return applyPatch(baseIt, patchIts);
        } catch (Throwable t) {
            // Clean up in case of an error during setup.
            // XXX Handle exception during close.
            Iter.close(baseIt);
            patchIts.forEach(Iter::close);
            throw new IOException(t);
        }
    }

    /**
     * Applies patch records to a base iterator of quads.
     *
     * @param baseIt The base iterator of quads
     * @param patchIts List of patch record iterators
     * @return An iterator over the patched quads
     */
    public static IteratorCloseable<ByteBuffer> applyPatch(Iterator<ByteBuffer> baseIt, List<? extends Iterator<PatchRecord<ByteBuffer>>> patchIts) {
        return new PatchApplyIterator<>(baseIt, patchIts, RawIter::compareBytes, bb -> RawIter.toString(bb, StandardCharsets.UTF_8));
    }
}
