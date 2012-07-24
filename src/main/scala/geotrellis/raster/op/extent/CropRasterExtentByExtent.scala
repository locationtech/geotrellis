package geotrellis.raster.op.extent

import geotrellis._

/**
 * Given a geographical extent and grid height/width, return an object used to
 * load raster data.
 */
case class CropRasterExtentByExtent(g:Op[RasterExtent], e:Op[Extent])
extends Op2(g,e) ({
  (geo,ext) => {
    val cols = ((ext.ymax - ext.ymin) / geo.cellheight).toInt
    val rows = ((ext.xmax - ext.xmin) / geo.cellwidth).toInt
    Result(RasterExtent(ext, geo.cellwidth, geo.cellheight, cols, rows))
  }
})
