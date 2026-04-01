package danexcodr.ai.core;

import java.util.*;

public class GramAnalyzer {

  public Set<String> analyze(List<List<String>> sequences, Set<String> candidateTokens) {
    Set<String> contentTokens = new HashSet<String>();
    
    // Only works with exactly 2 sequences for swap detection
    if (sequences.size() != 2) {
      return contentTokens;
    }
    
    List<String> seq1 = sequences.get(0);
    List<String> seq2 = sequences.get(1);
    
    if (seq1.size() != seq2.size()) {
      return contentTokens;
    }
    
    // Check if this is a true swap pattern:
    // 1. All candidate tokens appear in both sequences
    // 2. Every token's position changed (all moved)
    // 3. No token appears twice in the same sequence (not self-relation)
    
    Map<String, List<Integer>> positions1 = buildPositionMap(seq1);
    Map<String, List<Integer>> positions2 = buildPositionMap(seq2);
    
    int tokensMoved = 0;
    int totalCandidateTokens = 0;
    boolean hasSelfRelation = false;
    
    for (String token : candidateTokens) {
      List<Integer> pos1 = positions1.get(token);
      List<Integer> pos2 = positions2.get(token);
      
      if (pos1 != null && pos2 != null) {
        totalCandidateTokens++;
        
        // Check if token appears twice in same sequence (self-relation)
        if (pos1.size() > 1 || pos2.size() > 1) {
          hasSelfRelation = true;
        }
        
        // Check if positions changed
        if (!positionsEqual(pos1, pos2)) {
          tokensMoved++;
        }
      }
    }
    
    // Only trigger if:
    // - All candidate tokens moved
    // - No self-relation (no token appears twice in same sequence)
    // - At least 2 candidate tokens
    if (!hasSelfRelation && totalCandidateTokens >= 2 && tokensMoved == totalCandidateTokens) {
      // Find the smallest gram that captures the movement
      int length = seq1.size();
      for (int gramSize = length; gramSize >= 1; gramSize--) {
        Map<String, List<Integer>> grams1 = buildGramMap(seq1, gramSize);
        Map<String, List<Integer>> grams2 = buildGramMap(seq2, gramSize);
        
        Set<String> movingGrams = new HashSet<String>();
        for (Map.Entry<String, List<Integer>> entry : grams1.entrySet()) {
          String gram = entry.getKey();
          List<Integer> pos1List = entry.getValue();
          List<Integer> pos2List = grams2.get(gram);
          
          if (pos2List != null && !positionsEqual(pos1List, pos2List)) {
            movingGrams.add(gram);
          }
        }
        
        if (!movingGrams.isEmpty()) {
          for (String gram : movingGrams) {
            String[] tokens = gram.split("\\|");
            for (String token : tokens) {
              if (candidateTokens.contains(token)) {
                contentTokens.add(token);
              }
            }
          }
          break;
        }
      }
    }
    
    return contentTokens;
  }
  
  private Map<String, List<Integer>> buildPositionMap(List<String> sequence) {
    Map<String, List<Integer>> posMap = new HashMap<String, List<Integer>>();
    for (int i = 0; i < sequence.size(); i++) {
      String token = sequence.get(i);
      List<Integer> positions = posMap.get(token);
      if (positions == null) {
        positions = new ArrayList<Integer>();
        posMap.put(token, positions);
      }
      positions.add(i);
    }
    return posMap;
  }
  
  private boolean positionsEqual(List<Integer> pos1, List<Integer> pos2) {
    if (pos1.size() != pos2.size()) {
      return false;
    }
    Set<Integer> set1 = new HashSet<Integer>(pos1);
    Set<Integer> set2 = new HashSet<Integer>(pos2);
    return set1.equals(set2);
  }
  
  private Map<String, List<Integer>> buildGramMap(List<String> sequence, int gramSize) {
    Map<String, List<Integer>> gramMap = new HashMap<String, List<Integer>>();
    
    for (int i = 0; i <= sequence.size() - gramSize; i++) {
      StringBuilder keyBuilder = new StringBuilder();
      for (int j = 0; j < gramSize; j++) {
        if (j > 0) keyBuilder.append("|");
        keyBuilder.append(sequence.get(i + j));
      }
      String key = keyBuilder.toString();
      
      List<Integer> positions = gramMap.get(key);
      if (positions == null) {
        positions = new ArrayList<Integer>();
        gramMap.put(key, positions);
      }
      positions.add(i);
    }
    
    return gramMap;
  }
}