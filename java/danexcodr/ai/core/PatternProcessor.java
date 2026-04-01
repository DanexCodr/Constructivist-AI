package danexcodr.ai.core;

import danexcodr.ai.*;
import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternProcessor {

  private SymbolManager symbolManager;
  private PatternFamilyBuilder patternFamilyBuilder;
  private PatternFamilyManager patternFamilyManager;
  private OptionalFinder optionalFinder;
  private GramAnalyzer gramAnalyzer;
  private StructuralEquivalenceDetector equivalenceDetector;
  private Map<String, Set<String>> structuralEquivalents;
  private TermSelector termSelector;
  private PatternExtractor patternExtractor;
  private SequenceCollapser sequenceCollapser;

  private Set<String> learnedOptionalTokens = new HashSet<String>();
  private List<Pattern> allPatterns;
  private Map<String, List<Content>> relationPatternsByT1 = new HashMap<String, List<Content>>();
  private Map<String, List<Content>> relationPatternsByT2 = new HashMap<String, List<Content>>();
  private Map<String, Content> relationPatternsByKey = new HashMap<String, Content>();

  boolean familiesDirty;

  public PatternProcessor(
      SymbolManager symbolManager,
      PatternFamilyBuilder patternFamilyBuilder,
      PatternFamilyManager patternFamilyManager,
      OptionalFinder optionalFinder,
      GramAnalyzer gramAnalyzer,
      Map<String, Set<String>> structuralEquivalents,
      List<Pattern> allPatterns,
      boolean familiesDirty) {
    this.symbolManager = symbolManager;
    this.patternFamilyBuilder = patternFamilyBuilder;
    this.patternFamilyManager = patternFamilyManager;
    this.optionalFinder = optionalFinder;
    this.gramAnalyzer = gramAnalyzer;
    this.structuralEquivalents = structuralEquivalents;
    this.allPatterns = allPatterns;
    this.familiesDirty = familiesDirty;
    this.termSelector = new TermSelector();
    this.patternExtractor = new PatternExtractor();
    this.sequenceCollapser =
        new SequenceCollapser(patternFamilyManager, patternFamilyBuilder, structuralEquivalents);
    initialize();
  }

  private void initialize() {
    for (Pattern pattern : allPatterns) {
      if (pattern instanceof Content) {
        Content rp = (Content) pattern;
        addToRelationMaps(rp);
      }
    }
  }

  private void addToRelationMaps(Content rp) {
    if (!relationPatternsByT1.containsKey(rp.getT1())) {
      relationPatternsByT1.put(rp.getT1(), new ArrayList<Content>());
    }
    relationPatternsByT1.get(rp.getT1()).add(rp);

    if (!relationPatternsByT2.containsKey(rp.getT2())) {
      relationPatternsByT2.put(rp.getT2(), new ArrayList<Content>());
    }
    relationPatternsByT2.get(rp.getT2()).add(rp);

    String key = rp.getT1() + "|" + rp.getT2();
    relationPatternsByKey.put(key, rp);

    if (rp.isCommutative()) {
      String reverseKey = rp.getT2() + "|" + rp.getT1();
      relationPatternsByKey.put(reverseKey, rp);
    }
  }

  public Set<String> getLearnedOptionalTokens() {
    return learnedOptionalTokens;
  }

  public void setEquivalenceDetector(StructuralEquivalenceDetector equivalenceDetector) {
    this.equivalenceDetector = equivalenceDetector;
  }

  public List<Content> findAllContents(String term1, String term2) {
    List<Content> results = new ArrayList<Content>();

    String directKey = term1 + "|" + term2;
    Content directPattern = relationPatternsByKey.get(directKey);
    if (directPattern != null) {
      results.add(directPattern);
    }

    List<Content> fromT1 = relationPatternsByT1.get(term1);
    if (fromT1 != null) {
      for (Content rp : fromT1) {
        if (rp.getT2().equals(term2) && !results.contains(rp)) {
          results.add(rp);
        }
      }
    }

    List<Content> fromT2 = relationPatternsByT2.get(term2);
    if (fromT2 != null) {
      for (Content rp : fromT2) {
        if (rp.getT1().equals(term1) && rp.isCommutative() && !results.contains(rp)) {
          results.add(rp);
        }
      }
    }

    return results;
  }

  public List<Content> findAllContentsWithTerm(String term) {
    List<Content> results = new ArrayList<Content>();

    List<Content> fromTerm = relationPatternsByT1.get(term);
    if (fromTerm != null) {
      results.addAll(fromTerm);
    }

    List<Content> toTerm = relationPatternsByT2.get(term);
    if (toTerm != null) {
      for (Content rp : toTerm) {
        if (!results.contains(rp)) {
          results.add(rp);
        }
      }
    }

    return results;
  }

  public Content findContent(String term1, String term2, PatternFamily family) {
    String key = term1 + "|" + term2;
    Content rp = relationPatternsByKey.get(key);

    if (rp != null) {
      if (family == null || (rp.getFamilyId() != null && rp.getFamilyId().equals(family.getId()))) {
        return rp;
      }
    }

    if (relationPatternsByKey.containsKey(term2 + "|" + term1)) {
      rp = relationPatternsByKey.get(term2 + "|" + term1);
      if (rp != null && rp.isCommutative()) {
        if (family == null
            || (rp.getFamilyId() != null && rp.getFamilyId().equals(family.getId()))) {
          return rp;
        }
      }
    }

    return null;
  }

  public Content findContent(String term1, String term2) {
    return findContent(term1, term2, null);
  }

  public Content getFamilyForRelation(String term1, String term2) {
    String key = term1 + "|" + term2;
    Content rp = relationPatternsByKey.get(key);
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
    if (equivalentSequences == null || equivalentSequences.isEmpty()) {
      return;
    }

    Set<String> allTokensInSequences = getAllTokens(equivalentSequences);

    optionalFinder.clear();
    optionalFinder.analyze(equivalentSequences);
    Set<String> optionals = optionalFinder.get();
    learnedOptionalTokens.addAll(optionals);

    Set<String> nonOptionalTokens = new HashSet<String>(allTokensInSequences);
    nonOptionalTokens.removeAll(optionals);

    if (nonOptionalTokens.isEmpty()) {
      System.out.println("   All tokens are optional, skipping analysis.");
      return;
    }

    ContentFinder rawContentFinder = new ContentFinder(optionalFinder, gramAnalyzer);
    Set<String> contentTokens = rawContentFinder.identifyContent(equivalentSequences, null);

    if (contentTokens.size() < 2) {
      contentTokens = rawContentFinder.checkMovement(equivalentSequences, null);
    }
    if (contentTokens.size() < 2) {
      contentTokens = rawContentFinder.parify(equivalentSequences);
    }

    Set<String> protectedTokens = new HashSet<String>();
    for (String token : nonOptionalTokens) {
      if (!contentTokens.contains(token)) {
        protectedTokens.add(token);
      }
    }

    Map<String, Set<Integer>> tokenPositions = new HashMap<String, Set<Integer>>();
    for (List<String> sequence : equivalentSequences) {
      for (int i = 0; i < sequence.size(); i++) {
        String token = sequence.get(i);
        if (!tokenPositions.containsKey(token)) {
          tokenPositions.put(token, new HashSet<Integer>());
        }
        tokenPositions.get(token).add(i);
      }
    }

    Set<String> learnedStructuralTokens = getLearnedStructuralTokens();

    Set<String> gramContentTokens = gramAnalyzer.analyze(equivalentSequences, nonOptionalTokens);
    if (!gramContentTokens.isEmpty()) {
      System.out.println("   [System: Gram analysis detected moving token pairs]");
      if (!gramContentTokens.isEmpty()) {
        contentTokens = gramContentTokens;
      }
    }

    boolean needsContextBuild = false;
    if (!equivalentSequences.isEmpty() && !equivalentSequences.get(0).isEmpty()) {
      String firstToken = equivalentSequences.get(0).get(0);
      if (!firstToken.startsWith("PF")) {
        Symbol s = symbolManager.getSymbols().get(firstToken);
        needsContextBuild = (s == null || (s.leftContext.isEmpty() && s.rightContext.isEmpty()));
      }
    }

    if (needsContextBuild) {
      for (List<String> seq : equivalentSequences) {
        symbolManager.buildContext(seq);
      }
    }

    reevaluateEquivalents();

    if (contentTokens.size() < 2) {
      System.out.println("   Analysis failed to find content. Trying collapse...");
      attemptCollapseAndAnalysis(equivalentSequences);
      return;
    }

    Set<String> preliminaryStructuralTokens =
        termSelector.identifyStructuralTokens(equivalentSequences, contentTokens);
    
    // UPDATED: Use the new selectTermsByClosestCompanion method with self-relation support
    String[] preliminaryTerms = rawContentFinder.selectTermsByClosestCompanion(
        contentTokens, equivalentSequences, tokenPositions, preliminaryStructuralTokens);

    if (preliminaryTerms == null) {
      System.out.println(
          "   Could not select preliminary terms from content: "
              + contentTokens
              + ". Trying collapse...");
      attemptCollapseAndAnalysis(equivalentSequences);
      return;
    }

    String[] positionalTerms =
        termSelector.determinePositionalTerms(
            preliminaryTerms[0], preliminaryTerms[1], equivalentSequences);

    List<Structure> tempPatterns =
        patternExtractor.extractStructures(
            equivalentSequences, positionalTerms[0], positionalTerms[1], termSelector);

    Set<StructuralEquivalenceDetector.EquivalencePair> newPairs =
        equivalenceDetector.detectStructuralEquivalents(
            equivalentSequences,
            preliminaryStructuralTokens,
            positionalTerms[0],
            positionalTerms[1],
            scan());
    addDiscoveredEquivalents(newPairs);

    Set<String> finalContentTokens = contentTokens;

    if (!learnedStructuralTokens.containsAll(preliminaryStructuralTokens)) {
      ContentFinder contentFinder = new ContentFinder(optionalFinder, gramAnalyzer);
      finalContentTokens =
          contentFinder.identifyContent(equivalentSequences, learnedStructuralTokens);
      if (finalContentTokens.size() < 2) {
        finalContentTokens =
            contentFinder.checkMovement(equivalentSequences, learnedStructuralTokens);
      }
      if (finalContentTokens.size() < 2) {
        finalContentTokens = contentFinder.parify(equivalentSequences);
      }
    }

    if (finalContentTokens.size() < 2) {
      System.out.println("   Analysis failed after refinement. Trying collapse...");
      attemptCollapseAndAnalysis(equivalentSequences);
      return;
    }

    Set<String> finalStructuralTokens =
        termSelector.identifyStructuralTokens(equivalentSequences, finalContentTokens);
    
    // UPDATED: Use the new selectTermsByClosestCompanion method with self-relation support
    String[] finalTerms = rawContentFinder.selectTermsByClosestCompanion(
        finalContentTokens, equivalentSequences, tokenPositions, finalStructuralTokens);

    if (finalTerms == null) {
      System.out.println("   Could not select final terms from content. Trying collapse...");
      attemptCollapseAndAnalysis(equivalentSequences);
      return;
    }

    String[] finalPositionalTerms =
        termSelector.determinePositionalTerms(finalTerms[0], finalTerms[1], equivalentSequences);

    tempPatterns =
        patternExtractor.extractStructures(
            equivalentSequences, finalPositionalTerms[0], finalPositionalTerms[1], termSelector);

    updateFamiliesWithPatterns(tempPatterns, finalPositionalTerms[0], finalPositionalTerms[1]);

    detectCrossDomainEquivalents(
        equivalentSequences, finalPositionalTerms[0], finalPositionalTerms[1]);

    boolean isCommutative =
        patternExtractor.isCommutative(equivalentSequences, finalTerms[0], finalTerms[1]);

    System.out.println(
        "   Core relation: "
            + finalPositionalTerms[0]
            + (isCommutative ? " <-> " : " -> ")
            + finalPositionalTerms[1]);

    if (finalContentTokens.size() > 2) {
      Set<String> otherContent = new LinkedHashSet<String>(finalContentTokens);
      otherContent.remove(finalTerms[0]);
      otherContent.remove(finalTerms[1]);
      if (!otherContent.isEmpty()) {
        List<String> sortedOther = new ArrayList<String>(otherContent);
        Collections.sort(sortedOther);
        System.out.println("   Duals: " + sortedOther);
      }
    }

    Set<String> dualTokensInThisSet = new HashSet<String>();
    Set<String> pureStructuralTokens = new HashSet<String>();
    for (String token : finalStructuralTokens) {
      Symbol symbol = symbolManager.getSymbols().get(token);
      if (symbol != null && symbol.relations.contains("D")) {
        dualTokensInThisSet.add(token);
      } else {
        pureStructuralTokens.add(token);
      }
    }

    if (!dualTokensInThisSet.isEmpty()) {
      List<String> sortedDual = new ArrayList<String>(dualTokensInThisSet);
      Collections.sort(sortedDual);
      System.out.println("   Duals: " + sortedDual);
    }

    if (!pureStructuralTokens.isEmpty()) {
      List<String> sortedStruct = new ArrayList<String>(pureStructuralTokens);
      Collections.sort(sortedStruct);
      System.out.println("   Structurals: " + sortedStruct);
    }

    updateAllFamiliesWithNewEquivalents();

    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);

    PatternFamily patternFamily = null;

    for (PatternFamily family : families) {
      for (Structure sp : family.getMemberPatterns()) {
        if (!equivalentSequences.isEmpty()) {
          List<String> exampleSeq = equivalentSequences.get(0);
          Data abstractPattern =
              SequenceTransformer.abstractSequencePF(
                  exampleSeq, finalPositionalTerms[0], finalPositionalTerms[1]);

          boolean matches = false;

          if (sp.getData().equals(abstractPattern)) {
            matches = true;
          }

          if (!matches && isCommutative) {
            Data flippedPattern =
                SequenceTransformer.flipTermPatternWithTerms(
                    abstractPattern, finalPositionalTerms[0], finalPositionalTerms[1]);
            if (sp.getData().equals(flippedPattern)) {
              matches = true;
            }
          }

          if (!matches && isCommutative) {
            Data patternWithC = new Data();
            for (int i = 0; i < abstractPattern.size(); i++) {
              if (abstractPattern.isPlaceholder(i)) {
                SlotFlag flag = abstractPattern.getPlaceholderAt(i);
                if (flag == SlotFlag._1 || flag == SlotFlag._2) {
                  patternWithC.addPlaceholder(SlotFlag._C);
                } else {
                  patternWithC.addPlaceholder(flag);
                }
              } else if (abstractPattern.isToken(i)) {
                patternWithC.addToken(abstractPattern.getTokenAt(i));
              } else if (abstractPattern.isPFToken(i)) {
                patternWithC.addPFToken(abstractPattern.getPFTokenAt(i));
              }
            }
            if (sp.getData().equals(patternWithC)) {
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

    Content pattern =
        findOrCreatePatternForFamily(
            finalPositionalTerms[0], finalPositionalTerms[1], patternFamily, isCommutative);

    pattern.setFrequency(pattern.getFrequency() + equivalentSequences.size());
    for (List<String> seq : equivalentSequences) {
      pattern.addConcreteExample(new ArrayList<String>(seq));
    }
    pattern.setBaseTermsT1(getBaseTermsForSyntheticToken(finalPositionalTerms[0]));
    pattern.setBaseTermsT2(getBaseTermsForSyntheticToken(finalPositionalTerms[1]));

    addToRelationMaps(pattern);

    if (isCommutative) {
      Content reversePattern =
          findOrCreatePatternForFamily(
              finalPositionalTerms[1], finalPositionalTerms[0], patternFamily, isCommutative);
      reversePattern.setFrequency(reversePattern.getFrequency() + equivalentSequences.size());
      for (List<String> seq : equivalentSequences) {
        reversePattern.addConcreteExample(new ArrayList<String>(seq));
      }
      reversePattern.setBaseTermsT1(getBaseTermsForSyntheticToken(finalPositionalTerms[1]));
      reversePattern.setBaseTermsT2(getBaseTermsForSyntheticToken(finalPositionalTerms[0]));

      addToRelationMaps(reversePattern);
    }

    for (String token : finalContentTokens) addSymbolRole(token, "C");
    addSymbolRole(finalPositionalTerms[0], "1");
    addSymbolRole(finalPositionalTerms[1], "2");
    for (String token : finalStructuralTokens) addSymbolRole(token, "S");

    familiesDirty = true;
    families = patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);
    
    for (Pattern p : allPatterns) {
      if (!(p instanceof Content)) continue;
      Content rp = (Content) p;
      
      if (rp.getFamilyId() != null) {
        continue;
      }
      if (rp.getConcreteExamples().isEmpty()) {
        continue;
      }
      
      boolean familyAssigned = false;
      for (PatternFamily family : families) {
        if (familyAssigned) break;
        
        for (List<String> seq : rp.getConcreteExamples()) {
          if (familyAssigned) break;
          
          Data abstractPattern = SequenceTransformer.abstractSequencePF(seq, rp.getT1(), rp.getT2());
          Data flippedPattern = SequenceTransformer.flipTermPatternWithTerms(abstractPattern, rp.getT1(), rp.getT2());
          
          for (Structure sp : family.getMemberPatterns()) {
            Data familyPattern = sp.getData();
            boolean matchesNormal = familyPattern.equals(abstractPattern);
            boolean matchesFlipped = rp.isCommutative() && familyPattern.equals(flippedPattern);
            
            if (!matchesNormal && !matchesFlipped && rp.isCommutative()) {
              Data patternWithC = new Data();
              for (int i = 0; i < abstractPattern.size(); i++) {
                if (abstractPattern.isPlaceholder(i)) {
                  SlotFlag flag = abstractPattern.getPlaceholderAt(i);
                  if (flag == SlotFlag._1 || flag == SlotFlag._2) {
                    patternWithC.addPlaceholder(SlotFlag._C);
                  } else {
                    patternWithC.addPlaceholder(flag);
                  }
                } else if (abstractPattern.isToken(i)) {
                  patternWithC.addToken(abstractPattern.getTokenAt(i));
                } else if (abstractPattern.isPFToken(i)) {
                  patternWithC.addPFToken(abstractPattern.getPFTokenAt(i));
                }
              }
              matchesNormal = familyPattern.equals(patternWithC);
              
              Data flippedWithC = new Data();
              for (int i = 0; i < flippedPattern.size(); i++) {
                if (flippedPattern.isPlaceholder(i)) {
                  SlotFlag flag = flippedPattern.getPlaceholderAt(i);
                  if (flag == SlotFlag._1 || flag == SlotFlag._2) {
                    flippedWithC.addPlaceholder(SlotFlag._C);
                  } else {
                    flippedWithC.addPlaceholder(flag);
                  }
                } else if (flippedPattern.isToken(i)) {
                  flippedWithC.addToken(flippedPattern.getTokenAt(i));
                } else if (flippedPattern.isPFToken(i)) {
                  flippedWithC.addPFToken(flippedPattern.getPFTokenAt(i));
                }
              }
              matchesFlipped = familyPattern.equals(flippedWithC);
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

  private List<Pattern> scan() {
    List<Pattern> patternsForDetection = new ArrayList<Pattern>();
    patternsForDetection.addAll(allPatterns);

    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);

    for (PatternFamily family : families) {
      for (Structure sp : family.getMemberPatterns()) {
        patternsForDetection.add(sp);
      }
    }

    return patternsForDetection;
  }

  private void updateFamiliesWithPatterns(
      List<Structure> tempPatterns, String term1, String term2) {
    if (tempPatterns.isEmpty()) return;

    familiesDirty = true;
    patternFamilyManager.update(tempPatterns, structuralEquivalents);
  }

  private void updateAllFamiliesWithNewEquivalents() {
    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);
    for (PatternFamily family : families) {
      family.updateAliases(structuralEquivalents);
    }
    familiesDirty = true;
  }

  private Content findOrCreatePatternForFamily(
      String term1, String term2, PatternFamily family, boolean isCommutative) {
    if (family == null) {
      return findOrCreatePattern(term1, term2);
    }

    String key = term1 + "|" + term2;
    Content existing = relationPatternsByKey.get(key);

    if (existing != null) {
      if (family == null
          || (existing.getFamilyId() != null && existing.getFamilyId().equals(family.getId()))) {
        existing.updateTimestamp();
        return existing;
      }
    }

    if (isCommutative) {
      String reverseKey = term2 + "|" + term1;
      existing = relationPatternsByKey.get(reverseKey);
      if (existing != null && existing.isCommutative()) {
        if (family == null
            || (existing.getFamilyId() != null && existing.getFamilyId().equals(family.getId()))) {
          existing.updateTimestamp();
          return existing;
        }
      }
    }

    Content newPattern = new Content("RP" + allPatterns.size(), term1, term2);
    newPattern.setFamily(family);
    newPattern.setCommutative(isCommutative);
    newPattern.setFrequency(0);
    allPatterns.add(newPattern);
    addToRelationMaps(newPattern);
    familiesDirty = true;

    return newPattern;
  }

  private Content findOrCreatePattern(String term1, String term2) {
    String key = term1 + "|" + term2;
    Content existing = relationPatternsByKey.get(key);

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

    Content newPattern = new Content("RP" + allPatterns.size(), term1, term2);
    newPattern.setFrequency(0);
    allPatterns.add(newPattern);
    addToRelationMaps(newPattern);
    familiesDirty = true;
    return newPattern;
  }

  private void attemptCollapseAndAnalysis(List<List<String>> originalSequences) {
    Set<String> protectedTokens = new HashSet<String>();
    Set<String> allBatchTokens = getAllTokens(originalSequences);
    allBatchTokens.removeAll(optionalFinder.get());

    ContentFinder rawContentFinder = new ContentFinder(optionalFinder, gramAnalyzer);
    Set<String> batchContentTokens = rawContentFinder.identifyContent(originalSequences, null);

    if (batchContentTokens.size() < 2) {
      batchContentTokens = rawContentFinder.checkMovement(originalSequences, null);
    }

    for (String token : allBatchTokens) {
      if (!batchContentTokens.contains(token)) {
        protectedTokens.add(token);
      }
    }

    List<List<String>> collapsedSequences =
        sequenceCollapser.collapseSequences(originalSequences, protectedTokens);

    if (collapsedSequences != null && !collapsedSequences.isEmpty()) {
      if (collapsedSequences.get(0).size() <= 1) {
        System.out.println(
            "   [System: Sequence recognized as existing family instance: "
                + collapsedSequences.get(0)
                + "]");
        assignRolesFromFamilyMatch(originalSequences);
        return;
      }

      System.out.println(
          "   [System: Sequence collapsed for higher-level analysis: "
              + collapsedSequences.get(0)
              + "]");
      System.out.println(
          "   [System: Composite patterns are no longer supported. Treating as regular pattern.]");
      processEquivalenceSet(originalSequences);
      return;
    }

    System.out.println("   Set is ambiguous and cannot be collapsed.");
  }

  private void assignRolesFromFamilyMatch(List<List<String>> sequences) {
    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);
    for (List<String> sequence : sequences) {
      boolean matchedSequence = false;
      for (PatternFamily family : families) {
        if (matchedSequence) break;
        for (Structure sp : family.getMemberPatterns()) {
          Data data = sp.getData();
          if (sequence.size() != data.size()) continue;
          if (PatternMatcher.isSubSequenceMatch(
              sequence, 0, data, family, new HashSet<String>())) {
            for (int i = 0; i < data.size(); i++) {
              if (data.isPlaceholder(i)) {
                SlotFlag flag = data.getPlaceholderAt(i);
                String token = sequence.get(i);
                if (flag == SlotFlag._1) {
                  addSymbolRole(token, "1");
                  addSymbolRole(token, "C");
                } else if (flag == SlotFlag._2) {
                  addSymbolRole(token, "2");
                  addSymbolRole(token, "C");
                } else if (flag == SlotFlag._C || flag == SlotFlag._X) {
                  addSymbolRole(token, "C");
                }
              }
            }
            matchedSequence = true;
            break;
          }
        }
      }
    }
  }

  private void addSymbolRole(String token, String role) {
    if (!token.startsWith("PF") && symbolManager.getSymbols().containsKey(token)) {
      symbolManager.getSymbols().get(token).relations.add(role);
    }
  }

  private Set<String> getBaseTermsForSyntheticToken(String token) {
    if (!token.startsWith("PF")) return Collections.singleton(token);
    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);
    PatternFamily targetFamily = null;
    for (PatternFamily f : families) {
      if (f.getId().equals(token)) {
        targetFamily = f;
        break;
      }
    }
    Set<String> baseTerms = new HashSet<String>();
    if (targetFamily != null) {
      for (Pattern p : allPatterns) {
        if (p instanceof Content) {
          Content rp = (Content) p;
          if (rp.getFamilyId() != null && rp.getFamilyId().equals(token)) {
            baseTerms.add(rp.getT1());
            baseTerms.add(rp.getT2());
          }
        }
      }
    } else {
      baseTerms.add(token);
    }
    Set<String> finalBaseTerms = new HashSet<String>();
    for (String term : baseTerms) {
      if (term.startsWith("PF")) finalBaseTerms.addAll(getBaseTermsForSyntheticToken(term));
      else finalBaseTerms.add(term);
    }
    return finalBaseTerms;
  }

  private Set<String> getAllTokens(List<List<String>> sequences) {
    Set<String> allTokens = new HashSet<String>();
    for (List<String> sequence : sequences) allTokens.addAll(sequence);
    return allTokens;
  }


  private void addDiscoveredEquivalents(
      Set<StructuralEquivalenceDetector.EquivalencePair> newPairs) {
    if (!newPairs.isEmpty()) {
      familiesDirty = true;
      for (StructuralEquivalenceDetector.EquivalencePair pair : newPairs)
        addStructuralEquivalence(pair.w1, pair.w2);
    }
  }


  private void addStructuralEquivalence(String w1, String w2) {
    if (!structuralEquivalents.containsKey(w1))
      structuralEquivalents.put(w1, new HashSet<String>());
    if (!structuralEquivalents.containsKey(w2))
      structuralEquivalents.put(w2, new HashSet<String>());

    boolean changed1 = structuralEquivalents.get(w1).add(w2);
    boolean changed2 = structuralEquivalents.get(w2).add(w1);

    if (changed1 || changed2) {
      familiesDirty = true;
    }

    addSymbolRole(w1, "S");
    addSymbolRole(w2, "S");
  }

  public Set<String> getLearnedStructuralTokens() {
    Set<String> filtered = new HashSet<String>();
    List<PatternFamily> families =
        patternFamilyManager.get(structuralEquivalents, patternFamilyBuilder);

    for (PatternFamily family : families) {
      for (Structure sp : family.getMemberPatterns()) {
        Data data = sp.getData();
        for (int i = 0; i < data.size(); i++) {
          if (data.isToken(i)) {
            String token = data.getTokenAt(i);
            if (!token.equals("[1]") && !token.equals("[2]") && !token.equals("[C]") && !token.equals("[X]") && !token.startsWith("PF")) {
              filtered.add(token);
            }
          } else if (data.isAlias(i)) {
            String alias = data.getAliasAt(i);
            if (!alias.equals("[1]") && !alias.equals("[2]") && !alias.equals("[C]") && !alias.equals("[X]") && !alias.startsWith("PF")) {
              filtered.add(alias);
            }
          }
        }
      }
    }
    return filtered;
  }

  private void detectCrossDomainEquivalents(
      List<List<String>> equivalentSequences, String t1, String t2) {
    Set<StructuralEquivalenceDetector.EquivalencePair> crossDomainPairs =
        equivalenceDetector.detectStructuralEquivalents(
            equivalentSequences, getLearnedStructuralTokens(), t1, t2, scan());
    if (!crossDomainPairs.isEmpty()) {
      System.out.println(
          "   [System: Found " + crossDomainPairs.size() + " cross-domain equivalents]");
      addDiscoveredEquivalents(crossDomainPairs);
    }
  }

  public void reevaluateEquivalents() {
    boolean changed = equivalenceDetector.reevaluateEquivalents(structuralEquivalents);
    if (changed) {
      System.out.println("   [System: Reevaluated structural equivalents based on context.]");
      familiesDirty = true;
    }
  }

  public boolean hasCommutativeCollapse(List<String> sequence) {
    return sequenceCollapser.hasCommutativeCollapse(sequence);
  }
}
