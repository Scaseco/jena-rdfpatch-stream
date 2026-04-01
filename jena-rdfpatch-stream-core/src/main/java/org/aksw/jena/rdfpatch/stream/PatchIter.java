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

public class PatchIter {
//    public static final Comparator<Quad> COMPARATOR_QUAD = Comparator
//        .comparing(Quad::getSubject, NodeCmp::compareRDFTerms)
//        .thenComparing(Quad::getPredicate, NodeCmp::compareRDFTerms)
//        .thenComparing(Quad::getObject, NodeCmp::compareRDFTerms)
//        .thenComparing(Quad::getGraph, PatchIter::cmpGraph);

    public static final Comparator<Quad> COMPARATOR_QUAD = Comparator
        .comparing(q -> {
            String str = NodeFmtLib.strNQ(QuadUtils.canonicalize(q));
            return str;
        }, String::compareTo);

    public static int cmpGraph(Node a, Node b) {
        int result = (a == null || Quad.isDefaultGraph(a)) && (b == null || Quad.isDefaultGraph(b))
            ? 0
            : NodeCmp.compareRDFTerms(a, b);
        return result;
    }

    public static IteratorCloseable<Quad> safeIteratorQuads(String filenameOrURI, Lang lang, String base) {
        InputStream in = StreamManager.get().open(filenameOrURI);
        return safeIteratorQuads(in, lang, base);
    }

    public static IteratorCloseable<Quad> safeIteratorQuads(InputStream in, Lang lang, String base) {
         Iterator<Quad> parserIt = RDFDataMgr.createIteratorQuads(in, lang, base);
         return Iter.onCloseIO(parserIt, in);
    }

    public static IteratorCloseable<Quad> applyPatch(String baseFilenameOrUri, String ...patchFilenameOrURIs) {
        List<String> patches = List.of(patchFilenameOrURIs);
        return applyPatch(baseFilenameOrUri, patches);
    }

    public static IteratorCloseable<Quad> applyPatch(String baseFilenameOrUri, List<String> patchFilenameOrURIs) {
        IteratorCloseable<Quad> baseIt = AsyncParser.of(baseFilenameOrUri).asyncParseQuads();

        List<Iterator<PatchRecord<Quad>>> patchIts = new ArrayList<>(patchFilenameOrURIs.size());
        for (String f : patchFilenameOrURIs) {
            Iterator<PatchRecord<Quad>> it = toDelta(AsyncPatchIterator.of(f));
            patchIts.add(it);
        }
        return applyPatch(baseIt, patchIts);
    }

    public static IteratorCloseable<Quad> applyPatch(Iterator<Quad> baseIt, List<? extends Iterator<PatchRecord<Quad>>> patchIts) {
        return new PatchApplyIterator<>(baseIt, patchIts, COMPARATOR_QUAD);
    }

    public static IteratorCloseable<PatchRecord<Quad>> ofText(String filenameOrURI) {
        return PatchIter.toDelta(AsyncPatchIterator.of(filenameOrURI));
    }

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
