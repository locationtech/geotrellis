package geotrellis.raster

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import org.tukaani.xz._

object XZCompressor extends Compressor {

  override def compress(tile: Tile): Array[Byte] = {
    val in = tile.toBytes
    val baos = new ByteArrayOutputStream
    val xzos = new XZOutputStream(baos, new LZMA2Options)

    xzos.write(in, 0, in.size)

    xzos.close
    baos.close

    baos.toByteArray
  }

}

object XZDecompressor extends Decompressor {

  override def decompress(
    in: Array[Byte],
    cellType: CellType,
    cols: Int,
    rows: Int): Tile = {
    val baos = new ByteArrayOutputStream(in.size * 2)
    val is = new ByteArrayInputStream(in)
    val xzis = new XZInputStream(is)

    val buff = Array.ofDim[Byte](1024)
    var c = 0
    while (c != -1) {
      c = xzis.read(buff)
      if (c != -1) baos.write(buff, 0, c)
    }

    xzis.close
    is.close

    baos.close

    val out = baos.toByteArray
    ArrayTile.fromBytes(out, cellType, cols, rows)
  }

}

class XZCompressedTile(tile: Tile) extends CompressedTile(
  tile,
  XZCompressor,
  XZDecompressor
)
