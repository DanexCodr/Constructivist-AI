package danexcodr.ai.core;

import danexcodr.ai.PatternFamilyBuilder;
import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternFamilyManager {

    private List<Pattern> allPatterns; // Now only RelationPatterns
    private List<PatternFamily> cachedFamilies;
    private Map<String, PatternFamily> familyById = new HashMap<String, PatternFamily>();
    private boolean familiesDirty = true; // NEW: Manage dirty flag internally
    private List<StructuralPattern> allTemporaryPatterns = new ArrayList<StructuralPattern>(); // NEW: Store all temp patterns

    public PatternFamilyManager(List<Pattern> allPatterns, List<PatternFamily> cachedFamilies) {
        this.allPatterns = allPatterns;
        this.cachedFamilies = cachedFamilies;
        updateFamilyMap();
    }

    public List<PatternFamily> getPatternFamilies(Map<String, Set<String>> structuralEquivalents, 
                                                     PatternFamilyBuilder familyBuilder) {
        if (familiesDirty || cachedFamilies == null) {
            // Always create a fresh builder so patterns are never counted more than once.
            // The shared familyBuilder passed in already has patterns added by PatternProcessor;
            // reusing it would duplicate every pattern in allTemporaryPatterns and inflate
            // family frequency counters exponentially on each rebuild.
            PatternFamilyBuilder freshBuilder = new PatternFamilyBuilder(structuralEquivalents);
            for (StructuralPattern pattern : allTemporaryPatterns) {
                freshBuilder.addPattern(pattern);
            }
            cachedFamilies = freshBuilder.buildPatternFamilies();
            updateFamilyMap();
            familiesDirty = false;
        }
        return cachedFamilies;
    }
    
    // NEW: Update families with temporary structural patterns
    public void updateFamiliesWithPatterns(List<StructuralPattern> tempPatterns, 
                                          Map<String, Set<String>> structuralEquivalents) {
        if (tempPatterns.isEmpty()) return;
        
        // NEW: Accumulate patterns instead of replacing
        allTemporaryPatterns.addAll(tempPatterns);
        
        // Mark families as dirty so they'll be rebuilt with all patterns
        familiesDirty = true;
    }
    
    private void updateFamilyMap() {
        familyById.clear();
        if (cachedFamilies != null) {
            for (PatternFamily family : cachedFamilies) {
                familyById.put(family.getId(), family);
            }
        }
    }
    
    public PatternFamily getFamilyById(String id) {
        return familyById.get(id);
    }
    
    public List<PatternFamily> getCachedFamilies() {
        return cachedFamilies;
    }
    
    public void setCachedFamilies(List<PatternFamily> families) {
        this.cachedFamilies = families;
        updateFamilyMap();
        familiesDirty = false; // NEW: Reset when setting directly
    }
    
    // NEW: Mark families as dirty (e.g., when new equivalents are discovered)
    public void markFamiliesDirty() {
        this.familiesDirty = true;
    }
    
    // NEW: Clear all temporary patterns (for testing/reset)
    public void clearTemporaryPatterns() {
        allTemporaryPatterns.clear();
        familiesDirty = true;
    }
}