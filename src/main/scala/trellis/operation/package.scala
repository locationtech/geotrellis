package trellis

import trellis.geometry.Polygon
import trellis.raster.IntRaster

package object operation {
  type Op[A] = Operation[A]

  type SimpleOp[A] = SimpleOperation[A]
  type CachedOp[A] = CachedOperation[A]

  /* Provide some handy aliases for various Operation[T] types. */
  type PolygonOperation = Op[Polygon]
  type IntRasterOperation = Op[IntRaster]
  type RasterExtentOperation = Op[RasterExtent]
  type PNGOperation = Op[Array[Byte]]

  import trellis.operation.Literal.implicitLiteral

  type Callback = (Any) => Any
}
