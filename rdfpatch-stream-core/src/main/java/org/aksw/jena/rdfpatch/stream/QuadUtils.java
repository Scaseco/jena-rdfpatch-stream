package org.aksw.jena.rdfpatch.stream;

import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;

/**
 * Utility methods for working with quads.
 */
public class QuadUtils {
    private QuadUtils() {
    }

    /**
     * Canonicalizes a graph node by converting null and default graph to the default graph IRI.
     *
     * @param g The graph node to canonicalize
     * @return The canonicalized graph node
     */
    public static Node canonicalize(Node g) {
        return g == null || Quad.isDefaultGraph(g) ? Quad.defaultGraphIRI : g;
    }

    /**
     * Canonicalizes a quad by converting its graph node to the default graph IRI if null or default.
     *
     * @param quad The quad to canonicalize
     * @return The canonicalized quad
     */
    public static Quad canonicalize(Quad quad) {
        return Quad.create(canonicalize(quad.getGraph()), quad.getSubject(), quad.getPredicate(), quad.getObject());
    }
}
