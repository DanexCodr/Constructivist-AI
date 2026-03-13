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

    // CHANGED: Removed familiesDirty parameter
    public List<PatternFamily> getPatternFamilies(Map<String, Set<String>> structuralEquivalents, 
                                                     PatternFamilyBuilder familyBuilder) {
        if (familiesDirty || cachedFamilies == null) {
            // CHANGED: Build families from all accumulated patterns
            PatternFamilyBuilder builderToUse;
            if (familyBuilder != null) {
                builderToUse = familyBuilder;
            } else {
                builderToUse = new PatternFamilyBuilder(structuralEquivalents);
            }
            
            // Add ALL accumulated patterns to builder
            for (StructuralPattern pattern : allTemporaryPatterns) {
                builderToUse.addPattern(pattern);
            }
            
            cachedFamilies = builderToUse.buildPatternFamilies();
            updateFamilyMap();
            familiesDirty = false; // NEW: Reset internal flag
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