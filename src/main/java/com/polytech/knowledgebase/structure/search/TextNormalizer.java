package com.polytech.knowledgebase.structure.search;

import java.text.Normalizer;
import java.util.Locale;

public final class TextNormalizer {
    private TextNormalizer() {}
    public static String normalize(String text) {
        if (text == null) return "";
        return Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replace('ё', 'е').replaceAll("[^\\p{L}\\p{N}]+", " ").strip();
    }
    public static boolean containsPhrase(String normalizedText, String phrase) {
        String normalizedPhrase = normalize(phrase);
        return !normalizedPhrase.isEmpty() && (" " + normalizedText + " ").contains(" " + normalizedPhrase + " ");
    }
}
