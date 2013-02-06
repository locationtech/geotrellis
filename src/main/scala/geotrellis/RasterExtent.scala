package geotrellis

import scala.math.{min, max, round, ceil, floor}

case class GeoAttrsError(msg:String) extends Exception(msg)

/**
 * Represents grid coordinates of a subsection of a RasterExtent.
 * These coordinates are inclusive.
 */
case class GridBounds(colMin:Int,rowMin:Int,colMax:Int,rowMax:Int)

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
   * Convert map coordinate x to grid coordinate column.
   */
  def mapXToGrid(x:Double) = ((x - extent.xmin) / cellwidth).toInt
  
  def mapXToGridDouble(x:Double) = (x - extent.xmin) / cellwidth

    
  /**
   * Convert map coordinate y to grid coordinate row.
   */
  def mapYToGrid(y:Double) = ((extent.ymax - y) / cellheight).toInt
  
  /**
   * 
   */
  def mapYToGridDouble(y:Double) = ((extent.ymax) - y ) / cellheight
  
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

  /**
   * Gets the GridBounds for this RasterExtent that is the smallest subgrid
   * containing all points within the extent. The extent is considered inclusive
   * on it's north and west borders, exclusive on it's east and south borders.
   * See [[RasterExtent]] for a discussion of grid and extent boundry concepts.
   */
  def gridBoundsFor(subExtent:Extent):GridBounds = {
    if(!extent.containsExtent(subExtent)) { throw ExtentRangeError("") }
    // West and North boundrys are a simple mapToGrid call.
    val (colMin,rowMin) = mapToGrid(subExtent.xmin, subExtent.ymax)

    // If South East corner is on grid border lines, we want to still only include
    // what is to the West and\or North of the point. However if the border point
    // is not directly on a grid division, include the whole row and/or column that
    // contains the point.
    val colMax = ceil((subExtent.xmax - extent.xmin) / cellwidth).toInt - 1
    val rowMax = ceil((extent.ymax - subExtent.ymin) / cellheight).toInt - 1
    
    GridBounds(max(0,colMin),
               max(0,rowMin),
               min(cols-1,colMax),
               min(rows-1,rowMax))
  }
  
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
