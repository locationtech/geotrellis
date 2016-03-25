package geotrellis.raster

import geotrellis.vector.Extent


/**
  * A case class containing the width and height of a cell.
  *
  * @param  width   The width of a cell
  * @param  height  The height of a cell
  */
case class CellSize(width: Double, height: Double) {
  def resolution: Double = math.sqrt(width*height)
}

/**
  * The companion object for the [[CellSize]] type.
  */
object CellSize {

  /**
    * Create a new [[CellSize]] from an extent, a number of columns,
    * and a number of rows.
    *
    * @param   extent  The extent, which provides an overall height and width
    * @param   cols    The number of columns
    * @param   rows    The number of rows
    * @return          The CellSize
    */
  def apply(extent: Extent, cols: Int, rows: Int): CellSize =
    CellSize(extent.width / cols, extent.height / rows)

  /**
    * Create a new [[CellSize]] from an extent, a number of columns,
    * and a number of rows.
    *
    * @param   extent  The extent, which provides an overall height and width
    * @param   dims    The numbers of columns and rows as a tuple
    * @return          The CellSize
    */
  def apply(extent: Extent, dims: (Int, Int)): CellSize = {
    val (cols, rows) = dims
    apply(extent, cols, rows)
  }

  /**
    * Create a new [[CellSize]] from a string containing the width and
    * height separated by a comma.
    *
    * @param   s  The string
    * @return     The CellSize
    */
  def fromString(s:String) = {
    val Array(width, height) = s.split(",").map(_.toDouble)
    CellSize(width, height)
  }
}
