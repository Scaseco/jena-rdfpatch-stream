package org.aksw.jena.rdfpatch.stream;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.AbstractIterator;

import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfpatch.RDFChanges;
import org.apache.jena.rdfpatch.items.AddPrefix;
import org.apache.jena.rdfpatch.items.AddQuad;
import org.apache.jena.rdfpatch.items.ChangeItem;
import org.apache.jena.rdfpatch.items.DeletePrefix;
import org.apache.jena.rdfpatch.items.DeleteQuad;
import org.apache.jena.rdfpatch.items.HeaderItem;
import org.apache.jena.rdfpatch.items.Segment;
import org.apache.jena.rdfpatch.items.TxnAbort;
import org.apache.jena.rdfpatch.items.TxnBegin;
import org.apache.jena.rdfpatch.items.TxnCommit;
import org.apache.jena.rdfpatch.text.RDFPatchReaderText;
import org.apache.jena.riot.system.streammgr.StreamManager;

/**
 * Async iterator that parses RDF patch files and provides change items via a background thread.
 */
public class AsyncPatchIterator extends AbstractIterator<ChangeItem>
    implements IteratorCloseable<ChangeItem>
{
    /**
     * Creates an AsyncPatchIterator from a filename or URI.
     *
     * @param filenameOrURI The filename or URI of the patch file
     * @return An iterator over change items
     */
    public static IteratorCloseable<ChangeItem> of(String filenameOrURI) {
        InputStream in = StreamManager.get().open(filenameOrURI);
        return new AsyncPatchIterator(in);
    }

    /**
     * Queue for passing items from parser thread to consumer.
     */
    private final BlockingQueue<Object> queue = new ArrayBlockingQueue<>(1024);
    /**
     * Poison pill to signal end of data.
     */
    private final Object POISON = new Object();
    /**
     * Flag indicating if the iterator is closed.
     */
    private AtomicBoolean isClosed = new AtomicBoolean();
    /**
     * Background parser thread.
     */
    private final Thread parserThread;
    /**
     * Next item to return from computeNext().
     */
    private ChangeItem nextStep = null;

    /**
     * Creates a new AsyncPatchIterator that parses the given input stream.
     *
     * @param in The input stream to parse
     */
    public AsyncPatchIterator(InputStream in) {
        Objects.requireNonNull(in);
        this.parserThread = new Thread(() -> {
            try {
                try {
                    // Bridge pushes records into this iterator's queue
                    RDFChanges bridge = new PatchToChangesBridge();
                    new RDFPatchReaderText(in).apply(bridge);
                } catch (ClosedException e) {
                    // Clean termination requested by consumer - immediately make room for the poison.
                    // Implied: isClosed.get() == true
                    queue.clear();
                } catch (Throwable t) {
                    // Parser failed - but queue may be full of valid items.
                    // Try to append the throwable.
                    try {
                        queue.put(t);
                    } catch (InterruptedException e) {
                        // Async close - stop trying to put 't' onto the queue because the client is no longer interested.
                        queue.clear();
                    }
                }
            } finally {
                // Client is likely waiting on queue. We MUST wake him up and terminate with the POISON.
                while (true) {
                    try {
                        queue.put(POISON);
                    } catch (InterruptedException e) {
                        continue; // This operation MUST succeed, or the client will likely wait forever.
                    }
                    break;
                }
            }
        }, "RDF-Patch-Parser");
        this.parserThread.start();
    }

    /**
     * Internal bridge that translates "Push" events into "Pull" queue items.
     */
    private class PatchToChangesBridge implements RDFChanges {
        private void send(ChangeItem changeElt) {
            try {
                queue.put(changeElt);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ClosedException(); // Can only get interrupted by async call to close();
            }
        }

        @Override public void add(Node g, Node s, Node p, Node o) { send(new AddQuad(g, s, p, o)); }
        @Override public void delete(Node g, Node s, Node p, Node o) { send(new DeleteQuad(g, s, p, o)); }
        @Override public void txnBegin() { send(TxnBegin.object()); }
        @Override public void txnCommit() { send(TxnCommit.object()); }
        @Override public void txnAbort() { send(TxnAbort.object()); }
        @Override public void header(String field, Node value) { send(new HeaderItem(field, value)); }
        @Override public void addPrefix(Node gn, String prefix, String iri) { send(new AddPrefix(gn, prefix, iri)); }
        @Override public void deletePrefix(Node gn, String prefix) { send(new DeletePrefix(gn, prefix)); }
        @Override public void start() { }
        @Override public void finish() { }
        @Override public void segment() { send(new Segment()); }
    }

    @Override
    public ChangeItem computeNext() {
        if (isClosed.get()) {
            throw new ClosedException();
        }
        // Parser errors are put on the queue so that items and exceptions
        // are encountered in proper order.
        // if (error.get() != null) throw new RuntimeException("Parser thread failed", error.get());
        try {
            Object elt = queue.take();
            if (elt == POISON) {
                nextStep = endOfData();
            } else if (elt instanceof Throwable t) {
                throw new ItemException(t);
            } else {
                nextStep = (ChangeItem)elt;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return nextStep;
    }

//        @Override
//        public ChangeItem next() {
//            if (!hasNext()) throw new NoSuchElementException();
//            ChangeItem s = nextStep;
//            nextStep = null;
//            return s;
//        }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // parse thread should become interrupted - or the destination raises an exception
            // as a response, the parser places the POISON on the queue and the parser thread terminates.
            parserThread.interrupt();
        }
        try {
            parserThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Exception thrown when the iterator is closed.
     */
    public static class ClosedException extends RuntimeException {
        private ClosedException() {
        }
        private static final long serialVersionUID = 1L;
    }

    /**
     * Exception wrapping a parser error.
     */
    public static class ItemException extends RuntimeException {
        private ItemException() {
        }
        private static final long serialVersionUID = 1L;
        ItemException(Throwable cause) { super(cause); }
    }
}
