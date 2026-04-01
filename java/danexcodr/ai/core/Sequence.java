package danexcodr.ai.core;

import danexcodr.ai.pattern.*;
import danexcodr.ai.util.Combinatorics;
import java.util.*;

public class Sequence {

  private List<Pattern> allPatterns;
  private List<PatternFamily> currentFamilies;
  private Map<String, Set<String>> currentStructuralEquivalents;
  private PatternProcessor patternProcessor;
  private PatternFamilyManager familyManager;
  private SymbolManager symbolManager;

  public Sequence(List<Pattern> allPatterns, PatternProcessor patternProcessor) {
    this.allPatterns = allPatterns;
    this.patternProcessor = patternProcessor;
    this.currentFamilies = null;
    this.currentStructuralEquivalents = null;
    this.familyManager = null;
  }

  public void setCurrentFamilies(List<PatternFamily> families) {
    this.currentFamilies = families;
  }

  public void setCurrentStructuralEquivalents(Map<String, Set<String>> structuralEquivalents) {
    this.currentStructuralEquivalents = structuralEquivalents;
  }

  public void setFamilyManager(PatternFamilyManager familyManager) {
    this.familyManager = familyManager;
  }

  public void setSymbolManager(SymbolManager symbolManager) {
    this.symbolManager = symbolManager;
  }

  private PatternFamily getFreshFamily(PatternFamily family) {
    if (family == null || currentStructuralEquivalents == null) {
      return family;
    }

    family.updateAliases(currentStructuralEquivalents);
    return family;
  }

  private PatternFamily getFamilyForRelation(Content relation) {
    if (relation.getFamilyId() != null && familyManager != null) {
      PatternFamily family = familyManager.getById(relation.getFamilyId());
      return getFreshFamily(family);
    }
    return null;
  }

  public List<List<String>> infer(String T1, String T2) {
    return infer(T1, T2, true);
  }

  public List<List<String>> infer(String T1, String T2, boolean normalizeForAnswer) {
    Set<List<String>> shallowResults = new LinkedHashSet<List<String>>();
    boolean foundNonLogicalDirectRelation = false;

    // 1. Check for direct relations
    List<Content> directRelations = patternProcessor.findAllContents(T1, T2);
    for (Content directRelation : directRelations) {
        PatternFamily family = getFamilyForRelation(directRelation);

        if (family != null) {
            shallowResults.addAll(
                inferFromFamily(
                    family, T1, T2, directRelation.isCommutative(), null, normalizeForAnswer));
            foundNonLogicalDirectRelation = true;
        }
    }

    // 2. Check for reverse relations
    List<Content> reverseRelations = patternProcessor.findAllContents(T2, T1);
    for (Content reverseRelation : reverseRelations) {
        PatternFamily family = getFamilyForRelation(reverseRelation);

        if (family != null) {
            shallowResults.addAll(
                inferFromFamily(
                    family, T2, T1, reverseRelation.isCommutative(), null, normalizeForAnswer));
            foundNonLogicalDirectRelation = true;
        }
    }

    if (foundNonLogicalDirectRelation) {
        List<List<String>> results = new ArrayList<List<String>>(shallowResults);
        RelationFinder relationFinder =
            new RelationFinder(
                allPatterns, currentFamilies, currentStructuralEquivalents, patternProcessor);
        relationFinder.validateInferences(T1, T2, results);
        results = rankByStructuralContext(results);
        return results;
    }

    // 3. Find common connectors with proper transitive inference
    RelationFinder relationFinder =
        new RelationFinder(
            allPatterns, currentFamilies, currentStructuralEquivalents, patternProcessor);

    Set<String> T1Connectors = relationFinder.findConnectors(T1);
    Set<String> T2Connectors = relationFinder.findConnectors(T2);

    Set<String> commonConnectors = new HashSet<String>(T1Connectors);
    commonConnectors.retainAll(T2Connectors);

    for (String commonConnector : commonConnectors) {
        List<Content> T1toCommon = patternProcessor.findAllContents(T1, commonConnector);
        List<Content> commonToT1 = patternProcessor.findAllContents(commonConnector, T1);
        List<Content> firstLegs = new ArrayList<Content>();
        firstLegs.addAll(T1toCommon);
        firstLegs.addAll(commonToT1);

        List<Content> commonToT2 = patternProcessor.findAllContents(commonConnector, T2);
        List<Content> T2toCommon = patternProcessor.findAllContents(T2, commonConnector);
        List<Content> secondLegs = new ArrayList<Content>();
        secondLegs.addAll(commonToT2);
        secondLegs.addAll(T2toCommon);

        if (!firstLegs.isEmpty() && !secondLegs.isEmpty()) {
            for (Content firstLeg : firstLegs) {
                for (Content secondLeg : secondLegs) {
                    PatternFamily family1 = getFamilyForRelation(firstLeg);
                    PatternFamily family2 = getFamilyForRelation(secondLeg);

                    boolean canUseForTransitive = false;
                    PatternFamily familyToUse = null;
                    boolean commutativity = false;

                    if (!firstLeg.isCommutative() && !secondLeg.isCommutative()) {
                        canUseForTransitive = true;
                        familyToUse = family1;
                        commutativity = false;
                    }
                    else if (firstLeg.isCommutative() && !secondLeg.isCommutative()) {
                        canUseForTransitive = true;
                        familyToUse = family1;
                        commutativity = firstLeg.isCommutative();
                    }
                    else if (!firstLeg.isCommutative() && secondLeg.isCommutative()) {
                        canUseForTransitive = true;
                        familyToUse = family2;
                        commutativity = secondLeg.isCommutative();
                    }
                    else if (firstLeg.isCommutative()
                        && secondLeg.isCommutative()
                        && family1 != null
                        && family2 != null
                        && family1.getId().equals(family2.getId())) {
                        canUseForTransitive = true;
                        familyToUse = family1;
                        commutativity = true;
                    }
                    else if (!firstLeg.isCommutative()
                        && !secondLeg.isCommutative()
                        && family1 != null
                        && family2 != null
                        && family1.getId().equals(family2.getId())) {
                        canUseForTransitive = true;
                        familyToUse = family1;
                        commutativity = false;
                    }

                    if (canUseForTransitive && familyToUse != null) {
                        shallowResults.addAll(
                            inferFromFamily(
                                familyToUse, T1, T2, commutativity, null, normalizeForAnswer));
                    }
                }
            }
        }
    }

    // 4. Commutative Bridges
    Set<String> parents1 = new HashSet<String>();
    for (String conn : relationFinder.findConnectors(T1)) {
        List<Content> rps = patternProcessor.findAllContents(T1, conn);
        for (Content rp : rps) {
            if (rp.getT2().equals(conn) && !rp.isCommutative()) {
                parents1.add(conn);
            }
        }
    }

    Set<String> parents2 = new HashSet<String>();
    for (String conn : relationFinder.findConnectors(T2)) {
        List<Content> rps = patternProcessor.findAllContents(T2, conn);
        for (Content rp : rps) {
            if (rp.getT2().equals(conn) && !rp.isCommutative()) {
                parents2.add(conn);
            }
        }
    }

    for (String p1 : parents1) {
        for (String p2 : parents2) {
            List<Content> bridges = patternProcessor.findAllContents(p1, p2);
            if (bridges.isEmpty()) bridges = patternProcessor.findAllContents(p2, p1);

            for (Content bridge : bridges) {
                PatternFamily family = getFamilyForRelation(bridge);

                if (family != null && bridge.isCommutative()) {
                    shallowResults.addAll(
                        inferFromFamily(
                            family, T1, T2, bridge.isCommutative(), null, normalizeForAnswer));
                }
            }
        }
    }

    // 5. Deep Fallback with proper path utilization
    if (shallowResults.isEmpty()) {
        List<String> path = relationFinder.findDeepPath(T1, T2);

        if (path != null && path.size() > 2) {
            for (int i = 0; i < path.size() - 1; i++) {
                for (int j = i + 1; j < path.size(); j++) {
                    String node1 = path.get(i);
                    String node2 = path.get(j);

                    if ((node1.equals(T1) && node2.equals(T2)) || (node1.equals(T2) && node2.equals(T1))) {
                        continue;
                    }

                    List<Content> relations = patternProcessor.findAllContents(node1, node2);
                    if (relations.isEmpty()) {
                        relations = patternProcessor.findAllContents(node2, node1);
                    }

                    for (Content relation : relations) {
                        PatternFamily family = getFamilyForRelation(relation);

                        if (family != null) {
                            shallowResults.addAll(
                                inferFromFamily(
                                    family,
                                    T1,
                                    T2,
                                    relation.isCommutative(),
                                    null,
                                    normalizeForAnswer));
                        }
                    }
                }
            }

            String firstHopNode = path.get(1);
            List<Content> firstLegs = patternProcessor.findAllContents(T1, firstHopNode);
            if (firstLegs.isEmpty()) firstLegs = patternProcessor.findAllContents(firstHopNode, T1);

            String lastHopNode = path.get(path.size() - 2);
            List<Content> lastLegs = patternProcessor.findAllContents(lastHopNode, T2);
            if (lastLegs.isEmpty()) lastLegs = patternProcessor.findAllContents(T2, lastHopNode);

            if (!firstLegs.isEmpty() && !lastLegs.isEmpty()) {
                for (Content firstLeg : firstLegs) {
                    for (Content lastLeg : lastLegs) {
                        PatternFamily family1 = getFamilyForRelation(firstLeg);
                        PatternFamily family2 = getFamilyForRelation(lastLeg);

                        if (family1 != null && family2 != null) {

                            PatternFamily familyToUse = null;
                            boolean commutativity = false;

                            if (firstLeg.isCommutative()) {
                                familyToUse = family1;
                                commutativity = firstLeg.isCommutative();
                            } else if (lastLeg.isCommutative()) {
                                familyToUse = family2;
                                commutativity = lastLeg.isCommutative();
                            } else {
                                familyToUse = family1;
                                commutativity = false;
                            }

                            if (familyToUse != null) {
                                shallowResults.addAll(
                                    inferFromFamily(
                                        familyToUse,
                                        T1,
                                        T2,
                                        commutativity,
                                        null,
                                        normalizeForAnswer));
                            }
                        }
                    }
                }
            }
        }
    }

    List<List<String>> finalResults = new ArrayList<List<String>>(shallowResults);
    relationFinder.validateInferences(T1, T2, finalResults);
    finalResults = rankByStructuralContext(finalResults);
    return finalResults;
}

  public List<List<String>> inferFromFamily(
      PatternFamily family, String T1, String T2, boolean useCommutative) {
    return inferFromFamily(family, T1, T2, useCommutative, null, true);
  }

  public List<List<String>> inferFromFamily(
    PatternFamily family,
    String T1,
    String T2,
    boolean useCommutative,
    Map<String, Set<String>> currentStructuralEquivalents) {
    return inferFromFamily(
        family, T1, T2, useCommutative, currentStructuralEquivalents, true);
  }

  public List<List<String>> inferFromFamily(
    PatternFamily family,
    String T1,
    String T2,
    boolean useCommutative,
    Map<String, Set<String>> currentStructuralEquivalents,
    boolean normalizeForAnswer) {

  Set<List<String>> inferredSequences = new LinkedHashSet<List<String>>();

  if (family == null) {
    return new ArrayList<List<String>>(inferredSequences);
  }

  if (currentStructuralEquivalents != null) {
    family.updateAliases(currentStructuralEquivalents);
  }

  Map<String, Set<String>> aliases = family.getAliases();
  Map<String, String> tokenToAlias = family.getTokenToAlias();
  
  for (Structure sp : family.getMemberPatterns()) {
    if (sp.isCommutative() != useCommutative) {
      continue;
    }

    Data aliasedSlots = family.getAliasedSlots(sp);
    
    // Print aliased slots for debugging
    StringBuilder slotStr = new StringBuilder();
    for (int i = 0; i < aliasedSlots.size(); i++) {
      if (aliasedSlots.isPlaceholder(i)) {
        slotStr.append(aliasedSlots.getPlaceholderAt(i).toToken()).append(" ");
      } else if (aliasedSlots.isToken(i)) {
        slotStr.append(aliasedSlots.getTokenAt(i)).append(" ");
      } else if (aliasedSlots.isPFToken(i)) {
        slotStr.append(aliasedSlots.getPFTokenAt(i)).append(" ");
      } else if (aliasedSlots.isAlias(i)) {
        slotStr.append(aliasedSlots.getAliasAt(i)).append(" ");
      }
    }

    List<Set<Object>> slotsPass1 =
        PatternSlotFiller.fill(
            aliasedSlots, aliases, tokenToAlias, T1, T2, useCommutative, false);

    List<List<Object>> combos1 = Combinatorics.generate(slotsPass1);
    
    for (List<Object> combo : combos1) {
      List<String> sequence = new ArrayList<String>();
      for (Object obj : combo) {
        if (obj instanceof String) {
          sequence.add((String) obj);
        } else if (obj instanceof SlotFlag) {
          SlotFlag flag = (SlotFlag) obj;
          if (flag == SlotFlag._1) {
            sequence.add(T1);
          } else if (flag == SlotFlag._2) {
            sequence.add(T2);
          } else if (flag == SlotFlag._C) {
            sequence.add(T1);
          } else if (flag == SlotFlag._X) {
            sequence.add(T1);
          }
        }
      }
      if (!sequence.isEmpty()) {
        inferredSequences.add(normalizeForAnswer ? normalizeStructuralEquivalentChoice(sequence) : sequence);
      }
    }

    if (sp.isCommutative()) {
      List<Set<Object>> slotsFlipped =
          PatternSlotFiller.fill(
              aliasedSlots, aliases, tokenToAlias, T1, T2, useCommutative, true);

      List<List<Object>> combos2 = Combinatorics.generate(slotsFlipped);
      
      for (List<Object> combo : combos2) {
        List<String> sequence = new ArrayList<String>();
        for (Object obj : combo) {
          if (obj instanceof String) {
            sequence.add((String) obj);
          } else if (obj instanceof SlotFlag) {
            SlotFlag flag = (SlotFlag) obj;
            if (flag == SlotFlag._1) {
              sequence.add(T1);
            } else if (flag == SlotFlag._2) {
              sequence.add(T2);
            } else if (flag == SlotFlag._C) {
              sequence.add(T1);
            } else if (flag == SlotFlag._X) {
              sequence.add(T1);
            }
          }
        }
        if (!sequence.isEmpty()) {
          inferredSequences.add(normalizeForAnswer ? normalizeStructuralEquivalentChoice(sequence) : sequence);
        }
      }
    }
  }
  return new ArrayList<List<String>>(inferredSequences);
}

  private List<List<String>> rankByStructuralContext(List<List<String>> candidates) {
    if (symbolManager == null || candidates == null || candidates.size() <= 1) {
      return candidates;
    }

    final Map<List<String>, Integer> scores = new HashMap<List<String>, Integer>();
    final Map<List<String>, String> joined = new HashMap<List<String>, String>();
    for (List<String> candidate : candidates) {
      scores.put(candidate, Integer.valueOf(scoreByContext(candidate)));
      joined.put(candidate, toJoined(candidate));
    }

    Collections.sort(
        candidates,
        new Comparator<List<String>>() {
          @Override
          public int compare(List<String> a, List<String> b) {
            Integer sb = scores.get(b);
            Integer sa = scores.get(a);
            int byScore = sb.compareTo(sa);
            if (byScore != 0) {
              return byScore;
            }
            return joined.get(b).compareTo(joined.get(a));
          }
        });

    return candidates;
  }

  private int scoreByContext(List<String> sequence) {
    if (symbolManager == null || sequence == null || sequence.size() < 2) {
      return 0;
    }

    int score = 0;
    Map<String, Symbol> symbols = symbolManager.getSymbols();
    for (int i = 0; i < sequence.size() - 1; i++) {
      String left = symbolManager.getCanonical(sequence.get(i));
      String right = symbolManager.getCanonical(sequence.get(i + 1));

      Symbol leftSymbol = symbols.get(left);
      if (leftSymbol != null) {
        Integer rightFreq = leftSymbol.rightContext.get(right);
        if (rightFreq != null) {
          score += rightFreq.intValue();
        }
      }

      Symbol rightSymbol = symbols.get(right);
      if (rightSymbol != null) {
        Integer leftFreq = rightSymbol.leftContext.get(left);
        if (leftFreq != null) {
          score += leftFreq.intValue();
        }
      }
    }
    return score;
  }

  private List<String> normalizeStructuralEquivalentChoice(List<String> sequence) {
    if (sequence == null || sequence.isEmpty()) {
      return sequence;
    }

    List<String> normalized = new ArrayList<String>(sequence);
    for (int i = 0; i < normalized.size(); i++) {
      String preferred = preferredEquivalentForPosition(normalized, i);
      if (preferred != null) {
        normalized.set(i, preferred);
      }
    }
    return normalized;
  }

  private String preferredEquivalentForPosition(List<String> sequence, int index) {
    if (sequence == null || index < 0 || index >= sequence.size()) {
      return null;
    }
    String token = sequence.get(index);
    Set<String> candidates = equivalentCandidates(token);
    if (candidates.size() <= 1) {
      return token;
    }

    String leftNeighbor = index > 0 ? sequence.get(index - 1) : null;
    String rightNeighbor = index < sequence.size() - 1 ? sequence.get(index + 1) : null;

    String best = token;
    int bestScore = Integer.MIN_VALUE;
    for (String candidate : candidates) {
      int score = scoreCandidateInContext(candidate, leftNeighbor, rightNeighbor);
      if (score > bestScore) {
        bestScore = score;
        best = candidate;
      } else if (score == bestScore && token.equals(candidate)) {
        best = token;
      }
    }

    if (bestScore <= 0) {
      return token;
    }
    return best;
  }

  private Set<String> equivalentCandidates(String token) {
    LinkedHashSet<String> candidates = new LinkedHashSet<String>();
    if (token == null) {
      return candidates;
    }

    candidates.add(token);
    if (currentStructuralEquivalents == null || currentStructuralEquivalents.isEmpty()) {
      return candidates;
    }

    Set<String> direct = currentStructuralEquivalents.get(token);
    if (direct != null) {
      candidates.addAll(direct);
    }

    String canonical = canonicalOf(token);
    if (canonical != null) {
      candidates.add(canonical);
      Set<String> canonicalSet = currentStructuralEquivalents.get(canonical);
      if (canonicalSet != null) {
        candidates.addAll(canonicalSet);
      }
    }
    return candidates;
  }

  private int scoreCandidateInContext(String candidate, String leftNeighbor, String rightNeighbor) {
    if (symbolManager == null || candidate == null) {
      return 0;
    }

    Map<String, Symbol> symbols = symbolManager.getSymbols();
    if (symbols == null || symbols.isEmpty()) {
      return 0;
    }

    String canonicalCandidate = canonicalOf(candidate);
    Symbol candidateSymbol = symbols.get(canonicalCandidate);
    int score = 0;

    if (leftNeighbor != null) {
      String canonicalLeft = canonicalOf(leftNeighbor);
      Symbol leftSymbol = symbols.get(canonicalLeft);
      if (leftSymbol != null) {
        Integer freq = leftSymbol.rightContext.get(canonicalCandidate);
        if (freq != null) {
          score += freq.intValue();
        }
      }
      if (candidateSymbol != null) {
        Integer freq = candidateSymbol.leftContext.get(canonicalLeft);
        if (freq != null) {
          score += freq.intValue();
        }
      }
    }

    if (rightNeighbor != null) {
      String canonicalRight = canonicalOf(rightNeighbor);
      Symbol rightSymbol = symbols.get(canonicalRight);
      if (candidateSymbol != null) {
        Integer freq = candidateSymbol.rightContext.get(canonicalRight);
        if (freq != null) {
          score += freq.intValue();
        }
      }
      if (rightSymbol != null) {
        Integer freq = rightSymbol.leftContext.get(canonicalCandidate);
        if (freq != null) {
          score += freq.intValue();
        }
      }
    }

    return score;
  }

  private String canonicalOf(String token) {
    if (symbolManager == null) {
      return token;
    }
    return symbolManager.getCanonical(token);
  }

  private String toJoined(List<String> sequence) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < sequence.size(); i++) {
      if (i > 0) {
        sb.append(' ');
      }
      sb.append(sequence.get(i));
    }
    return sb.toString();
  }
}
