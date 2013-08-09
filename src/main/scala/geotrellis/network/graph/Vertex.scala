package geotrellis.network.graph

import geotrellis.network._

import spire.syntax._

sealed abstract class VertexType

case object StationVertex extends VertexType {
  def apply(location:Location,name:String) = Vertex(location,name,StationVertex)
}

case object StreetVertex extends VertexType {
  def apply(location:Location,name:String) = Vertex(location,name,StreetVertex)
}

case class Vertex(location:Location, name:String, vertexType:VertexType) {
  override
  def toString = {
    s"V($name,$location)"
  }

  override 
  def hashCode = location.hashCode

  override 
  def equals(other: Any) = 
    other match { 
      case that: Vertex => this.location == that.location
      case _ => false 
    }
}
