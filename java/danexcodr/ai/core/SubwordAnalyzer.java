package danexcodr.ai.core;

import java.util.*;

/**
 * Discovers morphological affixes (prefixes and suffixes) from the learned
 * symbol vocabulary in a fully emergent, data-driven way.  No linguistic
 * rules are hard-coded.  Any character sequence that appears as a shared
 * prefix or suffix across at least {@code MIN_AFFIX_FREQ} distinct vocabulary
 * entries, with a remaining stem of at least {@code MIN_STEM_LENGTH}
 * characters, is registered as a known affix.  Future input words are then
 * segmented at those boundaries, enabling the core pattern system to reason
 * at the sub-word level.
 *
 * <p>Affix tokens are decorated with a tilde marker so the rest of the
 * system can distinguish them from ordinary word symbols:
 * <ul>
 *   <li>prefix token: {@code "re~"}  (tilde at the right)</li>
 *   <li>suffix token: {@code "~ing"} (tilde at the left)</li>
 * </ul>
 *
 * <p>Compatible with Java 7.
 */
public class SubwordAnalyzer {

    /** Minimum characters an affix must contain to be considered. */
    static final int MIN_AFFIX_LENGTH = 2;

    /** Minimum characters that must remain as a stem after stripping the affix. */
    static final int MIN_STEM_LENGTH = 2;

    /**
     * Minimum number of distinct vocabulary words that must share a
     * prefix or suffix for it to be registered as a known affix.
     */
    static final int MIN_AFFIX_FREQ = 2;

    /** Marker character used to tag affix tokens. */
    public static final String AFFIX_MARKER = "~";

    // Qualified affixes, sorted longest-first for greedy matching.
    private List<String> knownPrefixes = new ArrayList<String>();
    private List<String> knownSuffixes = new ArrayList<String>();

    // Retained across calls to report only the newly discovered delta.
    private Set<String> prevPrefixSet = new HashSet<String>();
    private Set<String> prevSuffixSet = new HashSet<String>();

    /**
     * Scans the supplied vocabulary and refreshes the internal affix tables.
     * Should be called once after every learning batch.
     *
     * @param words current vocabulary (canonical symbol tokens)
     * @return sorted list of newly discovered affix display names
     *         (e.g. {@code ["re~", "~ing"]}); empty if nothing new was found
     */
    public List<String> analyzeVocabulary(Collection<String> words) {
        // Only consider plain (non-segmented) words long enough to yield a
        // valid affix AND a valid remaining stem.
        List<String> candidates = new ArrayList<String>();
        for (String w : words) {
            if (w != null
                    && !w.contains(AFFIX_MARKER)
                    && w.length() >= MIN_AFFIX_LENGTH + MIN_STEM_LENGTH) {
                candidates.add(w);
            }
        }

        Map<String, Integer> prefixCounts = new HashMap<String, Integer>();
        Map<String, Integer> suffixCounts = new HashMap<String, Integer>();

        for (String word : candidates) {
            int maxAffixLen = word.length() - MIN_STEM_LENGTH;
            for (int len = MIN_AFFIX_LENGTH; len <= maxAffixLen; len++) {
                String prefix = word.substring(0, len);
                Integer pc = prefixCounts.get(prefix);
                prefixCounts.put(prefix, pc == null ? 1 : pc + 1);

                String suffix = word.substring(word.length() - len);
                Integer sc = suffixCounts.get(suffix);
                suffixCounts.put(suffix, sc == null ? 1 : sc + 1);
            }
        }

        Set<String> qualPrefixes = new HashSet<String>();
        for (Map.Entry<String, Integer> e : prefixCounts.entrySet()) {
            if (e.getValue() >= MIN_AFFIX_FREQ) {
                qualPrefixes.add(e.getKey());
            }
        }

        Set<String> qualSuffixes = new HashSet<String>();
        for (Map.Entry<String, Integer> e : suffixCounts.entrySet()) {
            if (e.getValue() >= MIN_AFFIX_FREQ) {
                qualSuffixes.add(e.getKey());
            }
        }

        // Rebuild sorted lists — longest first for greedy segmentation.
        List<String> newPrefixes = new ArrayList<String>(qualPrefixes);
        Collections.sort(newPrefixes, new Comparator<String>() {
            public int compare(String a, String b) { return b.length() - a.length(); }
        });
        List<String> newSuffixes = new ArrayList<String>(qualSuffixes);
        Collections.sort(newSuffixes, new Comparator<String>() {
            public int compare(String a, String b) { return b.length() - a.length(); }
        });

        knownPrefixes = newPrefixes;
        knownSuffixes = newSuffixes;

        // Build the delta (only newly discovered affixes).
        List<String> newlyDiscovered = new ArrayList<String>();
        for (String p : qualPrefixes) {
            if (!prevPrefixSet.contains(p)) {
                newlyDiscovered.add(p + AFFIX_MARKER);
            }
        }
        for (String s : qualSuffixes) {
            if (!prevSuffixSet.contains(s)) {
                newlyDiscovered.add(AFFIX_MARKER + s);
            }
        }
        Collections.sort(newlyDiscovered);

        prevPrefixSet = qualPrefixes;
        prevSuffixSet = qualSuffixes;

        return newlyDiscovered;
    }

    /**
     * Segments a single word into subword tokens using greedy longest-match.
     * Returns a single-element list if no known affix applies.
     *
     * <p>Examples (assuming "re~" and "~ing" are known):
     * <ul>
     *   <li>"running"   → ["runn", "~ing"]</li>
     *   <li>"recount"   → ["re~", "count"]</li>
     *   <li>"replaying" → ["re~", "play", "~ing"]</li>
     * </ul>
     *
     * @param word the raw input token
     * @return list of one or more subword tokens
     */
    public List<String> segment(String word) {
        if (word == null
                || word.contains(AFFIX_MARKER)
                || word.length() < MIN_AFFIX_LENGTH + MIN_STEM_LENGTH) {
            return Collections.singletonList(word);
        }

        // Greedy longest-match prefix.
        String matchedPrefix = null;
        for (String prefix : knownPrefixes) {
            if (word.startsWith(prefix)
                    && word.length() - prefix.length() >= MIN_STEM_LENGTH) {
                matchedPrefix = prefix;
                break;
            }
        }

        // Greedy longest-match suffix.
        String matchedSuffix = null;
        for (String suffix : knownSuffixes) {
            if (word.endsWith(suffix)
                    && word.length() - suffix.length() >= MIN_STEM_LENGTH) {
                matchedSuffix = suffix;
                break;
            }
        }

        List<String> tokens = new ArrayList<String>();

        if (matchedPrefix != null && matchedSuffix != null) {
            int stemEnd = word.length() - matchedSuffix.length();
            if (matchedPrefix.length() < stemEnd) {
                // Both a prefix and a suffix fit without overlapping.
                tokens.add(matchedPrefix + AFFIX_MARKER);
                tokens.add(word.substring(matchedPrefix.length(), stemEnd));
                tokens.add(AFFIX_MARKER + matchedSuffix);
                return tokens;
            }
        }

        if (matchedPrefix != null) {
            tokens.add(matchedPrefix + AFFIX_MARKER);
            tokens.add(word.substring(matchedPrefix.length()));
            return tokens;
        }

        if (matchedSuffix != null) {
            tokens.add(word.substring(0, word.length() - matchedSuffix.length()));
            tokens.add(AFFIX_MARKER + matchedSuffix);
            return tokens;
        }

        return Collections.singletonList(word);
    }

    /**
     * Expands a token sequence by segmenting each word with known affixes.
     * Tokens that already carry the affix marker or match no known affix are
     * left unchanged.
     *
     * @param tokens the original token sequence
     * @return expanded sequence with subword units inserted in-place
     */
    public List<String> expandSequence(List<String> tokens) {
        List<String> expanded = new ArrayList<String>();
        for (String token : tokens) {
            expanded.addAll(segment(token));
        }
        return expanded;
    }

    /** Returns {@code true} when at least one affix has been discovered. */
    public boolean hasKnownAffixes() {
        return !knownPrefixes.isEmpty() || !knownSuffixes.isEmpty();
    }

    /**
     * Returns the known prefix strings (without the affix marker),
     * sorted longest first.
     */
    public List<String> getKnownPrefixes() {
        return Collections.unmodifiableList(knownPrefixes);
    }

    /**
     * Returns the known suffix strings (without the affix marker),
     * sorted longest first.
     */
    public List<String> getKnownSuffixes() {
        return Collections.unmodifiableList(knownSuffixes);
    }
}
