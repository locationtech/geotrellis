package geotrellis.spark.buffer

import geotrellis.raster.GridBounds

case class BufferedTile[T](tile: T, targetArea: GridBounds)
