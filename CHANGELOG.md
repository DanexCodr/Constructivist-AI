# Changelog

All notable changes to this project are documented in this file.

## [Unreleased]

### Added
- Added architecture and command-behavior documentation images under `docs/images/` generated via Python.
- Added explicit README sections for current scope and non-goals.

### Changed
- Updated README to reflect current command set (`l`, `p`, `a`, `g`, `v`, `q`) and current behavior.
- Clarified `analyze` vs `generate` behavior and output semantics.

## [0.2.0] - 2026-04-01

### Changed
- `generate` normalization now uses generic structural-equivalent selection based on learned left/right context frequencies.
- Removed article-specific hardcoded normalization heuristics from generation output.
- Kept command split: `analyze` returns broader/non-normalized alternatives; `generate` returns normalized output.

### Notes
- This is still a research prototype with deterministic symbolic inference and exact-token matching assumptions.

## [0.1.0-alpha] - Initial public prototype

### Added
- Symbol-centric context tracking (left/right co-occurrence maps).
- Pattern extraction and pattern family grouping.
- Structural-equivalence discovery from learned usage.
- Relation inference, transitive reasoning paths, and sequence generation.
- Interactive CLI loop with learning, processing, generation, analysis, and view commands.

### Limitations from start
- Exact-token brittleness and no probabilistic confidence model.
- No external NLP preprocessing pipeline.
- No benchmark suite or production deployment target.
