package geotrellis.operation

import geotrellis.process._
import geotrellis.RasterExtent
import geotrellis.IntRaster
import geotrellis.data.IntRasterReader


//TODO: Is this really what this operation is doing?
/**
 * Crop a raster to a given extent, using a nearest neighbor algorithm to resample.
 */
case class WarpRaster(r:Op[IntRaster], e:Op[RasterExtent]) extends Op2(r,e) ({
  (raster,rasterExtent) => {
    // this object will read from a raster as a data source
    // (instead of using an arg32/tif/etc) to load a new
    // raster
    Result(IntRasterReader.read(raster, Option(rasterExtent)))
  }
})
