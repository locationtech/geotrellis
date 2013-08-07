package geotrellis.network.graph

import geotrellis.network._

import spire.syntax._

import scala.collection.mutable

class PackedAnytimeEdges(vertexCount:Int,val edgeCount:Int) extends Serializable {
  /**
   * 'vertices' is an array that is indexed by vertex id,
   * that contains two peices of information:
   * the start index of the 'edges' array for
   * a given vertex, and the number of outbound
   * edges to read from that start index.
   *                 
   * ... [ i | n ] | [ i | n ] | [ i | n ] ...
   * where i = index in edges array
   *       n = number of edges to read
   */
  private val vertices = Array.ofDim[Int](vertexCount * 2)

  /**
   * 'edges' is an array of that is indexed based
   * on the 'vertices' array, and contains three peices
   * of information about an edge: the target vertex and 
   * the weight of the edge.
   *
   * ... [ v | w ] | [ v | w ] | [ v | w ] ...
   * where v = the connected vertex
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val edges = Array.ofDim[Int](edgeCount * 2)

  /**
   * Given a source vertex, call a function which takes
   * in the target vertex and the weight of the edge for each outbound
   * edge from the source.
   */
  def foreachOutgoingEdge(source:Int)(f:(Int,Int)=>Unit):Unit = {
    val start = vertices(source * 2)
    if(start == -1) { return }
    val end = vertices(source * 2 + 1) + start

    cfor(start)( _ < end, _ + 2 ) { i =>
      val edgeTarget = edges(i)
      f(edges(i),edges(i+1))
    }
  }
}

object PackedAnytimeEdges {
  def pack(vertices:Array[Vertex],vertexLookup:Map[Vertex,Int],unpacked:MutableGraph,edgeType:EdgeType) = {
    val vertexCount = vertices.length
    val packed = new PackedAnytimeEdges(vertexCount,unpacked.edgeCount(edgeType))

    // Pack edges
    var edgesIndex = 0
    var doubleAnies = 0

    cfor(0)(_ < vertexCount, _ + 1) { i =>
      val v = vertices(i)
      val edgeCount = unpacked.edgeCount(v,edgeType)
      if(edgeCount == 0) {
        // Record empty vertex
        packed.vertices(i*2) = -1
        packed.vertices(i*2+1) = 0
      } else {
        // Set the edge index for this vertex
        packed.vertices(i*2) = edgesIndex
        // Record the number of edge entries for this vertex
        packed.vertices(i*2+1) = edgeCount*2

        // Edges need to be sorted first by target and then by the thier start time.
        val edges =
          unpacked.edges(v)
            .filter(_.edgeType == edgeType)
            .toSeq
            .sortBy { e => (vertexLookup(e.target),-e.travelTime.toInt) }
            .toList
        
        var lastTarget = -1

        cfor(0)(_ < edgeCount, _ + 1) { i =>
          val e = edges(i)
          val t = vertexLookup(e.target)

          if(lastTarget != t) {
            packed.edges(edgesIndex) = t
            edgesIndex += 1
            packed.edges(edgesIndex) = e.travelTime.toInt
            edgesIndex += 1
            lastTarget = t
          }
        }
      }
    }
    packed
  }
}

class PackedTransitEdges(vertexCount:Int,val edgeCount:Int) extends Serializable {
  /**
   * 'vertices' is an array that is indexed by vertex id,
   * that contains two peices of information:
   * the start index of the 'edges' array for
   * a given vertex, and the number of outbound
   * edges to read from that start index.
   *                 
   * ... [ i | n ] | [ i | n ] | [ i | n ] ...
   * where i = index in edges array
   *       n = number of edges to read
   */
  private val vertices = Array.ofDim[Int](vertexCount * 2)

  /**
   * 'edges' is an array of that is indexed based
   * on the 'vertices' array, and contains three peices
   * of information about an edge: the target vertex and 
   * the weight of the edge.
   *
   * ... [ v | w ] | [ v | w ] | [ v | w ] ...
   * where v = the connected vertex
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val edges = Array.ofDim[Int](edgeCount * 2)

  /**
   * Given a source vertex, and a time, call a function which takes
   * in the target vertex and the weight of the edge for each outbound
   * edge from the source. If that source has both an AnyTime edge and
   * a time edge to a target, call the function against the edge with
   * the lower weight.
   */
  def foreachOutgoingEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    val start = vertices(source * 2)
    if(start == -1) { return }

    val end = vertices(source * 2 + 1) + start
    var target = -1
    var targetSpent = false
    var skipUntilTargetChange = false

    cfor(start)( _ < end, _ + 3 ) { i =>
      val edgeTarget = edges(i)

      // If we're skipping until the target changes,
      // just check if that's the case. If not, do nothing.
      if(skipUntilTargetChange) {
        if(edgeTarget != target) {
          skipUntilTargetChange = false
        }
      }

      if(!skipUntilTargetChange) {
        if(target != edgeTarget) {
          target = edgeTarget
        }

        val edgeTime = edges(i + 1)
        val edgeWeight = edges(i + 2)

        if(edgeTime >= time) {
          skipUntilTargetChange = true
          val actualWeight = edges(i+2) + (edgeTime - time)
          f(edgeTarget,actualWeight)
        }
      }
    }
  }
}

object PackedTransitEdges {
  def pack(vertices:Array[Vertex],vertexLookup:Map[Vertex,Int],unpacked:MutableGraph,edgeType:EdgeType) = {
    val vertexCount = vertices.length
    val packed = new PackedTransitEdges(vertexCount,unpacked.edgeCount(edgeType))

    // Pack edges
    var edgesIndex = 0
    var doubleAnies = 0

    cfor(0)(_ < vertexCount, _ + 1) { i =>
      val v = vertices(i)
      val edgeCount = unpacked.edgeCount(v,edgeType)
      if(edgeCount == 0) {
        // Record empty vertex
        packed.vertices(i*2) = -1
        packed.vertices(i*2+1) = 0
      } else {
        // Set the edge index for this vertex
        packed.vertices(i*2) = edgesIndex
        // Record the number of edge entries for this vertex
        packed.vertices(i*2+1) = edgeCount*2

        // Edges need to be sorted first by target and then by the thier start time.
        val edges =
          unpacked.edges(v)
            .filter(_.edgeType == edgeType)
            .toSeq
            .sortBy { e => (vertexLookup(e.target),e.travelTime.toInt) }
            .toList
        
        cfor(0)(_ < edgeCount, _ + 1) { i =>
          val e = edges(i)
          val t = vertexLookup(e.target)

          packed.edges(edgesIndex) = t
          edgesIndex += 1
          packed.edges(edgesIndex) = e.travelTime.toInt
          edgesIndex += 1
        }
      }
    }
    packed
  }
}

/**
 * A weighted, label based multi-graph.
 */
class TransitGraph(private val vertexMap:Array[Vertex],
                   private val locationToVertex:Map[Location,Int],
                   val walkingEdges:PackedAnytimeEdges,
                   val bikingEdges:PackedAnytimeEdges,
                   val transitEdges:PackedTransitEdges)
extends Serializable {
  val vertexCount = vertexMap.length
  val edgeCount = walkingEdges.edgeCount + bikingEdges.edgeCount + transitEdges.edgeCount

  /**
   * Given a source vertex, and a time, call a function which takes
   * in the target vertex and the weight of the edge for each outbound
   * edge from the source. If that source has both an AnyTime edge and
   * a time edge to a target, call the function against the edge with
   * the lower weight.
   */
  def foreachTransitEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    walkingEdges.foreachOutgoingEdge(source)(f)
    transitEdges.foreachOutgoingEdge(source,time)(f)
  }

  def foreachWalkingEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    walkingEdges.foreachOutgoingEdge(source)(f)
    transitEdges.foreachOutgoingEdge(source,time)(f)
  }

  def foreachBikingEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    walkingEdges.foreachOutgoingEdge(source)(f)
    transitEdges.foreachOutgoingEdge(source,time)(f)
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

    val walkingEdges = PackedAnytimeEdges.pack(vertices,vertexLookup,unpacked,WalkEdge)
    val bikingEdges = PackedAnytimeEdges.pack(vertices,vertexLookup,unpacked,BikeEdge)
    val transitEdges = PackedTransitEdges.pack(vertices,vertexLookup,unpacked,TransitEdge)
    new TransitGraph(vertexMap,locations.toMap,walkingEdges,bikingEdges,transitEdges)
  }
}
