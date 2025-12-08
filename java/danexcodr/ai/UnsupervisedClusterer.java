package danexcodr.ai;

import java.util.*;
import danexcodr.ai.core.SymbolManager;
import danexcodr.ai.core.Symbol;

/**
 * Implements the "mental" unsupervised clustering logic with Short-Term Memory Persistence.
 * Now supports input normalization via SymbolManager.
 */
public class UnsupervisedClusterer {

    private Main ai;
    private List<MemoryTrace> memoryTraces; 

    public UnsupervisedClusterer(Main ai) {
        this.ai = ai;
        this.memoryTraces = new ArrayList<MemoryTrace>();
    }

    private static class MemoryTrace {
        List<List<String>> cluster;
        long createdAt;

        MemoryTrace(List<List<String>> cluster) {
            this.cluster = cluster;
            this.createdAt = System.currentTimeMillis();
        }

        double getSalience(SymbolManager symbolManager) {
            double totalFrequency = 0.0;
            Set<String> uniqueWords = new HashSet<String>();
            
            for (List<String> seq : cluster) {
                // Use canonical forms for salience check
                for(String w : seq) {
                    uniqueWords.add(symbolManager.getCanonical(w));
                }
            }
            
            for (String word : uniqueWords) {
                if (symbolManager.getSymbols().containsKey(word)) {
                    Symbol s = symbolManager.getSymbols().get(word);
                    totalFrequency += s.frequency;
                }
            }

            long ageInMillis = System.currentTimeMillis() - createdAt;
            double ageInMinutes = ageInMillis / (1000.0 * 60.0); 

            return totalFrequency / (ageInMinutes + 1.0);
        }
    }

    public void resetClusters() {
        this.memoryTraces.clear();
    }

    private static class ClusterSorter implements Comparable<ClusterSorter> {
        List<List<String>> cluster;
        double averageLength;
        int originalIndex;

        ClusterSorter(List<List<String>> cluster, int originalIndex) {
            this.cluster = cluster;
            this.originalIndex = originalIndex;

            if (cluster == null || cluster.isEmpty()) {
                this.averageLength = 0;
            } else {
                double totalLength = 0;
                for (List<String> sentence : cluster) {
                    if (sentence != null) {
                        totalLength += sentence.size();
                    }
                }
                this.averageLength = totalLength / cluster.size();
            }
        }

        @Override
        public int compareTo(ClusterSorter other) {
            int avgCompare = Double.compare(this.averageLength, other.averageLength);
            if (avgCompare != 0) {
                return avgCompare; 
            }
            return Integer.valueOf(this.originalIndex).compareTo(other.originalIndex);
        }
    }

    public void testAndAddSentence(List<String> newSentence, String originalLine) {
        // 1. Ensure symbols exist (Pre-registers aliases if known)
        for (String word : newSentence) {
            ai.getSymbolManager().ensureSymbolExists(word);
        }
        
        // 2. Normalize the sentence for comparison
        List<String> normalizedSentence = new ArrayList<String>();
        for(String word : newSentence) {
            normalizedSentence.add(ai.getSymbolManager().getCanonical(word));
        }
        
        Set<String> newSentenceSet = new HashSet<String>(normalizedSentence);

        // 3. Loop through all existing MemoryTraces
        for (MemoryTrace trace : memoryTraces) {
            List<List<String>> cluster = trace.cluster;
            
            for (List<String> existingSentence : cluster) {
                // Normalize existing sentence for comparison
                Set<String> existingSentenceSet = new HashSet<String>();
                for(String w : existingSentence) {
                    existingSentenceSet.add(ai.getSymbolManager().getCanonical(w));
                }
                
                int difference = getWordDifference(newSentenceSet, existingSentenceSet);

                if (difference < 2) {
                    // Add the original raw sentence to preserve input fidelity, 
                    // but clustering was based on normalized form.
                    cluster.add(newSentence);
                    trace.createdAt = System.currentTimeMillis();
                    return; 
                }
            }
        }

        List<List<String>> newCluster = new ArrayList<List<String>>();
        newCluster.add(newSentence);
        memoryTraces.add(new MemoryTrace(newCluster));
    }
    
    private int getWordDifference(Set<String> set1, Set<String> set2) {
        Set<String> diff1 = new HashSet<String>(set1);
        diff1.removeAll(set2);
        
        Set<String> diff2 = new HashSet<String>(set2);
        diff2.removeAll(set1);
        
        return diff1.size() + diff2.size();
    }

    public void processClusters() {
        List<ClusterSorter> sortedMatureClusters = new ArrayList<ClusterSorter>();
        List<MemoryTrace> remainingTraces = new ArrayList<MemoryTrace>();
        
        for (int i = 0; i < memoryTraces.size(); i++) {
            MemoryTrace trace = memoryTraces.get(i);
            
            if (trace.cluster.size() > 1) {
                sortedMatureClusters.add(new ClusterSorter(trace.cluster, i));
            } else {
                // NEW: Single-Shot Learning Trigger
                // If a single sequence matches a known Commutative Pattern Family,
                // we treat it as a mature concept because the commutativity implies variation.
                List<String> singleSeq = trace.cluster.get(0);
                if (ai.hasCommutativeCollapse(singleSeq)) {
                     System.out.println("   [System: Single-shot learning triggered by Commutative Family match]");
                     sortedMatureClusters.add(new ClusterSorter(trace.cluster, i));
                } else {
                     remainingTraces.add(trace);
                }
            }
        }

        Collections.sort(sortedMatureClusters);
        
        int matureClusters = sortedMatureClusters.size();
        
        if (matureClusters > 0) {
            System.out.println("   Discovered " + matureClusters + " Concepts.");
        } 
        
        if (!remainingTraces.isEmpty()) {
            System.out.println("   [System] Holding " + remainingTraces.size() + " sequence(s) in short-term memory.");
        } else if (matureClusters == 0) {
            System.out.println("   No Concepts found.");
        }
        
        int clusterCount = 1;
        for (ClusterSorter sorter : sortedMatureClusters) {
            List<List<String>> cluster = sorter.cluster;
           
            System.out.println("   --- Found Concept " + (clusterCount++) + 
                               " (Size: " + cluster.size() + 
                               ", Avg. Len: " + String.format("%.2f", sorter.averageLength) + ") ---");
            
            for (List<String> seq : cluster) {
                ai.buildContext(seq);
            }
            
            ai.processEquivalenceSet(cluster);
        }

        this.memoryTraces = remainingTraces;
        performGarbageCollection();
    }

    private void performGarbageCollection() {
        double FORGETTING_THRESHOLD = 0.5;

        Iterator<MemoryTrace> it = memoryTraces.iterator();
        while (it.hasNext()) {
            MemoryTrace trace = it.next();
            if (trace.cluster.size() > 1) continue;

            double salience = trace.getSalience(ai.getSymbolManager());
            
            if (salience < FORGETTING_THRESHOLD) {
                List<String> seq = trace.cluster.get(0);
                String preview = (seq.size() > 3) ? seq.get(0) + "..." : seq.toString();
                System.out.println("   [Memory] Fading weak trace: " + preview + " (Score: " + String.format("%.2f", salience) + ")");
                it.remove(); 
            }
        }
    }
}