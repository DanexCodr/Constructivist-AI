package danexcodr.ai;

import danexcodr.ai.time.Timestamp;

import danexcodr.ai.core.*;
import danexcodr.ai.core.SymbolManager;
import danexcodr.ai.pattern.*;
import java.util.*;

/**
 * Constructivist AI - Learns logical patterns and relationships from sequences. This version uses a
 * unified learning command ([l]earn) that incorporates unsupervised "mental" clustering. All code
 * is compatible with Java 7.
 */
public class Main {

  // --- Core Data Structures ---

  private List<PatternFamily> cachedFamilies = null;
  private Map<String, Set<String>> structuralEquivalents =
      new HashMap<String, Set<String>>();

  private List<Pattern> allPatterns = new ArrayList<Pattern>(); // Now only RelationPatterns
  private boolean familiesDirty = true;

  // --- Helper Modules ---
  private Scanner scanner = new Scanner(System.in);
  private StructuralEquivalenceDetector equivalenceDetector;
  private PatternFamilyBuilder familyBuilder; // Initialize on-demand

  private SymbolManager symbolManager;
  private PatternFamilyManager familyManager;
  private OptionalFinder optionalFinder;
  private PatternProcessor patternProcessor;
  private BigramAnalyzer bigramAnalyzer;
  private ContentFinder contentFinder;

  public SymbolManager getSymbolManager() {
    return symbolManager;
  }

  // Helper method to format timestamps
  private String formatTimestamp(long timestamp) {
    return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
  }

public void printDiscoveredKnowledge() {
    System.out.println("\n=== Discovered Symbols (" + symbolManager.getSymbols().size() + ") ===");

    List<PatternFamily> families =
        familyManager.getPatternFamilies(structuralEquivalents, familyBuilder);

    Set<String> dualWords = new HashSet<String>();

    for (PatternFamily family : families) {
      if (family == null || family.getAliases() == null) continue;

      for (Set<String> structuralWords : family.getAliases().values()) {
        if (structuralWords == null) continue;

        for (String word : structuralWords) {
          if (symbolManager.getSymbols().containsKey(word)) {
            Symbol symbol = symbolManager.getSymbols().get(word);

            symbol.relations.add("S");

            if (symbol.relations.contains("C")) {
              dualWords.add(word);
            }
          }
        }
      }
    }

    // Include dual words identified by bigram analysis
    for (Symbol symbol : symbolManager.getSymbols().values()) {
      if (symbol.relations.contains("D")) {
        dualWords.add(symbol.token);
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
      if (dualWords.contains(symbol.token)) {
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
              + " | Created: " + formatTimestamp(symbol.getCreatedAt())
              + " | Modified: " + formatTimestamp(symbol.getLastModifiedAt())
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
      System.out.print(pattern.getId() + " - Frequency: " + pattern.getFrequency()
          + " | Created: " + formatTimestamp(timestamped.getCreatedAt())
          + " | Modified: " + formatTimestamp(timestamped.getLastModifiedAt()));

      if (pattern instanceof RelationPattern) {
        RelationPattern rp = (RelationPattern) pattern;
        System.out.print(
            " - Relation: " + rp.getT1() + (rp.isCommutative() ? " <-> " : " -> ") + rp.getT2());
        if (rp.isCommutative()) System.out.print(" [commutative]");
        
        // Get family by ID
        String familyId = rp.getFamilyId();
        if (familyId != null) {
            // Try to find the family in current families
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

    System.out.println(
        "\n=== Pattern Families (" + families.size() + ") ===");

    for (PatternFamily family : families) {
      if (family.getFrequency() > 0 && !family.getMemberPatterns().isEmpty()) {
        
        // --- Custom Family Printing Logic ---
        System.out.println("--" + family.getId());
        
        // 1. Print Aliases
        Map<String, Set<String>> reverseAliases = new TreeMap<String, Set<String>>(family.getAliases());
        for(Map.Entry<String, Set<String>> e : reverseAliases.entrySet()) {
             System.out.println(" " + e.getKey() + " | " + e.getValue());
        }

        // 2. Reconstruct Generalized Pattern from Longest Member
        StructuralPattern longest = null;
        for(StructuralPattern sp : family.getMemberPatterns()) {
             if(longest == null || sp.getStructuralSlots().size() > longest.getStructuralSlots().size()) {
                 longest = sp;
             }
        }
        
        if (longest != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("     ");
            List<String> slots = longest.getStructuralSlots();
            
            for(int i=0; i<slots.size(); i++) {
                String token = slots.get(i);
                String display = token;
                
                // Substitute with Alias if applicable
                if (family.getWordToAlias().containsKey(token)) {
                    display = family.getWordToAlias().get(token);
                } else if (token.startsWith("PF")) { 
                    // Box PF tokens purely for display
                    display = "[" + token + "]";
                }
                
                // Determine Optionality
                boolean isOptional = false;
                boolean isSpecial = token.equals("[1]") || token.equals("[2]") || 
                                    token.equals("[C]") || token.equals("[X]") || 
                                    token.startsWith("PF");
                
                if (!isSpecial) {
                     String targetAlias = family.getWordToAlias().get(token);
                     
                     for(StructuralPattern member : family.getMemberPatterns()) {
                         boolean memberHasIt = false;
                         for (String memberToken : member.getStructuralSlots()) {
                             if (memberToken.equals(token)) {
                                 memberHasIt = true; 
                                 break;
                             }
                             if (targetAlias != null && targetAlias.equals(family.getWordToAlias().get(memberToken))) {
                                 memberHasIt = true;
                                 break;
                             }
                         }
                         if (!memberHasIt) {
                             isOptional = true;
                             break;
                         }
                     }
                }
                
                sb.append(display);
                if(isOptional) sb.append("?");
                if(i < slots.size()-1) sb.append(", ");
            }
            System.out.println(sb.toString());
        }
        
        System.out.println("    Total frequency: " + family.getFrequency()
            + " | Created: " + formatTimestamp(family.getCreatedAt())
            + " | Modified: " + formatTimestamp(family.getLastModifiedAt()));
            
        // Show which RelationPatterns belong to this family
        List<RelationPattern> familyPatterns = new ArrayList<RelationPattern>();
        for (Pattern pattern : allPatterns) {
            if (pattern instanceof RelationPattern) {
                RelationPattern rp = (RelationPattern) pattern;
                if (family.getId().equals(rp.getFamilyId())) {
                    familyPatterns.add(rp);
                }
            }
        }
        
        if (!familyPatterns.isEmpty()) {
            System.out.println("    Contains relations:");
            for (RelationPattern rp : familyPatterns) {
                System.out.println("      " + rp.getT1() + (rp.isCommutative() ? " <-> " : " -> ") + 
                                 rp.getT2() + " (" + rp.getId() + ")");
            }
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

    Set<String> learnedStructuralWords = patternProcessor.getLearnedStructuralWords();
    learnedStructuralWords.removeAll(dualWords);

    System.out.println("\n=== Learned Structural Words ===");
    if (!learnedStructuralWords.isEmpty()) {
      List<String> sortedLearned = new ArrayList<String>(learnedStructuralWords);
      Collections.sort(sortedLearned);
      System.out.println(sortedLearned);
    } else {
      System.out.println("(None learned yet)");
    }

    if (!dualWords.isEmpty()) {
      List<String> sortedDual = new ArrayList<String>(dualWords);
      Collections.sort(sortedDual);
      System.out.println("\n=== Learned Dual Words ===");
      System.out.println(sortedDual);
    }
  }

public Main() {
    this.symbolManager = new SymbolManager();
    
    this.optionalFinder = new OptionalFinder();
    this.familyBuilder = new PatternFamilyBuilder(structuralEquivalents);
    this.familyManager = new PatternFamilyManager(allPatterns, cachedFamilies);

    this.bigramAnalyzer = new BigramAnalyzer(symbolManager);

    this.patternProcessor =
        new PatternProcessor(
            symbolManager,
            familyBuilder,
            familyManager,
            optionalFinder,
            bigramAnalyzer,
            structuralEquivalents,
            allPatterns,
            familiesDirty);
    
    this.equivalenceDetector = 
        new StructuralEquivalenceDetector(
            symbolManager.getSymbols(), 
            this.patternProcessor.getLearnedOptionalWords()
        );

    this.patternProcessor.setEquivalenceDetector(this.equivalenceDetector);
  }

  public void buildContext(List<String> sequence) {
    symbolManager.buildContext(sequence);
  }

  public Set<String> identifyContentWords(
      List<List<String>> sequences, Set<String> learnedStructuralWords) {
    return contentFinder.identifyContentWords(sequences, learnedStructuralWords);
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

    String[] words = line.split("\\s+");
    List<String> sequence = new ArrayList<String>();
    for (String word : words) {
      if (!word.isEmpty()) {
        sequence.add(word);
        symbolManager.ensureSymbolExists(word);
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

        if (pattern instanceof RelationPattern) {
          RelationPattern rp = (RelationPattern) pattern;
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

    // CHANGED: Also check pattern families
    List<PatternFamily> families = familyManager.getPatternFamilies(structuralEquivalents, familyBuilder);
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

    List<PatternFamily> families = familyManager.getPatternFamilies(
        structuralEquivalents, familyBuilder);
    
    Sequence sequence = new Sequence(allPatterns, patternProcessor);
    sequence.setCurrentFamilies(families);
    sequence.setCurrentStructuralEquivalents(structuralEquivalents);
    sequence.setFamilyManager(familyManager);
    
    List<List<String>> results = sequence.infer(term1, term2);

    if (results.isEmpty()) {
        System.out.println("   No sequences could be generated.");
    } else {
        System.out.println("   Generated " + results.size() + " unique sequences:");
        Collections.sort(
            results,
            new Comparator<List<String>>() {
                @Override
                public int compare(List<String> o1, List<String> o2) {
                    return o1.toString().compareTo(o2.toString());
                }
            });
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
    System.out.println("Commands: [l]earn, [p]rocess, [g]enerate, [v]iew, [q]uit");

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

      String[] words = line.split("\\s+");
      List<String> sequence = new ArrayList<String>();
      for (String word : words) {
        if (!word.isEmpty()) {
          sequence.add(word);
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