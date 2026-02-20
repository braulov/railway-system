package org.example

import java.util.*

/**
 * Forward may bit-vector dataflow pass:
 *
 *   in[v]  = OR(out[p]) over predecessors p
 *   out[v] = (in[v] \ {unload[v]}) ∪ {load[v]}
 */
class CargoDataflowPassSimple(private val problem: Problem) {

    data class Result(
        val inAtStation: Map<Int, BitSet>,   // station id -> BitSet over compressed cargo ids
        val typeByIndex: List<Int>           // compressed id -> original cargo label
    )

    fun run(): Result {
        val (typeByIndex, typeIndex) = compressCargoTypes(problem.unloadRaw, problem.loadRaw)
        val k = typeByIndex.size

        val unload = HashMap<Int, Int>(problem.unloadRaw.size * 2)
        val load = HashMap<Int, Int>(problem.loadRaw.size * 2)
        for (s in problem.unloadRaw.keys) {
            unload[s] = typeIndex.getValue(problem.unloadRaw.getValue(s))
            load[s] = typeIndex.getValue(problem.loadRaw.getValue(s))
        }

        val reachable = computeReachable(problem.graph, problem.start)

        val inMap = HashMap<Int, BitSet>(reachable.size * 2)
        val outMap = HashMap<Int, BitSet>(reachable.size * 2)
        for (v in reachable) {
            inMap[v] = BitSet(k)
            outMap[v] = BitSet(k)
        }

        val q: ArrayDeque<Int> = ArrayDeque()
        val inQueue = HashSet<Int>(reachable.size * 2)

        fun push(x: Int) {
            if (inQueue.add(x)) q.addLast(x)
        }

        val tmpIn = BitSet(k)
        val tmpOut = BitSet(k)

        push(problem.start)

        while (q.isNotEmpty()) {
            val v = q.removeFirst()
            inQueue.remove(v)

            tmpIn.clear()
            for (p in problem.graph.predecessors(v)) {
                outMap[p]?.let { tmpIn.or(it) }
            }

            tmpOut.clear()
            tmpOut.or(tmpIn)
            unload[v]?.let { tmpOut.clear(it) }
            load[v]?.let { tmpOut.set(it) }

            val inV = inMap.getValue(v)
            val outV = outMap.getValue(v)

            val changed = !tmpIn.equals(inV) || !tmpOut.equals(outV)
            if (changed) {
                inV.clear(); inV.or(tmpIn)
                outV.clear(); outV.or(tmpOut)
                for (to in problem.graph.successors(v)) push(to)
            }
        }

        return Result(inAtStation = inMap, typeByIndex = typeByIndex)
    }
}