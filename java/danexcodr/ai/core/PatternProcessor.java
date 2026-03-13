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
  
  private Set<String> learnedOptionalWords = new HashSet<String>();
  private List<Pattern> allPatterns; // Now only contains RelationPatterns

  boolean familiesDirty;
  
  private Map<String, List<RelationPattern>> relationPatternsByT1 = new HashMap<String, List<RelationPattern>>();
  private Map<String, List<RelationPattern>> relationPatternsByT2 = new HashMap<String, List<RelationPattern>>();
  private Map<String, RelationPattern> relationPatternsByKey = new HashMap<String, RelationPattern>();

  public PatternProcessor(
      SymbolManager symbolManager,
      PatternFamilyBuilder patternFamilyBuilder,
      PatternFamilyManager patternFamilyManager,
      OptionalFinder optionalFinder,
      BigramAnalyzer bigramAnalyzer,
      Map<String, Set<String>> structuralEquivalents,
      List<Pattern> allPatterns,
      boolean familiesDirty) {
    this.symbolManager = symbolManager;
    this.patternFamilyBuilder = patternFamilyBuilder;
    this.patternFamilyManager = patternFamilyManager;
    this.optionalFinder = optionalFinder;
    this.bigramAnalyzer = bigramAnalyzer;
    this.structuralEquivalents = structuralEquivalents;
    this.allPatterns = allPatterns;
    this.familiesDirty = familiesDirty;
    initializeRelationMaps();
  }

  private void initializeRelationMaps() {
    for (Pattern pattern : allPatterns) {
        if (pattern instanceof RelationPattern) {
            RelationPattern rp = (RelationPattern) pattern;
            addToRelationMaps(rp);
        }
    }
  }
  
  private void addToRelationMaps(RelationPattern rp) {
    if (!relationPatternsByT1.containsKey(rp.getT1())) {
        relationPatternsByT1.put(rp.getT1(), new ArrayList<RelationPattern>());
    }
    relationPatternsByT1.get(rp.getT1()).add(rp);
    
    if (!relationPatternsByT2.containsKey(rp.getT2())) {
        relationPatternsByT2.put(rp.getT2(), new ArrayList<RelationPattern>());
    }
    relationPatternsByT2.get(rp.getT2()).add(rp);
    
    String key = rp.getT1() + "|" + rp.getT2();
    relationPatternsByKey.put(key, rp);
    
    if (rp.isCommutative()) {
        String reverseKey = rp.getT2() + "|" + rp.getT1();
        relationPatternsByKey.put(reverseKey, rp);
    }
  }
  
  private void removeFromRelationMaps(RelationPattern rp) {
    List<RelationPattern> t1List = relationPatternsByT1.get(rp.getT1());
    if (t1List != null) {
        t1List.remove(rp);
        if (t1List.isEmpty()) {
            relationPatternsByT1.remove(rp.getT1());
        }
    }
    
    List<RelationPattern> t2List = relationPatternsByT2.get(rp.getT2());
    if (t2List != null) {
        t2List.remove(rp);
        if (t2List.isEmpty()) {
            relationPatternsByT2.remove(rp.getT2());
        }
    }
    
    String key = rp.getT1() + "|" + rp.getT2();
    relationPatternsByKey.remove(key);
    
    if (rp.isCommutative()) {
        String reverseKey = rp.getT2() + "|" + rp.getT1();
        relationPatternsByKey.remove(reverseKey);
    }
  }

  public Set<String> getLearnedOptionalWords() {
    return learnedOptionalWords;
  }

  public void setEquivalenceDetector(StructuralEquivalenceDetector equivalenceDetector) {
    this.equivalenceDetector = equivalenceDetector;
  }

  public List<RelationPattern> findAllRelationPatterns(String term1, String term2) {
    List<RelationPattern> results = new ArrayList<RelationPattern>();
    
    String directKey = term1 + "|" + term2;
    RelationPattern directPattern = relationPatternsByKey.get(directKey);
    if (directPattern != null) {
        results.add(directPattern);
    }
    
    List<RelationPattern> fromT1 = relationPatternsByT1.get(term1);
    if (fromT1 != null) {
        for (RelationPattern rp : fromT1) {
            if (rp.getT2().equals(term2) && !results.contains(rp)) {
                results.add(rp);
            }
        }
    }
    
    List<RelationPattern> fromT2 = relationPatternsByT2.get(term2);
    if (fromT2 != null) {
        for (RelationPattern rp : fromT2) {
            if (rp.getT1().equals(term1) && rp.isCommutative() && !results.contains(rp)) {
                results.add(rp);
            }
        }
    }
    
    return results;
  }

  public List<RelationPattern> findAllRelationPatternsWithTerm(String term) {
    List<RelationPattern> results = new ArrayList<RelationPattern>();
    
    List<RelationPattern> fromTerm = relationPatternsByT1.get(term);
    if (fromTerm != null) {
        results.addAll(fromTerm);
    }
    
    List<RelationPattern> toTerm = relationPatternsByT2.get(term);
    if (toTerm != null) {
        for (RelationPattern rp : toTerm) {
            if (!results.contains(rp)) {
                results.add(rp);
            }
        }
    }
    
    return results;
  }

  public RelationPattern findRelationPattern(String term1, String term2, PatternFamily family) {
    String key = term1 + "|" + term2;
    RelationPattern rp = relationPatternsByKey.get(key);
    
    if (rp != null) {
        if (family == null || (rp.getFamilyId() != null && rp.getFamilyId().equals(family.getId()))) {
            return rp;
        }
    }
    
    if (relationPatternsByKey.containsKey(term2 + "|" + term1)) {
        rp = relationPatternsByKey.get(term2 + "|" + term1);
        if (rp != null && rp.isCommutative()) {
            if (family == null || (rp.getFamilyId() != null && rp.getFamilyId().equals(family.getId()))) {
                return rp;
            }
        }
    }
    
    return null;
  }

  public RelationPattern findRelationPattern(String term1, String term2) {
    return findRelationPattern(term1, term2, null);
  }
  
  public RelationPattern getFamilyForRelation(String term1, String term2) {
    String key = term1 + "|" + term2;
    RelationPattern rp = relationPatternsByKey.get(key);
    if (rp != null && rp.getFamilyId() != null) {
        return rp;
    }
    
    String reverseKey = term2 + "|" + term1;
    rp = relationPatternsByKey.get(reverseKey);
    if (rp != null && rp.isCommutative() && rp.getFamilyId() != null) {
        return rp;
    }
    
    return null;
}

  public void processEquivalenceSet(List<List<String>> equivalentSequences) {
    // EARLY EXIT: Check for empty input
    if (equivalentSequences == null || equivalentSequences.isEmpty()) {
        return;
    }
    
    // CACHE 1: Get all words in sequences (compute once)
    Set<String> allWordsInSequences = getAllWords(equivalentSequences);
    
    // CACHE 2: Get optionals once
    optionalFinder.clearOptionals();
    optionalFinder.preprocessAndIdentifyOptionals(equivalentSequences);
    Set<String> optionals = optionalFinder.getOptionals();
    learnedOptionalWords.addAll(optionals);
    
    // Remove optionals from consideration early
    Set<String> nonOptionalWords = new HashSet<String>(allWordsInSequences);
    nonOptionalWords.removeAll(optionals);
    
    // Only proceed if we have content to process
    if (nonOptionalWords.isEmpty()) {
        System.out.println("   All words are optional, skipping analysis.");
        return;
    }
    
    // CACHE 3: Content analysis - compute once and reuse
    ContentFinder rawContentFinder = new ContentFinder(optionalFinder, bigramAnalyzer);
    Set<String> contentWords = rawContentFinder.identifyContentWords(equivalentSequences, null);
    
    // Fallback checks (only if needed)
    if (contentWords.size() < 2) {
        contentWords = rawContentFinder.runAnyMovementCheck(equivalentSequences, null);
    }
    if (contentWords.size() < 2) {
        contentWords = rawContentFinder.runParityFallback(equivalentSequences);
    }
    
    // CACHE 4: Protected words (structural words that aren't content)
    Set<String> protectedWords = new HashSet<String>();
    for (String word : nonOptionalWords) {
        if (!contentWords.contains(word)) {
            protectedWords.add(word);
        }
    }
    
    // CACHE 5: Word positions (compute once)
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
    
    // CACHE 6: Learned structural words (compute once)
    Set<String> learnedStructuralWords = getLearnedStructuralWords();
    
    // CACHE 7: Bigram analysis (compute once)
    Set<String> bigramContentWords = bigramAnalyzer.runBigramAnalysis(equivalentSequences, nonOptionalWords);
    if (!bigramContentWords.isEmpty()) {
        System.out.println("   [System: Bigram analysis detected moving word pairs]");
        // Use bigram results if they found content
        if (!bigramContentWords.isEmpty()) {
            contentWords = bigramContentWords;
        }
    }
    
    // Build context only if needed (avoid redundant building)
    boolean needsContextBuild = false;
    if (!equivalentSequences.isEmpty() && !equivalentSequences.get(0).isEmpty()) {
        String firstWord = equivalentSequences.get(0).get(0);
        if (!firstWord.startsWith("PF")) {
            Symbol s = symbolManager.getSymbols().get(firstWord);
            needsContextBuild = (s == null || (s.leftContext.isEmpty() && s.rightContext.isEmpty()));
        }
    }
    
    if (needsContextBuild) {
        // Build context for all sequences at once
        for (List<String> seq : equivalentSequences) {
            symbolManager.buildContext(seq);
        }
    }
    
    // Reevaluate equivalents once
    reevaluateEquivalents();
    
    // If content analysis failed, try collapse
    if (contentWords.size() < 2) {
        System.out.println("   Analysis failed to find content. Trying collapse...");
        attemptCollapseAndAnalysis(equivalentSequences);
        return;
    }
    
    // Use cached wordPositions for term selection
    Set<String> preliminaryStructuralWords = identifyStructuralWords(equivalentSequences, contentWords);
    String[] preliminaryTerms = selectTermsByClosestCompanion(contentWords, equivalentSequences, wordPositions, preliminaryStructuralWords);
    
    if (preliminaryTerms == null) {
        System.out.println("   Could not select preliminary terms from content: " + contentWords + ". Trying collapse...");
        attemptCollapseAndAnalysis(equivalentSequences);
        return;
    }
    
    // Use cached equivalentSequences for positional term determination
    String[] positionalTerms = determinePositionalTerms(preliminaryTerms[0], preliminaryTerms[1], equivalentSequences);
    
    // CHANGED: Extract structural patterns first (temporary)
    List<StructuralPattern> tempPatterns = extractStructuralPatterns(equivalentSequences, positionalTerms[0], positionalTerms[1]);
    
    // Use cached allPatterns for equivalence detection
    Set<StructuralEquivalenceDetector.EquivalencePair> newPairs = 
        equivalenceDetector.detectStructuralEquivalents(equivalentSequences, preliminaryStructuralWords, 
                                                      positionalTerms[0], positionalTerms[1], 
                                                      getCurrentPatternsForDetection());
    addDiscoveredEquivalents(newPairs);
    
    // Use cached learnedStructuralWords for final content check
    Set<String> finalContentWords = contentWords; // Start with cached
    
    // Only recompute if structure changed significantly
    if (!learnedStructuralWords.containsAll(preliminaryStructuralWords)) {
        ContentFinder contentFinder = new ContentFinder(optionalFinder, bigramAnalyzer);
        finalContentWords = contentFinder.identifyContentWords(equivalentSequences, learnedStructuralWords);
        if (finalContentWords.size() < 2) {
            finalContentWords = contentFinder.runAnyMovementCheck(equivalentSequences, learnedStructuralWords);
        }
        if (finalContentWords.size() < 2) {
            finalContentWords = contentFinder.runParityFallback(equivalentSequences);
        }
    }
    
    if (finalContentWords.size() < 2) {
        System.out.println("   Analysis failed after refinement. Trying collapse...");
        attemptCollapseAndAnalysis(equivalentSequences);
        return;
    }
    
    // Use cached equivalentSequences for remaining processing
    Set<String> finalStructuralWords = identifyStructuralWords(equivalentSequences, finalContentWords);
    String[] finalTerms = selectTermsByClosestCompanion(finalContentWords, equivalentSequences, wordPositions, finalStructuralWords);
    
    if (finalTerms == null) {
        System.out.println("   Could not select final terms from content. Trying collapse...");
        attemptCollapseAndAnalysis(equivalentSequences);
        return;
    }
    
    String[] finalPositionalTerms = determinePositionalTerms(finalTerms[0], finalTerms[1], equivalentSequences);
    
    // CHANGED: Extract patterns again with final terms
    tempPatterns = extractStructuralPatterns(equivalentSequences, finalPositionalTerms[0], finalPositionalTerms[1]);
    
    // CHANGED: Update families with temporary patterns
    updateFamiliesWithPatterns(tempPatterns, finalPositionalTerms[0], finalPositionalTerms[1]);
    
    detectCrossDomainEquivalents(equivalentSequences, finalPositionalTerms[0], finalPositionalTerms[1]);
    
    // Use cached equivalentSequences for commutativity check
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

    updateAllFamiliesWithNewEquivalents();
    
    // FETCH FAMILIES ONCE AND REUSE
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(
        structuralEquivalents, patternFamilyBuilder); // CHANGED: Removed familiesDirty parameter

    PatternFamily patternFamily = null;
    
    for (PatternFamily family : families) {
        for (StructuralPattern sp : family.getMemberPatterns()) {
            if (!equivalentSequences.isEmpty()) {
                List<String> exampleSeq = equivalentSequences.get(0);
                List<String> abstractPattern = SequenceTransformer.abstractSequencePF(
                    exampleSeq, finalPositionalTerms[0], finalPositionalTerms[1]);
                
                boolean matches = false;
                
                if (sp.getStructuralSlots().equals(abstractPattern)) {
                    matches = true;
                }
                
                if (!matches && isCommutative) {
                    List<String> flippedPattern = SequenceTransformer.flipTermPatternWithTerms(
                        abstractPattern, finalPositionalTerms[0], finalPositionalTerms[1]);
                    if (sp.getStructuralSlots().equals(flippedPattern)) {
                        matches = true;
                    }
                }
                
                if (!matches && isCommutative) {
                    List<String> patternWithC = new ArrayList<String>();
                    for (String token : abstractPattern) {
                        if (token.equals("[1]") || token.equals("[2]")) {
                            patternWithC.add("[C]");
                        } else {
                            patternWithC.add(token);
                        }
                    }
                    if (sp.getStructuralSlots().equals(patternWithC)) {
                        matches = true;
                    }
                }
                
                if (matches) {
                    patternFamily = family;
                    break;
                }
            }
        }
        if (patternFamily != null) break;
    }

    RelationPattern pattern = findOrCreatePatternForFamily(
        finalPositionalTerms[0], finalPositionalTerms[1], patternFamily, isCommutative);
        
    pattern.incrementFrequency();
    for (List<String> seq : equivalentSequences) {
        pattern.addConcreteExample(new ArrayList<String>(seq));
    }
    pattern.setBaseTermsT1(getBaseTermsForSyntheticToken(finalPositionalTerms[0]));
    pattern.setBaseTermsT2(getBaseTermsForSyntheticToken(finalPositionalTerms[1]));

    addToRelationMaps(pattern);

    if (isCommutative) {
        RelationPattern reversePattern = findOrCreatePatternForFamily(
            finalPositionalTerms[1], finalPositionalTerms[0], patternFamily, isCommutative);
        reversePattern.incrementFrequency();
        for (List<String> seq : equivalentSequences) {
            reversePattern.addConcreteExample(new ArrayList<String>(seq));
        }
        reversePattern.setBaseTermsT1(getBaseTermsForSyntheticToken(finalPositionalTerms[1]));
        reversePattern.setBaseTermsT2(getBaseTermsForSyntheticToken(finalPositionalTerms[0]));
        
        addToRelationMaps(reversePattern);
    }

    for (String word : finalContentWords) addSymbolRole(word, "C");
    addSymbolRole(finalPositionalTerms[0], "1");
    addSymbolRole(finalPositionalTerms[1], "2");
    for (String word : finalStructuralWords) addSymbolRole(word, "S");

    familiesDirty = true;
    // REUSE the families list we already fetched
    families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);

    for (Pattern p : allPatterns) {
        if (!(p instanceof RelationPattern)) continue;
        RelationPattern rp = (RelationPattern) p;
        
        if (rp.getFamilyId() != null) continue;
        
        if (rp.getConcreteExamples().isEmpty()) continue;
        
        boolean familyAssigned = false;
        for (PatternFamily family : families) {
            if (familyAssigned) break;
            
            for (List<String> seq : rp.getConcreteExamples()) {
                if (familyAssigned) break;
                
                List<String> abstractPattern = SequenceTransformer.abstractSequencePF(seq, rp.getT1(), rp.getT2());
                List<String> flippedPattern = SequenceTransformer.flipTermPatternWithTerms(abstractPattern, rp.getT1(), rp.getT2());
                
                for (StructuralPattern sp : family.getMemberPatterns()) {
                    if (familyAssigned) break;
                    
                    boolean matchesNormal = sp.getStructuralSlots().equals(abstractPattern);
                    boolean matchesFlipped = rp.isCommutative() && sp.getStructuralSlots().equals(flippedPattern);
                    
                    if (!matchesNormal && !matchesFlipped && rp.isCommutative()) {
                        List<String> patternWithC = new ArrayList<String>();
                        for (String token : abstractPattern) {
                            if (token.equals("[1]") || token.equals("[2]")) {
                                patternWithC.add("[C]");
                            } else {
                                patternWithC.add(token);
                            }
                        }
                        matchesNormal = sp.getStructuralSlots().equals(patternWithC);
                        
                        List<String> flippedWithC = new ArrayList<String>();
                        for (String token : flippedPattern) {
                            if (token.equals("[1]") || token.equals("[2]")) {
                                flippedWithC.add("[C]");
                            } else {
                                flippedWithC.add(token);
                            }
                        }
                        matchesFlipped = sp.getStructuralSlots().equals(flippedWithC);
                    }
                    
                    if (matchesNormal || matchesFlipped) {
                        rp.setFamily(family);
                        familyAssigned = true;
                        break;
                    }
                }
            }
        }
    }
    
    if (!optionals.isEmpty()) {
        List<String> sortedOpt = new ArrayList<String>(optionals);
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

  // NEW: Get current patterns for detection (from families)
  private List<Pattern> getCurrentPatternsForDetection() {
    List<Pattern> patternsForDetection = new ArrayList<Pattern>();
    
    // Add relation patterns
    patternsForDetection.addAll(allPatterns);
    
    // Add structural patterns from families
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(
        structuralEquivalents, patternFamilyBuilder);
    
    for (PatternFamily family : families) {
        for (StructuralPattern sp : family.getMemberPatterns()) {
            patternsForDetection.add(sp);
        }
    }
    
    return patternsForDetection;
  }

  // NEW: Update families with temporary patterns
  private void updateFamiliesWithPatterns(List<StructuralPattern> tempPatterns, String term1, String term2) {
    if (tempPatterns.isEmpty()) return;
    
    familiesDirty = true;
    patternFamilyManager.updateFamiliesWithPatterns(tempPatterns, structuralEquivalents);
  }

  private void updateAllFamiliesWithNewEquivalents() {
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);
    for (PatternFamily family : families) {
        family.updateAliases(structuralEquivalents);
    }
    familiesDirty = true;
  }

  private RelationPattern findOrCreatePatternForFamily(String term1, String term2, PatternFamily family, boolean isCommutative) {
    if (family == null) {
        return findOrCreatePattern(term1, term2);
    }
    
    String key = term1 + "|" + term2;
    RelationPattern existing = relationPatternsByKey.get(key);
    
    if (existing != null) {
        if (family == null || (existing.getFamilyId() != null && existing.getFamilyId().equals(family.getId()))) {
            existing.updateTimestamp();
            return existing;
        }
    }
    
    if (isCommutative) {
        String reverseKey = term2 + "|" + term1;
        existing = relationPatternsByKey.get(reverseKey);
        if (existing != null && existing.isCommutative()) {
            if (family == null || (existing.getFamilyId() != null && existing.getFamilyId().equals(family.getId()))) {
                existing.updateTimestamp();
                return existing;
            }
        }
    }
    
    RelationPattern newPattern = new RelationPattern("RP" + allPatterns.size(), term1, term2);
    newPattern.setFamily(family);
    newPattern.setCommutative(isCommutative);
    allPatterns.add(newPattern);
    addToRelationMaps(newPattern);
    familiesDirty = true;
    
    return newPattern;
  }

  private RelationPattern findOrCreatePattern(String term1, String term2) {
    String key = term1 + "|" + term2;
    RelationPattern existing = relationPatternsByKey.get(key);
    
    if (existing != null) {
        existing.updateTimestamp();
        return existing;
    }
    
    String reverseKey = term2 + "|" + term1;
    existing = relationPatternsByKey.get(reverseKey);
    if (existing != null && existing.isCommutative()) {
        existing.updateTimestamp();
        return existing;
    }
    
    RelationPattern newPattern = new RelationPattern("RP" + allPatterns.size(), term1, term2);
    allPatterns.add(newPattern);
    addToRelationMaps(newPattern);
    familiesDirty = true;
    return newPattern;
  }

  private void attemptCollapseAndAnalysis(List<List<String>> originalSequences) {
    Set<String> protectedWords = new HashSet<String>();
    Set<String> allBatchWords = getAllWords(originalSequences);
    allBatchWords.removeAll(optionalFinder.getOptionals());

    ContentFinder rawContentFinder = new ContentFinder(optionalFinder, bigramAnalyzer);
    Set<String> batchContentWords = rawContentFinder.identifyContentWords(originalSequences, null);
    
    if (batchContentWords.size() < 2) {
        batchContentWords = rawContentFinder.runAnyMovementCheck(originalSequences, null);
    }
    
    for (String word : allBatchWords) {
        if (!batchContentWords.contains(word)) {
            protectedWords.add(word);
        }
    }
    
    List<List<String>> collapsedSequences = collapseSequences(originalSequences, protectedWords);
    
    if (collapsedSequences != null && !collapsedSequences.isEmpty()) {
        if (collapsedSequences.get(0).size() <= 1) {
            System.out.println("   [System: Sequence recognized as existing family instance: " + collapsedSequences.get(0) + "]");
            assignRolesFromFamilyMatch(originalSequences);
            return;
        }
        
        System.out.println("   [System: Sequence collapsed for higher-level analysis: " + collapsedSequences.get(0) + "]");
        System.out.println("   [System: Composite patterns are no longer supported. Treating as regular pattern.]");
        processEquivalenceSet(originalSequences);
        return;
    }
    
    System.out.println("   Set is ambiguous and cannot be collapsed.");
  }

  private void assignRolesFromFamilyMatch(List<List<String>> sequences) {
      List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);
      for (List<String> sequence : sequences) {
          boolean matchedSequence = false;
          for (PatternFamily family : families) {
              if (matchedSequence) break;
              for (StructuralPattern sp : family.getMemberPatterns()) {
                  List<String> slots = sp.getStructuralSlots();
                  if (sequence.size() != slots.size()) continue;
                  if (PatternMatcher.isSubSequenceMatch(sequence, 0, slots, family, new HashSet<String>())) {
                      for (int i = 0; i < slots.size(); i++) {
                          String slot = slots.get(i);
                          String word = sequence.get(i);
                          if (slot.equals("[1]")) {
                              addSymbolRole(word, "1");
                              addSymbolRole(word, "C");
                          } else if (slot.equals("[2]")) {
                              addSymbolRole(word, "2");
                              addSymbolRole(word, "C");
                          } else if (slot.equals("[C]") || slot.equals("[X]")) {
                              addSymbolRole(word, "C");
                          }
                      }
                      matchedSequence = true;
                      break; 
                  }
              }
          }
      }
  }

  private void addSymbolRole(String word, String role) {
      if (!word.startsWith("PF") && symbolManager.getSymbols().containsKey(word)) {
          symbolManager.getSymbols().get(word).relations.add(role);
      }
  }
  
  private Set<String> getBaseTermsForSyntheticToken(String token) {
    if (!token.startsWith("PF")) return Collections.singleton(token);
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);
    PatternFamily targetFamily = null;
    for (PatternFamily f : families) {
        if (f.getId().equals(token)) { targetFamily = f; break; }
    }
    Set<String> baseTerms = new HashSet<String>();
    if (targetFamily != null) {
        for (Pattern p : allPatterns) {
            if (p instanceof RelationPattern) {
                RelationPattern rp = (RelationPattern) p;
                if (rp.getFamilyId() != null && rp.getFamilyId().equals(token)) {
                    baseTerms.add(rp.getT1()); baseTerms.add(rp.getT2());
                }
            }
        }
    } else { baseTerms.add(token); }
    Set<String> finalBaseTerms = new HashSet<String>();
    for (String term : baseTerms) {
        if (term.startsWith("PF")) finalBaseTerms.addAll(getBaseTermsForSyntheticToken(term));
        else finalBaseTerms.add(term);
    }
    return finalBaseTerms;
  }

  private Set<String> getAllWords(List<List<String>> sequences) {
    Set<String> allWords = new HashSet<String>();
    for (List<String> sequence : sequences) allWords.addAll(sequence);
    return allWords;
  }

  private String[] selectTermsByClosestCompanion(Set<String> contentWords, List<List<String>> sequences, Map<String, Set<Integer>> wordPositions, Set<String> structuralWords) {
    if (contentWords.size() < 2) return null;
    Set<String> filteredContentWords = new HashSet<String>(contentWords);
    filteredContentWords.removeAll(structuralWords);
    filteredContentWords.removeAll(optionalFinder.getOptionals());
    if (filteredContentWords.size() < 2) filteredContentWords = new HashSet<String>(contentWords);
    List<String> contentList = new ArrayList<String>(filteredContentWords);
    Collections.sort(contentList);
    return new String[] {contentList.get(0), contentList.get(1)};
  }

  private Set<String> identifyStructuralWords(List<List<String>> equivalentSequences, Set<String> contentWords) {
    Set<String> structuralWords = new LinkedHashSet<String>();
    for (List<String> sequence : equivalentSequences) {
        for (String word : sequence) {
            if (!contentWords.contains(word)) structuralWords.add(word);
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
            if (idx1 < idx2) w1_left_of_w2++; else if (idx2 < idx1) w2_left_of_w1++;
        }
    }
    return w2_left_of_w1 > w1_left_of_w2 ? new String[] {w2, w1} : new String[] {w1, w2};
  }

  private boolean isCommutative(List<List<String>> equivalentSequences, String term1, String term2) {
    Set<List<String>> patterns = new HashSet<List<String>>();
    for (List<String> sequence : equivalentSequences) patterns.add(SequenceTransformer.abstractSequencePF(sequence, term1, term2));
    Set<List<String>> flippedPatterns = new HashSet<List<String>>();
    for (List<String> pattern : patterns) flippedPatterns.add(SequenceTransformer.flipTermPatternWithTerms(pattern, term1, term2));
    return patterns.containsAll(flippedPatterns) && flippedPatterns.containsAll(patterns);
  }

  private void addDiscoveredEquivalents(Set<StructuralEquivalenceDetector.EquivalencePair> newPairs) {
    if (!newPairs.isEmpty()) {
      familiesDirty = true;
      for (StructuralEquivalenceDetector.EquivalencePair pair : newPairs) addStructuralEquivalence(pair.w1, pair.w2);
    }
  }

  // CHANGED: Return temporary patterns instead of storing in allPatterns
  private List<StructuralPattern> extractStructuralPatterns(List<List<String>> equivalentSequences, String term1, String term2) {
    Map<List<String>, Integer> patternCounts = new HashMap<List<String>, Integer>();
    for (List<String> sequence : equivalentSequences) {
      List<String> abstractPattern = SequenceTransformer.abstractSequencePF(sequence, term1, term2);
      patternCounts.put(abstractPattern, patternCounts.get(abstractPattern) == null ? 1 : patternCounts.get(abstractPattern) + 1);
    }
    
    boolean isCommutative = isCommutative(equivalentSequences, term1, term2);
    
    List<StructuralPattern> tempPatterns = new ArrayList<StructuralPattern>();
    
    for (Entry<List<String>, Integer> entry : patternCounts.entrySet()) {
      List<String> finalSlots = PatternSlotFiller.createFinalPatternSlots(entry.getKey(), term1, term2, isCommutative);
      StructuralPattern newPattern = new StructuralPattern("TEMP", finalSlots);
      newPattern.setFrequency(entry.getValue());
      newPattern.setCommutative(isCommutative);
      tempPatterns.add(newPattern); // Temporary, not added to allPatterns
    }
    
    return tempPatterns;
  }

  private void addStructuralEquivalence(String w1, String w2) {
    if (!structuralEquivalents.containsKey(w1)) structuralEquivalents.put(w1, new HashSet<String>());
    if (!structuralEquivalents.containsKey(w2)) structuralEquivalents.put(w2, new HashSet<String>());
    
    boolean changed1 = structuralEquivalents.get(w1).add(w2);
    boolean changed2 = structuralEquivalents.get(w2).add(w1);
    
    if (changed1 || changed2) {
        familiesDirty = true;
    }
    
    addSymbolRole(w1, "S"); 
    addSymbolRole(w2, "S");
  }

  public Set<String> getLearnedStructuralWords() {
    Set<String> filtered = new HashSet<String>();
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);
    
    for (PatternFamily family : families) {
        for (StructuralPattern sp : family.getMemberPatterns()) {
            for (String word : sp.getStructuralSlots()) {
                if (!word.equals("[1]") && !word.equals("[2]") && 
                    !word.equals("[C]") && !word.equals("[X]") && 
                    !word.startsWith("PF")) {
                    filtered.add(word);
                }
            }
        }
    }
    return filtered;
  }

  private void detectCrossDomainEquivalents(List<List<String>> equivalentSequences, String t1, String t2) {
    Set<StructuralEquivalenceDetector.EquivalencePair> crossDomainPairs = equivalenceDetector.detectStructuralEquivalents(equivalentSequences, getLearnedStructuralWords(), t1, t2, getCurrentPatternsForDetection());
    if (!crossDomainPairs.isEmpty()) {
        System.out.println("   [System: Found " + crossDomainPairs.size() + " cross-domain equivalents]");
        addDiscoveredEquivalents(crossDomainPairs);
    }
  }

  public void reevaluateEquivalents() {
    Set<StructuralEquivalenceDetector.EquivalencePair> pairsToRemove = new HashSet<StructuralEquivalenceDetector.EquivalencePair>();
    for (Entry<String, Set<String>> entry : structuralEquivalents.entrySet()) {
      for (String w2 : entry.getValue()) {
        if (equivalenceDetector.areNeighbors(entry.getKey(), w2)) pairsToRemove.add(equivalenceDetector.createEquivalencePair(entry.getKey(), w2));
      }
    }
    boolean changed = false;
    for (StructuralEquivalenceDetector.EquivalencePair pair : pairsToRemove) {
      if (structuralEquivalents.containsKey(pair.w1) && structuralEquivalents.get(pair.w1).remove(pair.w2)) {
          if (structuralEquivalents.get(pair.w1).isEmpty()) structuralEquivalents.remove(pair.w1); changed = true;
      }
      if (structuralEquivalents.containsKey(pair.w2) && structuralEquivalents.get(pair.w2).remove(pair.w1)) {
          if (structuralEquivalents.get(pair.w2).isEmpty()) structuralEquivalents.remove(pair.w2); changed = true;
      }
    }
    if (changed) {
        System.out.println("   [System: Reevaluated structural equivalents based on context.]");
        familiesDirty = true;
    }
  }

  private List<List<String>> collapseSequences(List<List<String>> sequences, Set<String> protectedWords) {
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);
    if (families.isEmpty()) return null;
    List<List<String>> collapsedList = new ArrayList<List<String>>();
    boolean anyChange = false;
    for (List<String> sequence : sequences) {
        List<String> collapsed = collapseSequence(sequence, families, protectedWords);
        if (collapsed.size() < sequence.size()) anyChange = true;
        collapsedList.add(collapsed);
    }
    return anyChange ? collapsedList : null;
  }

  private List<String> collapseSequence(List<String> sequence, List<PatternFamily> families, Set<String> protectedWords) {
    List<String> currentSequence = new ArrayList<String>(sequence);
    boolean changeOccurred = true;
    while (changeOccurred) {
        changeOccurred = false;
        int bestMatchStart = -1, bestMatchLength = -1;
        String bestFamilyId = null;
        for (PatternFamily family : families) {
            for (StructuralPattern sp : family.getMemberPatterns()) {
                List<String> slots = sp.getStructuralSlots();
                for (int i = 0; i <= currentSequence.size() - slots.size(); i++) {
                     if (PatternMatcher.isSubSequenceMatch(currentSequence, i, slots, family, protectedWords)) {
                         if (slots.size() > bestMatchLength) { bestMatchLength = slots.size(); bestMatchStart = i; bestFamilyId = family.getId(); }
                     }
                }
            }
        }
        if (bestMatchStart != -1) {
            List<String> nextSequence = new ArrayList<String>(currentSequence.subList(0, bestMatchStart));
            nextSequence.add(bestFamilyId);
            nextSequence.addAll(currentSequence.subList(bestMatchStart + bestMatchLength, currentSequence.size()));
            currentSequence = nextSequence; changeOccurred = true;
        }
    }
    return currentSequence;
  }

  public boolean hasCommutativeCollapse(List<String> sequence) {
    List<PatternFamily> families = patternFamilyManager.getPatternFamilies(structuralEquivalents, patternFamilyBuilder);
    if (families.isEmpty()) return false;
    List<String> collapsed = collapseSequence(sequence, families, new HashSet<String>());
    if (collapsed.size() == sequence.size()) return false;
    for (String token : collapsed) {
        if (token.startsWith("PF")) {
            for (PatternFamily f : families) {
                if (f.getId().equals(token)) {
                    for (StructuralPattern sp : f.getMemberPatterns()) if (sp.isCommutative()) return true;
                }
            }
        }
    }
    return false;
  }
  
  private PatternFamily findFamilyById(String id, List<PatternFamily> families) {
    for (PatternFamily family : families) {
        if (family.getId().equals(id)) {
            return family;
        }
    }
    return null;
  }
}