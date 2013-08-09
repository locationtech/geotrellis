package geotrellis.network.graph

import geotrellis.network._

import scala.collection.mutable

/**
 * A weighted, label based multi-graph.
 */
class TransitGraph(private val vertexMap:Array[Vertex],
                   private val locationToVertex:Map[Location,Int],
                   val walkEdges:PackedAnytimeEdges,
                   val bikeEdges:PackedAnytimeEdges,
                   val transitEdges:PackedTransitEdges)
extends Serializable {
  val vertexCount = vertexMap.length
  val edgeCount = walkEdges.edgeCount + bikeEdges.edgeCount + transitEdges.edgeCount

  def foreachTransitEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    walkEdges.foreachOutgoingEdge(source)(f)
    transitEdges.foreachOutgoingEdge(source,time)(f)
  }

  def foreachWalkEdge(source:Int)(f:(Int,Int)=>Unit):Unit = {
    walkEdges.foreachOutgoingEdge(source)(f)
  }

  def foreachBikeEdge(source:Int)(f:(Int,Int)=>Unit):Unit = {
    bikeEdges.foreachOutgoingEdge(source)(f)
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
    for(v <- vertexLookup.keys) {
      val i = vertexLookup(v)
      vertexMap(i) = v
      locations(v.location) = i
    }

    val walkEdges = PackedAnytimeEdges.pack(vertices,vertexLookup,unpacked,WalkEdge)
    val bikeEdges = PackedAnytimeEdges.pack(vertices,vertexLookup,unpacked,BikeEdge)
    val transitEdges = PackedTransitEdges.pack(vertices,vertexLookup,unpacked,TransitEdge)
    new TransitGraph(vertexMap,locations.toMap,walkEdges,bikeEdges,transitEdges)
  }
}
