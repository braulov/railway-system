package org.example

import java.io.BufferedReader

object Parser {
    private val ws = Regex("\\s+")

    fun parse(br: BufferedReader): Problem {
        val (sCount, tCount) = br.readLine().trim().split(ws).map { it.toInt() }

        val unloadRaw = HashMap<Int, Int>(sCount * 2)
        val loadRaw = HashMap<Int, Int>(sCount * 2)
        val graph = Graph()
        val stations = HashSet<Int>(sCount * 2)

        repeat(sCount) {
            val (sStr, uStr, lStr) = br.readLine().trim().split(ws, limit = 3)
            val s = sStr.toInt()
            unloadRaw[s] = uStr.toInt()
            loadRaw[s] = lStr.toInt()
            stations.add(s)
            graph.addVertex(s)
        }

        repeat(tCount) {
            val (u, v) = br.readLine().trim().split(ws).map { it.toInt() }
            graph.addEdge(u, v)
        }

        val start = br.readLine().trim().toInt()
        graph.addVertex(start)

        return Problem(
            stations = stations,
            unloadRaw = unloadRaw,
            loadRaw = loadRaw,
            graph = graph,
            start = start
        )
    }
}