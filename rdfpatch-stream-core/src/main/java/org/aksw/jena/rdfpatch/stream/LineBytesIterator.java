package org.aksw.jena.rdfpatch.stream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.google.common.collect.AbstractIterator;

import org.apache.commons.io.IOUtils;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.iterator.IteratorCloseable;

public class LineBytesIterator extends AbstractIterator<byte[]> implements IteratorCloseable<byte[]> {
    private ReadableByteChannel channel;
    private ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 8).limit(0);
    private ByteArrayOutputStream lineBuf = new ByteArrayOutputStream();

    protected LineBytesIterator(ReadableByteChannel channel) {
        super();
        this.channel = channel;
    }

    public static IteratorCloseable<byte[]> of(ReadableByteChannel channel) throws IOException {
        return new LineBytesIterator(channel);
    }

    public static IteratorCloseable<byte[]> of(Path path) throws IOException {
        FileChannel channel = FileChannel.open(path, StandardOpenOption.READ);
        return of(channel);
    }

    public static IteratorCloseable<byte[]> of(InputStream in) throws IOException {
        ReadableByteChannel channel = Channels.newChannel(in);
        return of(channel);
    }

    public static IteratorCloseable<ByteBuffer> ofByteBuffers(Path path) throws IOException {
        IteratorCloseable<byte[]> base = of(path);
        return (IteratorCloseable<ByteBuffer>)Iter.map(base, ByteBuffer::wrap);
    }

    public static IteratorCloseable<ByteBuffer> ofByteBuffers(InputStream in) throws IOException {
        IteratorCloseable<byte[]> base = of(in);
        return (IteratorCloseable<ByteBuffer>)Iter.map(base, ByteBuffer::wrap);
    }

    @Override
    protected byte[] computeNext() {
        byte[] result;
        outer: while (true) {
            while (buf.hasRemaining()) {
                byte b = buf.get();
                if (b == '\n') {
                    result = lineBuf.toByteArray();
                    lineBuf.reset();
                    break outer;
                }
                lineBuf.write(b);
            }
            buf.compact();
            try {
                int n = channel.read(buf);
                if (n == -1) {
                    if (lineBuf.size() > 0) {
                        result = lineBuf.toByteArray();
                        lineBuf.reset();
                    } else {
                        result = endOfData();
                    }
                    break;
                }
                buf.flip();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    @Override
    public void close() {
        IOUtils.closeQuietly(channel);
    }
}
