package danexcodr.ai.core;

import static danexcodr.ai.Config.*;
import java.util.*;

public class ContentFinder {

  private OptionalFinder optionalFinder;
  private GramAnalyzer gramAnalyzer;

  public ContentFinder(OptionalFinder optionalFinder, GramAnalyzer gramAnalyzer) {
    this.optionalFinder = optionalFinder;
    this.gramAnalyzer = gramAnalyzer;
  }

  private static class Score implements Comparable<Score> {
    String token;
    double movementScore;

    Score(String token, double score) {
      this.token = token;
      this.movementScore = score;
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

  public Set<String> identifyContent(List<List<String>> sequences, Set<String> learnedStructurals) {

    if (sequences == null || sequences.isEmpty()) {
        return new HashSet<String>();
    }

    Map<String, Set<Integer>> positions = new HashMap<String, Set<Integer>>();
    Set<String> all = new HashSet<String>();

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

    Set<String> structuralNeighborsOfOptionals = new HashSet<String>();
    if (!optionalFinder.get().isEmpty()) {
        for (List<String> sequence : sequences) {
            for (int i = 0; i < sequence.size(); i++) {
                String token = sequence.get(i);
                if (optionalFinder.get().contains(token)) {
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
    all.removeAll(optionalFinder.get());

    if (all.isEmpty()) {
        return new HashSet<String>();
    }

    Set<String> gramContentTokens = gramAnalyzer.analyze(sequences, all);
    if (!gramContentTokens.isEmpty()) {
        return gramContentTokens;
    }

    Set<String> swapStructuralTokens = detectStructuralSwap(positions, all, sequences);

    double maxMovementCount = 1.0;
    for (String token : all) {
        Set<Integer> poss = positions.get(token);
        int movementCount = (poss != null) ? poss.size() : 0;
        if (movementCount > maxMovementCount) {
            maxMovementCount = movementCount;
        }
    }

    final List<Score> Scores = new ArrayList<Score>();
    for (String token : all) {
        Set<Integer> poss = positions.get(token);
        int movementCount = (poss != null) ? poss.size() : 0;
        double score = (maxMovementCount > 0) ? (movementCount / maxMovementCount) : 0.0;

        if (swapStructuralTokens.contains(token)) {
            score *= 0.1;
        } else if (structuralNeighborsOfOptionals.contains(token) && movementCount <= 1) {
            score *= 0.1;
        }

        Scores.add(new Score(token, score));
    }

    Collections.sort(Scores);
        
    Set<String> contents = new LinkedHashSet<String>();
    for (Score ws : Scores) {
        if (ws.movementScore >= MAX_SENSITIVITY) {
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

  /**
   * Only detect obvious swap structural tokens. Tokens that DON'T move while other
   * tokens swap around them.
   */
  private Set<String> detectStructuralSwap(
      Map<String, Set<Integer>> positions, Set<String> all, List<List<String>> sequences) {
    Set<String> structuralTokens = new HashSet<String>();

    if (sequences.size() != 2 || sequences.get(0).size() != sequences.get(1).size()) {
        return structuralTokens;
    }

    int swappingTokens = 0;
    int fixedTokens = 0;

    for (String token : all) {
        Set<Integer> poss = positions.get(token);
        if (poss != null) {
            if (poss.size() == 2) {
                swappingTokens++;
            } else if (poss.size() == 1) {
                fixedTokens++;
            }
        }
    }

    if (swappingTokens == 2 && fixedTokens >= 1) {
        for (String token : all) {
            Set<Integer> poss = positions.get(token);
            if (poss != null && poss.size() == 1) {
                structuralTokens.add(token);
            }
        }
    }

    return structuralTokens;
  }

  /**
   * Selects the two terms for the core relation.
   * Prioritizes self-relation when a token appears in multiple operand positions.
   */
  public String[] selectTermsByClosestCompanion(
      Set<String> contentTokens,
      List<List<String>> sequences,
      Map<String, Set<Integer>> tokenPositions,
      Set<String> structuralTokens) {
    
    if (contentTokens == null || contentTokens.isEmpty()) {
      return null;
    }
    
    // Step 1: Filter out tokens that are definitely structural
    Set<String> candidateTokens = new HashSet<String>(contentTokens);
    candidateTokens.removeAll(structuralTokens);
    candidateTokens.removeAll(optionalFinder.get());
    
    if (candidateTokens.isEmpty()) {
      candidateTokens = new HashSet<String>(contentTokens);
    }
    
    // Step 2: Check for self-relation candidates (tokens that appear multiple times in same sequence)
    String selfRelationToken = findSelfRelationCandidate(sequences, candidateTokens);
    
    if (selfRelationToken != null) {
      return new String[] {selfRelationToken, selfRelationToken};
    }
    
    // Step 3: Try to find two distinct tokens
    if (candidateTokens.size() >= 2) {
      List<String> tokenList = new ArrayList<String>(candidateTokens);
      Collections.sort(tokenList);
      
      // Prefer tokens that appear in both sequences as content
      String first = selectBestToken(tokenList, sequences);
      String second = selectBestTokenDifferent(tokenList, first, sequences);
      
      if (second != null) {
        return new String[] {first, second};
      }
      
      return new String[] {tokenList.get(0), tokenList.get(1)};
    }
    
    // Step 4: Single token fallback - use self-relation
    if (candidateTokens.size() == 1) {
      String token = candidateTokens.iterator().next();
      return new String[] {token, token};
    }
    
    return null;
  }

  /**
   * Finds a token that appears multiple times in the same sequence,
   * indicating it fills both operand slots (self-relation).
   */
  private String findSelfRelationCandidate(List<List<String>> sequences, Set<String> candidateTokens) {
    for (List<String> sequence : sequences) {
      Map<String, Integer> tokenCount = new HashMap<String, Integer>();
      for (String token : sequence) {
        if (candidateTokens.contains(token)) {
          Integer count = tokenCount.get(token);
          tokenCount.put(token, (count == null) ? 1 : count + 1);
        }
      }
      
      for (Map.Entry<String, Integer> entry : tokenCount.entrySet()) {
        // If a token appears more than once in the same sequence, it's a self-relation candidate
        if (entry.getValue() >= 2) {
          return entry.getKey();
        }
      }
    }
    return null;
  }

  /**
   * Selects the best token from the list (most frequent, appears in both sequences, etc.)
   */
  private String selectBestToken(List<String> tokens, List<List<String>> sequences) {
    if (tokens.isEmpty()) return null;
    
    // Score tokens based on how many sequences they appear in
    Map<String, Integer> sequenceCount = new HashMap<String, Integer>();
    for (String token : tokens) {
      int count = 0;
      for (List<String> seq : sequences) {
        if (seq.contains(token)) {
          count++;
        }
      }
      sequenceCount.put(token, count);
    }
    
    // Find token with highest sequence count
    String best = tokens.get(0);
    int bestScore = sequenceCount.get(best);
    for (String token : tokens) {
      int score = sequenceCount.get(token);
      if (score > bestScore) {
        bestScore = score;
        best = token;
      }
    }
    
    return best;
  }

  /**
   * Selects a token different from the first token.
   */
  private String selectBestTokenDifferent(List<String> tokens, String first, List<List<String>> sequences) {
    List<String> remaining = new ArrayList<String>(tokens);
    remaining.remove(first);
    
    if (remaining.isEmpty()) {
      return null;
    }
    
    return selectBestToken(remaining, sequences);
  }

  /**
   * Parity analysis - tokens that appear more often in even positions
   */
  public Set<String> parify(List<List<String>> sequences) {
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

    allInSequences.removeAll(optionalFinder.get());

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
   * Simple "Any Movement Check" - identifies content tokens based on ANY position changes.
   */
  public Set<String> checkMovement(
      List<List<String>> sequences, Set<String> learnedStructurals) {
    if (sequences == null || sequences.isEmpty()) {
      return new HashSet<String>();
    }

    Map<String, Set<Integer>> positions = new HashMap<String, Set<Integer>>();
    Set<String> all = new HashSet<String>();

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

    all.removeAll(optionalFinder.get());

    if (all.isEmpty()) {
      return new HashSet<String>();
    }

    Set<String> contents = new LinkedHashSet<String>();

    for (String token : all) {
      Set<Integer> poss = positions.get(token);
      if (poss != null && poss.size() > 1) {
        contents.add(token);
      }
    }

    if (learnedStructurals != null) {
      contents.removeAll(learnedStructurals);
    }

    if (contents.size() > CONCEPT_LIMIT) {
      return new HashSet<String>();
    }

    return contents;
  }
}