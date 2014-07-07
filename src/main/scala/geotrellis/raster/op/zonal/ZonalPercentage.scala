package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis.raster._

import scala.collection.mutable

import spire.syntax.cfor._

/**
 * Given a raster and a raster representing it's zones, sets all pixels
 * within each zone to the percentage of those pixels having values equal 
 * to that of the given pixel.
 *
 * Percentages are integer values from 0 - 100.
 * 
 * @note    ZonalPercentage does not currently support Double raster data.
 *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *          the data values will be rounded to integers.
 */
case class ZonalPercentage(r: Op[Raster], zones: Op[Raster]) 
     extends Op2(r, zones) ({
  (r, zones) => 
    val zonesToValueCounts = mutable.Map[Int,mutable.Map[Int,Int]]()       
    val zoneTotals = mutable.Map[Int,Int]()

    val (cols,rows) = (r.rasterExtent.cols,r.rasterExtent.rows)
    if(r.rasterExtent.cols != zones.rasterExtent.cols ||
       r.rasterExtent.rows != zones.rasterExtent.rows) {
      sys.error(s"The zone raster is not the same dimensions as the data raster.")
    }

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val value = r.get(col,row)
        val zone = zones.get(col,row)

        if(!zonesToValueCounts.contains(zone)) { 
          zonesToValueCounts(zone) = mutable.Map[Int,Int]()
          zoneTotals(zone) = 0
        }
        zoneTotals(zone) += 1

        val valueCounts = zonesToValueCounts(zone)
        if(!valueCounts.contains(value)) {
          valueCounts(value) = 0
        }
        valueCounts(value) += 1
      }
    }

    val data = IntArrayRasterData.empty(cols,rows)

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val v = r.get(col,row)
        val z = zones.get(col,row)
        val count = zonesToValueCounts(z)(v)
        val total = zoneTotals(z)
        data.set(col,row,math.round((count/total.toDouble)*100).toInt)
      }
    }

    Result(Raster(data,r.rasterExtent))
})
