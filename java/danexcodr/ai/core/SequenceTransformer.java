package danexcodr.ai.core;

import danexcodr.ai.pattern.Data;
import java.util.*;

public class SequenceTransformer {

  public static Data abstractSequence(List<String> sequence, String term1, String term2) {
    Data data = new Data();
    for (String token : sequence) {
      if (token.equals(term1)) {
        data.addPlaceholder(SlotFlag._1);
      } else if (token.equals(term2)) {
        data.addPlaceholder(SlotFlag._2);
      } else {
        data.addToken(token);
      }
    }
    return data;
  }

  public static Data abstractSequencePF(List<String> sequence, String term1, String term2) {
    Data data = new Data();
    for (String token : sequence) {
      if (token.equals(term1)) {
        if (token.startsWith("PF")) {
          data.addPFToken(token);
        } else {
          data.addPlaceholder(SlotFlag._1);
        }
      } else if (token.equals(term2)) {
        if (token.startsWith("PF")) {
          data.addPFToken(token);
        } else {
          data.addPlaceholder(SlotFlag._2);
        }
      } else {
        data.addToken(token);
      }
    }
    return data;
  }

  public static Data flipTermPattern(Data pattern) {
    Data flipped = new Data();
    for (int i = 0; i < pattern.size(); i++) {
      if (pattern.isPlaceholder(i)) {
        SlotFlag flag = pattern.getPlaceholderAt(i);
        if (flag == SlotFlag._1) {
          flipped.addPlaceholder(SlotFlag._2);
        } else if (flag == SlotFlag._2) {
          flipped.addPlaceholder(SlotFlag._1);
        } else {
          flipped.addPlaceholder(flag);
        }
      } else if (pattern.isPFToken(i)) {
        flipped.addPFToken(pattern.getPFTokenAt(i));
      } else if (pattern.isToken(i)) {
        flipped.addToken(pattern.getTokenAt(i));
      }
    }
    return flipped;
  }

  public static Data flipTermPatternWithTerms(Data pattern, String term1, String term2) {
    Data flipped = new Data();
    for (int i = 0; i < pattern.size(); i++) {
      if (pattern.isPlaceholder(i)) {
        SlotFlag flag = pattern.getPlaceholderAt(i);
        if (flag == SlotFlag._1) {
          flipped.addPlaceholder(SlotFlag._2);
        } else if (flag == SlotFlag._2) {
          if (term1.startsWith("PF")) {
            flipped.addPFToken(term1);
          } else {
            flipped.addPlaceholder(SlotFlag._1);
          }
        } else {
          flipped.addPlaceholder(flag);
        }
      } else if (pattern.isToken(i)) {
        String token = pattern.getTokenAt(i);
        if (token.equals(term1)) {
          flipped.addToken(term2);
        } else if (token.equals(term2)) {
          flipped.addToken(term1);
        } else {
          flipped.addToken(token);
        }
      } else if (pattern.isPFToken(i)) {
        flipped.addPFToken(pattern.getPFTokenAt(i));
      }
    }
    return flipped;
  }

  public static Data convertPFToPlaceholders(
      Data sequence, Map<String, String> pfToPlaceholder) {
    Data converted = new Data();
    for (int i = 0; i < sequence.size(); i++) {
      if (sequence.isPFToken(i)) {
        String pfToken = sequence.getPFTokenAt(i);
        if (pfToPlaceholder.containsKey(pfToken)) {
          converted.addToken(pfToPlaceholder.get(pfToken));
        } else {
          converted.addPFToken(pfToken);
        }
      } else if (sequence.isPlaceholder(i)) {
        converted.addPlaceholder(sequence.getPlaceholderAt(i));
      } else if (sequence.isToken(i)) {
        converted.addToken(sequence.getTokenAt(i));
      }
    }
    return converted;
  }

  public static List<String> extractPFTokens(Data sequence) {
    List<String> pfTokens = new ArrayList<String>();
    for (int i = 0; i < sequence.size(); i++) {
      if (sequence.isPFToken(i)) {
        pfTokens.add(sequence.getPFTokenAt(i));
      }
    }
    return pfTokens;
  }
}