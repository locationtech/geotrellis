package geotrellis

import scala.math.{min, max, round, ceil, floor}

case class GeoAttrsError(msg:String) extends Exception(msg)

/**
 * RasterExtent objects represent the geographic extent (envelope) of a raster.
 */
case class RasterExtent(extent:Extent, cellwidth:Double, cellheight:Double, cols:Int, rows:Int) {

  if (cellwidth  <= 0.0) throw GeoAttrsError("invalid cell-width")
  if (cellheight <= 0.0) throw GeoAttrsError("invalid cell-height")
  if (cols <= 0) throw GeoAttrsError("invalid cols")
  if (rows <= 0) throw GeoAttrsError("invalid rows")

  val width  = extent.width
  val height = extent.height

  /**
   * The size of the extent, e.g. cols * rows.
   */
  def size = cols * rows

  ///**
  // * Create a copy of this instance
  // */
  //def copy = RasterExtent(extent, cellwidth, cellheight, cols, rows)

  /**
   * Compare this object with another GeoAttrs object, as per the comparison
   * rules in Extent#compare.
   */
  def compare(other:RasterExtent) = extent.compare(other.extent)

  /**
   * Determine if the underlying extent contains the given point.
   */
  def containsPoint(x:Double, y:Double) = extent.containsPoint(x, y)
  
  /**
   * Convert map coordinates (x,y) to grid coordinates (col,row).
   */
  def mapToGrid(x:Double, y:Double) = {
    val col = ((x - extent.xmin) / cellwidth).toInt
    val row = ((extent.ymax - y) / cellheight).toInt
    (col, row)
  }

  /**
   * Convert map coordinate tuple (x,y) to grid coordinates (col,row).
   */
  def mapToGrid(mapCoord:(Double,Double)):(Int,Int) = {
    val (x,y) = mapCoord;
    mapToGrid(x,y)
  }

  // TODO: try to remove calls to max and min if possible
  /**
    * The map coordinate of a grid cell is the center point.
    */  
  def gridToMap(col:Int, row:Int) = {
    val x = max(min(col * cellwidth + extent.xmin + (cellwidth / 2), extent.xmax), extent.xmin)
    val y = min(max(extent.ymax - (row * cellheight) - (cellheight / 2), extent.ymin), extent.ymax)
    (x, y)
  }

  def mapToGrid2(x:Double, y:Double) = {
    val col = round((x - extent.xmin) / cellwidth).toInt
    val row = round((y - extent.ymin) / cellheight).toInt
    (col, row)
  }

  //def gridToMap2(col:Int, row:Int) = {
  //  val x = col * cellwidth + extent.xmin + (cellwidth / 2)
  //  val y = extent.ymax - row * cellheight - (cellheight / 2) 
  //  (x, y)
  //}

  /**
   * Combine two different GeoAttrs (which must have the same cellsizes).
   * The result is a new extent at the same resolution.
   *
   * TODO: this version currently warps the grid. we need two versions.
   *
   * TODO: relatedly, the translate version should require the grids to be
   * properly aligned.
   */
  def combine (that:RasterExtent) = {
    if (cellwidth != that.cellwidth)
      throw GeoAttrsError("illegal cellwidths: %s and %s".format(cellwidth, that.cellwidth))
    if (cellheight != that.cellheight)
      throw GeoAttrsError("illegal cellheights: %s and %s".format(cellheight, that.cellheight))

    val newExtent = extent.combine(that.extent)
    val newRows = ceil(newExtent.height / cellheight).toInt
    val newCols = ceil(newExtent.width / cellwidth).toInt

    RasterExtent(newExtent, cellwidth, cellheight, newCols, newCols)
  }
}
