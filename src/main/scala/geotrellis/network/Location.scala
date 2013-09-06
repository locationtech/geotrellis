package geotrellis.network

import scala.collection.mutable

case class Location(lat:Double,long:Double)
object Location {
  implicit def location2Tuple(l:Location):(Double,Double) = 
    (l.lat,l.long)

  def apply(tup:(Double,Double)):Location = Location(tup._1,tup._2)
}

case class NamedLocation(name:String,location:Location)

class NamedLocations() extends Serializable {
  private val map = mutable.Map[Location,NamedLocation]()

  def +=(nl:NamedLocation) = { map(nl.location) = nl }

  def apply(l:Location) = map(l)

  def lookup(l:Location) = map.get(l)

  def findName(n:String) = 
    map.values.find(_.name == n)

  def foreach(f:NamedLocation=>Unit) = 
    map.values.foreach(f)

  def mergeIn(other:NamedLocations) = {
    for(nl <- other) { this += nl }
    this
  }
}

object NamedLocations {
  val EMPTY = new NamedLocations

  def apply(nls:Iterable[NamedLocation]) = {
    val result = new NamedLocations()
    for(nl <- nls) { result += nl }
    result
  }
}

case class NamedWay(name:String,start:Location,end:Location)

class NamedWays() extends Serializable {
  private val map = mutable.Map[(Location,Location),NamedWay]()

  def +=(nw:NamedWay) = { map((nw.start,nw.end)) = nw }

  def apply(start:Location,end:Location) = map((start,end))

  def lookup(start:Location,end:Location) = map.get((start,end))

  def foreach(f:NamedWay=>Unit) = 
    map.values.foreach(f)

  def mergeIn(other:NamedWays) = {
    for(nw <- other) { this += nw }
    this
  }
}

object NamedWays {
  val EMPTY = new NamedWays
}
