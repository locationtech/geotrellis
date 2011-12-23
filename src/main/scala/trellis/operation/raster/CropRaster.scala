package trellis.operation

import scala.math.{ceil, floor, max, min}
import trellis.Extent
import trellis.RasterExtent
import trellis.raster.IntRaster
import trellis.constant.NODATA
import trellis.process.Server

// this operation doesn't currently work.
// we need to create unit tests and get it working.

// TODO: fix this

/**
  * Return a raster cropped to the given [[trellis.geoattrs.RasterExtent]].
  * N.B. Not currently working.
  */

case class CropRaster(r:IntRasterOperation, e:Op[Extent]) extends SimpleOp[IntRaster] {
                    
  def childOperations = List(r)

  def _value(server:Server) = {
    val raster = server.run(r)
    val geo = raster.rasterExtent
    //println(geo)

    val extent = server.run(e)
    val xmin = extent.xmin
    val xmax = extent.xmax
    val ymin = extent.ymin
    val ymax = extent.ymax

    // first we figure out which part of the current raster is desired
    val col1 = floor((xmin - geo.extent.xmin) / geo.cellwidth).toInt
    val col2 = ceil((xmax - geo.extent.xmin) / geo.cellwidth).toInt
    val cols = col2 - col1

    val row1 = floor((ymin - geo.extent.ymin) / geo.cellheight).toInt
    val row2 = ceil((ymax - geo.extent.ymin) / geo.cellheight).toInt
    val rows = row2 - row1

    // then we initialize a new array containing the data
    val olddata = raster.data
    printf("olddata-len = %d\n", olddata.length)

    val newdata = Array.fill(cols * rows)(NODATA)
    printf("newdata-len = %d\n", newdata.length)

    printf("row(%d - %d)\n", row1, row2)
    printf("col(%d - %d)\n", col1, col2)

    val startrow = max(row1, 0)
    val startcol = max(col1, 0)
    val endrow = min(row2, geo.rows)
    val endcol = min(col2, geo.cols)

    printf("c=(%d,%d,%d; %d-%d) r=(%d,%d,%d; %d-%d)\n".format(col1, col2, cols, startcol, endcol,
                                                              row1, row2, rows, startrow, endrow))

    for(row <- startrow until endrow) {
      val src_y = row * geo.cols
      val dst_y = (row - row1) * cols
      for(col <- startcol until endcol) {
        val old = olddata(src_y + col)
        newdata(dst_y + (col - col1)) = old
      }
    }

    val xmin2 = geo.extent.xmin + col1 * geo.cellwidth
    val xmax2 = geo.extent.xmin + col2 * geo.cellwidth
    val ymin2 = geo.extent.ymin + row1 * geo.cellheight
    val ymax2 = geo.extent.ymin + row2 * geo.cellheight

    val extent2 = Extent(xmin2, ymin2, xmax2, ymax2)
    val geo2 = RasterExtent(extent2, geo.cellwidth, geo.cellheight, cols, rows)
    IntRaster(newdata, rows, cols, geo2)
  }
}
