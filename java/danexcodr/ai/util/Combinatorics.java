package danexcodr.ai.util;

import java.util.*;

public class Combinatorics {

  public static List<List<Object>> generate(List<Set<Object>> slots) {
    List<List<Object>> combinations = new ArrayList<List<Object>>();
    combinations.add(new ArrayList<Object>());

    for (Set<Object> slotOptions : slots) {
      if (slotOptions == null || slotOptions.isEmpty()) {
        continue;
      }

      List<List<Object>> newCombinations = new ArrayList<List<Object>>();

      if (combinations.isEmpty()) {
        for (Object option : slotOptions) {
          List<Object> newCombo = new ArrayList<Object>();
          newCombo.add(option);
          newCombinations.add(newCombo);
        }
      } else {
        for (List<Object> combo : combinations) {
          for (Object option : slotOptions) {
            List<Object> newCombo = new ArrayList<Object>(combo);
            newCombo.add(option);
            newCombinations.add(newCombo);
          }
        }
      }
      combinations = newCombinations;
    }

    return combinations;
  }

  public static Set<List<Object>> deduplicate(List<List<Object>> combinations) {
    Set<List<Object>> uniqueCombinations = new LinkedHashSet<List<Object>>();
    for (List<Object> combo : combinations) {
      uniqueCombinations.add(combo);
    }
    return uniqueCombinations;
  }
}