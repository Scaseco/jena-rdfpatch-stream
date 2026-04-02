package org.aksw.jena.rdfpatch.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.aksw.jena.rdfpatch.stream.PatchIter;
import org.aksw.jena.rdfpatch.stream.PatchRecord;
import org.aksw.jena.rdfpatch.stream.QuadUtils;
import org.apache.jena.atlas.iterator.Iter;
import org.apache.jena.atlas.iterator.IteratorCloseable;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.AsyncParser;
import org.apache.jena.sparql.core.Quad;

public class TestRdfPatchStream {

    private static void assertPatch(String expected, String base, String ...patches) {
        List<Quad> exp;
        try (Stream<Quad> stream = AsyncParser.of(expected).streamQuads()) {
            exp = stream.map(QuadUtils::canonicalize).toList();
        }

        Iterator<Quad> it = PatchIter.applyPatch(base, patches);
        List<Quad> actual = Iter.toList(Iter.map(it, QuadUtils::canonicalize));

        String expStr = exp.stream().map(NodeFmtLib::strNQ).collect(Collectors.joining("\n"));
        String actualStr = actual.stream().map(NodeFmtLib::strNQ).collect(Collectors.joining("\n"));

        assertEquals(expStr, actualStr);
    }

    @Test
    public void testReadRdfP() {
        IteratorCloseable<PatchRecord<Quad>> it = PatchIter.ofText("delete_all.rdfp");
        List<PatchRecord<Quad>> records = Iter.toList(it);
        assertEquals(5, records.size());
    }

    @Test
    public void testPatch1() {
        assertPatch("expected1.nq", "base.nq", "patch1.rdfp");
    }

    @Test
    public void testPatch2() {
        assertPatch("expected2.nq", "base.nq", "patch1.rdfp", "patch2.rdfp");
    }

    @Test
    public void test_deletAll() {
        assertPatch("empty.nq", "base.nq", "delete_all.rdfp");
    }
}
