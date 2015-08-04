package geotrellis.raster.imagery

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.io.geotiff.{MultiBandGeoTiff, GeoTiffMultiBandTile, SingleBandGeoTiff}

import java.io.File
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import spire.syntax.cfor._

object Imagery {

  def cloudRemovalSingleBand(images: Seq[Tile]) : Tile = {
    val dummyTile = images(0)
    println(dummyTile.findMinMax)
    //println(dummyTile.get(510, 140))

    val newTile = dummyTile.map((col, row, x) => images.minBy(y => y.get(col, row)).get(col, row))
    //println(newTile.get(510, 140))
    newTile
  }

  def cloudRemovalMultiBand(images: Seq[MultiBandTile]): MultiBandTile = {

    val numBands = images.head.bandCount
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

  def main(args: Array[String]) : Unit = {
    val dirRed = new File(args(0))
    val dirGreen = new File(args(1))
    val dirBlue = new File(args(1))

    val fileListRed = dirRed.listFiles.filter(_.isFile).toList.toArray
    val fileListGreen = dirGreen.listFiles.filter(_.isFile).toList.toArray
    val fileListBlue = dirBlue.listFiles.filter(_.isFile).toList.toArray

    val numImages = fileListRed.length
    val multiBands = Array.ofDim[MultiBandTile](numImages)

    val crs = SingleBandGeoTiff(fileListRed(0).toString).crs
    val extent = SingleBandGeoTiff(fileListRed(0).toString).extent

    cfor(1)(_ < numImages, _ + 1) { i =>
      val red = SingleBandGeoTiff(fileListRed(i).toString).tile.rescale(0, 255).convert(TypeByte)
      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile.rescale(0, 255).convert(TypeByte)
      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile.rescale(0, 255).convert(TypeByte)

//      val red = SingleBandGeoTiff(fileListRed(i).toString).tile
//      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile
//      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile

      multiBands(i) = ArrayMultiBandTile(Array(red, green, blue))

      val rgb = IntArrayTile(Array.ofDim[Int](512 * 512), 512, 512)

      cfor(0)(_ < 512, _ + 1) { row =>
        cfor(0)(_ < 512, _ + 1) { col =>
          var v = 0
          var i = 0
          while (v == 0 && i < numImages) {
            v = {
              val r = red.get(col, row)
              val g = green.get(col, row)
              val b = blue.get(col, row)
              if (r == 0 && g == 0 && b == 0) 0
              else {
                val cr = if (isNoData(r)) 128 else r.toByte & 0xFF
                val cg = if (isNoData(g)) 128 else g.toByte & 0xFF
                val cb = if (isNoData(b)) 128 else b.toByte & 0xFF

                (cr << 24) | (cg << 16) | (cb << 8) | 0xFF
              }
            }
            i += 1
          }
          rgb.set(col, row, v)
        }
      }
      rgb.renderPng.write("/tmp/image" + i + ".png")
    }
    //val cloudless = cloudRemovalMultiBand(multiBands)
    //GeoTiffWriter.write(MultiBandGeoTiff(multiBands(i), extent, crs), "/tmp/image" + i + ".tif")
  }

  //      val cloudless = cloudRemovalMultiBand(multiBands)
  //      val r = cloudless.band(0).rescale(0, 255).convert(TypeByte)
  //      val g = cloudless.band(1).rescale(0, 255).convert(TypeByte)
  //      val b = cloudless.band(2).rescale(0, 255).convert(TypeByte)
  //      val finalimage = ArrayMultiBandTile(Array(r, g, b))
  //val crs = SingleBandGeoTiff(fileListRed(0).toString).crs
  //val extent = SingleBandGeoTiff(fileListRed(0).toString).extent
  //GeoTiffWriter.write(MultiBandGeoTiff(finalimage, extent, crs), "/tmp/image.tif") }

}