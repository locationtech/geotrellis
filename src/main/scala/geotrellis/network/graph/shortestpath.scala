package geotrellis.network.graph

import geotrellis.network._

import scala.collection.mutable.{ListBuffer,PriorityQueue}

import spire.syntax._

abstract sealed class PathType

case object WalkPath extends PathType
case object BikePath extends PathType 

/** Path includes public transit and walking */
case class TransitPath(weeklySchedule:WeeklySchedule) extends PathType { 
  override def equals(o: Any) = 
    o match {
      case that: TransitPath => weeklySchedule.equals(that.weeklySchedule)
      case _ => false
    }

  override def hashCode = weeklySchedule.hashCode
} 

trait ShortestPathTree {
  def travelTimeTo(target:Int):Duration
  def reachableVertices:Set[Int]
}

object ShortestPathTree {
  // Optimization: Set an empty SPT array once you know the 
  // graph vertex count, since Array.clone is so much faster than
  // Array.fill
  private [graph] var _sptArray:Array[Int] = null
  def initSptArray(vertexCount:Int) = { _sptArray = Array.fill[Int](vertexCount)(-1) }

  def departure(from:Int, startTime:Time, graph:TransitGraph) =
    new ShortestDeparturePathTree(from,startTime,graph,None,WalkPath)

  def departure(from:Int,startTime:Time,graph:TransitGraph,pathType:PathType) =
    new ShortestDeparturePathTree(from,startTime,graph,None,pathType)

  def departure(from:Int,startTime:Time,graph:TransitGraph,maxDuration:Duration) =
    new ShortestDeparturePathTree(from,startTime,graph,Some(maxDuration),WalkPath)

  def departure(from:Int,startTime:Time,graph:TransitGraph,maxDuration:Duration,pathType:PathType) =
    new ShortestDeparturePathTree(from,startTime,graph,Some(maxDuration),pathType)

  def arrival(to:Int, arriveTime:Time, graph:TransitGraph) =
    new ShortestArrivalPathTree(to,arriveTime,graph,None,WalkPath)

  def arrival(to:Int, arriveTime:Time, graph:TransitGraph,pathType:PathType) =
    new ShortestArrivalPathTree(to,arriveTime,graph,None,pathType)

  def arrival(to:Int,arriveTime:Time,graph:TransitGraph,maxDuration:Duration) =
    new ShortestArrivalPathTree(to,arriveTime,graph,Some(maxDuration),WalkPath)

  def arrival(to:Int,arriveTime:Time,graph:TransitGraph,maxDuration:Duration,pathType:PathType) =
    new ShortestArrivalPathTree(to,arriveTime,graph,Some(maxDuration),pathType)
}

class ShortestDeparturePathTree(val startVertex:Int,
                       val startTime:Time,
                       graph:TransitGraph,
                       val maxDuration:Option[Duration],
                       pathType:PathType) 
extends ShortestPathTree {
  /**
   * Array containing arrival times of the current shortest
   * path to the index vertex.
   */
  private val shortestPathTimes = 
    if(ShortestPathTree._sptArray != null) { ShortestPathTree._sptArray.clone }
    else { Array.fill[Int](graph.vertexCount)(-1) }

  private val _reachableVertices = 
    ListBuffer[Int]()

  def reachableVertices:Set[Int] = _reachableVertices.toSet

  shortestPathTimes(startVertex) = 0

  // dijkstra's

  object VertexOrdering extends Ordering[Int] {
    def compare(a:Int, b:Int) =
      -(shortestPathTimes(a) compare shortestPathTimes(b))
  }

  val queue = PriorityQueue[Int]()(VertexOrdering)

  val tripStart = startTime.toInt
  val duration = 
    maxDuration match {
      case Some(d) => d.toInt + tripStart
      case None => Int.MaxValue
    }

  val edgeIterator = 
    pathType match {
      case WalkPath =>
        graph.getEdgeIterator(Walking,EdgeDirection.Outgoing)
      case BikePath =>
        graph.getEdgeIterator(Biking,EdgeDirection.Outgoing)
      case TransitPath(weeklySchedule) =>
        graph.getEdgeIterator(Seq(Walking,PublicTransit(weeklySchedule)),EdgeDirection.Outgoing)
    }

  edgeIterator.foreachEdge(startVertex,tripStart) { (target,weight) =>
    val t = tripStart + weight
    if(t <= duration) {
      shortestPathTimes(target) = t
      queue += target
      _reachableVertices += target
    }
  }

  while(!queue.isEmpty) {
    val currentVertex = queue.dequeue
    val currentTime = shortestPathTimes(currentVertex)

    edgeIterator.foreachEdge(currentVertex, currentTime) { (target,weight) =>
      val t = currentTime + weight
      if(t <= duration) {
        val currentTime = shortestPathTimes(target)
        if(currentTime == -1 || currentTime > t) {
          _reachableVertices += target
          shortestPathTimes(target) = t
          queue += target
        }
      }
    }
  }

  def travelTimeTo(target:Int):Duration = {
    val tt = shortestPathTimes(target)
    if(tt == -1) Duration.UNREACHABLE
    else new Duration(shortestPathTimes(target) - startTime.toInt)
  }
}

class ShortestArrivalPathTree(val destinationVertex:Int,
                              val arrivalTime:Time,
                              graph:TransitGraph,
                              val maxDuration:Option[Duration],
                              pathType:PathType) 
extends ShortestPathTree {
  /**
   * Array containing departure times of the current shortest
   * path to the index vertex.
   */
  private val shortestPathTimes = 
    if(ShortestPathTree._sptArray != null) { ShortestPathTree._sptArray.clone }
    else { Array.fill[Int](graph.vertexCount)(-1) }

  private val _reachableVertices = 
    ListBuffer[Int]()

  def reachableVertices:Set[Int] = _reachableVertices.toSet

  shortestPathTimes(destinationVertex) = 0

  // dijkstra's

  object VertexOrdering extends Ordering[Int] {
    def compare(a:Int, b:Int) = 
      shortestPathTimes(a) compare shortestPathTimes(b)
  }

  val queue = PriorityQueue[Int]()(VertexOrdering)

  val tripStart = arrivalTime.toInt
  val duration = 
    maxDuration match {
      case Some(d) => tripStart - d.toInt
      case None => Int.MinValue
    }

  val edgeIterator = 
    pathType match {
      case WalkPath =>
        graph.getEdgeIterator(Walking,EdgeDirection.Incoming)
      case BikePath =>
        graph.getEdgeIterator(Biking,EdgeDirection.Incoming)
      case TransitPath(weeklySchedule) =>
        graph.getEdgeIterator(Seq(Walking,PublicTransit(weeklySchedule)),EdgeDirection.Incoming)
    }

  edgeIterator.foreachEdge(destinationVertex,tripStart) { (target,weight) =>
    val t = tripStart - weight
    if(t >= duration) {
      shortestPathTimes(target) = t
      queue += target
      _reachableVertices += target
    }
  }

  while(!queue.isEmpty) {
    val currentVertex = queue.dequeue
    val currentTime = shortestPathTimes(currentVertex)

    edgeIterator.foreachEdge(currentVertex, currentTime) { (target,weight) =>
      val t = currentTime - weight
      if(t >= duration) {
        val currentTime = shortestPathTimes(target)
        if(currentTime == -1 || currentTime < t) {
          _reachableVertices += target
          shortestPathTimes(target) = t
          queue += target
        }
      }
    }
  }

  def travelTimeTo(target:Int):Duration = {
    val tt = shortestPathTimes(target)
    if(tt == -1) Duration.UNREACHABLE
    else new Duration(arrivalTime.toInt - shortestPathTimes(target))
  }
}
