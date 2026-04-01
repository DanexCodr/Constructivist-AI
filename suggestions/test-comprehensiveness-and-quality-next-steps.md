# Test Comprehensiveness & Quality Next Steps

## What was checked

- End-to-end scripted run through:
  - learning (`l`)
  - view/format output (`v`)
  - inference/generation (`a`)
  - direct processing (`p`)
  - invalid command handling
  - clean exit (`q`)
- Output markers now validated by the `Test` harness itself to catch regressions in:
  - startup/command format
  - learning/pattern-family reporting
  - inference generation
  - unknown command behavior

## Issues found (current behavior)

1. **Article grammar quality in generated output**
   - Example outputs include forms like:
     - `cat is a animal`
     - `cat is an mammal`
   - Why this matters:
     - inference quality is reduced by contradictory article variants.

2. **Command menu wording mismatch**
   - Command list includes `[g]enerate`, but command handling uses `[a]nalyze` for generation behavior.
   - Why this matters:
     - UX confusion and test/readme drift risk.

3. **Timestamp format duplication (now reduced)**
   - Timestamp format literal existed in multiple classes.
   - This has been centralized in `Config` for consistency.

## Recommended fixes (prioritized)

1. **Add deterministic article-resolution pass in generation**
   - Keep structural learning as-is, but when producing final sequence text:
     - if token before noun is `a/an`, choose one best candidate using known context frequencies.
     - suppress alternate output variant once best article is selected.
   - Expected result:
     - remove contradictory forms (`a animal`, `an mammal`) while retaining learned flexibility.

2. **Align command UX text and behavior**
   - Either:
     - expose `[g]enerate` alias in parser, or
     - remove `[g]enerate` from command list.
   - Expected result:
     - no menu-command mismatch and clearer interactive usage.

3. **Extend test assertions to quality checks**
   - Keep marker-based checks (already added), then add deterministic negative checks:
     - fail if output includes known-invalid article bigrams (e.g., `a animal`, `an mammal`).
   - Gate this after article-resolution fix to avoid expected failures now.

4. **Add focused scenario sets for learning edge-cases**
   - Split current single script into named scenarios:
     - basic relation learning
     - commutative learning
     - optional-token handling
     - unknown command/process behavior
   - Expected result:
     - faster diagnosis when one area regresses.

5. **Stabilize text formatting checks**
   - Keep checking semantic markers, not full line snapshots, to avoid false failures due to benign frequency/order shifts.

## Java 7 compatibility note

- All added test/config changes use Java 7-compatible language and APIs.
- No lambdas, streams, or Java 8+ APIs were introduced.
