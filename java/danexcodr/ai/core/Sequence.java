package danexcodr.ai.core;

import java.util.*;
import danexcodr.ai.pattern.*;


public class Sequence {

    private List<Pattern> allPatterns;

    public Sequence(List<Pattern> allPatterns) {
        this.allPatterns = allPatterns;
    }

    public List<List<String>> infer(String T1, String T2) {
        Set<List<String>> shallowResults = new LinkedHashSet<List<String>>();
        boolean foundNonLogicalDirectRelation = false;

        // ==========================================
        // 1. DIRECT RELATIONS
        // ==========================================
        RelationPattern directRelation = PatternProcessor.findRelationPattern(T1, T2);
        if (directRelation != null) {
            if (directRelation.getFamily() != null) {
                shallowResults.addAll(
                        inferFromFamily(directRelation.getFamily(), T1, T2, directRelation.isCommutative())
                );
                if (!directRelation.getFamily().isLogical()) {
                    foundNonLogicalDirectRelation = true;
                }
            }
        }

        RelationPattern reverseRelation = PatternProcessor.findRelationPattern(T2, T1);
        if (reverseRelation != null) {
            if (reverseRelation.getFamily() != null) {
                shallowResults.addAll(
                        inferFromFamily(reverseRelation.getFamily(), T2, T1, reverseRelation.isCommutative())
                );
                if (!reverseRelation.getFamily().isLogical()) {
                    foundNonLogicalDirectRelation = true;
                }
            }
        }

        if (foundNonLogicalDirectRelation) {
            List<List<String>> results = new ArrayList<List<String>>(shallowResults);
            RelationFinder relationFinder = new RelationFinder(allPatterns);
            relationFinder.validateInferences(T1, T2, results);
            return results;
        }

        RelationFinder relationFinder = new RelationFinder(allPatterns);

        // ==========================================
        // 2. SINGLE-HOP (TRANSITIVE)
        // ==========================================
        Set<String> T1Connectors = relationFinder.findConnectors(T1);
        Set<String> T2Connectors = relationFinder.findConnectors(T2);

        Set<String> commonConnectors = new HashSet<String>(T1Connectors);
        commonConnectors.retainAll(T2Connectors);

        for (String commonConnector : commonConnectors) {
            RelationPattern T1tC = PatternProcessor.findRelationPattern(T1, commonConnector);
            if (T1tC == null) T1tC = PatternProcessor.findRelationPattern(commonConnector, T1);

            RelationPattern CtT2 = PatternProcessor.findRelationPattern(commonConnector, T2);
            if (CtT2 == null) CtT2 = PatternProcessor.findRelationPattern(T2, commonConnector);

            if (T1tC != null && CtT2 != null) {
                boolean T1L = (T1tC.getFamily() != null && T1tC.getFamily().isLogical());
                boolean T2L = (CtT2.getFamily() != null && CtT2.getFamily().isLogical());

                if(T1L || T2L) {
                    PatternFamily familyToUse = null;
                    boolean commutativity = false;
                    String sourceRelation = "";

                    // 1. If First Leg (T1 -> C) is Commutative, use First Leg's Family.
                    // 2. If First Leg is Non-Commutative, use Second Leg's Family.
                    
                    if (T1tC.isCommutative()) {
                         familyToUse = T1tC.getFamily();
                         commutativity = T1tC.isCommutative();
                         sourceRelation = T1 + "->" + commonConnector + " (First Leg Commutative)";
                    } else {
                         familyToUse = CtT2.getFamily();
                         commutativity = CtT2.isCommutative();
                         sourceRelation = commonConnector + "->" + T2 + " (First Leg Non-Commutative)";
                    }
                    shallowResults.addAll(inferFromFamily(familyToUse, T1, T2, commutativity));
                }
            }
        }

        // ==========================================
        // 3. LOGICAL BRIDGES (The "is not" logic)
        // ==========================================
        Set<String> parents1 = new HashSet<String>();
        for (String conn : relationFinder.findConnectors(T1)) {
            RelationPattern rp = PatternProcessor.findRelationPattern(T1, conn);
            if (rp != null && rp.getT2().equals(conn) && !rp.isCommutative()) {
                parents1.add(conn);
            }
        }

        Set<String> parents2 = new HashSet<String>();
        for (String conn : relationFinder.findConnectors(T2)) {
            RelationPattern rp = PatternProcessor.findRelationPattern(T2, conn);
            if (rp != null && rp.getT2().equals(conn) && !rp.isCommutative()) {
                parents2.add(conn);
            }
        }

        for (String p1 : parents1) {
            for (String p2 : parents2) {
                RelationPattern bridge = PatternProcessor.findRelationPattern(p1, p2);
                if (bridge == null) bridge = PatternProcessor.findRelationPattern(p2, p1);

                if (bridge != null && bridge.getFamily() != null && bridge.isCommutative()) {
                    shallowResults.addAll(
                            inferFromFamily(bridge.getFamily(), T1, T2, bridge.isCommutative())
                    );
                }
            }
        }

        // ==========================================
        // 4. DEEP FALLBACK
        // ==========================================
        if (shallowResults.isEmpty()) {
            List<String> path = relationFinder.findDeepPath(T1, T2);
            if (path != null && path.size() > 2) {

                String firstHopNode = path.get(1);
                RelationPattern firstLeg = PatternProcessor.findRelationPattern(T1, firstHopNode);
                if (firstLeg == null) firstLeg = PatternProcessor.findRelationPattern(firstHopNode, T1);

                String lastHopNode = path.get(path.size() - 2);
                RelationPattern lastLeg = PatternProcessor.findRelationPattern(lastHopNode, T2);
                if (lastLeg == null) lastLeg = PatternProcessor.findRelationPattern(T2, lastHopNode);

                if (firstLeg != null && lastLeg != null && firstLeg.getFamily() != null && lastLeg.getFamily() != null) {
                    
                    PatternFamily familyToUse = null;
                    boolean commutativity = false;
                    String sourceRelation = "";

                    // FIX: Apply same Commutativity Rule to Deep Path
                    if (firstLeg.isCommutative()) {
                         familyToUse = firstLeg.getFamily();
                         commutativity = firstLeg.isCommutative();
                         sourceRelation = T1 + "->" + firstHopNode + " (First Leg Commutative)";
                    } else {
                         familyToUse = lastLeg.getFamily();
                         commutativity = lastLeg.isCommutative();
                         sourceRelation = lastHopNode + "->" + T2 + " (Last Leg Inheritance)";
                    }
                    
                    shallowResults.addAll(
                            inferFromFamily(familyToUse, T1, T2, commutativity) 
                    );
                }
            }
        }

        List<List<String>> finalResults = new ArrayList<List<String>>(shallowResults);
        relationFinder.validateInferences(T1, T2, finalResults);
        return finalResults;
    }

    public static List<List<String>> inferFromFamily(
      PatternFamily family, String T1, String T2, boolean useCommutative) {
    Set<List<String>> inferdSequences = new LinkedHashSet<List<String>>();
    Map<String, Set<String>> aliases = family.getAliases();
    Map<String, String> wordToAlias = family.getWordToAlias();

    for (StructuralPattern sp : family.getMemberPatterns()) {
      if (sp.isCommutative() != useCommutative) {
        continue;
      }

      List<String> structuralSlots = sp.getStructuralSlots();

      // Pass 1: Standard Order
      List<Set<String>> slotsPass1 = new ArrayList<Set<String>>();
      boolean hasC = false;

      for (String token : structuralSlots) {
        if (token.equals("[1]")) {
          slotsPass1.add(new HashSet<String>(Arrays.asList(T1)));
        } else if (token.equals("[2]")) {
          slotsPass1.add(new HashSet<String>(Arrays.asList(T2)));
        } else if (token.equals("[C]")) {
          // First [C] gets T1, Second [C] gets T2
          if (!hasC) {
            slotsPass1.add(new HashSet<String>(Arrays.asList(T1)));
            hasC = true;
          } else {
            slotsPass1.add(new HashSet<String>(Arrays.asList(T2)));
          }
        } else if (token.equals("[X]")) {
          // If T1 is the PF, then T2 is the variable [X], and vice versa.
          if (T1.startsWith("PF")) {
            slotsPass1.add(new HashSet<String>(Arrays.asList(T2)));
          } else {
            slotsPass1.add(new HashSet<String>(Arrays.asList(T1)));
          }
        } else if (wordToAlias.containsKey(token)) {
          String alias = wordToAlias.get(token);
          Set<String> equivalents = aliases.get(alias);
          slotsPass1.add(
              (equivalents != null) ? equivalents : new HashSet<String>(Arrays.asList(token)));
        } else {
          slotsPass1.add(new HashSet<String>(Arrays.asList(token)));
        }
      }
      inferdSequences.addAll(getCombinations(slotsPass1));

      // Pass 2: Flipped (Only if Commutative)
      if (sp.isCommutative()) {
        List<Set<String>> slotsFlipped = new ArrayList<Set<String>>();
        hasC = false;

        for (String token : structuralSlots) {
          if (token.equals("[1]")) {
            slotsFlipped.add(new HashSet<String>(Arrays.asList(T2)));
          } else if (token.equals("[2]")) {
            slotsFlipped.add(new HashSet<String>(Arrays.asList(T1)));
          } else if (token.equals("[C]")) {
            // Flipped: First [C] gets T2, Second [C] gets T1
            if (!hasC) {
              slotsFlipped.add(new HashSet<String>(Arrays.asList(T2)));
              hasC = true;
            } else {
              slotsFlipped.add(new HashSet<String>(Arrays.asList(T1)));
            }
          } else if (token.equals("[X]")) {
            // Logic mirrors Pass 1 but assumes implicit flip of role if applicable.
            // (Note: [X] patterns are typically non-commutative, so this may not trigger)
            if (T1.startsWith("PF")) {
               slotsFlipped.add(new HashSet<String>(Arrays.asList(T2)));
            } else {
               slotsFlipped.add(new HashSet<String>(Arrays.asList(T1)));
            }
          } else if (wordToAlias.containsKey(token)) {
            String alias = wordToAlias.get(token);
            Set<String> equivalents = aliases.get(alias);
            slotsFlipped.add(
                (equivalents != null) ? equivalents : new HashSet<String>(Arrays.asList(token)));
          } else {
            slotsFlipped.add(new HashSet<String>(Arrays.asList(token)));
          }
        }
        inferdSequences.addAll(getCombinations(slotsFlipped));
      }
    }

    return new ArrayList<List<String>>(inferdSequences);
  }

    public static List<List<String>> getCombinations(List<Set<String>> slots) {
        List<List<String>> combinations = new ArrayList<List<String>>();
        combinations.add(new ArrayList<String>());

        for (Set<String> slotOptions : slots) {
            if (slotOptions == null || slotOptions.isEmpty()) {
                continue;
            }

            List<List<String>> newCombinations = new ArrayList<List<String>>();
            if (combinations.isEmpty()) {
                for(String option : slotOptions) {
                    List<String> newCombo = new ArrayList<String>();
                    newCombo.add(option);
                    newCombinations.add(newCombo);
                }
            } else {
                for (List<String> combo : combinations) {
                    for (String option : slotOptions) {
                        List<String> newCombo = new ArrayList<String>(combo);
                        newCombo.add(option);
                        newCombinations.add(newCombo);
                    }
                }
            }
            combinations = newCombinations;
        }
        return combinations;
    }
}