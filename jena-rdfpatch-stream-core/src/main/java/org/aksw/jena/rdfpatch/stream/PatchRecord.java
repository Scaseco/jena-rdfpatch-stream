package org.aksw.jena.rdfpatch.stream;

public record PatchRecord<T>(Type type, T value) {
    public enum Type {
        ADDED, DELETED
    }

    public static <T> PatchRecord<T> add(T value) {
        return new PatchRecord<>(Type.ADDED, value);
    }

    public static <T> PatchRecord<T> delete(T value) {
        return new PatchRecord<>(Type.DELETED, value);
    }
}
