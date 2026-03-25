package danexcodr.ai.core;

import static danexcodr.ai.Config.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class ContentFinder {

  private OptionalFinder optionalFinder;
  private BigramAnalyzer bigramAnalyzer;
 
  
  // FIX: Cache the threshold so we don't re-learn or drift within the same batch
  private Double cachedThreshold = null; 

  public ContentFinder(OptionalFinder optionalFinder, BigramAnalyzer bigramAnalyzer) {
    this.optionalFinder = optionalFinder;
    this.bigramAnalyzer = bigramAnalyzer;
  }

  private static class Score implements Comparable<Score> {
    String token;
    double movementScore;
    double contextScore;
    double entropyScore;

    Score(String token, double movementScore, double contextScore, double entropyScore, double score) {
      this.token = token;
      this.movementScore = score;
      this.contextScore = contextScore;
      this.entropyScore = entropyScore;
    }

    @Override
    public int compareTo(Score other) {
      int scoreCompare = Double.compare(other.movementScore, this.movementScore);
      if (scoreCompare != 0) {
        return scoreCompare;
      }
      return this.token.compareTo(other.token);
    }

    @Override
    public int hashCode() {
      int result = 17;
      long temp = Double.doubleToLongBits(movementScore);
      result = 31 * result + (int) (temp ^ (temp >>> 32));
      result = 31 * result + (token != null ? token.hashCode() : 0);
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;

      Score that = (Score) obj;

      if (Double.compare(that.movementScore, this.movementScore) != 0) return false;
      if (this.token == null) {
        return that.token == null;
      } else {
        return this.token.equals(that.token);
      }
    }
  }

  public Set<String> identifyContentWords(List<List<String>> sequences, Set<String> learnedStructurals) {

    if (sequences == null || sequences.isEmpty()) {
        return new HashSet<String>();
    }

    Map<String, Set<Integer>> positions = new HashMap<String, Set<Integer>>();
    Map<String, Map<Integer, Integer>> positionCounts = new HashMap<String, Map<Integer, Integer>>();
    Map<String, Set<String>> leftContexts = new HashMap<String, Set<String>>();
    Map<String, Set<String>> rightContexts = new HashMap<String, Set<String>>();
    Set<String> all = new HashSet<String>();

    for (List<String> sequence : sequences) {
        all.addAll(sequence);
        for (int i = 0; i < sequence.size(); i++) {
            String token = sequence.get(i);
            if (!positions.containsKey(token)) {
                positions.put(token, new HashSet<Integer>());
            }
            positions.get(token).add(i);

            if (!positionCounts.containsKey(token)) {
                positionCounts.put(token, new HashMap<Integer, Integer>());
            }
            Map<Integer, Integer> counts = positionCounts.get(token);
            Integer current = counts.get(i);
            counts.put(i, (current == null) ? 1 : current + 1);

            if (i > 0) {
                if (!leftContexts.containsKey(token)) {
                    leftContexts.put(token, new HashSet<String>());
                }
                leftContexts.get(token).add(sequence.get(i - 1));
            }
            if (i < sequence.size() - 1) {
                if (!rightContexts.containsKey(token)) {
                    rightContexts.put(token, new HashSet<String>());
                }
                rightContexts.get(token).add(sequence.get(i + 1));
            }
        }
    }

    Set<String> structuralNeighborsOfOptionals = new HashSet<String>();
    if (!optionalFinder.getOptionals().isEmpty()) {
        for (List<String> sequence : sequences) {
            for (int i = 0; i < sequence.size(); i++) {
                String token = sequence.get(i);
                if (optionalFinder.getOptionals().contains(token)) {
                    if (i > 0) {
                        structuralNeighborsOfOptionals.add(sequence.get(i - 1));
                    }
                    if (i < sequence.size() - 1) {
                        structuralNeighborsOfOptionals.add(sequence.get(i + 1));
                    }
                }
            }
        }
    }
    all.removeAll(optionalFinder.getOptionals());

    if (all.isEmpty()) {
        return new HashSet<String>();
    }

    Set<String> bigramContentWords = bigramAnalyzer.runBigramAnalysis(sequences, all);
    if (!bigramContentWords.isEmpty()) {
        return bigramContentWords;
    }

    Set<String> swapStructuralWords = detectSwapStructuralWords(positions, all, sequences);

    double maxMovementCount = 1.0;
    double maxContextDiversity = 1.0;
    for (String token : all) {
        Set<Integer> poss = positions.get(token);
        int movementCount = (poss != null) ? poss.size() : 0;
        if (movementCount > maxMovementCount) {
            maxMovementCount = movementCount;
        }

        Set<String> left = leftContexts.get(token);
        Set<String> right = rightContexts.get(token);
        int contextDiversity = ((left != null) ? left.size() : 0) + ((right != null) ? right.size() : 0);
        if (contextDiversity > maxContextDiversity) {
            maxContextDiversity = contextDiversity;
        }
    }

    final List<Score> Scores = new ArrayList<Score>();
    for (String token : all) {
        Set<Integer> poss = positions.get(token);
        int movementCount = (poss != null) ? poss.size() : 0;
        double movementNorm = (maxMovementCount > 0) ? (movementCount / maxMovementCount) : 0.0;

        Set<String> left = leftContexts.get(token);
        Set<String> right = rightContexts.get(token);
        int contextDiversity = ((left != null) ? left.size() : 0) + ((right != null) ? right.size() : 0);
        double contextNorm = (maxContextDiversity > 0) ? (contextDiversity / maxContextDiversity) : 0.0;

        double entropyNorm = normalizedPositionEntropy(positionCounts.get(token));

        double score = (0.50 * movementNorm) + (0.30 * contextNorm) + (0.20 * entropyNorm);

        if (learnedStructurals != null && learnedStructurals.contains(token)) {
            score *= 0.5;
        }

        if (swapStructuralWords.contains(token)) {
            score *= 0.1;
        } else if (structuralNeighborsOfOptionals.contains(token) && movementCount <= 1) {
            score *= 0.1;
        }

        Scores.add(new Score(token, movementNorm, contextNorm, entropyNorm, score));
    }

    Collections.sort(Scores);
    double adaptiveThreshold = computeAdaptiveThreshold(Scores);
        
    // ========================================

    Set<String> contents = new LinkedHashSet<String>();
    for (Score ws : Scores) {
        if (ws.movementScore >= adaptiveThreshold) {
            contents.add(ws.token);
        } else {
            break;
        }
    }

    if (contents.size() > CONCEPT_LIMIT) {
        List<String> sortedByScore = new ArrayList<String>(contents);
        Collections.sort(sortedByScore, new Comparator<String>() {
            @Override
            public int compare(String w1, String w2) {
                double score1 = 0.0;
                double score2 = 0.0;
                for (Score ws : Scores) {
                    if (ws.token.equals(w1)) score1 = ws.movementScore;
                    if (ws.token.equals(w2)) score2 = ws.movementScore;
                }
                return Double.compare(score2, score1);
            }
        });
        
        Set<String> trimmed = new LinkedHashSet<String>();
        for (int i = 0; i < CONCEPT_LIMIT && i < sortedByScore.size(); i++) {
            trimmed.add(sortedByScore.get(i));
        }
        return trimmed;
    }

    return contents;
}

private double normalizedPositionEntropy(Map<Integer, Integer> countsByPosition) {
    if (countsByPosition == null || countsByPosition.size() <= 1) {
        return 0.0;
    }

    double total = 0.0;
    for (Integer count : countsByPosition.values()) {
        total += count;
    }
    if (total <= 0.0) {
        return 0.0;
    }

    double entropy = 0.0;
    for (Integer count : countsByPosition.values()) {
        if (count == null || count <= 0) continue;
        double p = count / total;
        entropy -= p * log2(p);
    }

    double maxEntropy = log2(countsByPosition.size());
    if (maxEntropy <= 0.0) {
        return 0.0;
    }
    return entropy / maxEntropy;
}

private double log2(double value) {
    return Math.log(value) / Math.log(2.0);
}

private double computeAdaptiveThreshold(List<Score> scores) {
    if (scores == null || scores.isEmpty()) {
        return MAX_SENSITIVITY;
    }

    List<Double> values = new ArrayList<Double>();
    for (Score s : scores) {
        values.add(s.movementScore);
    }
    Collections.sort(values);

    int idx = (int) Math.floor((values.size() - 1) * 0.5);
    if (idx < 0) idx = 0;
    if (idx >= values.size()) idx = values.size() - 1;

    double median = values.get(idx);
    return Math.max(MAX_SENSITIVITY, median);
}

/**
 * SIMPLE FIX: Only detect obvious swap structural tokens Words that DON'T move while other
 * tokens swap around them
 */
private Set<String> detectSwapStructuralWords(
    Map<String, Set<Integer>> positions, Set<String> all, List<List<String>> sequences) {
    Set<String> structuralWords = new HashSet<String>();

    // Only detect swaps when we have exactly 2 sequences of same length
    if (sequences.size() != 2 || sequences.get(0).size() != sequences.get(1).size()) {
        return structuralWords;
    }

    // Check if this is a clear swap pattern:
    // - Exactly 2 tokens have 2 poss each (they swap)
    // - Remaining tokens have 1 pos each (they're fixed)
    int swappingWords = 0;
    int fixedWords = 0;

    for (String token : all) {
        Set<Integer> poss = positions.get(token);
        if (poss != null) {
            if (poss.size() == 2) {
                swappingWords++;
            } else if (poss.size() == 1) {
                fixedWords++;
            }
        }
    }

    // Only apply swap detection if we have exactly 2 swapping tokens and at least 1 fixed token
    if (swappingWords == 2 && fixedWords >= 1) {
        for (String token : all) {
            Set<Integer> poss = positions.get(token);
            if (poss != null && poss.size() == 1) {
                structuralWords.add(token); // Fixed tokens in swap pattern are structural
            }
        }
    }

    return structuralWords;
}

  public Set<String> runParityFallback(List<List<String>> sequences) {
    if (sequences == null || sequences.isEmpty()) {
      return new HashSet<String>();
    }
    Map<String, Integer> oddCount = new HashMap<String, Integer>();
    Map<String, Integer> evenCount = new HashMap<String, Integer>();
    Set<String> allInSequences = new HashSet<String>();

    for (List<String> sequence : sequences) {
      allInSequences.addAll(sequence);
      for (int i = 0; i < sequence.size(); i++) {
        String token = sequence.get(i);

        if (i % 2 == 0) {
          Integer count = evenCount.get(token);
          evenCount.put(token, (count == null) ? 1 : count + 1);
        } else {
          Integer count = oddCount.get(token);
          oddCount.put(token, (count == null) ? 1 : count + 1);
        }
      }
    }

    allInSequences.removeAll(optionalFinder.getOptionals());

    Set<String> contents = new LinkedHashSet<String>();
    for (String token : allInSequences) {
      Integer odds = oddCount.get(token);
      Integer evens = evenCount.get(token);

      if (odds == null) odds = 0;
      if (evens == null) evens = 0;

      if (evens > odds) {
        contents.add(token);
      }
    }

    if (contents.size() > CONCEPT_LIMIT) {
      return new HashSet<String>();
    }

    return contents;
  }

  /**
   * Simple "Any Movement Check" - identifies content tokens based on ANY posal changes. This
   * catches cases where tokens swap or reorder without structural changes.
   *
   * @param sequences The equivalent sequences to analyze
   * @param learnedStructurals Previously identified structural tokens to downweight
   * @return Set of content tokens identified by posal movement
   */
  public Set<String> runAnyMovementCheck(
      List<List<String>> sequences, Set<String> learnedStructurals) {
    if (sequences == null || sequences.isEmpty()) {
      return new HashSet<String>();
    }

    Map<String, Set<Integer>> positions = new HashMap<String, Set<Integer>>();
    Set<String> all = new HashSet<String>();

    // Collect all token poss across sequences
    for (List<String> sequence : sequences) {
      all.addAll(sequence);
      for (int i = 0; i < sequence.size(); i++) {
        String token = sequence.get(i);
        if (!positions.containsKey(token)) {
          positions.put(token, new HashSet<Integer>());
        }
        positions.get(token).add(i);
      }
    }

    // Remove optional tokens from consideration
    all.removeAll(optionalFinder.getOptionals());

    if (all.isEmpty()) {
      return new HashSet<String>();
    }

    Set<String> contents = new LinkedHashSet<String>();

    // Core logic: ANY token that appears in multiple poss = content
    for (String token : all) {
      Set<Integer> poss = positions.get(token);
      if (poss != null && poss.size() > 1) {
        // This token MOVED = it's content
        contents.add(token);
      }
    }

    // Apply structural knowledge filter if available
    if (learnedStructurals != null) {
      contents.removeAll(learnedStructurals);
    }

    // Respect the maximum content tokens limit
    if (contents.size() > CONCEPT_LIMIT) {
      return new HashSet<String>();
    }

    return contents;
  }
}
