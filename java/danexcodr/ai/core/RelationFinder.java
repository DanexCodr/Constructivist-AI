package danexcodr.ai.core;

import static danexcodr.ai.Config.*;
import danexcodr.ai.pattern.*;
import java.util.*;

public class RelationFinder {

    List<Pattern> allPatterns;

    public RelationFinder(List<Pattern> allPatterns) {
        this.allPatterns = allPatterns;
    }

    public Set<String> findConnectors(String word) {
        Set<String> connectors = new HashSet<String>();
        for (Pattern p : allPatterns) {
            if (!(p instanceof RelationPattern)) continue;
            RelationPattern rp = (RelationPattern) p;

            if (rp.getT1().equals(word)) {
                connectors.add(rp.getT2());
            }
            if (rp.getT2().equals(word)) {
                connectors.add(rp.getT1());
            }
        }
        return connectors;
    }

    /**
     * Standard 1-hop transitive lookup
     */
    public List<List<String>> findTransitiveRelations(String term1, String term2) {
        Set<List<String>> transitiveResults = new LinkedHashSet<List<String>>();

        Set<String> directConnections = findConnectors(term1);

        for (String intermediate : directConnections) {
            RelationPattern intermediateToTarget = PatternProcessor.findRelationPattern(intermediate, term2);
            if (intermediateToTarget != null && intermediateToTarget.getFamily() != null) {
                transitiveResults.addAll(Sequence.inferFromFamily(intermediateToTarget.getFamily(), term1, term2, false)
                );
            }

            RelationPattern targetToIntermediate = PatternProcessor.findRelationPattern(term2, intermediate);
            if (targetToIntermediate != null && targetToIntermediate.getFamily() != null) {
                if (hasTransitiveEvidence(term1, intermediate, term2)) {
                    transitiveResults.addAll(
                            Sequence.inferFromFamily(targetToIntermediate.getFamily(), term1, term2, false)
                    );
                }
            }
        }

        return new ArrayList<List<String>>(transitiveResults);
    }

/**
     * NEW: Performs a Breadth-First Search to find a DEEP path between start and end terms
     * through purely Logical pattern families.
     */
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

            // Limit depth to prevent infinite loops or massive processing
            if (path.size() > TRAVERSE_DEPTH_LIMIT) continue;

            // Get all connectors of the last node
            Set<String> neighbors = findConnectors(lastNode);
            for (String neighbor : neighbors) {
                if (!visited.contains(neighbor)) {
                    // CRITICAL: Only traverse if the relationship is LOGICAL
                    RelationPattern relation = PatternProcessor.findRelationPattern(lastNode, neighbor);
                    
                    if (relation != null && relation.getFamily() != null && relation.getFamily().isLogical()) {
                        List<String> newPath = new ArrayList<String>(path);
                        newPath.add(neighbor);
                        queue.add(newPath);
                        visited.add(neighbor);
                    }
                }
            }
        }
        return null; // No path found
    }

    private boolean hasTransitiveEvidence(String term1, String intermediate, String term2) {
        RelationPattern firstHop = PatternProcessor.findRelationPattern(term1, intermediate);
        RelationPattern secondHop = PatternProcessor.findRelationPattern(intermediate, term2);

        return firstHop != null && secondHop != null &&
                firstHop.getFamily() != null && secondHop.getFamily() != null &&
                firstHop.getFamily().isLogical() && secondHop.getFamily().isLogical();
    }

    public void validateInferences(String term1, String term2, List<List<String>> generatedSequences) {
        Set<String> connectors1 = findConnectors(term1);
        Set<String> connectors2 = findConnectors(term2);

        Set<String> commonConnectors = new HashSet<String>(connectors1);
        commonConnectors.retainAll(connectors2);

        for (String common : commonConnectors) {
            RelationPattern t1ToCommon = PatternProcessor.findRelationPattern(term1, common);
            RelationPattern t2ToCommon = PatternProcessor.findRelationPattern(term2, common);

            if (t1ToCommon != null && t2ToCommon != null &&
                    PatternProcessor.findRelationPattern(term1, term2) == null &&
                    PatternProcessor.findRelationPattern(term2, term1) == null) {

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