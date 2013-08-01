package geotrellis.network.graph

import geotrellis.network._

import spire.syntax._

import scala.collection.mutable

/**
 * A weighted, label based multi-graph.
 */
class TransitGraph(private val vertexMap:Array[Vertex],
                   private val locationToVertex:Map[Location,Int],
                   val edgeCount:Int) 
extends Serializable {

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
  val vertexCount = vertexMap.length
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
   * Edges with -2 start time must be in the beginning of the list
   * of edges to a specific target. Each edge to a specific target
   * are grouped together, with the edge start time increasing.
   * There is only one -2 start time edge allowed per target per vertex.
   * 
   * For example, for edges to vertices 5,6,7, the chain might be:
   * ... [ 5 | -2 | 100 ] | [ 5 | 3000 | 2000 ] | [ 6 | 1000 | 400 ] |
         [ 6 | 6000 | 234 ] | [ 7 | -2 | 41204 ] ...
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

    val end = vertices(source * 2 + 1) + start
    var target = -1

//    println(s"VERTEX START READING EDGES AT $start, READ ${end-start}")

    var anyTime = false
    var anyTimeWeight = 0
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
//          println(s"SWITCHING TO TARGET $edgeTarget")
          if(anyTime) {
            f(target,anyTimeWeight)
            anyTime = false
          }
          target = edgeTarget
        }

        val edgeTime = edges(i + 1)
        val edgeWeight = edges(i + 2)

        if(edgeTime == -2) {
          anyTime = true
          anyTimeWeight = edgeWeight
        } else if(edgeTime >= time) {
          skipUntilTargetChange = true
          val actualWeight = edges(i+2) + (edgeTime - time)
          if(anyTime) {
            if(actualWeight < anyTimeWeight) {
  //            println("Calling because anytime weight was greater than time weight")
              f(target,actualWeight)
            } else {
    //          println("Calling with anytime, weight was less than time weight")
              f(target,anyTimeWeight)
            }
            anyTime = false
//            target = edges(i)
          } else /*if(target == -1 || target != edges(i))*/ {
//            target = edges(i)
//            println(s"DEPARTURE TIME: ${Time(edgeTime)} vs ${Time(time)} (adds ${edgeTime - time} seconds)")
            f(edges(i),edges(i+2) + (edgeTime - time))
          }
        }
      }
    }

    if(anyTime) {
      // Call the previous target's anyTime edge
      f(target,anyTimeWeight)
    }
  }

  def foreach(f:Int=>Unit) = 
    cfor(0)(_<vertexCount, _+1)(f)

  def location(v:Int) = vertexMap(v).location
  def vertexAt(l:Location) = locationToVertex(l)
  def vertexFor(v:Int) = vertexMap(v)

  override
  def toString() = {
    var s = "(PACKED)\n"
    s += "Vertex\t\tEdges\n"
    s += "---------------------------------\n"
    cfor(0)( _ < vertexCount, _ + 1) { v =>
      val edgeStrings = mutable.Set[String]()
      s += s"$v (at ${location(v)})\t\t"
      val start = vertices(v * 2)
      if(start == -1) {
        s += "\n"
      } else {                   
        val end = vertices(v * 2 + 1) + start
        cfor(start)(_ < end, _ + 3) { i =>
          edgeStrings += s"(${edges(i)},${edges(i+1)},${edges(i+2)})"
                                   }
        s += edgeStrings.mkString(",") + "\n"
      }
    }
    s
  }
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

    val packed = new TransitGraph(vertexMap,locations.toMap,unpacked.edgeCount)
    
    // Pack edges
    var edgesIndex = 0
    var doubleAnies = 0

    cfor(0)(_ < size, _ + 1) { i =>
      val v = vertices(i)
      val edgeCount = unpacked.edgeCount(v)
      if(edgeCount == 0) {
        // Record empty vertex
        packed.vertices(i*2) = -1
        packed.vertices(i*2+1) = 0
      } else {
        val v = vertexMap(i)

        // Set the edge index for this vertex
        packed.vertices(i*2) = edgesIndex
        // Record the number of edge entries for this vertex
        packed.vertices(i*2+1) = edgeCount*3

        // Edges need to be sorted first by target and then by the thier start time.
        val edges = 
          unpacked.edges(v)
                  .toSeq
                  .sortBy { e => (vertexLookup(e.target),e.time.toInt) }
                  .toList
 

        var anyTimeTargets = mutable.Set[Vertex]()
        var continue = false
       

        cfor(0)(_ < edgeCount, _ + 1) { i =>
          val e = edges(i)

          // Does this vertex already have an ANY edge to the target?
          val alreadyHasAny = anyTimeTargets.contains(e.target)
          if( !e.time.isAny ||
              !alreadyHasAny ) {
            // Record the target as it's integer mapping
            packed.edges(edgesIndex) = vertexLookup(e.target)
            edgesIndex += 1
            // Record the start time of the edge
            packed.edges(edgesIndex) = e.time.toInt
            edgesIndex += 1
            //Record the weight of the edge.
            packed.edges(edgesIndex) = e.travelTime.toInt
            edgesIndex += 1
          }
          if(e.time.toInt == -2) {
            // Only allow one anytime edge
            if(anyTimeTargets.contains(e.target)) {
              doubleAnies += 1
            } else { 
              anyTimeTargets += e.target 
            }
          }
        }
      }
    }

    if(doubleAnies > 0) {
      sys.error(s"There were $doubleAnies cases where there were mutliple AnyTime edges.")
    }
    packed
  }
}
