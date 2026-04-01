package danexcodr.ai.core;

import danexcodr.ai.PatternFamilyBuilder;
import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternFamilyManager {

  private List<PatternFamily> cachedFamilies;
  private Map<String, PatternFamily> familyById = new HashMap<String, PatternFamily>();
  private boolean familiesDirty = true; // Manage dirty flag internally
  private List<Structure> allTemporaryPatterns =
      new ArrayList<Structure>(); // Store all temp patterns

  public PatternFamilyManager(List<PatternFamily> cachedFamilies) {
    this.cachedFamilies = cachedFamilies;
    updateMap();
  }

  // Removed familiesDirty parameter
  public List<PatternFamily> get(
      Map<String, Set<String>> structuralEquivalents, PatternFamilyBuilder familyBuilder) {
    if (familiesDirty || cachedFamilies == null) {
      // Build families from all accumulated patterns
      PatternFamilyBuilder builderToUse;
      if (familyBuilder != null) {
        builderToUse = familyBuilder;
      } else {
        builderToUse = new PatternFamilyBuilder(structuralEquivalents);
      }

      // Add ALL accumulated patterns to builder
      for (Structure pattern : allTemporaryPatterns) {
        builderToUse.addPattern(pattern);
      }

      cachedFamilies = builderToUse.buildPatternFamilies();
      updateMap();
      familiesDirty = false; // Reset internal flag
    }
    return cachedFamilies;
  }

  // Update families with temporary structural patterns
  public void update(List<Structure> tempPatterns, Map<String, Set<String>> structuralEquivalents) {
    if (tempPatterns.isEmpty()) return;

    // Accumulate patterns instead of replacing
    allTemporaryPatterns.addAll(tempPatterns);

    // Mark families as dirty so they'll be rebuilt with all patterns
    familiesDirty = true;
  }

  private void updateMap() {
    familyById.clear();
    if (cachedFamilies != null) {
      for (PatternFamily family : cachedFamilies) {
        familyById.put(family.getId(), family);
      }
    }
  }

  public PatternFamily getById(String id) {
    return familyById.get(id);
  }

  public List<PatternFamily> getCached() {
    return cachedFamilies;
  }

  public void setCached(List<PatternFamily> families) {
    this.cachedFamilies = families;
    updateMap();
    familiesDirty = false; // Reset when setting directly
  }

  // Mark families as dirty (e.g., when new equivalents are discovered)
  public void markDirty() {
    this.familiesDirty = true;
  }

  // Clear all temporary patterns (for testing/reset)
  public void clear() {
    allTemporaryPatterns.clear();
    familiesDirty = true;
  }
}
