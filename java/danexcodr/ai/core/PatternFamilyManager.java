package danexcodr.ai.core;

import danexcodr.ai.PatternFamilyBuilder;
import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternFamilyManager {

private static List<Pattern> allPatterns;
private static List<PatternFamily> cachedFamilies;

public PatternFamilyManager(List<Pattern> allPatterns, List<PatternFamily> cachedFamilies) {
this.allPatterns = allPatterns;
  this.cachedFamilies = cachedFamilies;
}

        public static List<PatternFamily> getPatternFamilies(Map<String, Set <String>> structuralEquivalents, PatternFamilyBuilder familyBuilder, boolean familiesDirty) {
            if (!familiesDirty && cachedFamilies != null) {
                return cachedFamilies;
            }
            familyBuilder = new PatternFamilyBuilder(structuralEquivalents, new ArrayList<Pattern>(allPatterns));
            cachedFamilies = familyBuilder.buildPatternFamilies();
            familiesDirty = false;
            return cachedFamilies;
        }
    }