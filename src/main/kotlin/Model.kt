package org.example

/**
 * Raw input model.
 */
data class Problem(
    val stations: Set<Int>,
    val unloadRaw: Map<Int, Int>,
    val loadRaw: Map<Int, Int>,
    val graph: Graph,
    val start: Int
)