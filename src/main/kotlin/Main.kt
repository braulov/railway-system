package org.example

import java.util.*

fun main(args: Array<String>) {

    if (args.isNotEmpty() && args[0] == "stress") {
        Stress.runAll()
        return
    }

    val br = System.`in`.bufferedReader()
    val problem = Parser.parse(br)

    val pass = CargoDataflowPassSimple(problem)
    val result = pass.run()

    for (s in problem.stations.sorted()) {
        val bs = result.inAtStation[s] ?: BitSet()
        val cargos = mutableListOf<Int>()
        for (i in 0 until result.typeByIndex.size) {
            if (bs.get(i)) cargos.add(result.typeByIndex[i])
        }
        cargos.sort()
        println("$s: $cargos")
    }
}