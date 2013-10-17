package geotrellis.raster

import geotrellis._

/**
 * This trait represents a raster data which represents a lazily-applied
 * cropping of an underlying raster data object.
 */
case class ResampledArrayRasterData(underlying:ArrayRasterData,
                                    src:RasterExtent,
                                    dst:RasterExtent) extends ArrayRasterData {
  def length = dst.cols * dst.rows
  def rows = dst.rows
  def cols = dst.cols

  // TODO: use a similar strategy to loadRaster() one to handle Doubles.
  def mutable:MutableRasterData = {
    val re = src

    // keep track of cell size in our source raster
    val src_cellwidth =  re.cellwidth
    val src_cellheight = re.cellheight
    val src_cols = re.cols
    val src_rows = re.rows
    val src_xmin = re.extent.xmin
    val src_ymin = re.extent.ymin
    val src_xmax = re.extent.xmax
    val src_ymax = re.extent.ymax

    // the dimensions to resample to
    val target = dst
    val dst_cols = target.cols
    val dst_rows = target.rows

    // calculate the dst cell size
    val dst_cellwidth  = (target.extent.xmax - target.extent.xmin) / dst_cols
    val dst_cellheight = (target.extent.ymax - target.extent.ymin) / dst_rows

    // save "normalized map coordinates" for destination cell (0, 0)
    val xbase = target.extent.xmin - src_xmin + (dst_cellwidth / 2)
    val ybase = target.extent.ymax - src_ymin - (dst_cellheight / 2)

    // track height/width in map units
    val src_map_width  = src_xmax - src_xmin
    val src_map_height = src_ymax - src_ymin
    val src_size = src_rows * src_cols
    
    // this is the resampled destination array
    val dst_size = dst_cols * dst_rows
    val arr = RasterData.emptyByType(getType, dst_cols, dst_rows)

    // these are the min and max columns we will access on this row
    val min_col = (xbase / src_cellwidth).asInstanceOf[Int]
    val max_col = ((xbase + dst_cols * dst_cellwidth) / src_cellwidth).asInstanceOf[Int]

    // start at the Y-center of the first dst grid cell
    var y = ybase

    // loop over rows
    var dst_row = 0
    while (dst_row < dst_rows) {

      // calculate the Y grid coordinate to read from
      val src_row = (src_rows - (y / src_cellheight).asInstanceOf[Int] - 1)

      // pre-calculate some spans we'll use a bunch
      val src_span = src_row * src_cols
      val dst_span = dst_row * dst_cols

      // xyz
      if (src_span + min_col < src_size && src_span + max_col >= 0) {

        // start at the X-center of the first dst grid cell
        var x = xbase
  
        // loop over cols
        var dst_col = 0
        while (dst_col < dst_cols) {
  
          // calculate the X grid coordinate to read from
          val src_col = (x / src_cellwidth).asInstanceOf[Int]
  
          // compute src and dst indices and ASSIGN!
          val src_i = src_span + src_col

          if (src_col >= 0 && src_col < src_cols && src_i < src_size && src_i >= 0) {
            val dst_i = dst_span + dst_col
            arr(dst_i) = underlying(src_i)
          }
  
          // increase our X map coordinate
          x += dst_cellwidth
          dst_col += 1
        }
      }

      // decrease our Y map coordinate
      y -= dst_cellheight
      dst_row += 1
    }

    arr
  }

  def force = mutable

  def getType = underlying.getType
  def copy = this
  def alloc(cols:Int, rows:Int) = underlying.alloc(cols, rows)

  def apply(i:Int) = get(i % cols, i / cols)
  def applyDouble(i:Int) = getDouble(i % cols, i / cols)

  final private val xbase = dst.extent.xmin - src.extent.xmin + (dst.cellwidth / 2)
  @inline final def srcCol(dstCol:Int) = {
    ((xbase + dstCol * dst.cellwidth) / src.cellwidth).toInt
  }

  final private val ybase = dst.extent.ymax - src.extent.ymin - (dst.cellheight / 2)
  @inline final def srcRow(dstRow:Int) = {
    src.rows - ((ybase - dstRow * dst.cellheight) / src.cellheight).toInt - 1
  }

  override def get(col:Int, row:Int):Int = {
    val scol = srcCol(col)
    if (scol < 0 || src.cols <= scol) return NODATA
    val srow = srcRow(row)
    if (srow < 0 || src.rows <= srow) return NODATA
    underlying.get(scol, srow)
  }

  override def getDouble(col:Int, row:Int):Double = {
    val scol = srcCol(col)
    if (scol < 0 || src.cols <= scol) return Double.NaN
    val srow = srcRow(row)
    if (srow < 0 || src.rows <= srow) return Double.NaN
    underlying.getDouble(scol, srow)
  }

  def foreach(f:Int => Unit) =
    for (c <- 0 until cols; r <- 0 until rows) f(get(c, r))

  def map(f:Int => Int) = LazyMap(this, f)
  def mapIfSet(f:Int => Int) = LazyMapIfSet(this, f)
  def combine(other:RasterData)(f:(Int, Int) => Int) = other match {
    case a:ArrayRasterData => LazyCombine(this, a, f)
    case o => o.combine(this)((z2, z1) => f(z1, z2))
  }

  def foreachDouble(f:Double => Unit) =
    for (c <- 0 until cols; r <- 0 until rows) f(getDouble(c, r))

  def mapDouble(f:Double => Double) = LazyMapDouble(this, f)
  def mapIfSetDouble(f:Double => Double) = LazyMapIfSetDouble(this, f)
  def combineDouble(other:RasterData)(f:(Double, Double) => Double) = other match {
    case a:ArrayRasterData => LazyCombineDouble(this, a, f)
    case o => o.combineDouble(this)((z2, z1) => f(z1, z2))
  }
}
