package geotrellis.raster.op.extent

import geotrellis._

/**
 *
 */
case class GetExtent(xmin:Op[Double], ymin:Op[Double], xmax:Op[Double], ymax:Op[Double])
extends Op4 (xmin, ymin, xmax, ymax) ((x1,y1,x2,y2) => Result(Extent(x1,y1,x2,y2)))

case class CombineExtents(e1:Op[Extent], e2:Op[Extent])
extends Op2(e1, e2)((e1, e2) => Result(e1.combine(e2)))
