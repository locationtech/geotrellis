package geotrellis.network.graph

import geotrellis.network._
import geotrellis.network.graph._

import scala.collection.mutable

class MutableGraph() {
  private val locationsToVertices = mutable.Map[Location,Vertex]()

  private val edgeSets = mutable.Map[Vertex,EdgeSet]()

  def edgeCount(mode:TransitMode) = edgeSets.values.foldLeft(0)(_+_.edgeCount(mode))

  def edgeCount(v:Vertex,mode:TransitMode) = 
    edgeSets(v).edgeCount(mode)

  def edges(v:Vertex) =
    edgeSets(v)

  def vertexCount = locationsToVertices.size

  def addVertex(v:Vertex) = {
    locationsToVertices(v.location) = v
    edgeSets(v) = EdgeSet(v)
  }

  def +=(v:Vertex) =
    addVertex(v)

  def addWithEdges(v:Vertex,edgeSet:EdgeSet) = {
    locationsToVertices(v.location) = v
    edgeSets(v) = edgeSet
  }

  /** Does not check if target vertex is in the graph */
  def addEdge(source:Vertex,edge:Edge) = {
    edgeSets(source).addEdge(edge)
  }

  def vertices() = 
    locationsToVertices.values

  def vertexAtLocation(location:Location) = 
    locationsToVertices(location)

  def locations() = locationsToVertices.keys

  def contains(v:Vertex) = 
    edgeSets.contains(v)

  def pack():TransitGraph = {
    TransitGraph.pack(this)
  }

  override
  def toString = {
    var s = "(MUTABLE)\n"
    s += "Vertex\t\tEdges\n"
    s += "---------------------------------\n"
    for(v <- vertices) {
      val edgeStrings = mutable.Set[String]()
      s += s"$v\t\t"
      for(e <- edgeSets(v)) {
        edgeStrings += s"$e"
      }
      s += edgeStrings.mkString(",") + "\n"
    }
    s
  }
}

object MutableGraph {
  def apply() = new MutableGraph()

  def apply(vertices:Iterable[Vertex]) = {
    val g = new MutableGraph()
    for(v <- vertices) { g += v }
    g
  }

  def merge(g1:MutableGraph,g2:MutableGraph):MutableGraph = {
    val mg = MutableGraph()
    for(v <- g1.vertices) { mg.addWithEdges(v, g1.edges(v)) }
    for(v <- g2.vertices) { mg.addWithEdges(v, g2.edges(v)) }
    mg
  }
}
