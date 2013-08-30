package geotrellis.network.graph

import geotrellis.network._

import scala.collection.mutable
import spire.syntax._

/**
  * Represents edges of a directed weighted graph
  * where the edges are not time dependant and is not backed by a multigraph.
  */
class PackedAnytimeEdges(vertexCount:Int,val edgeCount:Int) extends Serializable {
  /**
   * 'verticesToOutgoing' is an array that is indexed by vertex id,
   * that contains two peices of information:
   * the start index of the 'edges' array for
   * a given vertex, and the number of outbound
   * edges to read from that start index.
   *                 
   * ... [ i | n ] | [ i | n ] | [ i | n ] ...
   * where i = index in edges array
   *       n = number of edges to read
   */
  val verticesToOutgoing = Array.ofDim[Int](vertexCount * 2)

  /**
   * 'outgoingEdges' is an array of that is indexed based
   * on the 'verticesToOutgoing' array, and contains three peices
   * of information about an edge: the target vertex and 
   * the weight of the edge.
   *
   * ... [ v | w ] | [ v | w ] | [ v | w ] ...
   * where v = the connected vertex
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val outgoingEdges = Array.ofDim[Int](edgeCount * 2)

  /**
   * 'verticesToIncoming' is an array that is indexed by vertex id,
   * that contains two peices of information:
   * the start index of the 'outgoingEdges' array for
   * a given vertex, and the number of outbound
   * edges to read from that start index.
   *                 
   * ... [ i | n ] | [ i | n ] | [ i | n ] ...
   * where i = index in outgoingEdges array
   *       n = number of outgoingEdges to read
   */
  private val verticesToIncoming = Array.ofDim[Int](vertexCount * 2)
 
  /**
   * 'incomingEdges' is an array of that is indexed based
   * on the 'verticesToOutgoing' array, and contains three peices
   * of information about an edge: the target vertex and 
   * the weight of the edge.
   *
   * ... [ v | w ] | [ v | w ] | [ v | w ] ...
   * where v = the connected vertex
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val incomingEdges = Array.ofDim[Int](edgeCount * 2)

  /**
   * Given a source vertex, call a function which takes
   * in the target vertex and the weight of the edge for each outbound
   * edge from the source.
   */
  def foreachOutgoingEdge(source:Int)(f:(Int,Int)=>Unit):Unit = {
    val start = verticesToOutgoing(source * 2)
    if(start == -1) { return }
    val end = (verticesToOutgoing(source * 2 + 1)*2) + start

    cfor(start)( _ < end, _ + 2 ) { i =>
      val edgeTarget = outgoingEdges(i)
      f(outgoingEdges(i),outgoingEdges(i+1))
    }
  }

  /**
   * Given a source vertex, call a function which takes
   * in the target vertex and the weight of the edge for each outbound
   * edge from the source.
   */
  def foreachIncomingEdge(source:Int)(f:(Int,Int)=>Unit):Unit = {
    val start = verticesToIncoming(source * 2)
    if(start == -1) { return }
    val end = (verticesToIncoming(source * 2 + 1)*2) + start

    cfor(start)( _ < end, _ + 2 ) { i =>
      val edgeTarget = incomingEdges(i)
      f(incomingEdges(i),incomingEdges(i+1))
    }
  }
}

object PackedAnytimeEdges {
  def pack(vertices:Array[Vertex],vertexLookup:Map[Vertex,Int],unpacked:MutableGraph,mode:TransitMode) = {
    val vertexCount = vertices.length
    val packed = new PackedAnytimeEdges(vertexCount,unpacked.edgeCount(mode))

    // Pack edges
    var outgoingEdgesIndex = 0
    val incomingEdges = mutable.Map[Int,mutable.ListBuffer[(Int,Edge)]]()

    cfor(0)(_ < vertexCount, _ + 1) { i =>
      val v = vertices(i)
      val edgeCount = unpacked.edgeCount(v,mode)
      if(edgeCount == 0) {
        // Record empty vertex
        packed.verticesToOutgoing(i*2) = -1
        packed.verticesToOutgoing(i*2+1) = 0
      } else {
        // Set the edge index for this vertex
        packed.verticesToOutgoing(i*2) = outgoingEdgesIndex
        // Record the number of edge entries for this vertex
        packed.verticesToOutgoing(i*2+1) = edgeCount

        // Edges need to be sorted first by target and then by the thier start time.
        val edges =
          unpacked.edges(v)
            .filter(_.mode == mode)
            .toSeq
            .sortBy { e => (vertexLookup(e.target),-e.travelTime.toInt) }
            .toList
        
        if(edges.length != edgeCount) { sys.error("Unexpected edge count.") }

        var lastTarget = -1

        cfor(0)(_ < edgeCount, _ + 1) { j =>
          val e = edges(j)
          val t = vertexLookup(e.target)

          if(lastTarget != t) {
            packed.outgoingEdges(outgoingEdgesIndex) = t
            outgoingEdgesIndex += 1
            packed.outgoingEdges(outgoingEdgesIndex) = e.travelTime.toInt
            outgoingEdgesIndex += 1
            lastTarget = t
          }

          if(!incomingEdges.contains(t)) { incomingEdges(t) = mutable.ListBuffer[(Int,Edge)]() }
          incomingEdges(t) += ((i,e))
        }
      }
    }

    // Create incoming edges
    var incomingEdgesIndex = 0
    cfor(0)(_ < vertexCount, _ + 1) { v =>
      if(!incomingEdges.contains(v)) {
        packed.verticesToIncoming(v*2) = -1
        packed.verticesToIncoming(v*2+1) = 0
      } else {
        val edgeCount = incomingEdges(v).length
        val edges = incomingEdges(v).sortBy { t => (t._1,-t._2.time.toInt) }.toList

        packed.verticesToIncoming(v*2) = incomingEdgesIndex
        packed.verticesToIncoming(v*2+1) = edgeCount

        cfor(0)(_ < edgeCount, _ + 1) { i =>
          val tup = edges(i)
          val s = tup._1
          val e = tup._2
          packed.incomingEdges(incomingEdgesIndex) = s
          incomingEdgesIndex += 1
          packed.incomingEdges(incomingEdgesIndex) = e.travelTime.toInt
          incomingEdgesIndex += 1
        }
      }
    }
    packed
  }
}
