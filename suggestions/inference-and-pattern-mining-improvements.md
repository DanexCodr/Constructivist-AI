# Inference and Finder Pattern-Mining Improvements

This document formulates concrete improvements for the current AI inference and finder pipeline with one governing rule:

**Core philosophy: no hardcoded words.**  
All behavior should emerge from data distributions, context statistics, and learned structure.

---

## 1) Guiding Design Principles

1. **Data-driven over token-driven**
   - Never bake specific vocabulary into logic.
   - Use positional behavior, co-occurrence, context entropy, and relation confidence.

2. **Confidence everywhere**
   - Replace binary decisions (`is content`, `is structural`, `is equivalent`) with scores.
   - Inference should rank outputs by confidence, not just return first valid match.

3. **Composable finders**
   - Split detection into multiple strategies (movement, context, distribution, optionality, structural variation).
   - Aggregate with weighted consensus.

4. **Adaptive thresholds**
   - Avoid global static cutoffs where possible.
   - Calibrate thresholds from rolling corpus statistics.

---

## 2) Finder Improvements (Pattern Mining)

## 2.1 ContentFinder: move from heuristics to scoring ensemble

### Current limits
- Strong reliance on movement-based and parity fallbacks.
- Sensitive to sentence shape and ordering artifacts.

### Improvements
- Introduce a **multi-signal score** per token:
  - Position entropy
  - Left/right context diversity
  - Co-occurrence concentration
  - Stability across template slots
  - Swap contribution score
- Compute final content score as weighted sum, then threshold by percentile or calibrated confidence.
- Keep top candidates by score confidence, not fixed concept count.

## 2.2 OptionalFinder: generalize beyond single-word deletion

### Current limits
- Works mainly when sequence lengths differ by exactly one.

### Improvements
- Use generalized sequence alignment to detect optional spans.
- Track optionality conditioned by context (a token can be optional in one pattern family but mandatory in another).
- Maintain optionality confidence over time; decay stale assumptions.

## 2.3 Structural equivalence mining: confidence-based family formation

### Current limits
- Mostly position/template exactness and binary equivalence.

### Improvements
- Score equivalence edges using:
  - Shared slot distributions
  - Context profile overlap
  - Relation neighborhood similarity
- Build families from weighted graphs (cluster by confidence, not hard matches).
- Allow partial and evolving family membership with confidence cutoffs.

## 2.4 Bigram and swap analysis: expand to generalized permutation motifs

### Current limits
- Narrow patterns (small fixed forms) can be over-specialized.

### Improvements
- Detect broader reordering motifs across multiple samples.
- Score token role by contribution to transformation consistency.
- Merge reorder evidence with content scoring instead of hard branching.

---

## 3) Inference Improvements

## 3.1 Confidence-ranked relation traversal

- Treat relation graph edges as weighted by reliability and recency.
- Path confidence should combine:
  - Edge confidence
  - Path length penalty
  - Family consistency bonus
- Return ranked candidate inferences, not only one pass/fail result.

## 3.2 Multi-hop inference with controllable risk

- Replace fixed-depth traversal with confidence budget:
  - Continue traversal while accumulated uncertainty stays below threshold.
- Add cycle and redundancy penalties to avoid noisy expansions.
- Prefer short/high-confidence paths over deep weak paths.

## 3.3 Contradiction and ambiguity handling

- Maintain competing hypotheses when evidence conflicts.
- Add a consistency check layer:
  - Contradictory paths lower confidence.
  - Reinforcing independent paths increase confidence.

## 3.4 Explainable inference traces

- For each predicted relation, preserve:
  - Top path(s)
  - Edge confidences
  - Family transitions used
- This supports debugging, tuning, and trust without hardcoding semantics.

---

## 4) Architecture Suggestions

1. **Strategy interfaces for finder modules**
   - Content detection strategy
   - Optional detection strategy
   - Structural equivalence strategy
   - Path ranking strategy

2. **Shared scoring core**
   - Central scoring utilities for entropy, overlap, confidence calibration.
   - Prevent duplicated logic and inconsistent scoring behavior.

3. **Pattern memory with decay and reinforcement**
   - Confidence increases with repeated agreement.
   - Confidence decays with age and contradiction.

4. **Evaluation hooks**
   - Attach metrics collection directly to learning and inference flow.
   - Support offline replay of sentence streams for regression checks.

---

## 5) Evaluation and Success Metrics

Use measurable criteria for each change:

- **Inference precision@k** on held-out relation checks
- **Inference coverage** (how many valid relations can be inferred)
- **Contradiction rate** in discovered relations
- **Pattern stability** across repeated learning sessions
- **Finder agreement score** (cross-strategy consistency)
- **Confidence calibration quality** (high-confidence predictions should be more often correct)

---

## 6) Prioritized Roadmap

## Phase 1: High-impact foundations
- Add confidence scores to content/optional/equivalence outputs.
- Add ranked inference output based on weighted path quality.
- Add trace logging for inference explanations.

## Phase 2: Finder robustness
- Replace single heuristic decisions with ensemble scoring.
- Introduce generalized optional-span detection.
- Build weighted structural family graph.

## Phase 3: Adaptivity and long-run quality
- Implement adaptive threshold calibration from corpus statistics.
- Add contradiction-aware memory updates (reinforce/decay).
- Add regression dataset replay and metric dashboarding.

---

## 7) Non-Negotiable Constraint

All future improvements must continue enforcing:

- No hardcoded vocabulary lists
- No special-case word rules
- No domain-specific token exceptions in core logic

Generalization must come from learned structure, context, and confidence-based reasoning.
