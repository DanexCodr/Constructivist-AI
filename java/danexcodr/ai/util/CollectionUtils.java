package danexcodr.ai.util;

import java.util.*;

public class CollectionUtils {

  public static <T> Set<T> intersect(Set<T> set1, Set<T> set2) {
    Set<T> result = new HashSet<T>(set1);
    result.retainAll(set2);
    return result;
  }

  public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
    Set<T> result = new HashSet<T>(set1);
    result.addAll(set2);
    return result;
  }

  public static <T> Set<T> difference(Set<T> set1, Set<T> set2) {
    Set<T> result = new HashSet<T>(set1);
    result.removeAll(set2);
    return result;
  }

  public static <T> List<T> sort(List<T> list) {
    List<T> sorted = new ArrayList<T>(list);
    Collections.sort(
        sorted,
        new Comparator<T>() {
          @Override
          public int compare(T o1, T o2) {
            return o1.toString().compareTo(o2.toString());
          }
        });
    return sorted;
  }

  public static <T> Set<T> getAll(List<List<T>> sequences) {
    Set<T> all = new HashSet<T>();
    for (List<T> sequence : sequences) {
      all.addAll(sequence);
    }
    return all;
  }
}
