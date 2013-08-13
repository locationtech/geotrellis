package geotrellis.network.graph

import geotrellis.network._

import spire.syntax._

/**
  * Represents edges of a undirected weighted graph.
  */
class PackedAnytimeEdges(vertexCount:Int,val edgeCount:Int) extends Serializable {
  /**
   * 'verticesToEdges' is an array that is indexed by vertex id,
   * that contains two peices of information:
   * the start index of the 'edges' array for
   * a given vertex, and the number of outbound
   * edges to read from that start index.
   *                 
   * ... [ i | n ] | [ i | n ] | [ i | n ] ...
   * where i = index in edges array
   *       n = number of edges to read
   */
  private val verticesToEdges = Array.ofDim[Int](vertexCount * 2)

  /**
   * 'edges' is an array of that is indexed based
   * on the 'verticesToEdges' array, and contains three peices
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
    val start = verticesToEdges(source * 2)
    if(start == -1) { return }
    val end = (verticesToEdges(source * 2 + 1)*2) + start

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

    cfor(0)(_ < vertexCount, _ + 1) { i =>
      val v = vertices(i)
      val edgeCount = unpacked.edgeCount(v,edgeType)
      if(edgeCount == 0) {
        // Record empty vertex
        packed.verticesToEdges(i*2) = -1
        packed.verticesToEdges(i*2+1) = 0
      } else {
        // Set the edge index for this vertex
        packed.verticesToEdges(i*2) = edgesIndex
        // Record the number of edge entries for this vertex
        packed.verticesToEdges(i*2+1) = edgeCount

        // Edges need to be sorted first by target and then by the thier start time.
        val edges =
          unpacked.edges(v)
            .filter(_.edgeType == edgeType)
            .toSeq
            .sortBy { e => (vertexLookup(e.target),-e.travelTime.toInt) }
            .toList
        
        if(edges.length != edgeCount) { sys.error("Unexpected edge count.") }

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
