package geotrellis.io

import geotrellis._
import geotrellis.data.geotiff._

/**
  * Render a raster as a GeoTiff.
  */
case class RenderGeoTiff(r:Op[Raster], compression:Compression) extends Op1(r) ({
  r => {
    val settings = r.rasterType match {
      case TypeBit | TypeByte => Settings(ByteSample, Signed, true, compression)
      case TypeShort => Settings(ShortSample, Signed, true, compression)
      case TypeInt => Settings(IntSample, Signed, true, compression)
      case TypeFloat => Settings(IntSample, Floating, true, compression)
      case TypeDouble => Settings(LongSample, Floating, true, compression)
    }
    Result(Encoder.writeBytes(r, settings))
  }
})

