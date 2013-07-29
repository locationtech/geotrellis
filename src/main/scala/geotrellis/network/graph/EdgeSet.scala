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

  private var _edgeCount = 0
  def edgeCount = _edgeCount

  def hasAnyTimeEdgeTo(target:Vertex) =
    if(!edgesToTargets.contains(target)) {
      false 
    } else {
      edgesToTargets(target).filter(_.time != Time.ANY).isEmpty
    }

  def addEdge(target:Vertex,time:Time,travelTime:Duration):Unit = {
    if(!edgesToTargets.contains(target)) { edgesToTargets(target) = mutable.ListBuffer[Edge]() }

    val edgesToTarget = edgesToTargets(target)
    var set = false

    cfor(0)( _ < edgesToTarget.length && !set, _ + 1) { i =>
      if(time > edgesToTarget(i).time) {
        edgesToTarget.insert(i,Edge(target, time, travelTime))
        set = true
      }   
    }
    if(!set) { edgesToTarget += Edge(target, time, travelTime) }

    _edgeCount += 1
  }

  override
  def toString = {
    s"EdgeSet(${vertex})"
  }
}
