package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.raster._

import scala.collection.mutable

import spire.syntax._

/**
 * Given a raster, a raster representing it's zones, and a map
 * between Zone values and a reclassification value, sets all 
 * pixels within each zone to whatever new value, if any, has been 
 * explicitly chosen for the particular combination of existing 
 * values in that zone (regardless of the number of pixels 
 * holding each existing value).
 *
 * @note    ZonalClassification does not currently support Double raster data.
 *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *          the data values will be rounded to integers.
 */
case class ZonalClassification(r: Op[Raster], zones: Op[Raster])(zoneMap:Int=>Int) 
     extends Op3(r, zones, zoneMap) ({
  (r, zones, zoneMap) => 
    val (cols,rows) = (r.rasterExtent.cols,r.rasterExtent.rows)
    if(r.rasterExtent.cols != zones.rasterExtent.cols ||
       r.rasterExtent.rows != zones.rasterExtent.rows) {
      sys.error(s"The zone raster is not the same dimensions as the data raster.")
    }
    val data = IntArrayRasterData.empty(cols,rows)

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val v = r.get(col,row)
        val z = zones.get(col,row)
        data.set(col,row,zoneMap(z))
      }
    }

    Result(Raster(data,r.rasterExtent))
})
