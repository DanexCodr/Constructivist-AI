package danexcodr.ai.core;

import danexcodr.ai.pattern.Data;
import java.util.*;

public class PatternSlotFiller {

  public static List<Set<Object>> fill(
    Data data,
    Map<String, Set<String>> aliases,
    Map<String, String> tokenToAlias,
    String T1,
    String T2,
    boolean useCommutative,
    boolean isFlipped) {

  List<Set<Object>> slots = new ArrayList<Set<Object>>();
  boolean hasC = false;

  if (isFlipped && !useCommutative) {
    isFlipped = false;
  }

  String firstTerm = isFlipped ? T2 : T1;
  String secondTerm = isFlipped ? T1 : T2;

  for (int i = 0; i < data.size(); i++) {
    Set<Object> slotOptions = new HashSet<Object>();
    
    if (data.isPlaceholder(i)) {
      SlotFlag flag = data.getPlaceholderAt(i);

      if (flag == SlotFlag._1) {
        slotOptions.add(firstTerm);
      } else if (flag == SlotFlag._2) {
        slotOptions.add(secondTerm);
      } else if (flag == SlotFlag._C) {
        if (!hasC) {
          slotOptions.add(firstTerm);
          hasC = true;
        } else {
          slotOptions.add(secondTerm);
        }
      } else if (flag == SlotFlag._X) {
        if (T1.startsWith("PF")) {
          slotOptions.add(T2);
        } else {
          slotOptions.add(T1);
        }
      }
      
    } else if (data.isPFToken(i)) {
      String pfToken = data.getPFTokenAt(i);
      slotOptions.add(pfToken);
      
    } else if (data.isToken(i)) {
      String token = data.getTokenAt(i);
      
      if (aliases.containsKey(token)) {
        Set<String> familyEquivalents = aliases.get(token);
        if (familyEquivalents != null) {
          slotOptions.addAll(familyEquivalents);
        }
      } else if (tokenToAlias.containsKey(token)) {
        String aliasKey = tokenToAlias.get(token);
        Set<String> familyEquivalents = aliases.get(aliasKey);
        if (familyEquivalents != null) {
          slotOptions.addAll(familyEquivalents);
        }
      } else {
        slotOptions.add(token);
      }
      
    } else if (data.isAlias(i)) {
      String aliasToken = data.getAliasAt(i);
      
      if (aliases.containsKey(aliasToken)) {
        Set<String> familyEquivalents = aliases.get(aliasToken);
        
        if (familyEquivalents != null) {
          slotOptions.addAll(familyEquivalents);
        }
      } else if (tokenToAlias.containsKey(aliasToken)) {
        String aliasKey = tokenToAlias.get(aliasToken);
        Set<String> familyEquivalents = aliases.get(aliasKey);
        if (familyEquivalents != null) {
          slotOptions.addAll(familyEquivalents);
        }
      } else {
        slotOptions.add(aliasToken);
      }
    }
    slots.add(slotOptions);
  }
  
  return slots;
}

  public static Data finalize(
      Data abstractPattern, 
      String t1, 
      String t2, 
      boolean isCommutative) {

    Data finalData = new Data();
    boolean t1IsPF = t1.startsWith("PF");
    boolean t2IsPF = t2.startsWith("PF");

    for (int i = 0; i < abstractPattern.size(); i++) {
      if (abstractPattern.isPFToken(i)) {
        finalData.addPFToken(abstractPattern.getPFTokenAt(i));
        continue;
      }

      if (abstractPattern.isPlaceholder(i)) {
        SlotFlag flag = abstractPattern.getPlaceholderAt(i);
        
        if (flag == SlotFlag._1) {
          if (isCommutative) {
            finalData.addPlaceholder(SlotFlag._C);
          } else if (t2IsPF && !t1IsPF) {
            finalData.addPlaceholder(SlotFlag._X);
          } else {
            finalData.addPlaceholder(SlotFlag._1);
          }
        } else if (flag == SlotFlag._2) {
          if (isCommutative) {
            finalData.addPlaceholder(SlotFlag._C);
          } else if (t1IsPF && !t2IsPF) {
            finalData.addPlaceholder(SlotFlag._X);
          } else {
            finalData.addPlaceholder(SlotFlag._2);
          }
        } else {
          finalData.addPlaceholder(flag);
        }
        
        continue;
      }

      if (abstractPattern.isToken(i)) {
        finalData.addToken(abstractPattern.getTokenAt(i));
      }
    }
    
    return finalData;
  }
}