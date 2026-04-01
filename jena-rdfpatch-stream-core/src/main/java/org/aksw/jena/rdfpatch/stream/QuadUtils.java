package org.aksw.jena.rdfpatch.stream;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;

public class QuadUtils {
    public static Node canonicalize(Node g) {
        return g == null || Quad.isDefaultGraph(g) ? Quad.defaultGraphIRI : g;
    }

    public static Quad canonicalize(Quad quad) {
        return Quad.create(canonicalize(quad.getGraph()), quad.getSubject(), quad.getPredicate(), quad.getObject());
    }
}
