package org.example

import java.util.*

/** Compress cargo labels to 0..K-1. */
fun compressCargoTypes(
    unloadRaw: Map<Int, Int>,
    loadRaw: Map<Int, Int>
): Pair<List<Int>, Map<Int, Int>> {
    val all = LinkedHashSet<Int>()
    unloadRaw.values.forEach(all::add)
    loadRaw.values.forEach(all::add)

    val typeByIndex = all.toList()
    val typeIndex = HashMap<Int, Int>(typeByIndex.size * 2)
    for (i in typeByIndex.indices) typeIndex[typeByIndex[i]] = i
    return typeByIndex to typeIndex
}

/** Reachable vertices from start (BFS). */
fun computeReachable(g: Graph, start: Int): Set<Int> {
    val seen = HashSet<Int>()
    val q: ArrayDeque<Int> = ArrayDeque()
    seen.add(start)
    q.add(start)
    while (q.isNotEmpty()) {
        val v = q.removeFirst()
        for (to in g.successors(v)) {
            if (seen.add(to)) q.addLast(to)
        }
    }
    return seen
}