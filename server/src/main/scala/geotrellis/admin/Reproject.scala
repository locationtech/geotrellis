package geotrellis.admin

import geotrellis._
import geotrellis.feature._

import org.geotools.geometry.jts.JTS
import com.vividsolutions.jts.geom
import org.geotools.referencing.CRS
import org.opengis.referencing.crs.{CoordinateReferenceSystem => Crs}
import org.opengis.referencing.operation.MathTransform

import scala.collection.mutable

import com.vividsolutions.jts.{ geom => jts }

object Projections {
    val WebMercator = CRS.decode("EPSG:3857")

    val LatLong = CRS.decode("EPSG:4326")
}

object Reproject {
  private val geometryFactory = new geom.GeometryFactory()

  private val transformCache:mutable.Map[(Crs,Crs),MathTransform] = 
    new mutable.HashMap[(Crs,Crs),MathTransform]()
  
  def cacheTransform(crs1:Crs,crs2:Crs) = {
    transformCache((crs1,crs2)) = CRS.findMathTransform(crs1,crs2,true)
  }

  private def initCache() = {
    cacheTransform(Projections.LatLong,Projections.WebMercator)
    cacheTransform(Projections.WebMercator,Projections.LatLong)
  }

  initCache()

  def apply[D](feature:Geometry[D],fromCRS:Crs,toCRS:Crs):Geometry[D] = {
    if(!transformCache.contains((fromCRS,toCRS))) { cacheTransform(fromCRS,toCRS) }
    feature.mapGeom( g => 
      JTS.transform(g, transformCache((fromCRS,toCRS)))
    )
  }

  def apply[D](e:Extent,fromCRS:Crs,toCRS:Crs):Extent = {
    if(!transformCache.contains((fromCRS,toCRS))) { cacheTransform(fromCRS,toCRS) }    
    val min  = geometryFactory.createPoint(new geom.Coordinate(e.xmin,e.ymin));
    val max = geometryFactory.createPoint(new geom.Coordinate(e.xmax,e.ymax));
    val newMin = 
      JTS.transform(min, transformCache((fromCRS,toCRS))).asInstanceOf[geom.Point]
    val newMax = 
      JTS.transform(max, transformCache((fromCRS,toCRS))).asInstanceOf[geom.Point]
    val ne = Extent(newMin.getX(),newMin.getY(),newMax.getX(),newMax.getY())
    Extent(newMin.getX(),newMin.getY(),newMax.getX(),newMax.getY())
  }
}
