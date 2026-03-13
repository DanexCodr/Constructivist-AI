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
    Set<String> contentFromBigrams = new HashSet<String>();
    
    // Only run for exactly 2 sequences of same length
    if (sequences.size() != 2) {
        return contentFromBigrams;
    }
    
    List<String> seq1 = sequences.get(0);
    List<String> seq2 = sequences.get(1);
    
    if (seq1.size() != seq2.size()) {
        return contentFromBigrams;
    }
    
    // Check if this is a true swap (no words in same position)
    boolean isTrueSwap = true;
    for (int i = 0; i < seq1.size(); i++) {
        if (seq1.get(i).equals(seq2.get(i))) {
            isTrueSwap = false;
            break;
        }
    }
    
    if (!isTrueSwap) {
        return contentFromBigrams; // Not a swap, just optional words
    }
    
    // Check for bigram swap pattern
    if (seq1.size() % 2 != 0) {
        return contentFromBigrams; // Must be even length for bigram swap
    }
    
    int halfLength = seq1.size() / 2;
    List<String> firstBigramSeq1 = seq1.subList(0, halfLength);
    List<String> secondBigramSeq1 = seq1.subList(halfLength, seq1.size());
    List<String> firstBigramSeq2 = seq2.subList(0, halfLength);
    List<String> secondBigramSeq2 = seq2.subList(halfLength, seq2.size());
    
    // Check for swap pattern
    boolean isSwapPattern = false;
    
    if (secondBigramSeq1.equals(firstBigramSeq2) && firstBigramSeq1.equals(secondBigramSeq2)) {
        isSwapPattern = true;
    } else if (firstBigramSeq1.equals(secondBigramSeq2) && secondBigramSeq1.equals(firstBigramSeq2)) {
        isSwapPattern = true;
    }
    
    if (isSwapPattern) {
        // For true swap patterns, second word of each moving bigram is content
        // First word might be dual (both structural and content), but be conservative
        
        if (firstBigramSeq1.size() == 2) {
            String secondWord = firstBigramSeq1.get(1); // Second word is content
            if (all.contains(secondWord)) {
                contentFromBigrams.add(secondWord);
            }
        }
        
        if (secondBigramSeq1.size() == 2) {
            String secondWord = secondBigramSeq1.get(1); // Second word is content
            if (all.contains(secondWord)) {
                contentFromBigrams.add(secondWord);
            }
        }
        
        // IMPORTANT: Don't mark anything as dual here - let the main analysis decide
        // Remove the dual marking code entirely
    }
    
    return contentFromBigrams;
}
    }