package geotrellis.raster.imagery

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.io.geotiff.SingleBandGeoTiff

import java.io.File
import spire.syntax.cfor._

object Imagery {

  def cloudlessValue(images: Array[Tile], col: Int, row: Int, threshold: Int): Int = {
    var sum = 0
    var count = 0
    cfor(0)(_ < images.size, _ + 1) { i =>
      val v = images(i).get(col, row)
      if(isData(v) && v < threshold) {
        sum += v
        count += 1
      }
    }
    (sum / count)
  }

  def cloudRemovalSingleBand(images: Array[Tile]) : Tile = {
    val dummyTile = images(0)
    val threshold = 10000

    dummyTile.map((col, row, x) => cloudlessValue(images, col, row, threshold))
  }

  def cloudRemovalMultiBand(images: Array[MultiBandTile]): MultiBandTile = {

    val numBands = images(0).bandCount
    val numImages = images.length

    val cloudlessTiles = new Array[Tile](numBands)

    cfor(0)(i => i < numBands, i => i + 1) { i =>
      val singleTiles = new Array[Tile](numImages)

      cfor(0)(j => j < numImages, j => j + 1) { j =>
        singleTiles(j) = images(j).band(i)
      }
      cloudlessTiles(i) = cloudRemovalSingleBand(singleTiles)
    }
    ArrayMultiBandTile(cloudlessTiles)
  }

  def resampleToByte(t: Tile, minVal: Int, maxVal: Int): Tile = {
    val byteTile = ByteArrayTile.empty(t.cols, t.rows)
    t.foreach { (col, row, z) =>
      val v = if(isData(z))
        ((z - minVal).toDouble/maxVal)*255
      else 0

      byteTile.set(col, row, v.toInt)
    }
    byteTile
  }

  def writePng(t: MultiBandTile, filename: String) = {

    assert(t.bandCount == 3)

    val imRows = t.rows
    val imCols = t.cols

    val redBand = t.band(0)
    val greenBand = t.band(1)
    val blueBand = t.band(2)

    val cloudlessRedByte = resampleToByte(redBand, redBand.findMinMax._1, redBand.findMinMax._2)
    val cloudlessGreenByte = resampleToByte(greenBand, greenBand.findMinMax._1, greenBand.findMinMax._2)
    val cloudlessBlueByte = resampleToByte(blueBand, blueBand.findMinMax._1, blueBand.findMinMax._2)

    val rgb = IntArrayTile(Array.ofDim[Int](imCols * imRows), imCols, imRows)

    cfor(0)(_ < imRows, _ + 1) { row =>
      cfor(0)(_ < imCols, _ + 1) { col =>
        var v = 0
        v = {
          val r = cloudlessRedByte.get(col, row)
          val g = cloudlessGreenByte.get(col, row)
          val b = cloudlessBlueByte.get(col, row)
          if (r == 0 && g == 0 && b == 0) 0xFF
          else {
            val cr = if (isNoData(r)) 128 else r.toByte & 0xFF
            val cg = if (isNoData(g)) 128 else g.toByte & 0xFF
            val cb = if (isNoData(b)) 128 else b.toByte & 0xFF

            (cr << 24) | (cg << 16) | (cb << 8) | 0xFF
          }
        }
        rgb.set(col, row, v)
      }
    }
    rgb.renderPng.write(filename)
  }

  def main(args: Array[String]) : Unit = {
    val dirRed = new File(args(0))
    val dirGreen = new File(args(1))
    val dirBlue = new File(args(2))

    val fileListRed = dirRed.listFiles.filter(_.isFile).toList.toArray
    val fileListGreen = dirGreen.listFiles.filter(_.isFile).toList.toArray
    val fileListBlue = dirBlue.listFiles.filter(_.isFile).toList.toArray

    val numImages = fileListRed.length

    assert(numImages == fileListBlue.length && numImages == fileListGreen.length)

    val multiBands = Array.ofDim[MultiBandTile](numImages)

    cfor(0)(_ < numImages, _ + 1) { i =>
      val red = SingleBandGeoTiff(fileListRed(i).toString).tile
      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile
      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile

      multiBands(i) = ArrayMultiBandTile(Array(red, green, blue))
    }

    val cloudless = cloudRemovalMultiBand(multiBands)
    writePng(cloudless, "/tmp/cloudless.png")
  }
}