package geotrellis.raster.op.local

import geotrellis._
import geotrellis.raster._
import geotrellis.logic.Collect

import spire.syntax.cfor._

/**
 * The mean of values at each location in a set of Rasters.
 */
object Mean extends Serializable {
  def apply(rs:Op[Raster]*)(implicit d:DI):Op[Raster] = apply(rs)

  def apply(rs:Seq[Op[Raster]]):Op[Raster] = 
    logic.Collect(rs).map { rs =>
      if(Set(rs.map(_.rasterExtent)).size != 1) {
        val rasterExtents = rs.map(_.rasterExtent).toSeq
        throw new GeoAttrsError("Cannot combine rasters with different raster extents." +
          s"$rasterExtents are not all equal")
      }

      val layerCount = rs.length
      if(layerCount == 0) {
        sys.error(s"Can't compute mean of empty sequence")
      } else {
        val newRasterType = rs.map(_.rasterType).reduce(_.union(_))
        val re = rs(0).rasterExtent
        val cols = re.cols
        val rows = re.rows
        val data = RasterData.allocByType(newRasterType,cols,rows)
        if(newRasterType.isDouble) {
          cfor(0)(_ < cols, _ + 1) { col =>
            cfor(0)(_ < rows, _ + 1) { row =>
              var count = 0
              var sum = 0.0
              cfor(0)(_ < layerCount, _ + 1) { i =>
                val v = rs(i).getDouble(col,row)
                if(isData(v)) { 
                  count += 1
                  sum += v
                }
              }

              if(count > 0) {
                data.setDouble(col,row,sum/count)
              } else {
                data.setDouble(col,row,Double.NaN)
              }
            }
          }
        } else {
          cfor(0)(_ < cols, _ + 1) { col =>
            cfor(0)(_ < rows, _ + 1) { row =>
              var count = 0
              var sum = 0
              cfor(0)(_ < layerCount, _ + 1) { i =>
                val v = rs(i).get(col,row)
                if(isData(v)) {
                  count += 1
                  sum += v
                }
              }
              if(count > 0) {
                data.set(col,row,sum/count)
              } else {
                data.set(col,row,NODATA)
              }
            }
          }
        }
        ArrayRaster(data,re)
      }
    }
    .withName("Mean")
}
