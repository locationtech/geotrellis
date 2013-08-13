package geotrellis.network.graph

import geotrellis.network._

import scala.collection.mutable.{ListBuffer,PriorityQueue}

import spire.syntax._

abstract sealed class PathType

case object WalkPath extends PathType
case object BikePath extends PathType 
case object TransitPath extends PathType // includes walking

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
    new ShortestDeparturePathTree(from,startTime,graph,None,TransitPath)

  def departure(from:Int,startTime:Time,graph:TransitGraph,maxDuration:Duration) =
    new ShortestDeparturePathTree(from,startTime,graph,Some(maxDuration),TransitPath)

  def departure(from:Int,startTime:Time,graph:TransitGraph,maxDuration:Duration,pathType:PathType) =
    new ShortestDeparturePathTree(from,startTime,graph,Some(maxDuration),pathType)

  def arrival(to:Int, arriveTime:Time, graph:TransitGraph) =
    new ShortestArrivalPathTree(to,arriveTime,graph,None,TransitPath)

  def arrival(to:Int,arriveTime:Time,graph:TransitGraph,maxDuration:Duration) =
    new ShortestArrivalPathTree(to,arriveTime,graph,Some(maxDuration),TransitPath)

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

  val foreachEdge:(Int,Int,(Int,Int)=>Unit)=>Unit = { (sv, t, f) =>
    pathType match {
      case WalkPath =>
        graph.foreachWalkEdge(sv)(f)
      case BikePath =>
        graph.foreachBikeEdge(sv)(f)
      case TransitPath =>
        graph.foreachTransitEdge(sv,t)(f)
    }
  }

  foreachEdge(startVertex,tripStart, { (target,weight) =>
    val t = tripStart + weight
    if(t <= duration) {
      shortestPathTimes(target) = t
      queue += target
      _reachableVertices += target
    }
  })

  while(!queue.isEmpty) {
    val currentVertex = queue.dequeue
    val currentTime = shortestPathTimes(currentVertex)

    foreachEdge(currentVertex, currentTime, { (target,weight) =>
      val t = currentTime + weight
      if(t <= duration) {
        val currentTime = shortestPathTimes(target)
        if(currentTime == -1 || currentTime > t) {
          _reachableVertices += target
          shortestPathTimes(target) = t
          queue += target
        }
      }
    })
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

  // val shortestPaths =
  //   Array.fill[ListBuffer[Int]](graph.vertexCount)(ListBuffer[Int]())  ///////DEBUG

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

  val foreachEdge:(Int,Int,(Int,Int)=>Unit)=>Unit = { (sv, t, f) =>
    pathType match {
      case WalkPath =>
        graph.foreachWalkEdge(sv)(f)
      case BikePath =>
        graph.foreachBikeEdge(sv)(f)
      case TransitPath =>
        graph.foreachIncomingTransitEdge(sv,t)(f)
    }
  }

  foreachEdge(destinationVertex,tripStart, { (target,weight) =>
    val t = tripStart - weight
    if(t >= duration) {
      shortestPathTimes(target) = t
      queue += target
      _reachableVertices += target
//      shortestPaths(target) = shortestPaths(destinationVertex) :+ destinationVertex //DBOEU
    }
  })

  while(!queue.isEmpty) {
    val currentVertex = queue.dequeue
    val currentTime = shortestPathTimes(currentVertex)

    foreachEdge(currentVertex, currentTime, { (target,weight) =>
      val t = currentTime - weight
      if(t >= duration) {
        val currentTime = shortestPathTimes(target)
        if(currentTime == -1 || currentTime < t) {
          _reachableVertices += target
          shortestPathTimes(target) = t
          queue += target
//          shortestPaths(target) = shortestPaths(currentVertex) :+ currentVertex //DUBEG
        }
      }
    })
  }

  def travelTimeTo(target:Int):Duration = {
    val tt = shortestPathTimes(target)
    if(tt == -1) Duration.UNREACHABLE
    else new Duration(arrivalTime.toInt - shortestPathTimes(target))
  }
}
