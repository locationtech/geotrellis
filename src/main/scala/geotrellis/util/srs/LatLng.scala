package geotrellis.util.srs

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
