package org.aksw.jena.rdfpatch.stream;

/**
 * A record representing a patch operation (ADD or DELETE) on a value.
 *
 * @param <T> The type of the value being patched
 * @param type The type of patch operation (ADDED or DELETED)
 * @param value The value being patched
 */
public record PatchRecord<T>(Type type, T value) {
    /**
     * Patch operation type.
     */
    public enum Type {
        /**
         * Indicates the value was added.
         */
        ADDED,
        /**
         * Indicates the value was deleted.
         */
        DELETED
    }

    /**
     * Creates a patch record for an added value.
     *
     * @param <T> The type of the value
     * @param value The value that was added
     * @return A patch record with type ADDED
     */
    public static <T> PatchRecord<T> add(T value) {
        return new PatchRecord<>(Type.ADDED, value);
    }

    /**
     * Creates a patch record for a deleted value.
     *
     * @param <T> The type of the value
     * @param value The value that was deleted
     * @return A patch record with type DELETED
     */
    public static <T> PatchRecord<T> delete(T value) {
        return new PatchRecord<>(Type.DELETED, value);
    }
}
