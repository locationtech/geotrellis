package geotrellis.util.srs

import geotrellis._
import geotrellis.feature.Feature.factory
import com.vividsolutions.jts.geom._
import scala.collection.JavaConversions._

class NoTransformationException(src:SpatialReferenceSystem,target:SpatialReferenceSystem) 
    extends Exception(s"SpatialReferenceSystem ${src.name} has no logic to transform to ${target.name}")

/** Spatial Reference System (SRS) */
abstract class SpatialReferenceSystem {
  val name:String

  def transform(x:Double,y:Double,targetSRS:SRS):(Double,Double)

  def transform(e:Extent,targetSRS:SRS):Extent = {
    val (xmin,ymin) = transform(e.xmin,e.ymin,targetSRS)
    val (xmax,ymax) = transform(e.xmax,e.ymax,targetSRS)
    Extent(xmin,ymin,xmax,ymax)
  }

  def transform(c:Coordinate,targetSRS:SRS):Coordinate = {
    val (x,y) = transform(c.x,c.y,targetSRS)
    new Coordinate(x,y)
  }

  def transform(p:Point,targetSRS:SRS):Point =
    factory.createPoint(transform(p.getCoordinate,targetSRS))

  def transform(mp:MultiPoint,targetSRS:SRS):MultiPoint = {
    val len = mp.getNumGeometries
    val transformedPoints = 
      (for(i <- 0 until len) yield { 
        transform(mp.getGeometryN(i).asInstanceOf[Point], targetSRS) 
      }).toArray
    factory.createMultiPoint(transformedPoints)
  }

  def transform(ls:LineString,targetSRS:SRS):LineString =
    factory.createLineString(ls.getCoordinateSequence
                               .toCoordinateArray
                               .map(transform(_,targetSRS)).toArray)

  def transform(lr:LinearRing,targetSRS:SRS):LinearRing =
    factory.createLinearRing(lr.getCoordinateSequence
                               .toCoordinateArray
                               .map(transform(_,targetSRS)).toArray)

  def transform(p:Polygon,targetSRS:SRS):Polygon = {
    val exterior = transform(p.getExteriorRing.asInstanceOf[LinearRing],targetSRS)
    val interiorRings = {
      val len = p.getNumInteriorRing
      (for(i <- 0 until len) yield {
        transform(p.getInteriorRingN(i).asInstanceOf[LinearRing],targetSRS)
      }).toArray
    }

    factory.createPolygon(exterior,interiorRings)
  }

  def transform(mp:MultiPolygon,targetSRS:SRS):MultiPolygon = {
    val len = mp.getNumGeometries
    val transformedPolys = 
      (for(i <- 0 until len) yield { 
        transform(mp.getGeometryN(i).asInstanceOf[Polygon], targetSRS) 
      }).toArray
    factory.createMultiPolygon(transformedPolys)
  }

  def transform(g:Geometry,targetSRS:SRS):Geometry =
    g match {
      case point:Point               => transform(point,targetSRS)
      case polygon:Polygon           => transform(polygon,targetSRS)
      case multiPoint:MultiPoint     => transform(multiPoint,targetSRS)
      case multiPolygon:MultiPolygon => transform(multiPolygon,targetSRS)
      case line:LineString           => transform(line,targetSRS)
      case multiLine:MultiLineString => transform(multiLine,targetSRS)
      case gc:GeometryCollection     => transform(gc,targetSRS)
      case _                         => sys.error(s"Unknown geometry: ${g.getGeometryType}")
    }

  def transform(gc:GeometryCollection,targetSRS:SRS):GeometryCollection = {
    val len = gc.getNumGeometries
    val transformedGeoms = 
      (for(i <- 0 until len) yield { 
        transform(gc.getGeometryN(i), targetSRS) 
      }).toArray
    factory.createGeometryCollection(transformedGeoms)
  }
}

object SpatialReferenceSystem {
  val originShift = 2 * math.Pi * 6378137 / 2.0
}

