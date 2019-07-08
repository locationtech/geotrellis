package geotrellis.raster.mapalgebra.focal


/** The Unit of measurement of a Tile. */
trait TileUnit

case object Feet extends TileUnit
case object Meters extends TileUnit
