package geotrellis.raster.op

import geotrellis._

case class ConvertType(r:Op[Raster], t:Op[RasterType]) extends Op2(r, t)({
  (r, t) => Result(r.convert(t))
})
