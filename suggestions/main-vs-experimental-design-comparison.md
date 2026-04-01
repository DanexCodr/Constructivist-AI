# Main vs Experimental Design Comparison

## Branch merge status

- Base used: `origin/main` (`a372e61`)
- Merged in: `origin/experimental` (`4d6f9b5`)
- Result branch: `merged-main-experimental`
- Merge commit: `27c5300`
- Validation: project compiles and `java -cp bin danexcodr.ai.Test` passes on the merged branch.

## High-level design intent

### `main` branch design
- Stability-focused baseline with tested pattern mining and inference flow.
- Includes improved content scoring and adaptive thresholding from merged PR history.
- Emphasizes maintainability and incremental quality hardening.

### `experimental` branch design
- Broader architectural refactor and experimentation across core pattern/inference classes.
- Introduces new modeling primitives (`Data`, `SlotFlag`) and new analysis path (`GramAnalyzer`).
- Pushes deeper generalization and representational flexibility at the cost of larger change surface.

## What looked better in each branch

### Better in `main`
- Lower operational risk and clearer release readiness.
- More conservative evolution of existing behavior.
- Better baseline confidence for productionizing current logic.

### Better in `experimental`
- Stronger architectural exploration for next-gen pattern representation.
- Cleaner separation opportunities between structure/content/data roles.
- Better long-term extensibility potential in core pattern machinery.

## Must-haves to keep in the merged future state

1. **From `main`: reliability guardrails**
   - Keep the validated baseline inference/pattern pipeline behavior and regression safety.

2. **From `main`: mature scoring behavior**
   - Preserve adaptive/ensemble-style content detection improvements that improved robustness.

3. **From `experimental`: new primitives**
   - Keep and formalize `Data` and `SlotFlag` concepts where they simplify reasoning and slot typing.

4. **From `experimental`: expanded analyzers**
   - Retain `GramAnalyzer` direction and integrate it with existing analyzers only where tests confirm gains.

5. **From both: compatibility first**
   - Any deeper refactor should be gated by compile + scripted test harness validation.

## Recommended combined design direction

- Treat `main` behavior as the correctness baseline.
- Layer `experimental` abstractions in behind compatibility boundaries.
- Keep confidence-driven, data-driven pattern mining as the core philosophy.
- Enforce incremental merge policy: small slices, each slice validated by compile + `Test` harness before onward changes.
