package geotrellis.data.geotiff

/**
 * SampleFormat is used by geotiff.Settings to indicate how to interpret each
 * sample.
 *
 * For instance, an 8-bit sample whose value is 0xfe will be interpreted as
 * -2 with Signed, but at 254 with Unsigned.
 *
 * Floating is defined but not yet supported by geotiff.Encoder.
 */
sealed abstract class SampleFormat(val kind:Int)
case object Unsigned extends SampleFormat(Const.unsigned)
case object Signed extends SampleFormat(Const.signed)
case object Floating extends SampleFormat(Const.floating)
