package danexcodr.ai.core;

import danexcodr.ai.*;
import static danexcodr.ai.Config.*;
import danexcodr.ai.pattern.*;
import java.util.*;
import java.util.Map.Entry;

public class PatternProcessor {

  private SymbolManager symbolManager;
  private PatternFamilyBuilder patternFamilyBuilder;
  private PatternFamilyManager patternFamilyManager;
  private OptionalFinder optionalFinder;
  private BigramAnalyzer bigramAnalyzer;
  private StructuralEquivalenceDetector equivalenceDetector;
  private Map<String, Set<String>> structuralEquivalents;
  
  Map<String, CompositePattern> compositionPatterns;
  private Set<String> learnedOptionalWords = new HashSet<String>();
  private static List<Pattern> allPatterns = new ArrayList<Pattern>();

  boolean familiesDirty;

  public PatternProcessor(
      SymbolManager symbolManager,
      PatternFamilyBuilder patternFamilyBuilder,
      PatternFamilyManager patternFamilyManager,
      OptionalFinder optionalFinder,
      BigramAnalyzer bigramAnalyzer,
      Map<String, Set<String>> structuralEquivalents,
      Map<String, CompositePattern> compositionPatterns,
      List<Pattern> allPatterns,
      boolean familiesDirty) {
    this.symbolManager = symbolManager;
    this.patternFamilyBuilder = patternFamilyBuilder;
    this.patternFamilyManager = patternFamilyManager;
    this.optionalFinder = optionalFinder;
    this.bigramAnalyzer = bigramAnalyzer;
    this.structuralEquivalents = structuralEquivalents;
    this.compositionPatterns = compositionPatterns;
    this.allPatterns = allPatterns;
    this.familiesDirty = familiesDirty;
  }

public Set<String> getLearnedOptionalWords() {
        return learnedOptionalWords;
    }

public void setEquivalenceDetector(StructuralEquivalenceDetector equivalenceDetector) {
    this.equivalenceDetector = equivalenceDetector;
  }

/**
   * Generates storage slots based on specific definitions:
   * 1. Commutative -> [C], [C]
   * 2. PF + Normal -> PF, [X]
   * 3. Normal Directed -> [1], [2]
   */
  private List<String> createFinalPatternSlots(List<String> abstractPattern, String t1, String t2, boolean isCommutative) {
    List<String> finalSlots = new ArrayList<String>();
    boolean t1IsPF = t1.startsWith("PF");
    boolean t2IsPF = t2.startsWith("PF");

    for (String slot : abstractPattern) {
      if (slot.equals("[1]")) {
        if (isCommutative) {
          finalSlots.add("[C]");
        } else if (t2IsPF && !t1IsPF) {
          finalSlots.add("[X]"); // T2 is PF, so T1 (this slot) is the variable [X]
        } else {
          finalSlots.add("[1]");
        }
      } else if (slot.equals("[2]")) {
        if (isCommutative) {
          finalSlots.add("[C]");
        } else if (t1IsPF && !t2IsPF) {
          finalSlots.add("[X]"); // T1 is PF, so T2 (this slot) is the variable [X]
        } else {
          finalSlots.add("[2]");
        }
      } else {
        finalSlots.add(slot); // Structural word or existing "PF..." token
      }
    }
    return finalSlots;
  }

public void processEquivalenceSet(List<List<String>> equivalentSequences) {
    
    optionalFinder.preprocessAndIdentifyOptionals(equivalentSequences);
    learnedOptionalWords.addAll(optionalFinder.getOptionals());
    boolean contextNeedsBuilding = false;
    if (!equivalentSequences.isEmpty() && !equivalentSequences.get(0).isEmpty()) {
        String firstWord = equivalentSequences.get(0).get(0);
        if (!firstWord.startsWith("PF")) {
            Symbol s = symbolManager.getSymbols().get(firstWord);
            if (s == null || (s.leftContext.isEmpty() && s.rightContext.isEmpty())) {
                contextNeedsBuilding = true;
            }
        }
    }
    if (contextNeedsBuilding) {
        for (List<String> seq : equivalentSequences) {
            symbolManager.buildContext(seq);
        }
    }

    reevaluateEquivalents();
    
    List<List<String>> collapsedSequences = collapseSequences(equivalentSequences);
    boolean wasCollapsed = false;

    if (collapsedSequences != null && !collapsedSequences.isEmpty()) {
        if (collapsedSequences.get(0).size() <= 1) {
             System.out.println("   [System: Sequence recognized as existing family instance: " + collapsedSequences.get(0) + "]");
             return; 
        }
        equivalentSequences = collapsedSequences;
        wasCollapsed = true;
        System.out.println("   [System: Sequence collapsed for higher-level analysis: " + equivalentSequences.get(0) + "]");
    }

    Map<String, Set<Integer>> wordPositions = new HashMap<String, Set<Integer>>();
    for (List<String> sequence : equivalentSequences) {
        for (int i = 0; i < sequence.size(); i++) {
            String word = sequence.get(i);
            if (!wordPositions.containsKey(word)) {
                wordPositions.put(word, new HashSet<Integer>());
            }
            wordPositions.get(word).add(i);
        }
    }

    Set<String> learnedStructuralWords = getLearnedStructuralWords();

    Set<String> allWordsInSequences = getAllWords(equivalentSequences);
    allWordsInSequences.removeAll(optionalFinder.getOptionals());

    Set<String> bigramContentWords = bigramAnalyzer.runBigramAnalysis(equivalentSequences, allWordsInSequences);
    if (!bigramContentWords.isEmpty()) {
        System.out.println("   [System: Bigram analysis detected moving word pairs]");
    }

    ContentFinder contentFinder = new ContentFinder(optionalFinder, bigramAnalyzer);
    Set<String> preliminaryContentWords;

    if (wasCollapsed) {
        preliminaryContentWords = contentFinder.runParityFallback(equivalentSequences);
        if (preliminaryContentWords.size() < 2) {
             preliminaryContentWords = contentFinder.identifyContentWords(equivalentSequences, learnedStructuralWords);
        }
    } else {
        preliminaryContentWords = contentFinder.identifyContentWords(equivalentSequences, learnedStructuralWords);
    }

    if (preliminaryContentWords.size() < 2) {
        System.out.println("   [System: Standard movement analysis inconclusive. Trying Any Movement Check...]");
        preliminaryContentWords = contentFinder.runAnyMovementCheck(equivalentSequences, learnedStructuralWords);

        if (preliminaryContentWords.size() < 2) {
            System.out.println("   [System: Any Movement Check inconclusive. Trying weighted parity...]");
            preliminaryContentWords = contentFinder.runParityFallback(equivalentSequences);
            if (preliminaryContentWords.size() < 2) {
                if (preliminaryContentWords.size() > CONCEPT_LIMIT) {
                    System.out.println("   [System: Parity analysis found " + preliminaryContentWords.size() + " content words, exceeding max " + CONCEPT_LIMIT + ".]");
                }
                System.out.println("   Movement, Any-Movement, and Parity analysis failed to find content. Set is ambiguous.");
                return;
            }
        }
    }

    Set<String> preliminaryStructuralWords = identifyStructuralWords(equivalentSequences, preliminaryContentWords);

    String[] preliminaryTerms = selectTermsByClosestCompanion(preliminaryContentWords, equivalentSequences, wordPositions, preliminaryStructuralWords);
    if (preliminaryTerms == null) {
        System.out.println("   Could not select preliminary terms from content: " + preliminaryContentWords + ". Set is ambiguous.");
        return;
    }

    String[] positionalTerms = determinePositionalTerms(preliminaryTerms[0], preliminaryTerms[1], equivalentSequences);
 
    Set<StructuralEquivalenceDetector.EquivalencePair> newPairs = equivalenceDetector.detectStructuralEquivalents(equivalentSequences, preliminaryStructuralWords, positionalTerms[0], positionalTerms[1], allPatterns);
    addDiscoveredEquivalents(newPairs);

    Set<String> finalContentWords;
    if (wasCollapsed) {
        finalContentWords = contentFinder.runParityFallback(equivalentSequences); 
         if (finalContentWords.size() < 2) {
             finalContentWords = contentFinder.identifyContentWords(equivalentSequences, learnedStructuralWords);
        }
    } else {
        finalContentWords = contentFinder.identifyContentWords(equivalentSequences, learnedStructuralWords);
    }

    if (finalContentWords.size() < 2) {
        finalContentWords = contentFinder.runAnyMovementCheck(equivalentSequences, learnedStructuralWords);
        if (finalContentWords.size() < 2) {
            finalContentWords = contentFinder.runParityFallback(equivalentSequences);
            if (finalContentWords.size() < 2) {
                System.out.println("   Analysis failed after refinement. Set is ambiguous.");
                return;
            }
        }
    }

    Set<String> finalStructuralWords = identifyStructuralWords(equivalentSequences, finalContentWords);

    Set<String> dualWordsInThisSet = new HashSet<String>();
    Set<String> pureStructuralWords = new HashSet<String>();
    for (String word : finalStructuralWords) {
        Symbol symbol = symbolManager.getSymbols().get(word);
        if (symbol != null && symbol.relations.contains("D")) {
            dualWordsInThisSet.add(word);
        } else {
            pureStructuralWords.add(word);
        }
    }

    String[] finalTerms = selectTermsByClosestCompanion(finalContentWords, equivalentSequences, wordPositions, finalStructuralWords);
    if (finalTerms == null) {
        System.out.println("   Could not select final terms from content: " + finalContentWords + ". Set is ambiguous.");
        return;
    }

    String[] finalPositionalTerms = determinePositionalTerms(finalTerms[0], finalTerms[1], equivalentSequences);
    
    extractStructuralPatterns(equivalentSequences, finalPositionalTerms[0], finalPositionalTerms[1]);
    
    detectCrossDomainEquivalents(equivalentSequences, finalPositionalTerms[0], finalPositionalTerms[1]);

    boolean isCommutative = isCommutative(equivalentSequences, finalTerms[0], finalTerms[1]);
    System.out.println("   Core relation: " + finalPositionalTerms[0] + (isCommutative ? " <-> " : " -> ") + finalPositionalTerms[1]);

    if (finalContentWords.size() > 2) {
        Set<String> otherContent = new LinkedHashSet<String>(finalContentWords);
        otherContent.remove(finalTerms[0]);
        otherContent.remove(finalTerms[1]);
        if (!otherContent.isEmpty()) {
            List<String> sortedOther = new ArrayList<String>(otherContent);
            Collections.sort(sortedOther);
            System.out.println("   Duals: " + sortedOther);
        }
    }

    if (!dualWordsInThisSet.isEmpty()) {
        List<String> sortedDual = new ArrayList<String>(dualWordsInThisSet);
        Collections.sort(sortedDual);
        System.out.println("   Duals: " + sortedDual);
    }

    if (!pureStructuralWords.isEmpty()) {
        List<String> sortedStruct = new ArrayList<String>(pureStructuralWords);
        Collections.sort(sortedStruct);
        System.out.println("   Structurals: " + sortedStruct);
    }

    // --- CREATE RELATION PATTERN AND POPULATE BASE TERMS ---
    RelationPattern pattern = findOrCreatePattern(finalPositionalTerms[0], finalPositionalTerms[1]);
    pattern.incrementFrequency();
    pattern.setCommutative(isCommutative);
    for (List<String> seq : equivalentSequences) {
        pattern.addConcreteExample(new ArrayList<String>(seq));
    }
    
    // Populate base terms for T1 and T2
    pattern.setBaseTermsT1(getBaseTermsForSyntheticToken(finalPositionalTerms[0]));
    pattern.setBaseTermsT2(getBaseTermsForSyntheticToken(finalPositionalTerms[1]));
    // ----------------------------------------------------

    RelationPattern reversePattern = null;
    if (isCommutative) {
        reversePattern = findOrCreatePattern(finalPositionalTerms[1], finalPositionalTerms[0]);
        reversePattern.incrementFrequency();
        reversePattern.setCommutative(isCommutative);
        for (List<String> seq : equivalentSequences) {
            reversePattern.addConcreteExample(new ArrayList<String>(seq));
        }
        // Populate base terms for reverse pattern
        reversePattern.setBaseTermsT1(getBaseTermsForSyntheticToken(finalPositionalTerms[1]));
        reversePattern.setBaseTermsT2(getBaseTermsForSyntheticToken(finalPositionalTerms[0]));
    }

    for (String word : finalContentWords) {
        if (!word.startsWith("PF") && symbolManager.getSymbols().containsKey(word)) {
            symbolManager.getSymbols().get(word).relations.add("C");
        }
    }
    if (!finalPositionalTerms[0].startsWith("PF") && symbolManager.getSymbols().containsKey(finalPositionalTerms[0])) {
        symbolManager.getSymbols().get(finalPositionalTerms[0]).relations.add("1");
    }
    if (!finalPositionalTerms[1].startsWith("PF") && symbolManager.getSymbols().containsKey(finalPositionalTerms[1])) {
        symbolManager.getSymbols().get(finalPositionalTerms[1]).relations.add("2");
    }

    for (String word : finalStructuralWords) {
        if (!word.startsWith("PF") && symbolManager.getSymbols().containsKey(word)) {
            symbolManager.getSymbols().get(word).relations.add("S");
        }
    }

    familiesDirty = true;
    List<PatternFamily> families = PatternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder, familiesDirty);

    for (Pattern p : allPatterns) {
        if (!(p instanceof RelationPattern)) continue;

        RelationPattern rp = (RelationPattern) p;
        rp.setFamily(null); 

        boolean familyAssigned = false;
        
        for (PatternFamily family : families) {
            if (familyAssigned) break;

            for (StructuralPattern sp : family.getMemberPatterns()) {
                if (familyAssigned) break;
                
                for (List<String> seq : rp.getConcreteExamples()) {
                    List<String> abstractPattern = abstractSequence(seq, rp.getT1(), rp.getT2());
                    List<String> flippedPattern = flipTermPattern(abstractPattern, rp.getT1(), rp.getT2());

                    boolean matchesNormal = sp.getStructuralSlots().equals(abstractPattern);
                    boolean matchesFlipped = rp.isCommutative() && sp.getStructuralSlots().equals(flippedPattern);

                    if (matchesNormal || matchesFlipped) {
                        rp.setFamily(family);
                        familyAssigned = true;
                        break;
                    }
                }
            }
        }
    }

    updateLogicalFamilyStatus();

    if (!learnedOptionalWords.isEmpty()) {
        List<String> sortedOpt = new ArrayList<String>(learnedOptionalWords);
        Collections.sort(sortedOpt);
        System.out.println("   Optionals: " + sortedOpt);
    }

    if (newPairs != null && !newPairs.isEmpty()) {
        Set<String> newEquivalentsStrings = new HashSet<String>();
        for (StructuralEquivalenceDetector.EquivalencePair pair : newPairs) {
            newEquivalentsStrings.add(pair.toString());
        }
        List<String> sortedEquivs = new ArrayList<String>(newEquivalentsStrings);
        Collections.sort(sortedEquivs);
        System.out.println("   Structural equivalents: " + sortedEquivs);
    }
}

/**
 * NEW: Retrieves the original T1/T2 terms that belong to the specified Pattern Family ID.
 * If the token is not synthetic, it returns the token itself.
 */
private Set<String> getBaseTermsForSyntheticToken(String token) {
    if (!token.startsWith("PF")) {
        // Not a Synthetic Token, return the token itself
        return Collections.singleton(token);
    }
    
    // Find the Pattern Family with this ID
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder, familiesDirty);
    PatternFamily targetFamily = null;
    for (PatternFamily f : families) {
        if (f.getId().equals(token)) {
            targetFamily = f;
            break;
        }
    }
    
    Set<String> baseTerms = new HashSet<String>();

    if (targetFamily != null) {
        // Collect T1 and T2 from all RelationPatterns belonging to this Family.
        for (Pattern p : allPatterns) {
            if (p instanceof RelationPattern) {
                RelationPattern rp = (RelationPattern) p;
                if (rp.getFamily() != null && rp.getFamily().getId().equals(token)) {
                    baseTerms.add(rp.getT1());
                    baseTerms.add(rp.getT2());
                }
            }
        }
    } else {
        // Fallback: If family not found, return the token itself
        baseTerms.add(token);
    }

    // Recurse for any synthetic tokens found in the baseTerms
    Set<String> finalBaseTerms = new HashSet<String>();
    for (String term : baseTerms) {
        if (term.startsWith("PF")) {
            finalBaseTerms.addAll(getBaseTermsForSyntheticToken(term)); // Recursive call
        } else {
            finalBaseTerms.add(term);
        }
    }
    
    return finalBaseTerms;
}


  private Set<String> getAllWords(List<List<String>> sequences) {
    Set<String> allWords = new HashSet<String>();
    for (List<String> sequence : sequences) {
      allWords.addAll(sequence);
    }
    return allWords;
  }

  private String[] selectTermsByClosestCompanion(
    Set<String> contentWords,
    List<List<String>> sequences,
    Map<String, Set<Integer>> wordPositions,
    Set<String> structuralWords) {

    if (contentWords.size() < 2) {
        return null;
    }

    Set<String> filteredContentWords = new HashSet<String>(contentWords);
    filteredContentWords.removeAll(structuralWords);
    filteredContentWords.removeAll(optionalFinder.getOptionals());

    if (filteredContentWords.size() < 2) {
        filteredContentWords = new HashSet<String>(contentWords);
        filteredContentWords.removeAll(optionalFinder.getOptionals());
        if (filteredContentWords.size() < 2) {
            filteredContentWords = new HashSet<String>(contentWords);
        }
    }

    Set<String> maxMovementTokens = new HashSet<String>();
    int maxMovement = 0;

    for (String word : filteredContentWords) {
        Set<Integer> positions = wordPositions.get(word);
        int movement = (positions != null) ? positions.size() : 0;
        if (movement > maxMovement) {
            maxMovement = movement;
            maxMovementTokens.clear();
            maxMovementTokens.add(word);
        } else if (movement == maxMovement) {
            maxMovementTokens.add(word);
        }
    }

    if (maxMovementTokens.size() >= 2) {
        List<String> maxList = new ArrayList<String>(maxMovementTokens);
        Collections.sort(maxList);
        String term1 = maxList.get(0);
        String term2 = maxList.get(1);
        return new String[] {term1, term2};
    }

    String highestMovementWord = null;
    if (!maxMovementTokens.isEmpty()) {
        highestMovementWord = maxMovementTokens.iterator().next();
    } else {
        List<String> contentList = new ArrayList<String>(filteredContentWords);
        Collections.sort(contentList);
        return new String[] {contentList.get(0), contentList.get(1)};
    }

    boolean hasOptionals = !optionalFinder.getOptionals().isEmpty();
    Map<String, Integer> companionDistanceCount = new HashMap<String, Integer>();

    for (List<String> sequence : sequences) {
        int targetIndex = sequence.indexOf(highestMovementWord);
        if (targetIndex == -1) continue;

        for (int i = 0; i < sequence.size(); i++) {
            String otherWord = sequence.get(i);
            if (filteredContentWords.contains(otherWord) && !otherWord.equals(highestMovementWord)) {
                int distance = Math.abs(i - targetIndex);

                if (hasOptionals) {
                    if (distance > 1) {
                        Integer count = companionDistanceCount.get(otherWord);
                        companionDistanceCount.put(otherWord, (count == null) ? 1 : count + 1);
                    }
                } else {
                    if (distance == 1) {
                        Integer count = companionDistanceCount.get(otherWord);
                        companionDistanceCount.put(otherWord, (count == null) ? 1 : count + 1);
                    }
                }
            }
        }
    }

    String bestCompanion = null;
    int bestCompanionCount = 0;

    for (Map.Entry<String, Integer> entry : companionDistanceCount.entrySet()) {
        if (entry.getValue() > bestCompanionCount) {
            bestCompanionCount = entry.getValue();
            bestCompanion = entry.getKey();
        }
    }

    if (bestCompanion != null) {
        return new String[] {highestMovementWord, bestCompanion};
    }

    for (String word : filteredContentWords) {
        if (!word.equals(highestMovementWord)) {
            Set<Integer> positions = wordPositions.get(word);
            int movement = (positions != null) ? positions.size() : 0;
            if (movement > 0) {
                return new String[] {highestMovementWord, word};
            }
        }
    }

    List<String> contentList = new ArrayList<String>(filteredContentWords);
    Collections.sort(contentList);
    return new String[] {contentList.get(0), contentList.get(1)};
}

  private String findMatchingFamily(List<String> sequencePart, List<PatternFamily> families) {
    if (sequencePart == null || sequencePart.isEmpty() || families == null) {
      return null;
    }
    List<List<String>> partAsList = new ArrayList<List<String>>();
    partAsList.add(sequencePart);

    ContentFinder contentFinder = new ContentFinder(optionalFinder, bigramAnalyzer);
    Set<String> contentWords = contentFinder.identifyContentWords(partAsList, null);

    if (contentWords.size() < 2) {
      return null;
    }

    Map<String, Set<Integer>> wordPositions = new HashMap<String, Set<Integer>>();
    for (int i = 0; i < sequencePart.size(); i++) {
      String word = sequencePart.get(i);
      if (!wordPositions.containsKey(word)) {
        wordPositions.put(word, new HashSet<Integer>());
      }
      wordPositions.get(word).add(i);
    }

    Set<String> structuralWords = identifyStructuralWords(partAsList, contentWords);

    String[] terms =
        selectTermsByClosestCompanion(contentWords, partAsList, wordPositions, structuralWords);
    if (terms == null) return null;

    List<String> abstractedPart = abstractSequence(sequencePart, terms[0], terms[1]);
    List<String> flippedAbstractedPart = flipTermPattern(abstractedPart, terms[0], terms[1]);

    for (PatternFamily family : families) {
      if (family == null) continue;
      for (StructuralPattern sp : family.getMemberPatterns()) {
        if (sp.getStructuralSlots().equals(abstractedPart)
            || sp.getStructuralSlots().equals(flippedAbstractedPart)) {
          return family.getId();
        }
      }
    }

    return null;
  }

  private Set<String> identifyStructuralWords(
    List<List<String>> equivalentSequences, Set<String> contentWords) {
    
    Set<String> structuralWords = new LinkedHashSet<String>();
    for (List<String> sequence : equivalentSequences) {
        for (String word : sequence) {
            if (!contentWords.contains(word)) {
                structuralWords.add(word);
            }
        }
    }
    return structuralWords;
}

  private String[] determinePositionalTerms(String w1, String w2, List<List<String>> equivalentSequences) {
    
    int w1_left_of_w2 = 0;
    int w2_left_of_w1 = 0;

    for (List<String> sequence : equivalentSequences) {
        int idx1 = sequence.indexOf(w1);
        int idx2 = sequence.indexOf(w2);

        if (idx1 != -1 && idx2 != -1) {
            if (idx1 < idx2) {
                w1_left_of_w2++;
            } else if (idx2 < idx1) {
                w2_left_of_w1++;
            }
        }
    }
    
    if (w2_left_of_w1 > w1_left_of_w2) {
        return new String[] {w2, w1};
    } else {
        return new String[] {w1, w2};
    }
}

  private boolean isCommutative(
      List<List<String>> equivalentSequences, String term1, String term2) {
    Set<List<String>> patterns = new HashSet<List<String>>();
    for (List<String> sequence : equivalentSequences) {
      patterns.add(abstractSequence(sequence, term1, term2));
    }

    Set<List<String>> flippedPatterns = new HashSet<List<String>>();
    for (List<String> pattern : patterns) {
      flippedPatterns.add(flipTermPattern(pattern, term1, term2));
    }

    boolean commutative = patterns.containsAll(flippedPatterns) && flippedPatterns.containsAll(patterns);
    return commutative;
}

  private void addDiscoveredEquivalents(
      Set<StructuralEquivalenceDetector.EquivalencePair> newPairs) {
    if (!newPairs.isEmpty()) {
      familiesDirty = true;
      for (StructuralEquivalenceDetector.EquivalencePair pair : newPairs) {
        addStructuralEquivalence(pair.w1, pair.w2);
      }
    }
  }

  private List<String> abstractSequence(List<String> sequence, String term1, String term2) {
    List<String> abstracted = new ArrayList<String>();
    for (String word : sequence) {
      if (word.equals(term1)) {
        if (word.startsWith("PF")) abstracted.add(word);
        else abstracted.add("[1]");
      } else if (word.equals(term2)) {
        if (word.startsWith("PF")) abstracted.add(word);
        else abstracted.add("[2]");
      } else {
        abstracted.add(word);
      }
    }
    return abstracted;
  }

  private List<String> flipTermPattern(List<String> pattern, String term1, String term2) {
    List<String> flipped = new ArrayList<String>();
    for (String token : pattern) {
      if (token.equals("[1]")) {
        flipped.add("[2]");
      } else if (token.equals("[2]")) {
        if (term1.startsWith("PF")) flipped.add(term1); 
        else flipped.add("[1]");
      } else if (token.startsWith("PF")) {
         if (term2.startsWith("PF")) flipped.add(term2);
         else flipped.add("[2]");
      } else {
        flipped.add(token);
      }
    }
    return flipped;
  }
  
  private List<String> flipTermPattern(List<String> pattern) {
      return flipTermPattern(pattern, "", "");
  }

  private void extractStructuralPatterns(
      List<List<String>> equivalentSequences, String term1, String term2) {
    Map<List<String>, Integer> patternCounts = new HashMap<List<String>, Integer>();
    Map<List<String>, Set<List<String>>> patternExamples =
        new HashMap<List<String>, Set<List<String>>>();

    for (List<String> sequence : equivalentSequences) {
      List<String> abstractPattern = abstractSequence(sequence, term1, term2);

      Integer count = patternCounts.get(abstractPattern);
      if (count == null) {
        patternCounts.put(abstractPattern, 1);
        patternExamples.put(abstractPattern, new HashSet<List<String>>());
      } else {
        patternCounts.put(abstractPattern, count + 1);
      }
      patternExamples.get(abstractPattern).add(sequence);
    }

    Set<List<String>> commutativePatterns = new HashSet<List<String>>();
    for (List<String> pattern : patternCounts.keySet()) {
      List<String> flippedPattern = flipTermPattern(pattern, term1, term2);
      if (patternCounts.containsKey(flippedPattern)) {
        commutativePatterns.add(pattern);
        commutativePatterns.add(flippedPattern);
      }
    }

    for (Entry<List<String>, Integer> entry : patternCounts.entrySet()) {
      List<String> abstractPattern = entry.getKey();
      boolean commutative = commutativePatterns.contains(abstractPattern);
      
      // Use the new helper to enforce [C] and [X] definitions
      List<String> finalSlots = createFinalPatternSlots(abstractPattern, term1, term2, commutative);

      boolean isComposite = false;
      for (String slot : finalSlots) {
        if (slot.startsWith("PF")) {
          isComposite = true;
          break;
        }
      }

      if (isComposite) {
        String key = finalSlots.toString();
        CompositePattern cp;
        if (compositionPatterns.containsKey(key)) {
          cp = compositionPatterns.get(key);
          cp.incrementFrequency();
        } else {
          cp = new CompositePattern("CP" + compositionPatterns.size(), finalSlots);
          if (commutative) {
            cp.setCommutative(true);
          }
          cp.setFrequency(entry.getValue());

          compositionPatterns.put(key, cp);
          allPatterns.add(cp);
          familiesDirty = true;
        }

        // Add examples AND populate variable fillers
        for (List<String> example : patternExamples.get(abstractPattern)) {
          cp.addConcreteExample(example);
          // Map example words to final slots (roughly, though positional matching is safer)
          if (example.size() == abstractPattern.size()) {
             for (int i = 0; i < abstractPattern.size(); i++) {
                 String abstractSlot = abstractPattern.get(i);
                 String finalSlot = finalSlots.get(i);
                 String filler = example.get(i);
                 
                 // If the final slot became a variable [X], we store the filler
                 if (finalSlot.equals("[X]") || finalSlot.equals("[1]") || finalSlot.equals("[2]")) {
                     cp.addVariableFiller(finalSlot, filler);
                 }
             }
          }
        }

      } else {
        StructuralPattern existing = findStructuralPattern(finalSlots);
        if (existing != null) {
          existing.incrementFrequency();
          for (List<String> example : patternExamples.get(abstractPattern)) {
            existing.addConcreteExample(example);
          }
          if (commutative) {
            existing.setCommutative(true);
          }
        } else {
          StructuralPattern newPattern =
              new StructuralPattern("P" + allPatterns.size(), finalSlots);
          newPattern.setFrequency(entry.getValue());
          for (List<String> example : patternExamples.get(abstractPattern)) {
            newPattern.addConcreteExample(example);
          }
          if (commutative) {
            newPattern.setCommutative(true);
          }
          allPatterns.add(newPattern);
          familiesDirty = true;
        }
      }
    }
  }

  private StructuralPattern findStructuralPattern(List<String> abstractPattern) {
    if (abstractPattern == null) return null;
    for (Pattern pattern : allPatterns) {
      if (pattern instanceof StructuralPattern) {
        StructuralPattern sp = (StructuralPattern) pattern;
        if (abstractPattern.equals(sp.getStructuralSlots())) {
          return sp;
        }
      }
    }
    return null;
  }

  public static RelationPattern findRelationPattern(String term1, String term2) {
    for (Pattern pattern : allPatterns) {
      if (pattern instanceof RelationPattern) {
        RelationPattern rp = (RelationPattern) pattern;
        if (rp.getT1().equals(term1) && rp.getT2().equals(term2)) {
          return rp;
        }
        if (rp.isCommutative() && rp.getT1().equals(term2) && rp.getT2().equals(term1)) {
          return rp;
        }
      }
    }
    return null;
  }

  private RelationPattern findOrCreatePattern(String term1, String term2) {
    RelationPattern existing = PatternProcessor.findRelationPattern(term1, term2);
    if (existing != null) {
        existing.updateTimestamp(); // --- UPDATE TIMESTAMP ---
        return existing;
    }

    RelationPattern reverseExisting = PatternProcessor.findRelationPattern(term2, term1);
    if (reverseExisting != null && reverseExisting.isCommutative()) {
        reverseExisting.updateTimestamp(); // --- UPDATE TIMESTAMP ---
        return reverseExisting;
    }

    RelationPattern newPattern = new RelationPattern("RP" + allPatterns.size(), term1, term2);
    allPatterns.add(newPattern);
    familiesDirty = true;
    return newPattern;
  }

  private void addStructuralEquivalence(String w1, String w2) {
    if (!structuralEquivalents.containsKey(w1)) {
      structuralEquivalents.put(w1, new HashSet<String>());
    }
    if (!structuralEquivalents.containsKey(w2)) {
      structuralEquivalents.put(w2, new HashSet<String>());
    }

    boolean changed1 = structuralEquivalents.get(w1).add(w2);
    boolean changed2 = structuralEquivalents.get(w2).add(w1);

    if (changed1 || changed2) {
      familiesDirty = true;
    }

    if (!w1.startsWith("PF") && symbolManager.getSymbols().containsKey(w1)) {
      symbolManager.getSymbols().get(w1).relations.add("S");
    }
    if (!w2.startsWith("PF") && symbolManager.getSymbols().containsKey(w2)) {
      symbolManager.getSymbols().get(w2).relations.add("S");
    }
  }

  public Set<String> getLearnedStructuralWords() {
    Set<String> coreStructuralWords = new LinkedHashSet<String>();
    Map<String, Integer> structuralWordCount = new HashMap<String, Integer>();

    for (Pattern pattern : allPatterns) {
        if (pattern instanceof StructuralPattern) {
            StructuralPattern sp = (StructuralPattern) pattern;
            for (String word : sp.getStructuralSlots()) {
                // FIX: Exclude [C] and [X] from being learned as structural words
                if (!word.equals("[1]") && !word.equals("[2]") && 
                    !word.equals("[C]") && !word.equals("[X]") && 
                    !word.startsWith("PF")) {
                    Symbol sym = symbolManager.getSymbols().get(word);
                    if (sym != null && sym.relations.contains("S")) {
                        Integer count = structuralWordCount.get(word);
                        structuralWordCount.put(word, (count == null) ? 1 : count + 1);
                    }
                }
            }
        }
    }

    for (Map.Entry<String, Integer> entry : structuralWordCount.entrySet()) {
        if (entry.getValue() > 0) {
            coreStructuralWords.add(entry.getKey());
        }
    }

    Set<String> allLearnedStructuralWords = new HashSet<String>(coreStructuralWords);
    Queue<String> queue = new LinkedList<String>(coreStructuralWords);
    Set<String> visited = new HashSet<String>(coreStructuralWords);

    while (!queue.isEmpty()) {
        String currentWord = queue.poll();
        Set<String> equivalents = structuralEquivalents.get(currentWord);
        if (equivalents != null) {
            for (String equivalent : equivalents) {
                if (!visited.contains(equivalent)) {
                    allLearnedStructuralWords.add(equivalent);
                    visited.add(equivalent);
                    queue.add(equivalent);
                }
            }
        }
    }

    Set<String> filtered = new HashSet<String>();
    for (String word : allLearnedStructuralWords) {
        Symbol sym = symbolManager.getSymbols().get(word);
        if (sym != null && !sym.relations.contains("D")) {
            filtered.add(word);
        }
    }
    return filtered;
}

// Add this method to PatternProcessor class
private void detectCrossDomainEquivalents(List<List<String>> equivalentSequences, String t1, String t2) {
    Set<StructuralEquivalenceDetector.EquivalencePair> crossDomainPairs = 
        equivalenceDetector.detectStructuralEquivalents(equivalentSequences, 
                                                       getLearnedStructuralWords(), 
                                                       t1, t2, 
                                                       allPatterns);
    
    if (!crossDomainPairs.isEmpty()) {
        System.out.println("   [System: Found " + crossDomainPairs.size() + " cross-domain equivalents]");
        addDiscoveredEquivalents(crossDomainPairs);
    }
}

  public void reevaluateEquivalents() {
    Set<StructuralEquivalenceDetector.EquivalencePair> pairsToRemove =
        new HashSet<StructuralEquivalenceDetector.EquivalencePair>();

    for (Entry<String, Set<String>> entry : structuralEquivalents.entrySet()) {
      String w1 = entry.getKey();
      for (String w2 : entry.getValue()) {
        if (equivalenceDetector.areNeighbors(w1, w2)) {
          pairsToRemove.add(equivalenceDetector.createEquivalencePair(w1, w2));
        }
      }
    }

    boolean changed = false;
    for (StructuralEquivalenceDetector.EquivalencePair pair : pairsToRemove) {
      boolean removed1 = false;
      boolean removed2 = false;
      if (structuralEquivalents.containsKey(pair.w1)) {
        removed1 = structuralEquivalents.get(pair.w1).remove(pair.w2);
        if (structuralEquivalents.get(pair.w1).isEmpty()) {
          structuralEquivalents.remove(pair.w1);
        }
      }
      if (structuralEquivalents.containsKey(pair.w2)) {
        removed2 = structuralEquivalents.get(pair.w2).remove(pair.w1);
        if (structuralEquivalents.get(pair.w2).isEmpty()) {
          structuralEquivalents.remove(pair.w2);
        }
      }
      if (removed1 || removed2) changed = true;
    }

    if (changed) {
      System.out.println("   [System: Reevaluated structural equivalents based on context.]");
      familiesDirty = true;
    }
  }

  private Set<String> findConjunctions() {
    Set<String> conj = new HashSet<String>();
    for (Pattern pattern : allPatterns) {
      if (pattern instanceof StructuralPattern) {
        StructuralPattern sp = (StructuralPattern) pattern;
        if (sp.isCommutative()) {
          for (String token : sp.getStructuralSlots()) {
            // FIX: Exclude [C] and [X]
            if (!token.equals("[1]") && !token.equals("[2]") &&
                !token.equals("[C]") && !token.equals("[X]")) {
              conj.add(token);
            }
          }
        }
      }
    }
    Set<String> allConjunctions = new HashSet<String>(conj);
    for (String cnj : conj) {
      Set<String> equivalents = structuralEquivalents.get(cnj);
      if (equivalents != null) {
        allConjunctions.addAll(equivalents);
      }
    }
    return allConjunctions;
  }

    private List<List<String>> collapseSequences(List<List<String>> sequences) {
        List<PatternFamily> families =
            PatternFamilyManager.getPatternFamilies(
                structuralEquivalents, patternFamilyBuilder, familiesDirty);
        
        if (families.isEmpty()) {
            return null;
        }

        List<List<String>> collapsedList = new ArrayList<List<String>>();
        boolean anyChange = false;

        for (List<String> sequence : sequences) {
            List<String> collapsed = collapseSequence(sequence, families);
            if (collapsed.size() < sequence.size()) {
                anyChange = true;
            }
            collapsedList.add(collapsed);
        }

        return anyChange ? collapsedList : null;
    }

    private List<String> collapseSequence(List<String> sequence, List<PatternFamily> families) {
        List<String> currentSequence = new ArrayList<String>(sequence);
        boolean changeOccurred = true;
        
        while (changeOccurred) {
            changeOccurred = false;
            
            int bestMatchStart = -1;
            int bestMatchLength = -1;
            String bestFamilyId = null;
            
            for (PatternFamily family : families) {
                for (StructuralPattern sp : family.getMemberPatterns()) {
                    List<String> slots = sp.getStructuralSlots();
                    
                    for (int i = 0; i <= currentSequence.size() - slots.size(); i++) {
                         if (isSubSequenceMatch(currentSequence, i, slots, family)) {
                             if (slots.size() > bestMatchLength) {
                                 bestMatchLength = slots.size();
                                 bestMatchStart = i;
                                 bestFamilyId = family.getId();
                             }
                         }
                    }
                }
            }
            
            if (bestMatchStart != -1) {
                List<String> nextSequence = new ArrayList<String>();
                for (int i = 0; i < bestMatchStart; i++) {
                    nextSequence.add(currentSequence.get(i));
                }
                nextSequence.add(bestFamilyId);
                for (int i = bestMatchStart + bestMatchLength; i < currentSequence.size(); i++) {
                    nextSequence.add(currentSequence.get(i));
                }
                currentSequence = nextSequence;
                changeOccurred = true;
            }
        }
        return currentSequence;
    }

    private boolean isSubSequenceMatch(List<String> seq, int startIdx, List<String> patternSlots, PatternFamily family) {
        for (int j = 0; j < patternSlots.size(); j++) {
            String slot = patternSlots.get(j);
            String token = seq.get(startIdx + j);
            
            // FIX: [C] and [X] are placeholders, not structural content to match
            if (slot.equals("[1]") || slot.equals("[2]") || 
                slot.equals("[C]") || slot.equals("[X]")) {
                continue;
            }
            if (slot.equals(token)) continue;
            
            String tokenAlias = family.getWordToAlias().get(token);
            String slotAlias = family.getWordToAlias().get(slot);
            
            if (family.getAliases().containsKey(slot)) {
                 if (tokenAlias != null && tokenAlias.equals(slot)) continue;
            }
            if (tokenAlias != null && slotAlias != null && tokenAlias.equals(slotAlias)) continue;
            
            return false;
        }
        return true;
    }

  public boolean hasCommutativeCollapse(List<String> sequence) {
    List<PatternFamily> families =
        PatternFamilyManager.getPatternFamilies(
            structuralEquivalents, patternFamilyBuilder, familiesDirty);

    if (families.isEmpty()) return false;

    // Simulate collapse
    List<String> collapsed = collapseSequence(sequence, families);

    // If no collapse occurred, return false
    if (collapsed.size() == sequence.size() && collapsed.equals(sequence)) {
        return false;
    }

    // Check if any token in the collapsed sequence is a PF ID corresponding to a Commutative Family
    for (String token : collapsed) {
        if (token.startsWith("PF")) {
            for (PatternFamily f : families) {
                if (f.getId().equals(token)) {
                    // Check if this family contains commutative patterns
                    for (StructuralPattern sp : f.getMemberPatterns()) {
                        if (sp.isCommutative()) {
                            return true;
                        }
                    }
                }
            }
        }
    }
    return false;
  }

  private void updateLogicalFamilyStatus() {
    Map<PatternFamily, Set<RelationPattern>> relationsByFamily =
        new HashMap<PatternFamily, Set<RelationPattern>>();

    for (Pattern p : allPatterns) {
      if (p instanceof RelationPattern) {
        RelationPattern rp = (RelationPattern) p;
        PatternFamily f = rp.getFamily();
        if (f != null) {
          if (!relationsByFamily.containsKey(f)) {
            relationsByFamily.put(f, new HashSet<RelationPattern>());
          }
          relationsByFamily.get(f).add(rp);
        }
      }
    }

    boolean logicFound = false;
    for (Map.Entry<PatternFamily, Set<RelationPattern>> entry : relationsByFamily.entrySet()) {
      PatternFamily family = entry.getKey();
      Set<RelationPattern> relations = entry.getValue();

      if (family.isLogical()) continue;
    
      Set<String> aT1w = new HashSet<String>();
      Set<String> aT2w = new HashSet<String>();

      for (RelationPattern rp : relations) {
        aT1w.add(rp.getT1());
        aT2w.add(rp.getT2());

        if (rp.isCommutative()) {
          aT1w.add(rp.getT2());
          aT2w.add(rp.getT1());
        }
      }

      Set<String> intersection = new HashSet<String>(aT1w);
      intersection.retainAll(aT2w);

      if (!intersection.isEmpty()) {
        family.setLogical(true);
        System.out.println("   [System: Emergent logic detected for " + family.getId() + "]");
        logicFound = true;
      }
    }
    if (logicFound) {
      familiesDirty = true;
    }
  }
}