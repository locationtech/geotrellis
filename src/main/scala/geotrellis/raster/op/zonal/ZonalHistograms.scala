package geotrellis.raster.op.zonal

import geotrellis._
import geotrellis._
import geotrellis.process._
import geotrellis.statistics._

/**
 * Given a raster, return a histogram summary of the cells within each zone.
 *
 * @note    ZonalHistorgram does not currently support Double raster data.
 *          If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *          the data values will be rounded to integers.
 */
case class ZonalHistogram(data: Op[Raster], zones: Op[Raster],
                           zonesArraySize: Op[Int], histArraySize: Op[Int]) 
     extends Op4(data, zones, zonesArraySize, histArraySize) ({
  (raster, zones, zonesArraySize, histArraySize) => {
    // build our map to hold results
    val histmap = Array.ofDim[Histogram](zonesArraySize)
    for(i <- 0 until histmap.length) {
      histmap(i) = ArrayHistogram(histArraySize)
    }

    // dereference some useful variables
    val geo   = raster.rasterExtent
    val rdata = raster.data.asArray.getOrElse(sys.error("need array"))
    val rows  = geo.rows
    val cols  = geo.cols

    // calculate the bounding box
    var xmin = geo.extent.xmin
    var ymin = geo.extent.ymin
    var xmax = geo.extent.xmax
    var ymax = geo.extent.ymax

    // save the bounding box as grid coordinates
    val (col1, row1) = geo.mapToGrid(xmin, ymin)
    val (col2, row2) = geo.mapToGrid(xmax, ymax)

    val zdata = zones.data.asArray.getOrElse(sys.error("need array"))

    // iterate over the cells in our bounding box; determine its zone, then
    // looking in the raster for a value to add to the zonal histogram.
    var row = row1
    while (row < row2) {
      var col = col1
      while (col < col2) {
        val i     = row * cols + col
        val value = rdata(i)
        if (value != NODATA) {
          val zone  = zdata(i)
          if (zone != NODATA) {
            val histogram = histmap(zone)
            histogram.countItem(value)
          }
        }
        col += 1
      }

      row += 1
    }

    // return an immutable mapping
    Result(histmap)
  }
})
