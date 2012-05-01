package geotrellis.data.png

sealed abstract class Compression(val n:Byte)
case object Deflate extends Compression(0)
