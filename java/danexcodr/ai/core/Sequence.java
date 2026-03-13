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

    private PatternFamily getFreshFamily(PatternFamily family) {
        if (family == null || currentStructuralEquivalents == null) {
            return family;
        }
        
        family.updateAliases(currentStructuralEquivalents);
        return family;
    }

    private PatternFamily getFamilyForRelation(RelationPattern relation) {
        if (relation.getFamilyId() != null && familyManager != null) {
            PatternFamily family = familyManager.getFamilyById(relation.getFamilyId());
            return getFreshFamily(family);
        }
        return null;
    }

    public List<List<String>> infer(String T1, String T2) {
        Set<List<String>> shallowResults = new LinkedHashSet<List<String>>();
        boolean foundNonLogicalDirectRelation = false;

        // 1. Check for direct relations
        List<RelationPattern> directRelations = patternProcessor.findAllRelationPatterns(T1, T2);
        for (RelationPattern directRelation : directRelations) {
            PatternFamily family = getFamilyForRelation(directRelation);
            
            if (family != null) {
                shallowResults.addAll(
                        inferFromFamily(family, T1, T2, directRelation.isCommutative())
                );
                foundNonLogicalDirectRelation = true;
            }
        }

        // 2. Check for reverse relations
        List<RelationPattern> reverseRelations = patternProcessor.findAllRelationPatterns(T2, T1);
        for (RelationPattern reverseRelation : reverseRelations) {
            PatternFamily family = getFamilyForRelation(reverseRelation);
            
            if (family != null) {
                shallowResults.addAll(
                        inferFromFamily(family, T2, T1, reverseRelation.isCommutative())
                );
                foundNonLogicalDirectRelation = true;
            }
        }

        if (foundNonLogicalDirectRelation) {
            List<List<String>> results = new ArrayList<List<String>>(shallowResults);
            RelationFinder relationFinder = new RelationFinder(allPatterns, currentFamilies, 
                                                              currentStructuralEquivalents, 
                                                              patternProcessor, this, familyManager);
            relationFinder.validateInferences(T1, T2, results);
            return results;
        }

        // 3. Find common connectors with proper transitive inference
        RelationFinder relationFinder = new RelationFinder(allPatterns, currentFamilies, 
                                                          currentStructuralEquivalents, 
                                                          patternProcessor, this, familyManager);

        Set<String> T1Connectors = relationFinder.findConnectors(T1);
        Set<String> T2Connectors = relationFinder.findConnectors(T2);
        
        Set<String> commonConnectors = new HashSet<String>(T1Connectors);
        commonConnectors.retainAll(T2Connectors);

        // RESTORED: Proper transitive inference logic from old version
        for (String commonConnector : commonConnectors) {
            // Get all possible first leg relations
            List<RelationPattern> T1toCommon = patternProcessor.findAllRelationPatterns(T1, commonConnector);
            List<RelationPattern> commonToT1 = patternProcessor.findAllRelationPatterns(commonConnector, T1);
            List<RelationPattern> firstLegs = new ArrayList<RelationPattern>();
            firstLegs.addAll(T1toCommon);
            firstLegs.addAll(commonToT1);
            
            // Get all possible second leg relations  
            List<RelationPattern> commonToT2 = patternProcessor.findAllRelationPatterns(commonConnector, T2);
            List<RelationPattern> T2toCommon = patternProcessor.findAllRelationPatterns(T2, commonConnector);
            List<RelationPattern> secondLegs = new ArrayList<RelationPattern>();
            secondLegs.addAll(commonToT2);
            secondLegs.addAll(T2toCommon);
            
            if (!firstLegs.isEmpty() && !secondLegs.isEmpty()) {
                for (RelationPattern firstLeg : firstLegs) {
                    for (RelationPattern secondLeg : secondLegs) {
                        PatternFamily family1 = getFamilyForRelation(firstLeg);
                        PatternFamily family2 = getFamilyForRelation(secondLeg);
                        
                        // FIXED: More permissive transitive logic from old version
                        boolean canUseForTransitive = false;
                        PatternFamily familyToUse = null;
                        boolean commutativity = false;
                        
                        // Case 1: Both non-commutative (standard transitive)
                        if (!firstLeg.isCommutative() && !secondLeg.isCommutative()) {
                            canUseForTransitive = true;
                            familyToUse = family1;
                            commutativity = false;
                        }
                        // Case 2: Commutative middle with non-commutative ends
                        else if (firstLeg.isCommutative() && !secondLeg.isCommutative()) {
                            canUseForTransitive = true;
                            familyToUse = family1;
                            commutativity = firstLeg.isCommutative();
                        }
                        // Case 3: Non-commutative start with commutative end
                        else if (!firstLeg.isCommutative() && secondLeg.isCommutative()) {
                            canUseForTransitive = true;
                            familyToUse = family2;
                            commutativity = secondLeg.isCommutative();
                        }
                        // Case 4: Both commutative through same family
                        else if (firstLeg.isCommutative() && secondLeg.isCommutative() &&
                                 family1 != null && family2 != null && 
                                 family1.getId().equals(family2.getId())) {
                            canUseForTransitive = true;
                            familyToUse = family1;
                            commutativity = true;
                        }
                        // Case 5: Both non-commutative through SAME family
                        else if (!firstLeg.isCommutative() && !secondLeg.isCommutative() &&
                                 family1 != null && family2 != null && 
                                 family1.getId().equals(family2.getId())) {
                            canUseForTransitive = true;
                            familyToUse = family1;
                            commutativity = false;
                        }
                        
                        if (canUseForTransitive && familyToUse != null) {
                            shallowResults.addAll(inferFromFamily(familyToUse, T1, T2, commutativity));
                        }
                    }
                }
            }
        }

        // 4. RESTORED: Commutative Bridges (parent bridge detection from old version)
        Set<String> parents1 = new HashSet<String>();
        for (String conn : relationFinder.findConnectors(T1)) {
            List<RelationPattern> rps = patternProcessor.findAllRelationPatterns(T1, conn);
            for (RelationPattern rp : rps) {
                if (rp.getT2().equals(conn) && !rp.isCommutative()) {
                    parents1.add(conn);
                }
            }
        }

        Set<String> parents2 = new HashSet<String>();
        for (String conn : relationFinder.findConnectors(T2)) {
            List<RelationPattern> rps = patternProcessor.findAllRelationPatterns(T2, conn);
            for (RelationPattern rp : rps) {
                if (rp.getT2().equals(conn) && !rp.isCommutative()) {
                    parents2.add(conn);
                }
            }
        }

        for (String p1 : parents1) {
            for (String p2 : parents2) {
                List<RelationPattern> bridges = patternProcessor.findAllRelationPatterns(p1, p2);
                if (bridges.isEmpty()) bridges = patternProcessor.findAllRelationPatterns(p2, p1);

                for (RelationPattern bridge : bridges) {
                    PatternFamily family = getFamilyForRelation(bridge);
                    
                    if (family != null && bridge.isCommutative()) {
                        // Use ANY commutative bridge between parents
                        shallowResults.addAll(
                                inferFromFamily(family, T1, T2, bridge.isCommutative())
                        );
                    }
                }
            }
        }

        // 5. RESTORED: Deep Fallback with proper path utilization
        if (shallowResults.isEmpty()) {
            List<String> path = relationFinder.findDeepPath(T1, T2);
            
            if (path != null && path.size() > 2) {
                // Try ALL possible pairs along the path, not just first and last
                for (int i = 0; i < path.size() - 1; i++) {
                    for (int j = i + 1; j < path.size(); j++) {
                        String node1 = path.get(i);
                        String node2 = path.get(j);
                        
                        // Skip if same as original terms (already tried)
                        if ((node1.equals(T1) && node2.equals(T2)) || 
                            (node1.equals(T2) && node2.equals(T1))) {
                            continue;
                        }
                        
                        // Try to find relation between these nodes
                        List<RelationPattern> relations = patternProcessor.findAllRelationPatterns(node1, node2);
                        if (relations.isEmpty()) {
                            relations = patternProcessor.findAllRelationPatterns(node2, node1);
                        }
                        
                        for (RelationPattern relation : relations) {
                            PatternFamily family = getFamilyForRelation(relation);
                            
                            if (family != null) {
                                // Use this family to generate sequences for original terms
                                shallowResults.addAll(
                                    inferFromFamily(family, T1, T2, relation.isCommutative())
                                );
                            }
                        }
                    }
                }
                
                // Original logic (for backward compatibility)
                String firstHopNode = path.get(1);
                List<RelationPattern> firstLegs = patternProcessor.findAllRelationPatterns(T1, firstHopNode);
                if (firstLegs.isEmpty()) firstLegs = patternProcessor.findAllRelationPatterns(firstHopNode, T1);

                String lastHopNode = path.get(path.size() - 2);
                List<RelationPattern> lastLegs = patternProcessor.findAllRelationPatterns(lastHopNode, T2);
                if (lastLegs.isEmpty()) lastLegs = patternProcessor.findAllRelationPatterns(T2, lastHopNode);

                if (!firstLegs.isEmpty() && !lastLegs.isEmpty()) {
                    // Try each combination of first and last leg
                    for (RelationPattern firstLeg : firstLegs) {
                        for (RelationPattern lastLeg : lastLegs) {
                            PatternFamily family1 = getFamilyForRelation(firstLeg);
                            PatternFamily family2 = getFamilyForRelation(lastLeg);
                            
                            if (family1 != null && family2 != null) {
                                
                                PatternFamily familyToUse = null;
                                boolean commutativity = false;
                                
                                // FIXED: More flexible family selection
                                if (firstLeg.isCommutative()) {
                                     familyToUse = family1;
                                     commutativity = firstLeg.isCommutative();
                                } else if (lastLeg.isCommutative()) {
                                     familyToUse = family2;
                                     commutativity = lastLeg.isCommutative();
                                } else {
                                     // Both non-commutative, just use first family
                                     familyToUse = family1;
                                     commutativity = false;
                                }
                                
                                if (familyToUse != null) {
                                    shallowResults.addAll(
                                            inferFromFamily(familyToUse, T1, T2, commutativity) 
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. REMOVED: The "try all families" fallback - too permissive and generates nonsense
        // If nothing found, return empty list instead of nonsense

        List<List<String>> finalResults = new ArrayList<List<String>>(shallowResults);
        relationFinder.validateInferences(T1, T2, finalResults);
        return finalResults;
    }

    public List<List<String>> inferFromFamily(
      PatternFamily family, String T1, String T2, boolean useCommutative) {
        return inferFromFamily(family, T1, T2, useCommutative, null);
    }

    public List<List<String>> inferFromFamily(
      PatternFamily family, String T1, String T2, boolean useCommutative,
      Map<String, Set<String>> currentStructuralEquivalents) {
        
        Set<List<String>> inferredSequences = new LinkedHashSet<List<String>>();
        
        if (family == null) {
            return new ArrayList<List<String>>(inferredSequences);
        }
        
        if (currentStructuralEquivalents != null) {
            family.updateAliases(currentStructuralEquivalents);
        }
        
        Map<String, Set<String>> aliases = family.getAliases();
        Map<String, String> wordToAlias = family.getWordToAlias();
        
        for (StructuralPattern sp : family.getMemberPatterns()) {
            if (sp.isCommutative() != useCommutative) {
                continue;
            }
          
            List<String> structuralSlots = family.getAliasedSlots(sp);
          
            List<Set<String>> slotsPass1 = PatternSlotFiller.fillPatternSlots(
                structuralSlots, aliases, wordToAlias, T1, T2, useCommutative, false);
            
            List<List<String>> combos1 = Combinatorics.generateCombinations(slotsPass1);
            inferredSequences.addAll(combos1);
         
            if (sp.isCommutative()) {
                List<Set<String>> slotsFlipped = PatternSlotFiller.fillPatternSlots(
                    structuralSlots, aliases, wordToAlias, T1, T2, useCommutative, true);
                
                List<List<String>> combos2 = Combinatorics.generateCombinations(slotsFlipped);
                inferredSequences.addAll(combos2);
            }
        }

        return new ArrayList<List<String>>(inferredSequences);
      }
}