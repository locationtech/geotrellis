package geotrellis.data.geotiff

/**
 * SampleSize is used by geotiff.Settings to indicate how wide each sample
 * (i.e. each raster cell) will be. Currently we only support 8-, 16-, and
 * 32-bit samples, although other widths (including 1- and 64-bit samples)
 * could be supported in the future.
 */
sealed abstract class SampleSize(val bits:Int)
case object ByteSample extends SampleSize(8)
case object ShortSample extends SampleSize(16)
case object IntSample extends SampleSize(32)
