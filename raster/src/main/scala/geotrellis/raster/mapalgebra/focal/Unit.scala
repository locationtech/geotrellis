package geotrellis.raster.mapalgebra.focal


/** The Unit of measurement of a Tile. */
trait Unit

case object Feet extends Unit
case object Meters extends Unit
