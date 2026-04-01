package org.aksw.jena.rdfpatch.stream;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.graph.Node;
import org.apache.jena.rdfpatch.items.AddQuad;
import org.apache.jena.rdfpatch.items.ChangeItem;
import org.apache.jena.rdfpatch.items.DeleteQuad;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.AsyncParser;
import org.apache.jena.riot.system.streammgr.StreamManager;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.NodeCmp;

/**
 * Utility class for working with RDF patch iterators.
 */
public class PatchIter {
    private PatchIter() {
    }

//    public static final Comparator<Quad> COMPARATOR_QUAD = Comparator
//        .comparing(Quad::getSubject, NodeCmp::compareRDFTerms)
//        .thenComparing(Quad::getPredicate, NodeCmp::compareRDFTerms)
//        .thenComparing(Quad::getObject, NodeCmp::compareRDFTerms)
//        .thenComparing(Quad::getGraph, PatchIter::cmpGraph);

    /**
     * Comparator for quads based on their canonical string representation.
     */
    public static final Comparator<Quad> COMPARATOR_QUAD = Comparator
        .comparing(q -> {
            String str = NodeFmtLib.strNQ(QuadUtils.canonicalize(q));
            return str;
        }, String::compareTo);

    /**
     * Compares two graph nodes, treating null and default graph as equal.
     *
     * @param a First graph node
     * @param b Second graph node
     * @return Comparison result
     */
    public static int cmpGraph(Node a, Node b) {
        int result = (a == null || Quad.isDefaultGraph(a)) && (b == null || Quad.isDefaultGraph(b))
            ? 0
            : NodeCmp.compareRDFTerms(a, b);
        return result;
    }

    /**
     * Opens a safe iterator for quads from a filename or URI.
     *
     * @param filenameOrURI The filename or URI of the RDF file
     * @param lang The RDF language
     * @param base The base URI
     * @return An iterator over quads
     */
    public static IteratorCloseable<Quad> safeIteratorQuads(String filenameOrURI, Lang lang, String base) {
        InputStream in = StreamManager.get().open(filenameOrURI);
        return safeIteratorQuads(in, lang, base);
    }

    /**
     * Opens a safe iterator for quads from an input stream.
     *
     * @param in The input stream
     * @param lang The RDF language
     * @param base The base URI
     * @return An iterator over quads
     */
    public static IteratorCloseable<Quad> safeIteratorQuads(InputStream in, Lang lang, String base) {
         Iterator<Quad> parserIt = RDFDataMgr.createIteratorQuads(in, lang, base);
         return Iter.onCloseIO(parserIt, in);
    }

    /**
     * Applies one or more patches to a base RDF file.
     *
     * @param baseFilenameOrUri The filename or URI of the base RDF file
     * @param patchFilenameOrURIs Variable arguments of patch file paths
     * @return An iterator over the patched quads
     */
    public static IteratorCloseable<Quad> applyPatch(String baseFilenameOrUri, String ...patchFilenameOrURIs) {
        List<String> patches = List.of(patchFilenameOrURIs);
        return applyPatch(baseFilenameOrUri, patches);
    }

      /**
     * Applies a list of patches to a base RDF file.
     *
     * @param baseFilenameOrUri The filename or URI of the base RDF file
     * @param patchFilenameOrURIs List of patch file paths
     * @return An iterator over the patched quads
     */
    public static IteratorCloseable<Quad> applyPatch(String baseFilenameOrUri, List<String> patchFilenameOrURIs) {
        IteratorCloseable<Quad> baseIt = AsyncParser.of(baseFilenameOrUri).asyncParseQuads();

        List<Iterator<PatchRecord<Quad>>> patchIts = new ArrayList<>(patchFilenameOrURIs.size());
        for (String f : patchFilenameOrURIs) {
            Iterator<PatchRecord<Quad>> it = toDelta(AsyncPatchIterator.of(f));
            patchIts.add(it);
        }
        return applyPatch(baseIt, patchIts);
    }

     /**
     * Applies patch records to a base iterator of quads.
     *
     * @param baseIt The base iterator of quads
     * @param patchIts List of patch record iterators
     * @return An iterator over the patched quads
     */
    public static IteratorCloseable<Quad> applyPatch(Iterator<Quad> baseIt, List<? extends Iterator<PatchRecord<Quad>>> patchIts) {
        return new PatchApplyIterator<>(baseIt, patchIts, COMPARATOR_QUAD);
    }

     /**
     * Creates a patch iterator from a text patch file.
     *
     * @param filenameOrURI The filename or URI of the patch file
     * @return An iterator over patch records
     */
    public static IteratorCloseable<PatchRecord<Quad>> ofText(String filenameOrURI) {
        return PatchIter.toDelta(AsyncPatchIterator.of(filenameOrURI));
    }

    /**
     * Converts a ChangeItem iterator to a PatchRecord iterator.
     *
     * @param it The ChangeItem iterator
     * @return An iterator over patch records
     */
    public static IteratorCloseable<PatchRecord<Quad>> toDelta(Iterator<ChangeItem> it) {
        Objects.requireNonNull(it);

        return (IteratorCloseable<PatchRecord<Quad>>)Iter.map(
            Iter.filter(it, item -> item instanceof AddQuad || item instanceof DeleteQuad),
            item -> {
                if (item instanceof AddQuad add) {
                    return PatchRecord.add(Quad.create(add.g, add.s, add.p, add.o));
                } else if (item instanceof DeleteQuad del) {
                    return PatchRecord.delete(Quad.create(del.g, del.s, del.p, del.o));
                }

                throw new IllegalStateException("Should never come here");
            });
    }
}
