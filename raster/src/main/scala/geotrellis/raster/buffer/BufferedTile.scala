package geotrellis.raster.buffer

import geotrellis.raster.GridBounds

case class BufferedTile[T](tile: T, targetArea: GridBounds[Int])
