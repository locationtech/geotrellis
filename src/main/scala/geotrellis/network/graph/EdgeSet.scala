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

  private val edgeCounts = mutable.Map[TransitMode,Int]()
  def edgeCount(mode:TransitMode) = edgeCounts.getOrElse(mode,0)

  def addEdge(edge:Edge):Unit = {
    val target = edge.target
    if(!edgesToTargets.contains(target)) { edgesToTargets(target) = mutable.ListBuffer[Edge]() }
    edgesToTargets(target) += edge
    if(!edgeCounts.contains(edge.mode)) { edgeCounts(edge.mode) = 0 }
    edgeCounts(edge.mode) += 1
  }

  override
  def toString = {
    s"EdgeSet(${vertex})"
  }
}
