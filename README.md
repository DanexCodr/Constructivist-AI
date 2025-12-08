## Constructivist AI

**A cognitive architecture for self-improving pattern learning**

[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-7%2B-blue)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Version-0.1.0--alpha-orange)]()

## Overview

Constructivist AI is a research implementation of a cognitive architecture that learns structured patterns from sequential data and uses discovered pattern properties to accelerate its own learning process. Unlike statistical machine learning approaches, this system builds explicit, interpretable representations of patterns and their structural properties.

## Core Principles

The architecture is based on constructivist learning theory, where:

- Knowledge is actively constructed rather than passively received

- Learning involves building and refining cognitive structures  

- New learning builds upon and integrates with existing structures

- The learning process itself can become more efficient over time

## Architecture Components

### Symbolic Foundation

- **Symbol Manager**: Maintains canonical representations with alias resolution

- **Context Tracking**: Records left/right neighbor frequencies for each symbol

### Pattern Learning  

- **Pattern Processor**: Extracts abstract patterns from concrete sequences

- **Unsupervised Clusterer**: Groups similar sequences for concept formation

- **Optionality Detection**: Identifies optional elements in patterns

### Structural Analysis

- **Structural Equivalence Detector**: Finds words that occupy equivalent structural positions

- **Pattern Family Builder**: Groups
patterns with shared structural vocabulary

- **Property Discovery**: Identifies pattern properties like commutativity

### Inference & Generation

- **Relation Finder**: Discovers transitive and logical relationships

- **Sequence Generator**: Produces valid sequences from learned patterns

## Key Capabilities

### 1. Explicit Pattern Representation
Patterns are represented symbolically with typed slots:
- `[1]`, `[2]` for directed relations
- `[C]` for commutative relations
- `[X]` for variable fillers in composite patterns
- `[PF#]` for nested pattern families

### 2. Property-Based Learning Acceleration
Once the system discovers a pattern property (e.g., commutativity), it can use that property to learn new patterns more efficiently.

### 3. Hierarchical Composition
Patterns can nest to form hierarchies, enabling complex structures from simple components.

### 4. Transparent Reasoning
All learned patterns and inferences are explicitly represented and examinable.

## Example Learning Session

```

Inferential AI - Learn from equivalent sequences
Commands:[l]earn, [p]rocess, [i]nfer, [v]iew, [q]uit

l
Enter sentences (empty line to finish):
$ cat is a feline
$ cat is feline
$
Discovered 1 Concepts.
--- Found Concept 1 (Size: 2, Avg. Len: 3.50) ---
Core relation: cat -> feline
Structurals: [a, is]
Optionals: [a]

l
Enter sentences (empty line to finish):
$ cat and dog
$ dog and cat
$
Discovered 1 Concepts.
--- Found Concept 1 (Size: 2, Avg. Len: 3.00) ---
Core relation: cat <-> dog
Structurals: [and]
Optionals: [a]

l
Enter sentences (empty line to finish):
$ cat and dog are mammals
$
[System: Single-shot learning triggered by Commutative Family match]
Discovered 1 Concepts.
--- Found Concept 1 (Size: 1, Avg. Len: 5.00) ---
[System: Sequence collapsed for higher-level analysis: [PF2, are, mammals]]
Core relation: PF2 -> mammals
Structurals: [are]
Optionals: [a]

```

## Technical Implementation

### Pattern Storage
```java
// Structural patterns capture word order with typed slots
StructuralPattern: [C] and [C] (commutative)

// Relation patterns capture directed relationships  
RelationPattern: cat -> feline

// Composite patterns nest pattern families
CompositePattern: [PF2] are [X]
```

## Learning Acceleration Logic

When a new sequence matches a known commutative pattern family, the system treats it as a mature concept immediately, recognizing that the commutative property implies variation.

## Building and Running

### Prerequisites

· Java 7 or higher

### Compilation

```bash
javac -d bin src/danexcodr/ai/**/*.java src/danexcodr/ai/core/*.java src/danexcodr/ai/pattern/*.java
```

### Execution

```bash
java -cp bin danexcodr.ai.Main
```

## Research Context

This work explores an alternative to both classical symbolic AI and modern statistical approaches. It demonstrates how a system can discover structural properties from data and use those properties to improve its own learning efficiency—an approach inspired by constructivist learning theory in cognitive science.

## Limitations and Future Work

Current limitations include:

· Brittle matching (exact token matching required)

· Limited to sequential, symbolic input

· Early research prototype stage

## Planned extensions:

· Probabilistic pattern matching

· Extended property discovery (associativity, transitivity)

· Natural language preprocessing interface

## Citation

If you use this software in academic work, please cite it as a research prototype exploring constructivist learning in artificial cognitive systems.

## License

MIT License - see [LICENSE](./LICENSE) file for details.

## Author

[DanexCodr](https://github.com/DanexCodr) - Exploring cognitive architectures and constantly learning.
