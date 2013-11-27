package geotrellis.util.srs

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
