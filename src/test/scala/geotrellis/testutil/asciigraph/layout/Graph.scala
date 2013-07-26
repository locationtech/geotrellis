package com.github.mdr.ascii.layout

import com.github.mdr.ascii.Diagram
import com.github.mdr.ascii.Box

object Graph {

  def fromDiagram(s: String): Graph[String] = fromDiagram(Diagram(s))

  def fromDiagram(diagram: Diagram): Graph[String] = {
    val boxToVertexMap: Map[Box, String] =
      (for (box ← diagram.childBoxes)
        yield (box -> box.text)).toMap

    val edges =
      for {
        edge ← diagram.allEdges
        box1 = edge.box1
        box2 = edge.box2
        vertex1 ← boxToVertexMap.get(box1)
        vertex2 ← boxToVertexMap.get(box2)
      } yield {
        if (edge.hasArrow2)
          vertex1 -> vertex2
        else
          vertex2 -> vertex1
      }
    val vertices = boxToVertexMap.values.toList
    Graph(vertices, edges)
  }

}

case class Graph[V](vertices: List[V], edges: List[(V, V)]) {

  val outMap: Map[V, List[V]] = edges.groupBy(_._1).map { case (k, vs) ⇒ (k, vs.map(_._2)) }

  val inMap: Map[V, List[V]] = edges.groupBy(_._2).map { case (k, vs) ⇒ (k, vs.map(_._1)) }

  require(outMap.keys.forall(vertices.contains))
  require(inMap.keys.forall(vertices.contains))

  def inVertices(v: V): List[V] = inMap.getOrElse(v, Nil)

  def outVertices(v: V): List[V] = outMap.getOrElse(v, Nil)

  def outDegree(v: V): Int = outVertices(v).size

  def inDegree(v: V): Int = inVertices(v).size

}