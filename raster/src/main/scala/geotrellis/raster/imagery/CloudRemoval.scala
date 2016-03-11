package geotrellis.raster.imagery

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.io.geotiff.SinglebandGeoTiff

import java.io.File
import spire.syntax.cfor._

object CloudRemoval {

  def cloudRemovalSingleband(images: Array[Tile], threshold: Int) : Tile = {
    val headImage = images(0)
    val result = ArrayTile.empty(headImage.cellType, headImage.cols, headImage.rows)

    cfor(0)(_ < result.rows, _ + 1) { row =>
      cfor(0)(_ < result.cols, _ + 1) { col =>
        var sum = 0
        var count = 0
        cfor(0)(_ < images.length, _ + 1) { i =>
          val v = images(i).get(col, row)
          if(isData(v) && v < threshold) {
            sum += v
            count += 1
          }
        }
        
        result.set(col, row, sum / count)
      }
    }

    result
  }

  def cloudRemovalMultiband(images: Array[MultibandTile], threshold: Int): MultibandTile = {
    val numBands = images(0).bandCount
    val numImages = images.length

    val cloudlessTiles = new Array[Tile](numBands)

    cfor(0)(i => i < numBands, i => i + 1) { i =>
      val singleTiles = new Array[Tile](numImages)

      cfor(0)(j => j < numImages, j => j + 1) { j =>
        singleTiles(j) = images(j).band(i)
      }
      cloudlessTiles(i) = cloudRemovalSingleband(singleTiles, threshold)
    }

    ArrayMultibandTile(cloudlessTiles)
  }

  def cloudRemovalMultiband(images: Array[MultibandTile]): MultibandTile = {
    cloudRemovalMultiband(images, 10000)
  }

}
