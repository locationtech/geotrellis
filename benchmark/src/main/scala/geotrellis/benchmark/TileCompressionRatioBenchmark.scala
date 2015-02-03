package geotrellis.benchmark

import geotrellis.raster._

import java.text.DecimalFormat

object TileCompressionRatioBenchmark extends GeoTrellisBenchmark {

  val compressions = Seq(Zip, RLE)

  val files = Seq(
    "SBN_farm_mkt",
    "SBN_recycle",
    "aspect-double",
    "mtsthelens_tiled"
  )

  val size = 256

  val df = new DecimalFormat("#.00")

  def main(args: Array[String]): Unit = run

  def run = {
    println("Starting compression ratio benchmark...")
    println()

    for (fileName <- files)
      for (compression <- compressions) {
        val tile = get(loadRaster(fileName, size, size))
        runCompression(fileName, tile, compression)
      }

    sys.exit(0)
  }

  def runCompression(name: String, tile: Tile, compression: TileCompression) = {
    val compressed = tile.compress(compression)
    val ratio = df.format(compressed.compressionRatio * 100)

    println(s"compression ratio for tile $name with cell type ${tile.cellType} and compression $compression is $ratio%")
  }

}
