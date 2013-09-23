package geotrellis.util.srs

import geotrellis._

class NoTransformationException(src:SpatialReferenceSystem,target:SpatialReferenceSystem) 
    extends Exception(s"SpatialReferenceSystem ${src.name} has no logic to transform to ${target.name}")

abstract class SpatialReferenceSystem {
  val name:String

  def transform(x:Double,y:Double,targetSRS:SpatialReferenceSystem):(Double,Double)

  def transform(e:Extent,targetSRS:SpatialReferenceSystem):Extent = {
    val (xmin,ymin) = transform(e.xmin,e.ymin,targetSRS)
    val (xmax,ymax) = transform(e.xmax,e.ymax,targetSRS)
    Extent(xmin,ymin,xmax,ymax)
  }
}

object SpatialReferenceSystem {
  val originShift = 2 * math.Pi * 6378137 / 2.0
}

/**
 * WGS 84 Datum ESPG:4326
 * 
 * http://spatialreference.org/ref/epsg/4326/
 */
case object LatLng extends SpatialReferenceSystem {
  val name = "WGS84 Datum EPSG:4326"

  def transform(x:Double,y:Double,targetSRS:SpatialReferenceSystem) =
    targetSRS match {
      case WebMercator =>
        val mx = x * SpatialReferenceSystem.originShift / 180.0
        val my1 = ( math.log( math.tan((90 + y) * math.Pi / 360.0 )) / (math.Pi / 180.0) )
        val my = my1 * SpatialReferenceSystem.originShift / 180 
        (mx, my)
      case _ =>
        throw new NoTransformationException(this,targetSRS)
    }
}

/**
 * WGS 84 Web Mercator, Spherical Mercator, EPSG:900913, EPSG:3857
 * 
 * http://spatialreference.org/ref/sr-org/7483/
 */
case object WebMercator extends SpatialReferenceSystem {
  val name = "Spherical Mercator EPSG:900913"

  def transform(x:Double,y:Double,targetSRS:SpatialReferenceSystem) =
    targetSRS match {
      case LatLng =>
        val xlng = (x / SpatialReferenceSystem.originShift) * 180.0
        val ylat1 = (y / SpatialReferenceSystem.originShift) * 180.0
        
        val ylat = 180 / math.Pi * (2 * math.atan( math.exp( ylat1 * math.Pi / 180.0)) - math.Pi / 2.0)
        (xlng, ylat)
      case _ =>
        throw new NoTransformationException(this,targetSRS)
    }
}
