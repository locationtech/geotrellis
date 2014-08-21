package geotrellis.proj4.proj

import org.osgeo.proj4j.proj._

import org.osgeo.proj4j.CRSFactory
import org.osgeo.proj4j.CoordinateReferenceSystem
import org.osgeo.proj4j.CoordinateTransform
import org.osgeo.proj4j.CoordinateTransformFactory
import org.osgeo.proj4j.ProjCoordinate
import org.osgeo.proj4j.util.ProjectionUtil

class ProjectionGridRoundTripper(cs: CoordinateReferenceSystem) {
  val ctFactory = new CoordinateTransformFactory
  val csFactory = new CRSFactory

  val WGS84_PARAM = "+title=long/lat:WGS84 +proj=longlat +datum=WGS84 +units=degrees"
  val WGS84 = csFactory.createFromParameters("WGS84", WGS84_PARAM)

  val gridSize = 4
  val debug = false

  val transInverse = ctFactory.createTransform(cs, WGS84)
  val transForward = ctFactory.createTransform(WGS84, cs)

  private var _transformCount = 0
  def transformCount = _transformCount
  
  def runGrid(tolerance: Double): (Boolean, (Double, Double, Double, Double)) = {
    val extent @ (minx, miny, maxx, maxy) = gridExtent(cs.getProjection())
    
    val p = new ProjCoordinate()
    val dx = (maxx - minx) / gridSize
    val dy = (maxy - miny) / gridSize
    for (ix <- 0 to gridSize) {
      for (iy <- 0 to gridSize) {
	p.x = 
          if(ix == gridSize )
	    maxx
	    else
              minx + ix * dx

	p.y = 
          if(iy == gridSize)
	    maxy
	  else
            miny + iy * dy
	
	if(!roundTrip(p, tolerance))
	  return (false, extent)
      }
    }
    return (true, extent)
  }
  
  private def roundTrip(p: ProjCoordinate, tolerance: Double): Boolean = {
    val p2 = new ProjCoordinate()
    val p3 = new ProjCoordinate()

    _transformCount += 1
    
    transForward.transform(p, p2)
    transInverse.transform(p2, p3)
    
    if (debug)
      System.out.println(ProjectionUtil.toString(p) + " -> " + ProjectionUtil.toString(p2) + " ->  " + ProjectionUtil.toString(p3))
    
    val dx = math.abs(p3.x - p.x)
    val dy = math.abs(p3.y - p.y)
    
    if (dx <= tolerance && dy <= tolerance) {
      true
    } else {
      System.out.println("FAIL: " + ProjectionUtil.toString(p) + " -> " + ProjectionUtil.toString(p2) + " ->  " + ProjectionUtil.toString(p3))
      false
    }
  }
  
  def gridExtent(proj: Projection): (Double, Double, Double, Double) = {
    // scan all lat/lon params to try and determine a reasonable extent    
    val lon = proj.getProjectionLongitudeDegrees()
    
    val latExtent = {
      val le1 = (Double.MaxValue, Double.MinValue)
      val le2 = updateLat(proj.getProjectionLatitudeDegrees(), le1)
      val le3 = updateLat(proj.getProjectionLatitude1Degrees(), le2)
      updateLat(proj.getProjectionLatitude2Degrees(), le3)
    }
    
    val centrex = lon
    var gridWidth = 10.0

    val centrey =
      if (latExtent._1 < Double.MaxValue && latExtent._2 > Double.MinValue) {
        // got a good candidate
        
        val dlat = latExtent._2 - latExtent._1
        if (dlat > 0) {
          gridWidth = 2 * dlat
        }

        (latExtent._2 + latExtent._1) /2
      } else {
        0.0
      }

    (centrex - gridWidth/2,
     centrey - gridWidth/2,
     centrex + gridWidth/2,
     centrey + gridWidth/2)
  }
  
  private def updateLat(lat: Double, latExtent: (Double, Double)): (Double, Double) =
    // 0.0 indicates not set (for most projections?)
    if (lat == 0.0) 
      latExtent
    else {
      (if (lat < latExtent._1) lat else latExtent._1,
       if (lat > latExtent._2) lat else latExtent._2)
    }
}
