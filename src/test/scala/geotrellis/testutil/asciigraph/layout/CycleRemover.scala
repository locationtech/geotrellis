package com.github.mdr.ascii.layout

import scala.collection.immutable.SortedMap
import scala.annotation.tailrec

class CycleRemover[V] {

  private class VertexInfoDatabase(graph: Graph[V]) {

    def runs: Set[V] = sources

    def getSinks: Set[V] = sinks

    def getLargestDegreeDiffVertex: Option[V] = degreeDiffToVertices.lastOption.flatMap(_._2.headOption)

    private var sources: Set[V] = Set() // excludes isolated vertices

    private var sinks: Set[V] = Set() // includes isolated vertices

    private var degreeDiffToVertices: SortedMap[Int, List[V]] = SortedMap() // out degree - in degree

    private var verticesToDegreeDiff: Map[V, Int] = Map()

    private var deletedVertices: Set[V] = Set()

    for (v ← graph.vertices) {
      val outDegree = graph.outDegree(v)
      val inDegree = graph.inDegree(v)
      if (outDegree == 0)
        sinks += v
      else if (inDegree == 0)
        sources += v
      val degreeDiff = outDegree - inDegree
      addVertexToDegreeDiffMaps(v, degreeDiff)
    }

    private def getInVertices(v: V): List[V] = graph.inVertices(v).filterNot(deletedVertices)

    private def getOutVertices(v: V): List[V] = graph.outVertices(v).filterNot(deletedVertices)

    def removeVertex(v: V) {
      if (sinks contains v)
        sinks -= v
      if (sources contains v)
        sources -= v
      removeVertexFromDegreeDiffMaps(v)

      val outVertices = getOutVertices(v)
      val inVertices = getInVertices(v)

      deletedVertices += v

      for (outVertex ← outVertices) {
        val previousDegreeDiff = removeVertexFromDegreeDiffMaps(outVertex)
        addVertexToDegreeDiffMaps(outVertex, previousDegreeDiff + 1)
        if (getInVertices(outVertex).isEmpty)
          sources += outVertex
      }

      for (inVertex ← inVertices) {
        val previousDegreeDiff = removeVertexFromDegreeDiffMaps(inVertex)
        addVertexToDegreeDiffMaps(inVertex, previousDegreeDiff - 1)
        if (getOutVertices(inVertex).isEmpty)
          sinks += inVertex
      }

    }

    private def addVertexToDegreeDiffMaps(v: V, degreeDiff: Int) = {
      degreeDiffToVertices += degreeDiff -> (v :: degreeDiffToVertices.getOrElse(degreeDiff, Nil))
      verticesToDegreeDiff += v -> degreeDiff
    }

    private def removeVertexFromDegreeDiffMaps(v: V): Int = {
      val degreeDiff = verticesToDegreeDiff(v)
      val vertices = degreeDiffToVertices(degreeDiff)
      val updatedVertices = vertices.filterNot(_ == v)
      if (updatedVertices.isEmpty)
        degreeDiffToVertices -= degreeDiff
      else
        degreeDiffToVertices += degreeDiff -> updatedVertices
      verticesToDegreeDiff -= v
      degreeDiff
    }

  }

  private def findVertexSequence(graph: Graph[V]): List[V] = {
    val db = new VertexInfoDatabase(graph)
    var left: List[V] = Nil
    var right: List[V] = Nil

    var continue = true
    while (continue) {
      @tailrec def processSinks() {
        val sinks = db.getSinks
        if (sinks.nonEmpty) {
          for (v ← sinks) {
            db.removeVertex(v)
            right ::= v
          }
          processSinks()
        }
      }
      processSinks()

      @tailrec def processSources() {
        val sources = db.runs
        if (sources.nonEmpty) {
          for (v ← sources) {
            db.removeVertex(v)
            left ::= v
          }
          processSources()
        }
      }
      processSources()

      db.getLargestDegreeDiffVertex match {
        case Some(v) ⇒
          db.removeVertex(v)
          left ::= v
        case None ⇒ continue = false
      }
    }

    left.reverse ++ right
  }

  private def removeSelfLoops(graph: Graph[V]): Graph[V] =
    graph.copy(edges = graph.edges.filterNot { case (v1, v2) ⇒ v1 == v2 })

  /**
   * @return graph without cycles and list of reversed edges.
   */
  def removeCycles(graph: Graph[V]): (Graph[V], List[(V, V)]) =
    removeCycles(removeSelfLoops(graph), findVertexSequence(graph))

  private def removeCycles(graph: Graph[V], vertexSequence: List[V]): (Graph[V], List[(V, V)]) = {
    var newEdges: List[(V, V)] = Nil
    var reversedEdges: List[(V, V)] = Nil
    for {
      (vertex, index) ← vertexSequence.zipWithIndex
      outVertex ← graph.outVertices(vertex)
    } {
      val outVertexIndex = vertexSequence.indexOf(outVertex).ensuring(_ >= 0)
      if (outVertexIndex < index) {
        reversedEdges ::= (outVertex, vertex)
        newEdges ::= (outVertex, vertex)
      } else
        newEdges ::= (vertex, outVertex)
    }
    (graph.copy(edges = newEdges), reversedEdges)
  }

}