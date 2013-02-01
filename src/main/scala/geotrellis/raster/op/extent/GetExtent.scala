package geotrellis.raster.op.extent

import geotrellis._

/**
 * Creates an extent out of boundry values.
 */
case class GetExtent(xmin:Op[Double], ymin:Op[Double], xmax:Op[Double], ymax:Op[Double])
     extends Op4 (xmin, ymin, xmax, ymax) ({
       (x1,y1,x2,y2) => Result(Extent(x1,y1,x2,y2))
})

/**
  * Return a the smallest extent that contains this extent and the provided
  * extent. This is provides a union of the two extents.
 */
case class CombineExtents(e1:Op[Extent], e2:Op[Extent]) extends Op2(e1, e2)({
  (e1, e2) => Result(e1.combine(e2))
})
