## Constructivist AI

**A symbolic research prototype for learning relational structure from sequences**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Research Prototype](https://img.shields.io/badge/Research-Prototype-yellow)]()
[![Java](https://img.shields.io/badge/Java-7%2B-blue)](https://openjdk.org/)

## What this is (currently)

Constructivist AI is a **transparent symbolic learning system**.
It learns from token sequences and builds explicit structures:

- symbols with left/right context frequencies,
- relation patterns,
- pattern families (`PF#`) with structural slots,
- structural equivalents (tokens that occupy equivalent structural roles),
- commutative/non-commutative relation behavior.

It is an experimental cognitive-architecture implementation focused on **inspectable reasoning**, not black-box prediction.

## What this is not yet

This project is **not yet**:

- a production NLP system,
- a probabilistic language model,
- robust to noisy spelling/typos/paraphrase,
- benchmarked against modern ML baselines,
- equipped with full linguistic preprocessing.

Matching is still mostly exact-token and research-oriented.

## Current command interface

The current CLI commands are:

`[l]earn, [p]rocess, [a]nalyze, [g]enerate, [v]iew, [q]uit`

### Command behavior highlights

- **`a / analyze`**: inference with broader/raw alternatives (non-normalized output path).
- **`g / generate`**: inference plus output normalization/ranking.
- Generation normalization now chooses structural equivalents via **learned context frequencies** (left/right neighbor usage), rather than hardcoded word rules.

## Architecture snapshots

### 1) Current system architecture

![Constructivist AI architecture](docs/images/architecture-current.svg)

### 2) Analyze vs Generate path

![Analyze vs Generate flow](docs/images/analyze-vs-generate.svg)

## Core capabilities

1. **Explicit pattern representation** with typed placeholders (`[1]`, `[2]`, `[C]`, `[X]`, `PF#`).
2. **Structural equivalence discovery** from usage context.
3. **Pattern family grouping** and reusable structural templates.
4. **Commutativity-aware reasoning** for relation inference.
5. **Transparent introspection** via `[v]iew` (symbols, patterns, families, relations).

## Build and run

### Prerequisites

- Java 7 or higher

### Compile

```bash
javac -d bin $(find java -name "*.java" | tr '\n' ' ')
```

### Run main CLI

```bash
java -cp bin danexcodr.ai.Main
```

### Run scripted harness

```bash
java -cp bin danexcodr.ai.Test
```

## Changelog

Project history is tracked in [CHANGELOG.md](CHANGELOG.md).

## Research context

This repository explores a constructivist approach: learning explicit structures first, then using discovered structural properties to improve future learning and generation behavior.

## License

MIT License - see [LICENSE](./LICENSE).

## Author

[DanexCodr](https://github.com/DanexCodr)
