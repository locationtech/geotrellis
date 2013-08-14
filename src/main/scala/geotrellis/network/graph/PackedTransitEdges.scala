package geotrellis.network.graph

import geotrellis.network._

import spire.syntax._

import scala.collection.mutable

class PackedTransitEdges(vertexCount:Int,val edgeCount:Int) extends Serializable {
  /**
   * 'verticesToOutgoing' is an array that is indexed by vertex id,
   * that contains two peices of information:
   * the start index of the 'outgoingEdges' array for
   * a given vertex, and the number of outbound
   * edges to read from that start index.
   *                 
   * ... [ i | n ] | [ i | n ] | [ i | n ] ...
   * where i = index in outgoingEdges array
   *       n = number of outgoingEdges to read
   */
  private val verticesToOutgoing = Array.ofDim[Int](vertexCount * 2)
 
  /**
   * 'outgoingEdges' is an array of that is indexed based
   * on the 'verticesToOutgoing' array, and contains three peices
   * of information about an edge: the target vertex, the
   * start time associated with this edge, and the weight
   * of the edge.
   *
   * ... [ v | t | w ] | [ v | t | w ] | [ v | t | w ] ...
   * where v = the connected vertex
   *       t = time edge can be traversed
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge,
   * plus the waiting time for that edge traversal to occur.
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val outgoingEdges = Array.ofDim[Int](edgeCount * 3)

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
   * on the 'verticesToIncoming' array, and contains three peices
   * of information about an edge: the target vertex, the
   * start time associated with this edge, and the weight
   * of the edge.
   * 
   * The order per target is by start time ascending.
   *
   * ... [ v | t | w ] | [ v | t | w ] | [ v | t | w ] ...
   * where v = the connected vertex
   *       t = time edge can be traversed
   *       w = weight of traversal
   * 
   * Weight is defined as time taken to traverse the given edge,
   * plus the waiting time for that edge traversal to occur.
   * 
   * Weight is defined as time taken to traverse the given edge.
   */
  val incomingEdges = Array.ofDim[Int](edgeCount * 3)

  /**
   * Given a source vertex, and a time, call a function which takes
   * in the target vertex and the weight of the edge for each outbound
   * edge from the source. The outbound edge is determined by the edge to
   * a target with the start time closest to but greater than the passed in
   * time
   */
  def foreachOutgoingEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    val start = verticesToOutgoing(source * 2)
    if(start == -1) { return }
    val end = (verticesToOutgoing(source * 2 + 1)*3) + start

    var target = -1
    var skipUntilTargetChange = false

    cfor(start)( _ < end, _ + 3 ) { i =>
      val edgeTarget = outgoingEdges(i)

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

        val edgeTime = outgoingEdges(i + 1)
        val edgeWeight = outgoingEdges(i + 2)

        if(edgeTime >= time) {
          skipUntilTargetChange = true
          val actualWeight = edgeWeight + (edgeTime - time)
          f(edgeTarget,actualWeight)
        }
      }
    }
  }

  /**
   * Given a source vertex, and a time, call a function which takes
   * in the target vertex and the weight of the edge for each incoming
   * edge to the source. The incoming edge is determined by the edge
   * with the latest start time such that the arrival time (edge start time
   * plus the edge weight) is before the passed in time.
   */
  def foreachIncomingEdge(source:Int,time:Int)(f:(Int,Int)=>Unit):Unit = {
    val start = verticesToIncoming(source * 2)
    if(start == -1) { return }
    val end = (verticesToIncoming(source * 2 + 1)*3) + start

    var target = -1
    var skipUntilTargetChange = false

    cfor(start)( _ < end, _ + 3 ) { i =>
      val edgeTarget = incomingEdges(i)

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

        val edgeTime = incomingEdges(i + 1)
        val edgeWeight = incomingEdges(i + 2)

        if(edgeTime + edgeWeight <= time) {
          skipUntilTargetChange = true
          val actualWeight = time - edgeTime
          f(edgeTarget,actualWeight)
        }
      }
    }
  }
}

object PackedTransitEdges {
  def pack(vertices:Array[Vertex],vertexLookup:Map[Vertex,Int],unpacked:MutableGraph,mode:TransitMode) = {
    val vertexCount = vertices.length
    val packed = new PackedTransitEdges(vertexCount,unpacked.edgeCount(mode))

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

        // Edges need to be sorted first by target and then by the their start time.
        val edges =
          unpacked.edges(v)
            .filter(_.mode == mode)
            .toSeq
            .sortBy { e => (vertexLookup(e.target),e.time.toInt) }
            .toList

        if(edges.length != edgeCount) { sys.error("Unexpected edge count for transit edges.") }
        
        cfor(0)(_ < edgeCount, _ + 1) { j =>
          val e = edges(j)
          val t = vertexLookup(e.target)

          packed.outgoingEdges(outgoingEdgesIndex) = t
          outgoingEdgesIndex += 1
          packed.outgoingEdges(outgoingEdgesIndex) = e.time.toInt
          outgoingEdgesIndex += 1
          packed.outgoingEdges(outgoingEdgesIndex) = e.travelTime.toInt
          outgoingEdgesIndex += 1

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
          packed.incomingEdges(incomingEdgesIndex) = e.time.toInt
          incomingEdgesIndex += 1
          packed.incomingEdges(incomingEdgesIndex) = e.travelTime.toInt
          incomingEdgesIndex += 1
        }
      }
    }
    packed
  }
}
