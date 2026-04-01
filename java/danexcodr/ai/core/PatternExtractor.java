package danexcodr.ai.core;

import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternExtractor {

  public boolean isCommutative(List<List<String>> equivalentSequences, String term1, String term2) {

    if (term1 != null && term2 != null && term1.equals(term2)) {
      return true;
    }

    Set<Data> patterns = new HashSet<>();
    for (List<String> sequence : equivalentSequences) {
      Data abstractPattern = SequenceTransformer.abstractSequencePF(sequence, term1, term2);
      patterns.add(abstractPattern);
    }

    Set<Data> flippedPatterns = new HashSet<>();
    for (Data pattern : patterns) {
      Data flipped = SequenceTransformer.flipTermPattern(pattern);
      flippedPatterns.add(flipped);
    }

    boolean isComm = patterns.containsAll(flippedPatterns) && flippedPatterns.containsAll(patterns);

    return isComm;
  }

  public List<Structure> extractStructures(
      List<List<String>> equivalentSequences, String term1, String term2, TermSelector termSelector) {

    String[] positionalTerms = termSelector.determinePositionalTerms(term1, term2, equivalentSequences);
    String actualT1 = positionalTerms[0];
    String actualT2 = positionalTerms[1];

    boolean isCommutative = isCommutative(equivalentSequences, actualT1, actualT2);

    Map<Data, Integer> patternCounts = new HashMap<>();
    for (List<String> sequence : equivalentSequences) {
      Data abstractPattern = SequenceTransformer.abstractSequencePF(sequence, actualT1, actualT2);
      patternCounts.put(
          abstractPattern,
          patternCounts.get(abstractPattern) == null ? 1 : patternCounts.get(abstractPattern) + 1);
    }

    List<Structure> tempPatterns = new ArrayList<>();

    for (Map.Entry<Data, Integer> patternEntry : patternCounts.entrySet()) {
      Data pattern = patternEntry.getKey();

      Data finalData = PatternSlotFiller.finalize(pattern, actualT1, actualT2, isCommutative);
      Structure newPattern = new Structure("TEMP");
      Data newData = newPattern.getData();

      for (int i = 0; i < finalData.size(); i++) {
        if (finalData.isPlaceholder(i)) {
          newData.addPlaceholder(finalData.getPlaceholderAt(i));
        } else if (finalData.isToken(i)) {
          newData.addToken(finalData.getTokenAt(i));
        } else if (finalData.isPFToken(i)) {
          newData.addPFToken(finalData.getPFTokenAt(i));
        }
      }

      newPattern.setCommutative(isCommutative);
      tempPatterns.add(newPattern);
    }

    return tempPatterns;
  }
}
