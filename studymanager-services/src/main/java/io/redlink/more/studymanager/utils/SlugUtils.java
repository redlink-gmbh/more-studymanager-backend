package io.redlink.more.studymanager.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtils {

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    /** Regex for a valid slug: lowercase letters, numbers, and single hyphens only */
    private static final Pattern VALID_SLUG = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");
    /**
     * Converts a string into a URL-friendly slug.
     * Example: "Hello World! How are you? Café" → "hello-world-how-are-you-cafe"
     */
    public static String toSlug(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }

        String slug = input.trim();

        // Normalize diacritics (é → e, ç → c, etc.)
        slug = Normalizer.normalize(slug, Normalizer.Form.NFD);
        slug = slug.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        // Convert to lowercase
        slug = slug.toLowerCase(Locale.ENGLISH);

        // Replace whitespace with hyphens
        slug = WHITESPACE.matcher(slug).replaceAll("-");

        // Remove all non-word characters (except hyphens)
        slug = NON_LATIN.matcher(slug).replaceAll("");

        // Collapse multiple hyphens
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");

        // Remove leading/trailing hyphens
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }

    /**
     * Checks if a string is a valid slug.
     * A valid slug must:
     *   - Contain only lowercase letters (a-z), digits (0-9), and hyphens (-)
     *   - Not start or end with a hyphen
     *   - Not contain consecutive hyphens
     *   - Not be empty
     */
    public static boolean isSlug(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return VALID_SLUG.matcher(str).matches();
    }
}
