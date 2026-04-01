package danexcodr.ai.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OptionalFinder {

  private Set<String> optionals = new HashSet<String>(); // Session-specific

  public Set<String> get() {
    return optionals;
  }

  public void clear() {
    optionals.clear();
  }

  public void analyze(List<List<String>> sequences) {

    if (sequences == null || sequences.size() < 2) {
      return;
    }

    // Clear existing optionals at the start
    optionals.clear();

    List<List<String>> sortedSequences = new ArrayList<List<String>>(sequences);
    Collections.sort(
        sortedSequences,
        new Comparator<List<String>>() {
          @Override
          public int compare(List<String> o1, List<String> o2) {
            return Integer.valueOf(o1.size()).compareTo(o2.size());
          }
        });

    for (int i = 0; i < sortedSequences.size(); i++) {
      for (int j = i + 1; j < sortedSequences.size(); j++) {
        List<String> shorter = sortedSequences.get(i);
        List<String> longer = sortedSequences.get(j);

        if (longer.size() == shorter.size() + 1) {
          String optionalToken = find(shorter, longer);
          if (optionalToken != null) {
            optionals.add(optionalToken);
          }
        }
      }
    }
  }

  private String find(List<String> shorter, List<String> longer) {
    if (shorter == null || longer == null || shorter.size() != longer.size() - 1) {
      return null;
    }
    for (int i = 0; i < longer.size(); i++) {
      List<String> tempLonger = new ArrayList<String>(longer);
      String removedToken = tempLonger.remove(i);
      if (tempLonger.equals(shorter)) {
        return removedToken;
      }
    }
    return null;
  }
}
