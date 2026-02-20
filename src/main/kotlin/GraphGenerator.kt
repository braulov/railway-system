package org.example

import java.util.*

interface GraphGenerator {
    fun generate(n: Int, m: Int, rnd: Random): Graph
}

class RandomGraphGenerator(
    private val allowSelfLoops: Boolean = true,
    private val allowMultiEdges: Boolean = true
) : GraphGenerator {

    private fun edgeKey(u: Int, v: Int): Long =
        (u.toLong() shl 32) or (v.toLong() and 0xffffffffL)

    override fun generate(n: Int, m: Int, rnd: Random): Graph {
        val graph = Graph()
        for (i in 1..n) graph.addVertex(i)

        if (allowMultiEdges) {
            repeat(m) {
                var u: Int
                var v: Int
                while (true) {
                    u = rnd.nextInt(n) + 1
                    v = rnd.nextInt(n) + 1
                    if (!allowSelfLoops && u == v) continue
                    break
                }
                graph.addEdge(u, v)
            }
            return graph
        }

        val used = HashSet<Long>(m * 2)
        var added = 0
        while (added < m) {
            val u = rnd.nextInt(n) + 1
            val v = rnd.nextInt(n) + 1
            if (!allowSelfLoops && u == v) continue
            if (used.add(edgeKey(u, v))) {
                graph.addEdge(u, v)
                added++
            }
        }

        return graph
    }
}

object Stress {
    private val rnd = Random(1L)

    fun runAll() {
        println("Running random correctness tests...")
        randomCorrectnessTests(iterations = 1500)
        println("Running performance stress test...")
        performanceStressTest()
        println("OK")
    }

    private fun genProblemSmall(
        n: Int,
        m: Int,
        k: Int,
        startIdx0: Int,
        generator: GraphGenerator = RandomGraphGenerator()
    ): Problem {
        val graph = generator.generate(n, m, rnd)
        val unloadRaw = HashMap<Int, Int>(n * 2)
        val loadRaw = HashMap<Int, Int>(n * 2)
        val stations = HashSet<Int>(n * 2)
        for (i in 1..n) {
            unloadRaw[i] = rnd.nextInt(k)
            loadRaw[i] = rnd.nextInt(k)
            stations.add(i)
        }
        val start = startIdx0 + 1
        return Problem(stations, unloadRaw, loadRaw, graph, start)
    }

    private fun genProblemLarge(
        n: Int,
        m: Int,
        k: Int,
        startIdx0: Int,
        generator: GraphGenerator = RandomGraphGenerator()
    ): Problem {
        val graph = generator.generate(n, m, rnd)
        val unloadRaw = HashMap<Int, Int>(n * 2)
        val loadRaw = HashMap<Int, Int>(n * 2)
        val stations = HashSet<Int>(n * 2)
        for (i in 0 until n) {
            val s = i + 1
            unloadRaw[s] = ((i * 2654435761L) % k).toInt()
            val tmp = ((i.toLong() * 6364136223846793005L) ushr 33).toInt()
            loadRaw[s] = ((tmp % k) + k) % k
            stations.add(s)
        }
        val start = startIdx0 + 1
        return Problem(stations, unloadRaw, loadRaw, graph, start)
    }

    private fun randomCorrectnessTests(iterations: Int) {
        repeat(iterations) { itIdx ->
            val n = rnd.nextInt(8) + 2
            val m = rnd.nextInt(n * n + 1)
            val k = rnd.nextInt(10) + 1
            val startIdx = rnd.nextInt(n)

            val problem = genProblemSmall(n, m, k, startIdx)
            val expected = solveByBruteforce(problem, k)
            val actual = solveByPassToBitmask(problem, k)

            if (!expected.contentEquals(actual)) {
                System.err.println("Mismatch on iteration $itIdx")
                dumpProblem(problem)
                System.err.println("Expected in-masks:")
                dumpMasks(expected)
                System.err.println("Actual in-masks:")
                dumpMasks(actual)
                throw IllegalStateException("Stress test failed")
            }
        }
    }

    private fun performanceStressTest() {
        val n = 50_000
        val m = 200_000
        val k = 1000
        val startIdx = 0

        val problem = genProblemLarge(n, m, k, startIdx)
        val pass = CargoDataflowPassSimple(problem)
        val res = pass.run()

        val sample = listOf(1, n / 2, n)
        var sum = 0
        for (s in sample) sum += res.inAtStation[s]?.cardinality() ?: 0
        println("Performance test done. Sample cardinality sum=$sum")
    }

    // ----- reference / conversion -----

    private fun solveByBruteforce(problem: Problem, k: Int): IntArray {
        val ids = problem.stations.sorted()
        val idToPos = ids.withIndex().associate { it.value to it.index }
        val n = ids.size

        val unload = IntArray(n)
        val load = IntArray(n)
        for (i in 0 until n) {
            val id = ids[i]
            unload[i] = problem.unloadRaw.getValue(id)
            load[i] = problem.loadRaw.getValue(id)
        }

        val succ = Array(n) { IntArray(0) }
        for (uId in ids) {
            val u = idToPos.getValue(uId)
            succ[u] = problem.graph.successors(uId).mapNotNull { idToPos[it] }.toIntArray()
        }

        val startPos = idToPos.getValue(problem.start)
        val maxMask = 1 shl k
        val visited = Array(n) { BooleanArray(maxMask) }
        val inMask = IntArray(n)

        val q: ArrayDeque<Pair<Int, Int>> = ArrayDeque()
        visited[startPos][0] = true
        q.add(startPos to 0)

        while (q.isNotEmpty()) {
            val (v, maskArrive) = q.removeFirst()
            inMask[v] = inMask[v] or maskArrive

            var maskDepart = maskArrive
            maskDepart = maskDepart and (1 shl unload[v]).inv()
            maskDepart = maskDepart or (1 shl load[v])

            for (to in succ[v]) {
                if (!visited[to][maskDepart]) {
                    visited[to][maskDepart] = true
                    q.add(to to maskDepart)
                }
            }
        }
        return inMask
    }

    private fun solveByPassToBitmask(problem: Problem, k: Int): IntArray {
        val pass = CargoDataflowPassSimple(problem)
        val res = pass.run()

        val ids = problem.stations.sorted()
        val idToPos = ids.withIndex().associate { it.value to it.index }
        val inMask = IntArray(ids.size)

        val typeByIndex = res.typeByIndex
        for (id in ids) {
            val bs = res.inAtStation[id] ?: BitSet()
            var mask = 0
            for (i in 0 until typeByIndex.size) {
                if (bs.get(i)) {
                    val label = typeByIndex[i]
                    if (label in 0 until k) mask = mask or (1 shl label)
                }
            }
            inMask[idToPos.getValue(id)] = mask
        }
        return inMask
    }

    private fun dumpProblem(problem: Problem) {
        val ids = problem.stations.sorted()
        System.err.println("Stations=${ids.size}, start=${problem.start}")
        for (s in ids) {
            System.err.println("station $s unload=${problem.unloadRaw[s]} load=${problem.loadRaw[s]}")
        }
        for (u in ids) {
            for (v in problem.graph.successors(u)) {
                System.err.println("edge $u -> $v")
            }
        }
    }

    private fun dumpMasks(masks: IntArray) {
        for (i in masks.indices) {
            System.err.println("idx=$i mask=${masks[i].toString(2)}")
        }
    }
}