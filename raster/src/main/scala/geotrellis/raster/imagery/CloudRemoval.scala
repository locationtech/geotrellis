package geotrellis.raster.imagery

import geotrellis.raster.{MultiBandTile, Tile}
import geotrellis.raster.io.geotiff.{GeoTiffMultiBandTile, SingleBandGeoTiff}

import java.io.File
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import spire.syntax.cfor._

object Imagery {

  def cloudRemovalSingleBand(images: Seq[Tile]) : Tile = {
    val dummyTile = images(0)
    println(dummyTile.findMinMax)
    println(dummyTile.get(510, 140))

    val newTile = dummyTile.map((col, row, x) => images.minBy(y => y.get(col, row)).get(col, row))
    println(newTile.get(510, 140))
    newTile
  }

  def cloudRemovalMultiBand(images: Seq[MultiBandTile]): MultiBandTile = {

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

    /* Return a MultiBand Tile here */
  }

  def main(args: Array[String]) : Unit = {
    val dir = new File(args(0))
    val fileList = dir.listFiles.filter(_.isFile).toList.toArray

    val numImages = fileList.length
    val images = new Array[Tile](numImages)

    cfor(0)(i => i < numImages, i => i + 1) { i =>
      images(i) = SingleBandGeoTiff(fileList(i).toString)
    }

    val cloudlessTile = cloudRemovalSingleBand(images)
    val crs = SingleBandGeoTiff(fileList(0).toString).crs
    val extent = SingleBandGeoTiff(fileList(0).toString).extent
    GeoTiffWriter.write(SingleBandGeoTiff(cloudlessTile, extent, crs), "/tmp/image.tif")
  }

}