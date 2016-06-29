package geotrellis.vectortile.protobuf

import java.nio.file.{ Files, Paths }
import vector_tile.vector_tile.Tile

// --- //

// TODO Make this a trait?
object Protobuf {
  def decode(bytes: Array[Byte]): Tile = Tile.parseFrom(bytes)

  def encode(tile: Tile): Array[Byte] = tile.toByteArray

  def decodeIO(file: String): Tile = {
    decode(Files.readAllBytes(Paths.get(file)))
  }

  def encodeIO(tile: Tile, file: String) = {
    Files.write(Paths.get(file), encode(tile))
  }
}
