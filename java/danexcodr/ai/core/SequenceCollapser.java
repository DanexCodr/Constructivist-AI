package danexcodr.ai.core;

import danexcodr.ai.PatternFamilyBuilder;
import danexcodr.ai.pattern.*;
import java.util.*;

public class SequenceCollapser {

  private PatternFamilyManager patternFamilyManager;
  private PatternFamilyBuilder patternFamilyBuilder;
  private Map<String, Set<String>> structuralEquivalents;

  public SequenceCollapser(
      PatternFamilyManager patternFamilyManager,
      PatternFamilyBuilder patternFamilyBuilder,
      Map<String, Set<String>> structuralEquivalents) {
    this.patternFamilyManager = patternFamilyManager;
    this.patternFamilyBuilder = patternFamilyBuilder;
    this.structuralEquivalents = structuralEquivalents;
  }

  public List<List<String>> collapseSequences(
      List<List<String>> sequences, Set<String> protectedTokens) {
    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);
    if (families.isEmpty()) return null;
    List<List<String>> collapsedList = new ArrayList<List<String>>();
    boolean anyChange = false;
    for (List<String> sequence : sequences) {
      List<String> collapsed = collapseSequence(sequence, families, protectedTokens);
      if (collapsed.size() < sequence.size()) anyChange = true;
      collapsedList.add(collapsed);
    }
    return anyChange ? collapsedList : null;
  }

  public boolean hasCommutativeCollapse(List<String> sequence) {
    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);
    if (families.isEmpty()) return false;
    List<String> collapsed = collapseSequence(sequence, families, new HashSet<String>());
    if (collapsed.size() == sequence.size()) return false;
    for (String token : collapsed) {
      if (token.startsWith("PF")) {
        for (PatternFamily f : families) {
          if (f.getId().equals(token)) {
            for (Structure sp : f.getMemberPatterns()) if (sp.isCommutative()) return true;
          }
        }
      }
    }
    return false;
  }

  private List<String> collapseSequence(
      List<String> sequence, List<PatternFamily> families, Set<String> protectedTokens) {
    List<String> currentSequence = new ArrayList<String>(sequence);
    boolean changeOccurred = true;
    while (changeOccurred) {
      changeOccurred = false;
      int bestMatchStart = -1, bestMatchLength = -1;
      String bestFamilyId = null;
      for (PatternFamily family : families) {
        for (Structure sp : family.getMemberPatterns()) {
          Data data = sp.getData();
          for (int i = 0; i <= currentSequence.size() - data.size(); i++) {
            if (PatternMatcher.isSubSequenceMatch(
                currentSequence, i, data, family, protectedTokens)) {
              if (data.size() > bestMatchLength) {
                bestMatchLength = data.size();
                bestMatchStart = i;
                bestFamilyId = family.getId();
              }
            }
          }
        }
      }
      if (bestMatchStart != -1) {
        List<String> nextSequence =
            new ArrayList<String>(currentSequence.subList(0, bestMatchStart));
        nextSequence.add(bestFamilyId);
        nextSequence.addAll(
            currentSequence.subList(bestMatchStart + bestMatchLength, currentSequence.size()));
        currentSequence = nextSequence;
        changeOccurred = true;
      }
    }
    return currentSequence;
  }
}
