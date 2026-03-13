package danexcodr.ai;

import java.util.*;
import danexcodr.ai.core.SymbolManager;
import danexcodr.ai.core.Symbol;

/**
 * Implements the "mental" unsupervised clustering logic with Short-Term Memory Persistence.
 */
public class UnsupervisedClusterer {

    private Main ai;
    private List<MemoryTrace> memoryTraces; 
    private Map<String, MemoryTrace> signatureToTrace;

    public UnsupervisedClusterer(Main ai) {
        this.ai = ai;
        this.memoryTraces = new ArrayList<MemoryTrace>();
        this.signatureToTrace = new HashMap<String, MemoryTrace>();
    }

    private static class MemoryTrace {
        List<List<String>> cluster;
        long createdAt;
        String signature;

        MemoryTrace(List<List<String>> cluster, String signature) {
            this.cluster = cluster;
            this.createdAt = System.currentTimeMillis();
            this.signature = signature;
        }

        double getSalience(SymbolManager symbolManager) {
            double totalFrequency = 0.0;
            Set<String> uniqueWords = new HashSet<String>();
            
            for (List<String> seq : cluster) {
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
        this.signatureToTrace.clear();
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

    private String createSignature(List<String> sentence, SymbolManager symbolManager) {
        List<String> canonical = new ArrayList<String>(sentence.size());
        for (String word : sentence) {
            canonical.add(symbolManager.getCanonical(word));
        }
        Collections.sort(canonical);
        return canonical.toString();
    }

    public void testAndAddSentence(List<String> newSentence, String originalLine) {
        // Ensure symbols exist
        for (String word : newSentence) {
            ai.getSymbolManager().ensureSymbolExists(word);
        }
        
        String signature = createSignature(newSentence, ai.getSymbolManager());
        
        if (signatureToTrace.containsKey(signature)) {
            MemoryTrace existingTrace = signatureToTrace.get(signature);
            
            for (List<String> existingSeq : existingTrace.cluster) {
                if (existingSeq.equals(newSentence)) {
                    existingTrace.createdAt = System.currentTimeMillis();
                    return;
                }
            }
            
            existingTrace.cluster.add(newSentence);
            existingTrace.createdAt = System.currentTimeMillis();
            return;
        }
        
        Set<String> newSentenceSet = new HashSet<String>();
        for(String word : newSentence) {
            newSentenceSet.add(ai.getSymbolManager().getCanonical(word));
        }
        
        MemoryTrace bestMatch = null;
        int bestDifference = Integer.MAX_VALUE;
        
        for (MemoryTrace trace : memoryTraces) {
            if (trace.cluster.isEmpty()) continue;
            
            List<String> firstSentence = trace.cluster.get(0);
            Set<String> existingSentenceSet = new HashSet<String>();
            for(String w : firstSentence) {
                existingSentenceSet.add(ai.getSymbolManager().getCanonical(w));
            }
            
            int difference = getWordDifference(newSentenceSet, existingSentenceSet);
            
            if (difference < 2 && difference < bestDifference) {
                bestDifference = difference;
                bestMatch = trace;
            }
        }
        
        if (bestMatch != null) {
            bestMatch.cluster.add(newSentence);
            bestMatch.createdAt = System.currentTimeMillis();
            signatureToTrace.put(signature, bestMatch);
            return;
        }

        List<List<String>> newCluster = new ArrayList<List<String>>();
        newCluster.add(newSentence);
        MemoryTrace newTrace = new MemoryTrace(newCluster, signature);
        memoryTraces.add(newTrace);
        signatureToTrace.put(signature, newTrace);
    }
    
    private int getWordDifference(Set<String> set1, Set<String> set2) {
        if (set1 == set2) return 0;
        
        int diff = 0;
        for (String w : set1) if (!set2.contains(w)) diff++;
        for (String w : set2) if (!set1.contains(w)) diff++;
        return diff;
    }

    public void processClusters() {
        List<ClusterSorter> sortedMatureClusters = new ArrayList<ClusterSorter>();
        List<MemoryTrace> remainingTraces = new ArrayList<MemoryTrace>();
        
        for (int i = 0; i < memoryTraces.size(); i++) {
            MemoryTrace trace = memoryTraces.get(i);
            
            if (trace.cluster.size() > 1) {
                sortedMatureClusters.add(new ClusterSorter(trace.cluster, i));
            } else {
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
        signatureToTrace.clear();
        for (MemoryTrace trace : memoryTraces) {
            if (trace.signature != null) {
                signatureToTrace.put(trace.signature, trace);
            }
        }
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
                
                if (trace.signature != null) {
                    signatureToTrace.remove(trace.signature);
                }
                it.remove(); 
            }
        }
    }
}