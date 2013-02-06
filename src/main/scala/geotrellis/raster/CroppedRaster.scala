package geotrellis.raster

import scala.math.{ceil, floor}

import geotrellis._

object CroppedRaster {
  def findUpperLeft(re:RasterExtent, x:Double, y:Double):(Int, Int) = {
    val e = re.extent

    // xmin=0.0 cw=21.0 x=20.0 -> floor((20 - 0) / 21.0) -> floor(0.95) -> 0
    // xmin=0.0 cw=20.0 x=20.0 -> floor((20 - 0) / 20.0) -> floor(1) -> 1
    // xmin=0.0 cw=20.0 x=19.0 -> floor((19 - 0) / 20.0) -> floor(0.95) -> 0
    val col = floor((x - e.xmin) / re.cellwidth).toInt

    // ymax=100.0 ch=20.0 y=79.0 -> floor((100 - 79) / 20.0) -> floor(1.05) -> 1
    // ymax=100.0 ch=20.0 y=80.0 -> floor((100 - 80) / 20.0) -> floor(1) -> 1
    // ymax=100.0 ch=20.0 y=81.0 -> floor((100 - 81) / 20.0) -> floor(0.95) -> 0
    val row = floor((e.ymax - y) / re.cellheight).toInt

    (col, row)
  }

  def findLowerRight(re:RasterExtent, x:Double, y:Double):(Int, Int) = {
    val e = re.extent

    // xmin=0.0 cw=21.0 x=20.0 -> ceil((20 - 0) / 21.0) -> ceil(0.95) -> 1
    // xmin=0.0 cw=20.0 x=20.0 -> ceil((20 - 0) / 20.0) -> ceil(1) -> 1
    // xmin=0.0 cw=20.0 x=19.0 -> ceil((19 - 0) / 20.0) -> ceil(0.95) -> 1
    val col = ceil((x - e.xmin) / re.cellwidth).toInt

    // ymax=100.0 ch=20.0 y=79.0 -> ceil((100 - 79) / 20.0) -> ceil(1.05) -> 2
    // ymax=100.0 ch=20.0 y=80.0 -> ceil((100 - 80) / 20.0) -> ceil(1) -> 1
    // ymax=100.0 ch=20.0 y=81.0 -> ceil((100 - 81) / 20.0) -> ceil(0.95) -> 1
    val row = ceil((e.ymax - y) / re.cellheight).toInt

    (col, row)
  }

  def apply(r:Raster, e:Extent) = {
    // dereference some useful data from the input raster
    val src = r.rasterExtent
    val cw = src.cellwidth
    val ch = src.cellheight

    // translate the upper-left (xmin/ymax) and lower-right (xmax/ymin) points
    val (col1, row1) = src.mapToGrid(e.xmin,e.ymax) //findUpperLeft(src, e.xmin, e.ymax) // north-west
    val (col2, row2) = findLowerRight(src, e.xmax, e.ymin) // south-east

    // translate back into an extent which contains the input extent and is
    // aligned to the raster's grid.
    val xmin = src.extent.xmin + col1 * cw // west boundary
    val xmax = src.extent.xmin + col2 * cw // east boundary
    val ymin = src.extent.ymax - row2 * ch // south boundary
    val ymax = src.extent.ymax - row1 * ch // north boundary

    // figure out the grid dimensions of our cropped raster data
    val cols = col2 - col1
    val rows = row2 - row1

    val dst = RasterExtent(Extent(xmin, ymin, xmax, ymax), cw, ch, cols, rows)

    r.data match {
      case a:ArrayRasterData => 
        Raster(CroppedArrayRasterData(a, dst, col1, row1, cols, rows), dst)
      case t:TiledRasterData =>
        Raster(CroppedTiledRasterData(t, dst, col1, row1, cols, rows), dst)
      case d => sys.error("unsupported crop: %s" format d)
    }
  }
}
