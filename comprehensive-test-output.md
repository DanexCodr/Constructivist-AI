# Comprehensive Test Output (Using `Main`, not `Test`)

Date: 2026-04-01
Repository: `DanexCodr/Constructivist-AI`

## Scope

Ran comprehensive interactive testing by scripting input directly into:

`java -cp bin danexcodr.ai.Main`

This intentionally avoids the `danexcodr.ai.Test` harness for primary validation.

## Build/Execution

Compilation used:

```bash
javac -d bin $(find java -name "*.java" | tr '\n' ' ')
```

`Main` was executed with multiple scripted scenarios via stdin redirection/piping.

---

## Scenario 1: Full command-path coverage (learn/view/analyze/generate/process/invalid/quit)

### Input focus
- Learn 23 mixed sentences (taxonomy, negation, commutative relation, conditionals, arithmetic forms).
- Run:
  - `v` (introspection dump)
  - `a` for multiple term pairs
  - `g` for same term pairs
  - `p` for known and unknown sequences
  - invalid command `x`
  - `q`

### Key observed output
- Startup/menu appears correctly:
  - `Commands: [l]earn, [p]rocess, [a]nalyze, [g]enerate, [v]iew, [q]uit`
- Learning succeeded:
  - `Discovered 10 Concepts.`
- View command produced populated sections:
  - `=== Discovered Symbols (22) ===`
  - `=== All Patterns (10) ===`
  - `=== Pattern Families (5) ===`
  - Structural equivalents:
    - `'a' ~ [an]`
    - `'an' ~ [a]`
- `a` generated broader output sets than `g` for equivalent prompts.
- `p` recognized known sequence:
  - `Matched relation pattern: cat -> feline (RP2)`
  - `Matched pattern family: PF3`
- `p` handled unknown sequence:
  - `No known patterns matched this sequence.`
- Invalid command handling:
  - `Unknown command: 'x'`
- Clean shutdown:
  - `Exiting Inferential AI.`

### Notable quality observations
- `a` includes grammatically inconsistent article variants, e.g.:
  - `cat is an mammal`
  - `cat is a animal`
- `g` reduces variants, but still produced:
  - `cat is a animal`
  - (did not include `cat is an animal` in this run)

---

## Scenario 2: Empty/invalid input handling

### Input focus
- `l` immediately ended with empty line.
- `v` on empty knowledge.
- `p` with empty sequence.
- `a` with empty first term.
- `g` with empty second term.
- `q`.

### Key observed output
- Learning with no data:
  - `No Concepts found.`
- Empty introspection state was clean:
  - `=== Discovered Symbols (0) ===`
  - `=== All Patterns (0) ===`
  - `=== Pattern Families (0) ===`
  - `(None discovered yet)`
  - `(None learned yet)`
- Empty process input:
  - `No sequence entered.`
- Empty analyze/generate term validation:
  - `Terms cannot be empty.`

### Result
- Input validation/error messaging is present and stable for these paths.

---

## Scenario 3: Minimal relation set / low-data behavior

### Input focus
- Learn:
  - `alpha links beta`
  - `beta links gamma`
  - `alpha links gamma`
- Then `v`, `a alpha/gamma`, `g alpha/gamma`, `q`.

### Key observed output
- Learning reported:
  - `[System] Holding 3 sequence(s) in short-term memory.`
- Symbols were registered:
  - `=== Discovered Symbols (4) ===`
- But no patterns/families formed:
  - `=== All Patterns (0) ===`
  - `=== Pattern Families (0) ===`
- Inference behavior:
  - `No sequences could be generated.` (for both `a` and `g`)

### Result
- System retains low-data inputs in memory but does not infer/generate without sufficient pattern formation.

---

## Overall findings from direct `Main` testing

1. **Core command loop works end-to-end** (`l/p/a/g/v/q` + unknown command path).
2. **Knowledge introspection output is detailed and internally consistent** after substantial learning input.
3. **`a` vs `g` behavior difference is visible**:
   - `a`: broader candidate output.
   - `g`: more filtered output.
4. **Article normalization remains imperfect in generation quality**:
   - Invalid bigrams still appear in some results (`a animal`, `an mammal` in analyze path).
5. **Empty input handling is robust** for tested command paths.
6. **Low-data scenario correctly avoids over-generating** and reports no generated sequences.

## Conclusion

Comprehensive testing using **`Main` directly (not `Test`)** is successful and reveals that command-path behavior, validation, and introspection are functioning, while generation grammar quality (article selection) still shows observable issues in output quality.

---

## Scenario 4: Comprehensive learning stress test (broader concept coverage)

### Input focus
- Learning-only stress set (41 training lines) spanning:
  - taxonomy (`cat/dog/wolf/lion` with `feline/canine/mammal/animal`)
  - commutative links (`X and Y`)
  - synonym-like symmetry (`red equals crimson`, `blue equals azure`)
  - conditional forms (`if hungry eat`, `if hungry then eat`, `if tired sleep`, `sleep if tired`)
  - arithmetic templates (`1 + 2 = 3`, `5 + 5 = 10`, reverse form)
- Then `v` and `q` to inspect learned state.

### Key observed output
- Learning discovered significantly larger structure:
  - `Discovered 18 Concepts.`
- Introspection counts:
  - `=== Discovered Symbols (32) ===`
  - `=== All Patterns (18) ===`
  - `=== Pattern Families (6) ===`
- Structural equivalents remained consistent and persistent:
  - `'a' ~ [an]`
  - `'an' ~ [a]`
- Learned structural tokens expanded to include domain-specific connectors:
  - `[3, =, a, an, and, equals, is, not, then]`
- Learned dual tokens included inferred alternatives from conditional/arithmetic sets:
  - `[+, 10, if, tired]`

### Representative learned relations
- Commutative relation families:
  - `canine <-> feline`
  - `canine <-> wolf`
  - `feline <-> lion`
  - `crimson <-> red`
  - `azure <-> blue`
- Directed relation families:
  - `cat -> feline`
  - `dog -> canine`
  - `wolf -> canine`
  - `lion -> feline`
  - `canine -> mammal`
  - `feline -> mammal`
  - `mammal -> animal`
  - `bird -> animal`
- Conditional concept behavior showed generalized structure across two condition/outcome pairs.

### Result
- Learning scales to a broader mixed corpus and yields richer pattern-family organization without runtime errors.
- Cross-domain structural-equivalent detection (`a`/`an`) remains active in larger runs.
