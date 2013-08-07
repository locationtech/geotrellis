package geotrellis.network.graph

import geotrellis.network._
import geotrellis.network.graph._

import scala.collection.mutable

import spire.syntax._

object EdgeSet {
  def apply(vertex:Vertex) = new EdgeSet(vertex)
}

class EdgeSet(val vertex:Vertex) extends Iterable[Edge] {
  val edgesToTargets = mutable.Map[Vertex,mutable.ListBuffer[Edge]]()

  def edges = 
    edgesToTargets.values.flatten

  def iterator = 
    edges.iterator

  private val edgeCounts = mutable.Map[EdgeType,Int]()
  def edgeCount(et:EdgeType) = edgeCounts.getOrElse(et,0)

  def hasAnyTimeEdgeTo(target:Vertex) =
    if(!edgesToTargets.contains(target)) {
      false 
    } else {
      edgesToTargets(target).filter(_.time != Time.ANY).isEmpty
    }

  def addEdge(edge:Edge):Unit = {
    val target = edge.target
    if(!edgesToTargets.contains(target)) { edgesToTargets(target) = mutable.ListBuffer[Edge]() }
    edgesToTargets(target) += edge
    if(!edgeCounts.contains(edge.edgeType)) { edgeCounts(edge.edgeType) = 0 }
    edgeCounts(edge.edgeType) += 1
  }

  override
  def toString = {
    s"EdgeSet(${vertex})"
  }
}
