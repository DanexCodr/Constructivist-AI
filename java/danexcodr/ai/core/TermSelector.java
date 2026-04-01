package danexcodr.ai.core;

import java.util.*;

public class TermSelector {

  public Set<String> identifyStructuralTokens(
      List<List<String>> equivalentSequences, Set<String> contentTokens) {
    Set<String> structuralTokens = new LinkedHashSet<String>();
    for (List<String> sequence : equivalentSequences) {
      for (String token : sequence) {
        if (!contentTokens.contains(token)) structuralTokens.add(token);
      }
    }
    return structuralTokens;
  }

  public String[] determinePositionalTerms(
      String w1, String w2, List<List<String>> equivalentSequences) {
    int w1_left_of_w2 = 0;
    int w2_left_of_w1 = 0;
    for (List<String> sequence : equivalentSequences) {
      int idx1 = sequence.indexOf(w1);
      int idx2 = sequence.indexOf(w2);
      if (idx1 != -1 && idx2 != -1) {
        if (idx1 < idx2) w1_left_of_w2++;
        else if (idx2 < idx1) w2_left_of_w1++;
      }
    }
    if (w2_left_of_w1 > w1_left_of_w2) {
      return new String[] {w2, w1};
    } else {
      return new String[] {w1, w2};
    }
  }
}
