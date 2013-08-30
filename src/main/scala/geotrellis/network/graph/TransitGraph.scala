package geotrellis.network.graph

import geotrellis.network._

import scala.collection.mutable

import spire.syntax._

abstract sealed class EdgeDirection

object EdgeDirection {
  case object Incoming extends EdgeDirection
  case object Outgoing extends EdgeDirection
}

/**
 * Class produced by a Transit Graph to iterate over edges.
 */
trait EdgeIterator {
  def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit
}

class EmptyEdgeIterator extends EdgeIterator { 
  def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = { }
}

/**
 * A weighted, label based multi-graph.
 */
class TransitGraph(private val vertexMap:Array[Vertex],
                   private val locationToVertex:Map[Location,Int],
                   val anytimeEdgeSets:Map[TransitMode,PackedAnytimeEdges],
                   val scheduledEdgeSets:Map[ScheduledTransit,PackedScheduledEdges])
extends Serializable {
  val vertexCount = vertexMap.length
  val edgeCount = anytimeEdgeSets.values.map(_.edgeCount).foldLeft(0)(_+_) +
                  scheduledEdgeSets.values.map(_.edgeCount).foldLeft(0)(_+_)
  val transitEdgeModes = scheduledEdgeSets.keys.toArray

  def getEdgeIterator(transitMode:TransitMode,edgeDirection:EdgeDirection):EdgeIterator =
    getEdgeIterator(Seq(transitMode),edgeDirection)

  def getEdgeIterator(transitModes:Seq[TransitMode],edgeDirection:EdgeDirection):EdgeIterator = {
    val anytimes = anytimeEdgeSets.keys
                                  .filter(transitModes.contains(_))
                                  .map(anytimeEdgeSets(_))
                                  .toArray

    val transits = scheduledEdgeSets.keys
                                  .filter(transitModes.contains(_))
                                  .map(scheduledEdgeSets(_))
                                  .toArray

    val aLen = anytimes.length
    val tLen = transits.length

    // Complicated for performance's sake.
    if(aLen == 0 && tLen == 0) { new EmptyEdgeIterator() }
    else if(tLen == 0) {
      if(aLen == 1) {
        val edgeSet = anytimes(0)
        if(edgeDirection == EdgeDirection.Outgoing) {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              edgeSet.foreachOutgoingEdge(source)(f)
            }
          }
        } else {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              edgeSet.foreachIncomingEdge(source)(f)
            }
          }
        }
      } else {
        if(edgeDirection == EdgeDirection.Outgoing) {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < aLen, _ + 1) { i =>
                anytimes(i).foreachOutgoingEdge(source)(f)
              }
            }
          }
        } else {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < aLen, _ + 1) { i =>
                anytimes(i).foreachIncomingEdge(source)(f)
              }
            }
          }
        }
      }
    } 
    else if(aLen == 0) {
      if(tLen == 1) {
        val edgeSet = transits(0)
        if(edgeDirection == EdgeDirection.Outgoing) {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              edgeSet.foreachOutgoingEdge(source,time)(f)
            }
          }
        } else {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              edgeSet.foreachIncomingEdge(source,time)(f)
            }
          }
        }
      } else {
        if(edgeDirection == EdgeDirection.Outgoing) {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < tLen, _ + 1) { i =>
                transits(i).foreachOutgoingEdge(source,time)(f)
              }
            }
          }
        } else {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < tLen, _ + 1) { i =>
                transits(i).foreachIncomingEdge(source,time)(f)
              }
            }
          }
        }
      }
    }
    else {
      if(aLen == 1) {
        val aEdgeSet = anytimes(0)
        if(tLen == 1) {

          val tEdgeSet = transits(0)
          if(edgeDirection == EdgeDirection.Outgoing) {
            new EdgeIterator {
              def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
                aEdgeSet.foreachOutgoingEdge(source)(f)
                tEdgeSet.foreachOutgoingEdge(source,time)(f)
              }
            }
          } else {
            new EdgeIterator {
              def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
                aEdgeSet.foreachIncomingEdge(source)(f)
                tEdgeSet.foreachIncomingEdge(source,time)(f)
              }
            }
          }

        } else {
          if(edgeDirection == EdgeDirection.Outgoing) {
            new EdgeIterator {
              def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
                aEdgeSet.foreachOutgoingEdge(source)(f)
                cfor(0)(_ < tLen, _ + 1) { i =>
                  transits(i).foreachOutgoingEdge(source,time)(f)
                }
              }
            }
          } else {
            new EdgeIterator {
              def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
                aEdgeSet.foreachIncomingEdge(source)(f)
                cfor(0)(_ < tLen, _ + 1) { i =>
                  transits(i).foreachIncomingEdge(source,time)(f)
                }
              }
            }
          }
        }
      } 
      else if(tLen == 1) {
        val tEdgeSet = transits(0)
        if(edgeDirection == EdgeDirection.Outgoing) {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < aLen, _ + 1) { i =>
                anytimes(i).foreachOutgoingEdge(source)(f)
              }
              tEdgeSet.foreachOutgoingEdge(source,time)(f)
            }
          }
        } else {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < aLen, _ + 1) { i =>
                anytimes(i).foreachIncomingEdge(source)(f)
              }
              tEdgeSet.foreachIncomingEdge(source,time)(f)
            }
          }
        }
      } else {
        if(edgeDirection == EdgeDirection.Outgoing) {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < aLen, _ + 1) { i =>
                anytimes(i).foreachOutgoingEdge(source)(f)
              }
              cfor(0)(_ < tLen, _ + 1) { i =>
                transits(i).foreachOutgoingEdge(source,time)(f)
              }
            }
          }
        } else {
          new EdgeIterator {
            def foreachEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
              cfor(0)(_ < aLen, _ + 1) { i =>
                anytimes(i).foreachIncomingEdge(source)(f)
              }
              cfor(0)(_ < tLen, _ + 1) { i =>
                transits(i).foreachIncomingEdge(source,time)(f)
              }
            }
          }
        }
      }
    }
  }

  def location(v:Int) = vertexMap(v).location
  def vertexAt(l:Location) = locationToVertex(l)
  def vertexFor(v:Int) = vertexMap(v)
}

object TransitGraph {
  def pack(unpacked:MutableGraph):TransitGraph = {
    val vertices = unpacked.vertices.toArray
    val size = vertices.length

    // Create an simple random index of vertices
    // for integer based isomorphic transit graph.
    val vertexLookup = vertices.zipWithIndex.toMap

    val vertexMap = Array.ofDim[Vertex](size)
    val locations = mutable.Map[Location,Int]()
    val modes = mutable.Set[TransitMode]()
    for(v <- vertexLookup.keys) {
      modes ++= unpacked.edges(v).map(_.mode)
      val i = vertexLookup(v)
      vertexMap(i) = v
      locations(v.location) = i
    }

    val anytimeEdgeSets = mutable.Map[TransitMode,PackedAnytimeEdges]()
    val scheduledEdgeSets = mutable.Map[ScheduledTransit,PackedScheduledEdges]()
    for(mode <- modes) {
      mode match {
        case s:ScheduledTransit => 
          scheduledEdgeSets(s) = PackedScheduledEdges.pack(vertices,vertexLookup,unpacked,mode)
        case _ =>
        anytimeEdgeSets(mode) = PackedAnytimeEdges.pack(vertices,vertexLookup,unpacked,mode)
      }
    }

    new TransitGraph(vertexMap,locations.toMap,anytimeEdgeSets.toMap,scheduledEdgeSets.toMap)
  }
}
