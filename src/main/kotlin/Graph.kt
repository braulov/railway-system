package org.example

/**
 * Directed graph with both succ and pred adjacency lists.
 * Each vertex maintains a list of outgoing (successor) and incoming (predecessor) edges.
 */
class Graph {
    private val succ: MutableMap<Int, MutableList<Int>> = HashMap()
    private val pred: MutableMap<Int, MutableList<Int>> = HashMap()

    fun addVertex(v: Int) {
        succ.getOrPut(v) { mutableListOf() }
        pred.getOrPut(v) { mutableListOf() }
    }

    /** Add a directed edge u -> v to the graph. */
    fun addEdge(u: Int, v: Int) {
        addVertex(u)
        addVertex(v)
        succ[u]!!.add(v)
        pred[v]!!.add(u)
    }

    fun successors(v: Int): List<Int> = succ[v].orEmpty()
    fun predecessors(v: Int): List<Int> = pred[v].orEmpty()
}