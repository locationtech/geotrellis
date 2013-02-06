package geotrellis.data.geotiff

/**
 * Compression is used by geotiff.Settings to indicate what kind of compression
 * to use.
 */
sealed abstract class Compression
case object Uncompressed extends Compression
case object Lzw extends Compression
