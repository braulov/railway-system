# Railway System -- Dataflow Analysis

This project implements a forward *may* bit-vector dataflow analysis for
a directed railway system.

Each station: unloads one cargo type on arrival, loads one cargo type
before departure

All trains start from a single station with no cargo.

The program computes, for each station, which cargo types **may be
present upon arrival**, considering all possible routes.

------------------------------------------------------------------------

## Algorithm

The solution uses a classic forward dataflow analysis:

in\[v\] = OR(out\[p\]) over all predecessors p\
out\[v\] = (in\[v\]  unload\[v\]) ∪ load\[v\]

Key implementation details:

-   BitSet-based representation for cargo sets
-   Worklist algorithm until fixed point
-   Reachability precomputation (BFS) to ignore unreachable stations
-   Optional stress tests with brute-force verification for small graphs

Time complexity:\
O((V + E) × K / word_size)

Where: V = number of stations, E = number of tracks, K = number of
cargo types

------------------------------------------------------------------------

## Project Structure

-   Graph.kt -- graph representation
-   Parser.kt -- input parsing
-   CargoDataflowPass.kt -- dataflow implementation
-   GraphGenerator.kt -- random graph generator + stress tests
-   Main.kt -- entry point

------------------------------------------------------------------------

## How to Run

### Normal execution

    ./gradlew run

### Stress tests

    ./gradlew run --args="stress"

------------------------------------------------------------------------

## Example

Input:

    3 3
    1 0 1
    2 1 2
    3 2 0
    1 2
    2 3
    3 2
    1

Output:

    1: []
    2: [0, 1]
    3: [0, 2]
