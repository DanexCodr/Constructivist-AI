package danexcodr.ai.core;

import static danexcodr.ai.Config.*;
import danexcodr.ai.pattern.*;
import java.util.*;

public class RelationFinder {

    List<Pattern> allPatterns;
    List<PatternFamily> currentFamilies;
    Map<String, Set<String>> structuralEquivalents;
    private PatternProcessor patternProcessor;
    private Sequence sequence;
    private PatternFamilyManager familyManager;

    public RelationFinder(List<Pattern> allPatterns, List<PatternFamily> currentFamilies, 
                          Map<String, Set<String>> structuralEquivalents,
                          PatternProcessor patternProcessor,
                          Sequence sequence,
                          PatternFamilyManager familyManager) {
        this.allPatterns = allPatterns;
        this.currentFamilies = currentFamilies;
        this.structuralEquivalents = structuralEquivalents;
        this.patternProcessor = patternProcessor;
        this.sequence = sequence;
        this.familyManager = familyManager;
    }

    public Set<String> findConnectors(String word) {
        Set<String> connectors = new HashSet<String>();
        
        List<Content> patterns = patternProcessor.findAllContentsWithTerm(word);
        for (Content rp : patterns) {
            if (rp.getT1().equals(word)) {
                connectors.add(rp.getT2());
            } else if (rp.getT2().equals(word)) {
                connectors.add(rp.getT1());
            }
        }
        
        return connectors;
    }

    public List<String> findDeepPath(String start, String end) {
        if (start.equals(end)) return new ArrayList<String>();

        Queue<List<String>> queue = new LinkedList<List<String>>();
        Set<String> visited = new HashSet<String>();

        List<String> initialPath = new ArrayList<String>();
        initialPath.add(start);
        queue.add(initialPath);
        visited.add(start);

        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            String lastNode = path.get(path.size() - 1);

            if (lastNode.equals(end)) {
                return path;
            }

            if (path.size() > TRAVERSE_DEPTH_LIMIT) continue;

            Set<String> neighbors = findConnectors(lastNode);
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    List<Content> relations = patternProcessor.findAllContents(lastNode, neighbor);
                    boolean relationFound = !relations.isEmpty();
                    
                    if (relationFound) {
                        List<String> newPath = new ArrayList<String>(path);
                        newPath.add(neighbor);
                        queue.add(newPath);
                        visited.add(neighbor);
                    }
                }
            }
        }
        return null;
    }

    public void validateInferences(String term1, String term2, List<List<String>> generatedSequences) {
        Set<String> connectors1 = findConnectors(term1);
        Set<String> connectors2 = findConnectors(term2);

        Set<String> commonConnectors = new HashSet<String>(connectors1);
        commonConnectors.retainAll(connectors2);

        for (String common : commonConnectors) {
            List<Content> t1ToCommons = patternProcessor.findAllContents(term1, common);
            List<Content> t2ToCommons = patternProcessor.findAllContents(term2, common);

            if (!t1ToCommons.isEmpty() && !t2ToCommons.isEmpty()) {
                List<Content> directRelations = patternProcessor.findAllContents(term1, term2);
                if (directRelations.isEmpty()) {
                    directRelations = patternProcessor.findAllContents(term2, term1);
                }
                
                if (directRelations.isEmpty()) {
                    Iterator<List<String>> iterator = generatedSequences.iterator();
                    while (iterator.hasNext()) {
                        List<String> sequence = iterator.next();
                        if (impliesCommutativeRelation(sequence, term1, term2)) {
                            iterator.remove();
                        }
                    }
                }
            }
        }
    }

    private boolean impliesCommutativeRelation(List<String> sequence, String term1, String term2) {
        int pos1 = sequence.indexOf(term1);
        int pos2 = sequence.indexOf(term2);

        return pos1 != -1 && pos2 != -1 &&
                Math.abs(pos1 - pos2) == 1 &&
                hasSymmetricContext(sequence, term1, term2);
    }

    private boolean hasSymmetricContext(List<String> sequence, String term1, String term2) {
        int idx1 = sequence.indexOf(term1);
        int idx2 = sequence.indexOf(term2);

        if (idx1 > 0 && idx2 > 0) {
            String before1 = sequence.get(idx1 - 1);
            String before2 = sequence.get(idx2 - 1);
            if (before1.equals(before2)) return true;
        }

        if (idx1 < sequence.size() - 1 && idx2 < sequence.size() - 1) {
            String after1 = sequence.get(idx1 + 1);
            String after2 = sequence.get(idx2 + 1);
            if (after1.equals(after2)) return true;
        }

        return false;
    }
}