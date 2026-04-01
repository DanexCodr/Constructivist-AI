package danexcodr.ai.core;

import danexcodr.ai.pattern.*;
import java.util.*;

public class PatternMatcher {

  public static boolean isSubSequenceMatch(
      List<String> sequence,
      int startIdx,
      Data patternData,
      PatternFamily family,
      Set<String> protectedTokens) {

    for (int j = 0; j < patternData.size(); j++) {
      String token = sequence.get(startIdx + j);

      if (patternData.isPlaceholder(j)) {
        if (protectedTokens.contains(token)) {
          return false;
        }
        continue;
      }

      if (patternData.isPFToken(j)) {
        if (!patternData.getPFTokenAt(j).equals(token)) {
          return false;
        }
        continue;
      }

      if (patternData.isAlias(j)) {
        String alias = patternData.getAliasAt(j);
        String tokenAlias = family.getTokenToAlias().get(token);
        if (tokenAlias == null || !tokenAlias.equals(alias)) {
          return false;
        }
        continue;
      }

      if (patternData.isToken(j)) {
        String patternToken = patternData.getTokenAt(j);
        if (!patternToken.equals(token)) {
          String tokenAlias = family.getTokenToAlias().get(token);
          String patternAlias = family.getTokenToAlias().get(patternToken);
          if (tokenAlias == null || patternAlias == null || 
              !tokenAlias.equals(patternAlias)) {
            return false;
          }
        }
      }
    }
    return true;
  }

  public static boolean matchesSequenceWithFamily(
      List<String> sequence, PatternFamily family, Set<String> protectedTokens) {

    for (Structure sp : family.getMemberPatterns()) {
      Data patternData = sp.getData();
      for (int i = 0; i <= sequence.size() - patternData.size(); i++) {
        if (isSubSequenceMatch(sequence, i, patternData, family, protectedTokens)) {
          return true;
        }
      }
    }
    return false;
  }

  public static boolean containsPatternFamily(List<String> sequence, PatternFamily family) {
    for (Structure sp : family.getMemberPatterns()) {
      Data patternData = sp.getData();
      
      if (sp.isCommutative()) {
        Set<String> requiredTokens = new HashSet<String>();
        for (int i = 0; i < patternData.size(); i++) {
          if (patternData.isToken(i)) {
            requiredTokens.add(patternData.getTokenAt(i));
          }
        }
        Set<String> seqSet = new HashSet<String>(sequence);
        return seqSet.containsAll(requiredTokens);
      } else {
        for (int i = 0; i <= sequence.size() - patternData.size(); i++) {
          boolean match = true;
          for (int j = 0; j < patternData.size(); j++) {
            if (patternData.isPlaceholder(j) || patternData.isAlias(j)) {
              continue;
            }
            if (patternData.isToken(j)) {
              if (!patternData.getTokenAt(j).equals(sequence.get(i + j))) {
                match = false;
                break;
              }
            }
            if (patternData.isPFToken(j)) {
              if (!patternData.getPFTokenAt(j).equals(sequence.get(i + j))) {
                match = false;
                break;
              }
            }
          }
          if (match) return true;
        }
      }
    }
    return false;
  }
}