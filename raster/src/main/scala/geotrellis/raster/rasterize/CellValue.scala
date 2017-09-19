package geotrellis.raster.rasterize

import geotrellis.raster.CellType


/** Cell value with its zindex and celltype to be used by the rasterizer. */
case class CellValue(value: Double, zindex: Double, celltype: CellType)
