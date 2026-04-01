package danexcodr.ai;

import danexcodr.ai.time.Timestamp;
import danexcodr.ai.core.*;
import danexcodr.ai.pattern.*;
import java.util.*;

public class Main {

  private List<PatternFamily> cachedFamilies = null;
  private Map<String, Set<String>> structuralEquivalents = new HashMap<String, Set<String>>();
  private List<Pattern> allPatterns = new ArrayList<Pattern>();
  private boolean familiesDirty = true;

  private Scanner scanner = new Scanner(System.in);
  private StructuralEquivalenceDetector equivalenceDetector;
  private PatternFamilyBuilder familyBuilder;
  private SymbolManager symbolManager;
  private PatternFamilyManager familyManager;
  private OptionalFinder optionalFinder;
  private PatternProcessor patternProcessor;
  private BigramAnalyzer bigramAnalyzer;

  public SymbolManager getSymbolManager() {
    return symbolManager;
  }

  private String formatTimestamp(long timestamp) {
    return new java.text.SimpleDateFormat(Config.TIMESTAMP_PATTERN)
        .format(new java.util.Date(timestamp));
  }

  private String summarizeConditionalContext(PatternFamily family) {
    boolean hasConditionalMarker = false;
    for (Set<String> tokenSet : family.getAliases().values()) {
      for (String token : tokenSet) {
        if ("if".equals(token) || "then".equals(token)) {
          hasConditionalMarker = true;
          break;
        }
      }
      if (hasConditionalMarker) {
        break;
      }
    }

    if (!hasConditionalMarker) {
      return null;
    }

    Map<String, Set<String>> aliasPositions = new TreeMap<>();

    for (Structure sp : family.getMemberPatterns()) {
      Data data = family.getAliasedSlots(sp);

      int idx1 = -1;
      int idx2 = -1;
      int sharedIdx = -1;
      for (int i = 0; i < data.size(); i++) {
        if (!data.isPlaceholder(i)) continue;
        SlotFlag flag = data.getPlaceholderAt(i);
        if (idx1 == -1 && flag == SlotFlag._1) {
          idx1 = i;
        }
        if (idx2 == -1 && flag == SlotFlag._2) {
          idx2 = i;
        }
        if (sharedIdx == -1 && (flag == SlotFlag._C || flag == SlotFlag._X)) {
          sharedIdx = i;
        }
      }

      if (idx1 == -1) {
        idx1 = sharedIdx;
      }
      if (idx2 == -1) {
        idx2 = sharedIdx;
      }

      if (idx1 == -1 || idx2 == -1) continue;

      for (int i = 0; i < data.size(); i++) {
        if (!data.isAlias(i)) continue;
        String alias = data.getAliasAt(i);
        Set<String> positions = aliasPositions.get(alias);
        if (positions == null) {
          positions = new LinkedHashSet<>();
          aliasPositions.put(alias, positions);
        }
        positions.add(categorizeAliasPosition(i, idx1, idx2));
      }
    }

    if (aliasPositions.isEmpty()) {
      return null;
    }

    StringBuilder sb = new StringBuilder();
    boolean first = true;
    for (Map.Entry<String, Set<String>> e : aliasPositions.entrySet()) {
      if (!first) sb.append("; ");
      sb.append(e.getKey()).append(" @ ").append(e.getValue());
      first = false;
    }
    return sb.toString();
  }

  private String categorizeAliasPosition(int aliasIndex, int idx1, int idx2) {
    if (idx1 < 0 || idx2 < 0) {
      return "unknown";
    }

    if (idx1 == idx2) {
      if (aliasIndex < idx1) {
        return "before shared slot";
      }
      if (aliasIndex > idx1) {
        return "after shared slot";
      }
      return "at shared slot";
    }

    int low = Math.min(idx1, idx2);
    int high = Math.max(idx1, idx2);

    if (aliasIndex < low) {
      return "before both [1], [2]";
    }
    if (aliasIndex > high) {
      return "after both [1], [2]";
    }
    if (aliasIndex == idx1 || aliasIndex == idx2) {
      return "at placeholder position";
    }
    if (idx1 < aliasIndex && aliasIndex < idx2) {
      return "between [1]->[2]";
    }
    if (idx2 < aliasIndex && aliasIndex < idx1) {
      return "between [2]->[1]";
    }
    return "adjacent";
  }

  public void printDiscoveredKnowledge() {
    System.out.println("\n=== Discovered Symbols (" + symbolManager.getSymbols().size() + ") ===");

    List<PatternFamily> families = familyManager.get(structuralEquivalents, familyBuilder);

    Set<String> dualTokens = new HashSet<String>();

    for (PatternFamily family : families) {
      if (family == null || family.getAliases() == null) continue;

      for (Set<String> structuralTokens : family.getAliases().values()) {
        if (structuralTokens == null) continue;

        for (String token : structuralTokens) {
          if (symbolManager.getSymbols().containsKey(token)) {
            Symbol symbol = symbolManager.getSymbols().get(token);
            symbol.relations.add("S");
            if (symbol.relations.contains("C")) {
              dualTokens.add(token);
            }
          }
        }
      }
    }

    for (Symbol symbol : symbolManager.getSymbols().values()) {
      if (symbol.relations.contains("D")) {
        dualTokens.add(symbol.token);
      }
    }

    List<Symbol> sortedSymbols = new ArrayList<Symbol>(symbolManager.getSymbols().values());
    Collections.sort(
        sortedSymbols,
        new Comparator<Symbol>() {
          @Override
          public int compare(Symbol o1, Symbol o2) {
            return o1.token.compareTo(o2.token);
          }
        });

    for (Symbol symbol : sortedSymbols) {
      List<String> leftKeys = new ArrayList<String>(symbol.leftContext.keySet());
      Collections.sort(leftKeys);
      List<String> rightKeys = new ArrayList<String>(symbol.rightContext.keySet());
      Collections.sort(rightKeys);

      List<String> relations = new ArrayList<String>(symbol.relations);
      if (dualTokens.contains(symbol.token)) {
        relations.clear();
        relations.add("D");
      } else {
        Collections.sort(relations);
      }

      System.out.println(
          "'"
              + symbol.token
              + "' - Roles: "
              + relations
              + " | Frequency: "
              + symbol.frequency
              + " | Created: "
              + formatTimestamp(symbol.getCreatedAt())
              + " | Modified: "
              + formatTimestamp(symbol.getLastModifiedAt())
              + " | Left: "
              + leftKeys
              + " | Right: "
              + rightKeys);
    }

    System.out.println("\n=== All Patterns (" + allPatterns.size() + ") ===");
    List<Pattern> sortedPatterns = new ArrayList<Pattern>(allPatterns);
    Collections.sort(
        sortedPatterns,
        new Comparator<Pattern>() {
          @Override
          public int compare(Pattern o1, Pattern o2) {
            String id1 = (o1 != null && o1.getId() != null) ? o1.getId() : "";
            String id2 = (o2 != null && o2.getId() != null) ? o2.getId() : "";
            return id1.compareTo(id2);
          }
        });

    for (Pattern pattern : sortedPatterns) {
      if (pattern == null) continue;
      Timestamp timestamped = (Timestamp) pattern;
      System.out.print(
          pattern.getId()
              + " - Frequency: "
              + pattern.getFrequency()
              + " | Created: "
              + formatTimestamp(timestamped.getCreatedAt())
              + " | Modified: "
              + formatTimestamp(timestamped.getLastModifiedAt()));

      if (pattern instanceof Content) {
        Content rp = (Content) pattern;
        System.out.print(
            " - Relation: " + rp.getT1() + (rp.isCommutative() ? " <-> " : " -> ") + rp.getT2());
        if (rp.isCommutative()) System.out.print(" [commutative]");

        String familyId = rp.getFamilyId();
        if (familyId != null) {
          PatternFamily fam = null;
          for (PatternFamily family : families) {
            if (family.getId().equals(familyId)) {
              fam = family;
              break;
            }
          }
          if (fam != null) {
            System.out.print(" (Family: " + fam.getId() + ")");
          } else {
            System.out.print(" (Family ID: " + familyId + " - not found in current families)");
          }
        }
      }
      System.out.println();
    }

    Collections.sort(
        families,
        new Comparator<PatternFamily>() {
          @Override
          public int compare(PatternFamily o1, PatternFamily o2) {
            return o1.getId().compareTo(o2.getId());
          }
        });

    System.out.println("\n=== Pattern Families (" + families.size() + ") ===");

    for (PatternFamily family : families) {
      if (family.getFrequency() > 0 && !family.getMemberPatterns().isEmpty()) {

        System.out.println("--" + family.getId());

        Map<String, Set<String>> reverseAliases =
            new TreeMap<String, Set<String>>(family.getAliases());
        for (Map.Entry<String, Set<String>> e : reverseAliases.entrySet()) {
          System.out.println(" " + e.getKey() + " | " + e.getValue());
        }

        Structure longest = null;
        for (Structure sp : family.getMemberPatterns()) {
          if (longest == null
              || sp.getData().size() > longest.getData().size()) {
            longest = sp;
          }
        }

        if (longest != null) {
          StringBuilder sb = new StringBuilder();
          sb.append("     ");
          Data data = longest.getData();

          for (int i = 0; i < data.size(); i++) {
            String display = null;
            boolean isOptional = false;

            if (data.isPlaceholder(i)) {
              display = data.getPlaceholderAt(i).toToken();
            } else if (data.isToken(i)) {
              String token = data.getTokenAt(i);
              if (family.getTokenToAlias().containsKey(token)) {
                display = family.getTokenToAlias().get(token);
              } else {
                display = token;
              }
              
              String targetAlias = family.getTokenToAlias().get(token);
              for (Structure member : family.getMemberPatterns()) {
                boolean memberHasIt = false;
                Data memberData = member.getData();
                for (int j = 0; j < memberData.size(); j++) {
                  if (memberData.isToken(j)) {
                    String memberToken = memberData.getTokenAt(j);
                    if (memberToken.equals(token)) {
                      memberHasIt = true;
                      break;
                    }
                    if (targetAlias != null && targetAlias.equals(family.getTokenToAlias().get(memberToken))) {
                      memberHasIt = true;
                      break;
                    }
                  } else if (memberData.isAlias(j)) {
                    String memberAlias = memberData.getAliasAt(j);
                    if (targetAlias != null && targetAlias.equals(memberAlias)) {
                      memberHasIt = true;
                      break;
                    }
                  }
                }
                if (!memberHasIt) {
                  isOptional = true;
                  break;
                }
              }
            } else if (data.isPFToken(i)) {
              display = "[" + data.getPFTokenAt(i) + "]";
            } else if (data.isAlias(i)) {
              display = data.getAliasAt(i);
            }

            if (display != null) {
              sb.append(display);
              if (isOptional) sb.append("?");
            }
            
            if (i < data.size() - 1) {
              sb.append(", ");
            }
          }
          System.out.println(sb.toString());
        }

        String contextualPositions = summarizeConditionalContext(family);
        if (contextualPositions != null) {
          System.out.println("     Context: " + contextualPositions);
        }

        System.out.println(
            "    Total frequency: "
                + family.getFrequency()
                + " | Created: "
                + formatTimestamp(family.getCreatedAt())
                + " | Modified: "
                + formatTimestamp(family.getLastModifiedAt()));

        List<String> familyPatternIds = new ArrayList<String>();
        for (Pattern pattern : allPatterns) {
          if (pattern instanceof Content) {
            Content rp = (Content) pattern;
            if (family.getId().equals(rp.getFamilyId())) {
              familyPatternIds.add(rp.getId());
            }
          }
        }

        if (!familyPatternIds.isEmpty()) {
          Collections.sort(
              familyPatternIds,
              new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                  int num1 = extractRPNumber(o1);
                  int num2 = extractRPNumber(o2);
                  return Integer.compare(num1, num2);
                }

                private int extractRPNumber(String rpId) {
                  if (rpId.startsWith("RP")) {
                    try {
                      return Integer.parseInt(rpId.substring(2));
                    } catch (NumberFormatException e) {
                      return 0;
                    }
                  }
                  return 0;
                }
              });

          List<String> rpNumbers = new ArrayList<String>();
          for (String id : familyPatternIds) {
            if (id.startsWith("RP")) {
              try {
                String num = id.substring(2);
                Integer.parseInt(num);
                rpNumbers.add(num);
              } catch (NumberFormatException e) {
                rpNumbers.add(id);
              }
            } else {
              rpNumbers.add(id);
            }
          }

          StringBuilder rpDisplay = new StringBuilder();
          for (int i = 0; i < rpNumbers.size(); i++) {
            rpDisplay.append(rpNumbers.get(i));
            if (i < rpNumbers.size() - 1) {
              rpDisplay.append(", ");
            }
          }
          System.out.println("    RP: " + rpDisplay.toString());
        }
      }
    }

    System.out.println("\n=== Structural Equivalents ===");
    List<String> sortedEquivKeys = new ArrayList<String>(structuralEquivalents.keySet());
    Collections.sort(sortedEquivKeys);
    boolean foundEquivalents = false;
    for (String key : sortedEquivKeys) {
      Set<String> entry = structuralEquivalents.get(key);
      if (entry != null && !entry.isEmpty()) {
        List<String> sortedSet = new ArrayList<String>(entry);
        Collections.sort(sortedSet);
        System.out.println("'" + key + "' ~ " + sortedSet);
        foundEquivalents = true;
      }
    }
    if (!foundEquivalents) System.out.println("(None discovered yet)");

    Set<String> learnedStructuralTokens = patternProcessor.getLearnedStructuralTokens();
    learnedStructuralTokens.removeAll(dualTokens);

    System.out.println("\n=== Learned Structural Tokens ===");
    if (!learnedStructuralTokens.isEmpty()) {
      List<String> sortedLearned = new ArrayList<String>(learnedStructuralTokens);
      Collections.sort(sortedLearned);
      System.out.println(sortedLearned);
    } else {
      System.out.println("(None learned yet)");
    }

    if (!dualTokens.isEmpty()) {
      List<String> sortedDual = new ArrayList<String>(dualTokens);
      Collections.sort(sortedDual);
      System.out.println("\n=== Learned Dual Tokens ===");
      System.out.println(sortedDual);
    }
  }

public Main() {
  this.symbolManager = new SymbolManager();
  this.optionalFinder = new OptionalFinder();
  this.familyBuilder = new PatternFamilyBuilder(structuralEquivalents);
  this.familyManager = new PatternFamilyManager(cachedFamilies);

  GramAnalyzer gramAnalyzer = new GramAnalyzer();

  this.patternProcessor =
      new PatternProcessor(
          symbolManager,
          familyBuilder,
          familyManager,
          optionalFinder,
          gramAnalyzer,
          structuralEquivalents,
          allPatterns,
          familiesDirty);

  this.equivalenceDetector =
      new StructuralEquivalenceDetector(
          symbolManager.getSymbols(), this.patternProcessor.getLearnedOptionalTokens());

  this.patternProcessor.setEquivalenceDetector(this.equivalenceDetector);
}

  public void buildContext(List<String> sequence) {
    symbolManager.buildContext(sequence);
  }

  public void processEquivalenceSet(List<List<String>> equivalentSequences) {
    patternProcessor.processEquivalenceSet(equivalentSequences);
  }

  public void processNewSequence() {
    System.out.print("     $   ");
    String line = scanner.nextLine().trim();
    if (line.isEmpty()) {
      System.out.println("   No sequence entered.");
      return;
    }

    String[] tokens = line.split("\\s+");
    List<String> sequence = new ArrayList<String>();
    for (String token : tokens) {
      if (!token.isEmpty()) {
        sequence.add(token);
        symbolManager.ensureSymbolExists(token);
      }
    }

    if (sequence.isEmpty()) {
      System.out.println("   Sequence was empty after splitting.");
      return;
    }

    buildContext(sequence);
    patternProcessor.reevaluateEquivalents();

    boolean matched = false;
    for (Pattern pattern : allPatterns) {
      if (pattern.matchesSequence(sequence)) {
        pattern.incrementFrequency();
        pattern.addConcreteExample(new ArrayList<String>(sequence));
        familiesDirty = true;

        if (pattern instanceof Content) {
          Content rp = (Content) pattern;
          System.out.println(
              "   Matched relation pattern: "
                  + rp.getT1()
                  + (rp.isCommutative() ? " <-> " : " -> ")
                  + rp.getT2()
                  + " ("
                  + rp.getId()
                  + ")");
        } else {
          System.out.println("   Matched unknown pattern type: (" + pattern.getId() + ")");
        }

        matched = true;
      }
    }

    List<PatternFamily> families = familyManager.get(structuralEquivalents, familyBuilder);
    for (PatternFamily family : families) {
      if (family.matchesSequence(sequence)) {
        family.incrementFrequency();
        System.out.println("   Matched pattern family: " + family.getId());
        matched = true;
      }
    }

    if (!matched) {
      System.out.println("   No known patterns matched this sequence.");
    }
  }

  private void handleGeneration() {
    System.out.println("       Enter terms:");
    System.out.print("       1.  ");
    String term1 = scanner.nextLine().trim();
    System.out.print("       2.  ");
    String term2 = scanner.nextLine().trim();

    if (term1.isEmpty() || term2.isEmpty()) {
      System.out.println("   Terms cannot be empty.");
      return;
    }

    if (!term1.startsWith("PF")) symbolManager.ensureSymbolExists(term1);
    if (!term2.startsWith("PF")) symbolManager.ensureSymbolExists(term2);

    List<PatternFamily> families = familyManager.get(structuralEquivalents, familyBuilder);

    Sequence sequence = new Sequence(allPatterns, patternProcessor);
    sequence.setCurrentFamilies(families);
    sequence.setCurrentStructuralEquivalents(structuralEquivalents);
    sequence.setFamilyManager(familyManager);
    sequence.setSymbolManager(symbolManager);

    List<List<String>> results = sequence.infer(term1, term2, true);

    if (results.isEmpty()) {
      System.out.println("   No sequences could be generated.");
    } else {
      System.out.println("   Generated " + results.size() + " unique sequences:");
      for (List<String> seq : results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seq.size(); i++) {
          sb.append(seq.get(i));
          if (i < seq.size() - 1) {
            sb.append(" ");
          }
        }
        System.out.println("     " + sb.toString());
      }
    }
  }

  private void handleAnalyze() {
    System.out.println("       Enter terms:");
    System.out.print("       1.  ");
    String term1 = scanner.nextLine().trim();
    System.out.print("       2.  ");
    String term2 = scanner.nextLine().trim();

    if (term1.isEmpty() || term2.isEmpty()) {
      System.out.println("   Terms cannot be empty.");
      return;
    }

    if (!term1.startsWith("PF")) symbolManager.ensureSymbolExists(term1);
    if (!term2.startsWith("PF")) symbolManager.ensureSymbolExists(term2);

    List<PatternFamily> families = familyManager.get(structuralEquivalents, familyBuilder);

    Sequence sequence = new Sequence(allPatterns, patternProcessor);
    sequence.setCurrentFamilies(families);
    sequence.setCurrentStructuralEquivalents(structuralEquivalents);
    sequence.setFamilyManager(familyManager);
    sequence.setSymbolManager(symbolManager);

    List<List<String>> results = sequence.infer(term1, term2, false);

    if (results.isEmpty()) {
      System.out.println("   No sequences could be generated.");
    } else {
      System.out.println("   Generated " + results.size() + " unique sequences:");
      for (List<String> seq : results) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < seq.size(); i++) {
          sb.append(seq.get(i));
          if (i < seq.size() - 1) {
            sb.append(" ");
          }
        }
        System.out.println("     " + sb.toString());
      }
    }
  }

  public void run() {
    System.out.println("Constructivist AI - Learn from equivalent sequences");
    System.out.println(Config.MAIN_COMMANDS_TEXT);

    UnsupervisedClusterer clusterer = new UnsupervisedClusterer(this);

    while (true) {
      System.out.print("\n  >   ");
      String command = scanner.nextLine().trim().toLowerCase();

      if (command.equals("q") || command.equals("quit") || command.equals("exit")) {
        break;
      } else if (command.equals("l") || command.equals("learn")) {
        handleUnifiedLearn(clusterer);
      } else if (command.equals("p") || command.equals("process")) {
        processNewSequence();
      } else if (command.equals("a") || command.equals("analyze")) {
        handleAnalyze();
      } else if (command.equals("g") || command.equals("generate")) {
        handleGeneration();
      } else if (command.equals("v") || command.equals("view")) {
        printDiscoveredKnowledge();
      } else {
        System.out.println("Unknown command: '" + command + "'");
      }
    }
    scanner.close();
    System.out.print("Exiting Inferential AI.");
  }

  private void handleUnifiedLearn(UnsupervisedClusterer clusterer) {
    System.out.println("       Enter sentences (empty line to finish):");
    while (true) {
      System.out.print("       $   ");
      String line = scanner.nextLine().trim();
      if (line.isEmpty()) {
        break;
      }

      String[] tokens = line.split("\\s+");
      List<String> sequence = new ArrayList<String>();
      for (String token : tokens) {
        if (!token.isEmpty()) {
          sequence.add(token);
        }
      }

      if (!sequence.isEmpty()) {
        clusterer.testAndAddSentence(sequence, line);
      } else {
        System.out.println("(AI: Ignoring empty line.)");
      }
    }

    clusterer.processClusters();
  }

  public boolean hasCommutativeCollapse(List<String> sequence) {
    return patternProcessor.hasCommutativeCollapse(sequence);
  }

  public static void main(String[] args) {
    Main ai = new Main();
    ai.run();
  }
}
