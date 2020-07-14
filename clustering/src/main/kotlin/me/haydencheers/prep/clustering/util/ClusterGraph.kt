package me.haydencheers.prep.clustering.util

import java.util.*

class ClusterGraph {
    private val nodes = mutableSetOf<String>()
    private val edges = mutableMapOf<String, MutableSet<String>>()

    fun getNodes(): Set<String> {
        return this.nodes.toSet()
    }

    fun addEdge(a: String, b: String) {
        nodes.add(a)
        nodes.add(b)

        edges.getOrPut(a) { mutableSetOf() }.add(b)
        edges.getOrPut(b) { mutableSetOf() }.add(a)
    }

    fun components(): List<GraphComponent> {
        val components = mutableListOf<GraphComponent>()
        val visited = mutableSetOf<String>()
        val toCheck = Stack<String>()
        toCheck.addAll(nodes)

        while (toCheck.isNotEmpty()) {
            val current = toCheck.pop()
            if (visited.contains(current)) continue

            val component = componentFrom(current)
            components.add(component)
            visited.addAll(component.componentNodes)
        }

        return components
    }

    private fun componentFrom(start: String): GraphComponent {
        val component = mutableSetOf<String>()

        val toCheck = Stack<String>()
        toCheck.add(start)

        while (toCheck.isNotEmpty()) {
            val current = toCheck.pop()
            if (component.contains(current)) continue
            component.add(current)

            val neighbours = neighboursFor(current)
            toCheck.addAll(neighbours)
        }

        return GraphComponent(component)
    }

    private fun neighboursFor(node: String): Set<String> {
        return (edges.getValue(node))
    }

    inner class GraphComponent(internal val componentNodes: Set<String>) {
        fun connectivityRatio(): Double {
            if (componentNodes.size == 2) return 0.0

            val componentSize = countEdges()
            val maxComponentSize = ((componentNodes.size) * (componentNodes.size-1)) / 2.0

            return componentSize / maxComponentSize
        }

        fun largestNodeDegree(): Int {
            return componentNodes.map { this@ClusterGraph.edges[it]!!.count() }
                .max() ?: 0
        }

        private fun countEdges(): Int {
            return componentNodes.map { this@ClusterGraph.edges[it]!! }
                .sumBy { it.size }
                .div(2)
        }
    }
}