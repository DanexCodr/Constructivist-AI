package danexcodr.ai.util;

import java.util.*;

public class Combinatorics {
    
public static List<List<String>> generateCombinations(List<Set<String>> slots) {
    List<List<String>> combinations = new ArrayList<List<String>>();
    combinations.add(new ArrayList<String>());

    for (Set<String> slotOptions : slots) {
        if (slotOptions == null || slotOptions.isEmpty()) {
            continue;
        }

        List<List<String>> newCombinations = new ArrayList<List<String>>();
        
        if (combinations.isEmpty()) {
            for (String option : slotOptions) {
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
    
    public static List<List<String>> generateCartesianProduct(List<Set<String>> sets) {
        return generateCombinations(sets);
    }
    
    public static Set<List<String>> deduplicateCombinations(List<List<String>> combinations) {
        Set<List<String>> uniqueCombinations = new LinkedHashSet<List<String>>();
        for (List<String> combo : combinations) {
            uniqueCombinations.add(combo);
        }
        return uniqueCombinations;
    }
}
