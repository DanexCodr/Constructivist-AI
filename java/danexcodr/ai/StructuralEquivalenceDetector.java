package danexcodr.ai;

import java.util.*;
import java.util.Map.Entry;

import danexcodr.ai.core.*;
import danexcodr.ai.pattern.*;

public class StructuralEquivalenceDetector {

  private Map<String, Symbol> symbols;
  private Set<String> allLearnedOptionals;

  public StructuralEquivalenceDetector(
      Map<String, Symbol> symbols, Set<String> allLearnedOptionals) {
    this.symbols = symbols;
    this.allLearnedOptionals = allLearnedOptionals;
  }

  public EquivalencePair createEquivalencePair(String w1, String w2) {
    return new EquivalencePair(w1, w2);
  }

  public static class EquivalencePair {
    public String w1;
    public String w2;

    EquivalencePair(String w1, String w2) {
      this.w1 = w1;
      this.w2 = w2;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;

      EquivalencePair that = (EquivalencePair) obj;

      return (w1.equals(that.w1) && w2.equals(that.w2))
          || (w1.equals(that.w2) && w2.equals(that.w1));
    }

    @Override
    public int hashCode() {
      return w1.hashCode() + w2.hashCode();
    }

    @Override
    public String toString() {
      return w1 + " ~  " + w2;
    }
  }

  private Map<String, Set<Data>> extractPatternTemplates(List<Pattern> allPatterns) {
  Map<String, Set<Data>> templates = new HashMap<String, Set<Data>>();

  for (Pattern pattern : allPatterns) {
    if (pattern instanceof Structure) {
      Structure sp = (Structure) pattern;
      Data data = sp.getData();

      Data template = new Data();
      for (int i = 0; i < data.size(); i++) {
        if (data.isPlaceholder(i)) {
          SlotFlag flag = data.getPlaceholderAt(i);
          if (flag == SlotFlag._1 || flag == SlotFlag._2) {
            template.addPlaceholder(flag);
          } else {
            template.addPlaceholder(flag);
          }
        } else if (data.isToken(i)) {
          String token = data.getTokenAt(i);
          if (allLearnedOptionals.contains(token)) {
            // Instead of adding an alias, add a special placeholder for optional
            template.addPlaceholder(SlotFlag._X); // Use [X] for optional
          } else {
            template.addToken(token);
          }
        } else if (data.isAlias(i)) {
          String alias = data.getAliasAt(i);
          if (allLearnedOptionals.contains(alias)) {
            template.addPlaceholder(SlotFlag._X);
          } else {
            template.addAlias(alias);
          }
        } else if (data.isPFToken(i)) {
          template.addPFToken(data.getPFTokenAt(i));
        }
      }

      String templateKey = template.toString() + "|" + sp.isCommutative();

      if (!templates.containsKey(templateKey)) {
        templates.put(templateKey, new HashSet<Data>());
      }
      templates.get(templateKey).add(data);
    }
  }
  return templates;
}

  private Map<String, Map<Integer, Set<String>>> analyzeTemplateSlots(
      Map<String, Set<Data>> templates) {
    Map<String, Map<Integer, Set<String>>> templateSlots =
        new HashMap<String, Map<Integer, Set<String>>>();

    for (Map.Entry<String, Set<Data>> entry : templates.entrySet()) {
      String templateKey = entry.getKey();
      Set<Data> concretePatterns = entry.getValue();

      if (concretePatterns.size() < 2) continue;

      Map<Integer, Set<String>> slotAnalysis = new HashMap<Integer, Set<String>>();

      int patternLength = getMaxPatternLength(concretePatterns);

      for (int i = 0; i < patternLength; i++) {
        Set<String> tokensAtPosition = new HashSet<String>();

        for (Data data : concretePatterns) {
          if (i < data.size()) {
            if (data.isToken(i)) {
              String token = data.getTokenAt(i);
              tokensAtPosition.add(token);
            } else if (data.isAlias(i)) {
              String alias = data.getAliasAt(i);
              tokensAtPosition.add(alias);
            }
          }
        }

        if (tokensAtPosition.size() > 1) {
          slotAnalysis.put(i, tokensAtPosition);
        }
      }

      if (!slotAnalysis.isEmpty()) {
        templateSlots.put(templateKey, slotAnalysis);
      }
    }

    return templateSlots;
  }

  private int getMaxPatternLength(Set<Data> patterns) {
    int maxLength = 0;
    for (Data data : patterns) {
      if (data.size() > maxLength) {
        maxLength = data.size();
      }
    }
    return maxLength;
  }

  private Set<EquivalencePair> detectTemplateBasedEquivalents(List<Pattern> allPatterns) {
    Set<EquivalencePair> equivalents = new HashSet<EquivalencePair>();

    Map<String, Set<Data>> templates = extractPatternTemplates(allPatterns);
    Map<String, Map<Integer, Set<String>>> templateSlots = analyzeTemplateSlots(templates);

    for (Map.Entry<String, Map<Integer, Set<String>>> templateEntry : templateSlots.entrySet()) {
      Map<Integer, Set<String>> slotAnalysis = templateEntry.getValue();

      for (Map.Entry<Integer, Set<String>> slotEntry : slotAnalysis.entrySet()) {
        Set<String> variableTokens = slotEntry.getValue();

        if (variableTokens.size() > 1) {
          List<String> tokenList = new ArrayList<String>(variableTokens);

          for (int i = 0; i < tokenList.size(); i++) {
            for (int j = i + 1; j < tokenList.size(); j++) {
              String w1 = tokenList.get(i);
              String w2 = tokenList.get(j);

              if (!areNeighbors(w1, w2)) {
                equivalents.add(new EquivalencePair(w1, w2));
              }
            }
          }
        }
      }
    }

    return equivalents;
  }

  public Set<EquivalencePair> detectStructuralEquivalents(
      List<List<String>> equivalentSequences,
      Set<String> structuralTokens,
      String t1,
      String t2,
      List<Pattern> allPatterns) {

    Set<EquivalencePair> foundEquivalents = new HashSet<EquivalencePair>();

    Set<EquivalencePair> templateEquivalents = detectTemplateBasedEquivalents(allPatterns);
    foundEquivalents.addAll(templateEquivalents);

    Set<EquivalencePair> contentBasedEquivalents =
        detectContentBasedEquivalents(equivalentSequences, structuralTokens, t1, t2, allPatterns);
    foundEquivalents.addAll(contentBasedEquivalents);

    return foundEquivalents;
  }

  private Set<EquivalencePair> detectContentBasedEquivalents(
      List<List<String>> equivalentSequences,
      Set<String> structuralTokens,
      String t1,
      String t2,
      List<Pattern> allPatterns) {

    Set<EquivalencePair> foundEquivalents = new HashSet<EquivalencePair>();

    Map<Set<String>, Set<Structure>> existingFamilies =
        groupPatternsByStructuralFamily(allPatterns);

    if (existingFamilies.isEmpty()) {
      return foundEquivalents;
    }

    Set<String> currentContentTokens = new HashSet<String>(Arrays.asList(t1, t2));

    Set<Data> currentPatternFamily = new HashSet<Data>();
    for (List<String> seq : equivalentSequences) {
      Data abstractPattern = SequenceTransformer.abstractSequence(seq, t1, t2);
      currentPatternFamily.add(abstractPattern);
    }

    Set<String> currentStructuralSignature = extractStructuralSignature(currentPatternFamily);

    for (Entry<Set<String>, Set<Structure>> familyEntry : existingFamilies.entrySet()) {
      Set<String> existingStructuralSignature = familyEntry.getKey();

      if (haveSimilarStructuralSignature(currentStructuralSignature, existingStructuralSignature)) {
        for (Data currentPattern : currentPatternFamily) {

          Data flippedCurrent = SequenceTransformer.flipTermPattern(currentPattern);
          boolean isCurrentCommutative =
              !currentPattern.equals(flippedCurrent)
                  && currentPatternFamily.contains(flippedCurrent);

          for (Structure existingPattern : familyEntry.getValue()) {

            if (existingPattern.isCommutative() != isCurrentCommutative) {
              continue;
            }

            boolean shareContext =
                shareSemanticContext(existingPattern, currentPattern, currentContentTokens);
            if (!shareContext) {
              continue;
            }

            boolean sameStructure =
                samePatternStructure(existingPattern.getData(), currentPattern);

            if (sameStructure) {
              findDifferingStructuralTokens(
                  existingPattern.getData(), currentPattern, foundEquivalents);
            }
          }
        }
      }
    }

    return foundEquivalents;
  }

  public boolean areNeighbors(String w1, String w2) {
    if (!symbols.containsKey(w1) || !symbols.containsKey(w2)) {
      return false;
    }

    Symbol s1 = symbols.get(w1);
    Symbol s2 = symbols.get(w2);

    if (s1.leftContext.containsKey(w2) || s1.rightContext.containsKey(w2)) {
      return true;
    }

    if (s2.leftContext.containsKey(w1) || s2.rightContext.containsKey(w1)) {
      return true;
    }

    return false;
  }

  public boolean reevaluateEquivalents(Map<String, Set<String>> structuralEquivalents) {
    Set<EquivalencePair> pairsToRemove = new HashSet<>();
    for (Entry<String, Set<String>> entry : structuralEquivalents.entrySet()) {
      for (String w2 : entry.getValue()) {
        if (areNeighbors(entry.getKey(), w2))
          pairsToRemove.add(createEquivalencePair(entry.getKey(), w2));
      }
    }
    boolean changed = false;
    for (EquivalencePair pair : pairsToRemove) {
      if (structuralEquivalents.containsKey(pair.w1)
          && structuralEquivalents.get(pair.w1).remove(pair.w2)) {
        if (structuralEquivalents.get(pair.w1).isEmpty()) structuralEquivalents.remove(pair.w1);
        changed = true;
      }
      if (structuralEquivalents.containsKey(pair.w2)
          && structuralEquivalents.get(pair.w2).remove(pair.w1)) {
        if (structuralEquivalents.get(pair.w2).isEmpty()) structuralEquivalents.remove(pair.w2);
        changed = true;
      }
    }
    return changed;
  }

  private Map<Set<String>, Set<Structure>> groupPatternsByStructuralFamily(
      List<Pattern> allPatterns) {
    Map<Set<String>, Set<Structure>> families = new HashMap<Set<String>, Set<Structure>>();

    for (Pattern pattern : allPatterns) {
      if (pattern instanceof Structure) {
        Structure sp = (Structure) pattern;
        Data data = sp.getData();
        Set<String> structuralSignature = new HashSet<String>();
        
        for (int i = 0; i < data.size(); i++) {
          if (data.isToken(i)) {
            structuralSignature.add(data.getTokenAt(i));
          } else if (data.isAlias(i)) {
            structuralSignature.add(data.getAliasAt(i));
          }
        }

        if (!families.containsKey(structuralSignature)) {
          families.put(structuralSignature, new HashSet<Structure>());
        }
        families.get(structuralSignature).add(sp);
      }
    }

    return families;
  }

  private Set<String> extractStructuralSignature(Set<Data> patternFamily) {
    Set<String> signature = new HashSet<String>();
    for (Data data : patternFamily) {
      for (int i = 0; i < data.size(); i++) {
        if (data.isToken(i)) {
          signature.add(data.getTokenAt(i));
        } else if (data.isAlias(i)) {
          signature.add(data.getAliasAt(i));
        }
      }
    }
    return signature;
  }

  private boolean haveSimilarStructuralSignature(Set<String> sig1, Set<String> sig2) {
    if (sig1.isEmpty() || sig2.isEmpty()) return false;
    return sig1.size() == sig2.size();
  }

  private boolean samePatternStructure(Data data1, Data data2) {
    if (data1.size() != data2.size()) {
      return false;
    }

    for (int i = 0; i < data1.size(); i++) {
      boolean data1IsPlaceholder = data1.isPlaceholder(i);
      boolean data2IsPlaceholder = data2.isPlaceholder(i);
      
      if (data1IsPlaceholder != data2IsPlaceholder) {
        return false;
      }
      
      if (data1IsPlaceholder) {
        SlotFlag flag1 = data1.getPlaceholderAt(i);
        SlotFlag flag2 = data2.getPlaceholderAt(i);
        if (flag1 != flag2) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean shareSemanticContext(
      Structure existingPattern, Data currentPattern, Set<String> currentContentTokens) {

    Set<String> existingContentTokens = new HashSet<String>();

    for (List<String> example : existingPattern.getConcreteExamples()) {
      for (String token : example) {
        if (symbols.containsKey(token)
            && (symbols.get(token).relations.contains("1")
                || symbols.get(token).relations.contains("2"))) {
          existingContentTokens.add(token);
        }
      }
    }

    for (String token : currentContentTokens) {
      if (existingContentTokens.contains(token)) {
        return true;
      }
    }

    return false;
  }

  private void findDifferingStructuralTokens(
      Data data1, Data data2, Set<EquivalencePair> foundEquivalents) {

    for (int i = 0; i < data1.size(); i++) {
      if (data1.isPlaceholder(i) || data2.isPlaceholder(i)) continue;
      
      String w1 = null;
      String w2 = null;
      
      if (data1.isToken(i)) {
        w1 = data1.getTokenAt(i);
      } else if (data1.isAlias(i)) {
        w1 = data1.getAliasAt(i);
      }
      
      if (data2.isToken(i)) {
        w2 = data2.getTokenAt(i);
      } else if (data2.isAlias(i)) {
        w2 = data2.getAliasAt(i);
      }
      
      if (w1 != null && w2 != null && !w1.equals(w2) && !areNeighbors(w1, w2)) {
        foundEquivalents.add(new EquivalencePair(w1, w2));
      }
    }
  }
}
