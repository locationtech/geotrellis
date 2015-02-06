package geotrellis.raster

package object compression {
  implicit class TileCompressor(tile: Tile) {
    def compress: CompressedTile = compress()

    def compress(compression: TileCompression = Zip): CompressedTile =
      CompressedTile(tile, compression)
  }
}
