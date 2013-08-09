package geotrellis.network.graph

import geotrellis.network._

import spire.syntax._

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
   * of information about an edge: the target vertex, the
   * start time associated with this edge, and the weight
   * of the edge.
   *
   * ... [ v | t | w ] | [ v | t | w ] | [ v | t | w ] ...
   * where v = the connected vertex
   *       t = time edge can be traversed (-2 if all time)
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge,
   * plus the waiting time for that edge traversal to occur.
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val edges = Array.ofDim[Int](edgeCount * 3)

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
    val end = (vertices(source * 2 + 1)*3) + start

    var target = -1
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
          val actualWeight = edgeWeight + (edgeTime - time)
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

    var totalEdgeCount = 0

    cfor(0)(_ < vertexCount, _ + 1) { i =>
      val v = vertices(i)
      val edgeCount = unpacked.edgeCount(v,edgeType)
      totalEdgeCount += edgeCount
      if(edgeCount == 0) {
        // Record empty vertex
        packed.vertices(i*2) = -1
        packed.vertices(i*2+1) = 0
      } else {
        // Set the edge index for this vertex
        packed.vertices(i*2) = edgesIndex
        // Record the number of edge entries for this vertex
        packed.vertices(i*2+1) = edgeCount

        // Edges need to be sorted first by target and then by the thier start time.
        val edges =
          unpacked.edges(v)
            .filter(_.edgeType == edgeType)
            .toSeq
            .sortBy { e => (vertexLookup(e.target),e.time.toInt) }
            .toList

        if(edges.length != edgeCount) { sys.error("Unexpected edge count for transit edges.") }
        
        cfor(0)(_ < edgeCount, _ + 1) { i =>
          val e = edges(i)
          val t = vertexLookup(e.target)

          packed.edges(edgesIndex) = t
          edgesIndex += 1
          packed.edges(edgesIndex) = e.time.toInt
          edgesIndex += 1
          packed.edges(edgesIndex) = e.travelTime.toInt
          edgesIndex += 1
        }
      }
    }
    packed
  }
}
