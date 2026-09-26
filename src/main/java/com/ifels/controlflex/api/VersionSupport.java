package com.ifels.controlflex.api;

/**
 * Package-private version-string helpers.
 *
 * <p>This lives in a class rather than as a {@code private static} method on
 * {@link IControlFlexPlugin} because the API is published as <b>Java 8</b> bytecode and private
 * interface methods were only introduced in Java 9. Keeping it package-private also avoids adding
 * anything to the public API surface.
 */
final class VersionSupport {

    private VersionSupport() {}

    /** Parse a version segment; non-numeric segments are treated as 0. */
    static int parseVersionPart(String part) {
        try {
            return Integer.parseInt(part);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
