package geotrellis.operation

import geotrellis._
import geotrellis.process._
import geotrellis.IntRaster

object HillshadeHelper {
  def hillshade(zenith:Double, slope:Double, azimuth:Double, aspect:Double):Int = {
    val h = 255 * ((math.cos(zenith) * math.cos(slope)) + 
                   (math.sin(zenith) * math.sin(slope) * math.cos(azimuth-aspect)))
    if (h < 0) {
      0
    } else {
      math.round(h).toInt
    }
  }

  // calculate zenith, in radians
  def zenith(altitude:Double) = (90 - altitude) * math.Pi / 180

  def azimuth(azimuth_degrees:Double) = {
    // convert from compass degrees to standard degrees 
    val a1 = 270.0 - azimuth_degrees 
    val a2 = if (a1 >= 360.0) { a1 - 360.0 } else { a1 }
    // convert to radians
    a2 * math.Pi / 180.0
  } 
}

/**
 * Hillshade creates a raster that, visually, adds a three dimensional appearance to an elevation raster.
 *
 * @param r elevation raster, with elevation in map units (if web mercator, meters)
 * @param altitude  altitude of illumination, angle above horizon, in degrees
 * @param azimuth direction of illumination, in compass degrees
 * 
 */
 // TODO: add optional shadows, zfactor to scale altitude
case class Hillshade(r:Op[IntRaster], azimuth:Op[Double], altitude:Op[Double]) 
  extends Op3(r, azimuth, altitude) ({
  (r,azimuthCompass, altitude) => {
    val zenith = HillshadeHelper.zenith(altitude)
    val azimuth = HillshadeHelper.azimuth(azimuthCompass)

//    println ("rows: %d".format(r.rasterExtent.rows))
 //   println ("cols: %d".format(r.rasterExtent.cols))
     
    val data = Array.fill(r.rasterExtent.rows * r.rasterExtent.cols)(NODATA)

    // skip border cells
    for (y <- 1 until r.rows - 1; x <- 1 until r.cols - 1) {
        val h = if (r.data(y * r.cols + x) == NODATA) {
           NODATA 
        } else {
          // using simple horizontal rate of change, as opposed to weighting neighbors (corner + 2mid + corner)
          val dx = (r.data(y * r.cols + x + 1) - r.data(y * r.cols + x - 1)) / (2 * r.rasterExtent.cellwidth)
          val dy = (r.data((y - 1) * r.cols + x) - r.data((y + 1) * r.cols + x)) / (2 * r.rasterExtent.cellheight)
          val slope = math.atan(math.sqrt(math.pow(dx, 2) + math.pow(dy,2))) // add z factor (multiplied in) if values are not in map units
          val aspect = if (dx != 0) {
            val aspect1 = math.atan2(dy, 0-dx) 
            if (aspect1 < 0) { (math.Pi * 2) + aspect1 } else { aspect1 }
          } else {
            if (dy > 0) {
              math.Pi * 2
            } else {
              if (dy < 0) { math.Pi * 2 - math.Pi / 2 } else { 0 } // not sure about this final result, same as last cell?
            }  
          }
          HillshadeHelper.hillshade(zenith, slope, azimuth, aspect)
        }
        data(y * r.rasterExtent.cols + x) = h  
      }
    Result(IntRaster(data, r.rasterExtent))
  }
}) 
