package danexcodr.ai.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class BigramAnalyzer {

private SymbolManager symbolManager;

         public BigramAnalyzer(SymbolManager symbolManager) {
           this.symbolManager = symbolManager;
         }
        /**
         * INDEPENDENT Bigram analysis - completely separate from individual movement check
         * NOW: Only runs for EVEN-length sequences and detects specific swap patterns
         */
        public Set<String> runBigramAnalysis(List<List<String>> sequences, Set<String> all) {
            Set<String> dualFromBigrams = new HashSet<String>();
            Set<String> contentFromBigrams = new HashSet<String>();
            
            // --- NEW: Check if all sequences have EVEN length ---
            boolean allEvenLength = true;
            for (List<String> sequence : sequences) {
                if (sequence.size() % 2 != 0) {
                    allEvenLength = false;
                    break;
                }
            }
            
            // Skip bigram analysis if sequences have ODD lengths
            if (!allEvenLength || sequences.size() < 2) {
                return new HashSet<String>();
            }
            // --- END NEW CHECK ---
            
            // SIMPLIFIED APPROACH: Look for clear swap patterns between sequences
            // Pattern: Sequence1: [A, B, C, D] and Sequence2: [C, D, A, B]
            // This means bigrams [A,B] and [C,D] swap positions
            
            // We need exactly 2 sequences for a simple swap detection
            if (sequences.size() != 2) {
                return new HashSet<String>();
            }
            
            List<String> seq1 = sequences.get(0);
            List<String> seq2 = sequences.get(1);
            
            // Check if this is a clear swap pattern: first half and second half swapped
            int halfLength = seq1.size() / 2;
            
            // Extract the two potential bigram pairs from sequence 1
            List<String> firstBigramSeq1 = seq1.subList(0, halfLength);
            List<String> secondBigramSeq1 = seq1.subList(halfLength, seq1.size());
            
            // Extract the two potential bigram pairs from sequence 2  
            List<String> firstBigramSeq2 = seq2.subList(0, halfLength);
            List<String> secondBigramSeq2 = seq2.subList(halfLength, seq2.size());
            
            // Check for swap pattern: [firstBigramSeq1, secondBigramSeq1] vs [secondBigramSeq1, firstBigramSeq1]
            // OR [firstBigramSeq1, secondBigramSeq1] vs [secondBigramSeq2, firstBigramSeq2] where the content matches
            
            boolean isSwapPattern = false;
            
            // Pattern 1: Direct swap - second half of seq1 becomes first half of seq2
            if (secondBigramSeq1.equals(firstBigramSeq2) && firstBigramSeq1.equals(secondBigramSeq2)) {
                isSwapPattern = true;
            }
            // Pattern 2: Reverse swap - first half of seq1 becomes second half of seq2  
            else if (firstBigramSeq1.equals(secondBigramSeq2) && secondBigramSeq1.equals(firstBigramSeq2)) {
                isSwapPattern = true;
            }
            
            if (isSwapPattern) {
                // For your example: 
                // seq1: ["before", "eating", "wash", "hands"] -> bigrams: ["before","eating"] and ["wash","hands"]
                // seq2: ["wash", "hands", "before", "eating"] -> bigrams: ["wash","hands"] and ["before","eating"]
                
                // In swap patterns, the first of each moving bigram is typically dual (both a structural and a content))
                // and the second word is typically just content
                
                // Analyze first bigram from sequence 1
                if (firstBigramSeq1.size() == 2) {
                    String W1 = firstBigramSeq1.get(0); // "before" - this should be dual
                    String W2 = firstBigramSeq1.get(1); // "eating" - this should be content
                    
                    if (all.contains(W1) && all.contains(W2)) {
                        dualFromBigrams.add(W1);
                        contentFromBigrams.add(W2);
                    }
                }
                
                // Analyze second bigram from sequence 1  
                if (secondBigramSeq1.size() == 2) {
                    String W1 = secondBigramSeq1.get(0); // "wash" - this should be dual
                    String W2 = secondBigramSeq1.get(1); // "hands" - this should be content
                    
                    if (all.contains(W1) && all.contains(W2)) {
                        dualFromBigrams.add(W1);
                        contentFromBigrams.add(W2);
                    }
                }
                
                // Mark dual words in symbols
                for (String dual : dualFromBigrams) {
                    if (symbolManager.getSymbols().containsKey(dual)) {
                        symbolManager.getSymbols().get(dual).relations.add("D");
                    }
                }
                
                return new HashSet<String>(contentFromBigrams);
            }
            
            return new HashSet<String>(); // Return empty if no clear bigram patterns found
        }
    }