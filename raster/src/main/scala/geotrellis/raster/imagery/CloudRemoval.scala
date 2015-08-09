package geotrellis.raster.imagery

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.io.geotiff.{MultiBandGeoTiff, GeoTiffMultiBandTile, SingleBandGeoTiff}

import java.io.File
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.vector.Extent
import spire.syntax.cfor._

object Imagery {

  var minValueBands = new Array[Int](3)
  var maxValueBands = new Array[Int](3)

  def cloudlessValue(images: Array[Tile], col: Int, row: Int, threshold: Int): Int = {
    var sum = 0
    var count = 0
    cfor(0)(_ < images.size, _ + 1) { i =>
      val v = images(i).get(col, row)
      if(v < threshold) {
        sum += v
        count += 1
      }
    }

    (sum / count)
  }


  def cloudRemovalSingleBand(images: Array[Tile]) : Tile = {
    val dummyTile = images(0)
    val threshold = 10000
    //println(dummyTile.findMinMax)
    //println(dummyTile.get(510, 140))
    dummyTile.map((col, row, x) => cloudlessValue(images, col, row, threshold))
    //dummyTile.map((col, row, x) => images.minBy(y => y.get(col, row)).get(col, row))
  }

  def cloudRemovalMultiBand(images: Array[MultiBandTile], extent: Extent, crs: CRS): MultiBandTile = {

    val numBands = images(0).bandCount
    val numImages = images.length

    val cloudlessTiles = new Array[Tile](numBands)

    cfor(0)(i => i < numBands, i => i + 1) { i =>
      val (minVal, maxVal) = images(0).band(i).findMinMax

      minValueBands(i) = minVal
      maxValueBands(i) = maxVal

      val singleTiles = new Array[Tile](numImages)

      cfor(0)(j => j < numImages, j => j + 1) { j =>
        singleTiles(j) = images(j).band(i)

        val (minVal, maxVal) = singleTiles(j).findMinMax
        if(minVal < minValueBands(i))
          minValueBands(i) = minVal

        if(maxVal > maxValueBands(i))
          maxValueBands(i) = maxVal
      }
      cloudlessTiles(i) = cloudRemovalSingleBand(singleTiles)
      GeoTiffWriter.write(SingleBandGeoTiff(cloudlessTiles(i), extent, crs), "/tmp/cloudlessimage" + i + ".tif")
    }
    ArrayMultiBandTile(cloudlessTiles)
  }

  def maxAll(a: Int, b:Int, c:Int): Int = {
    if(a > b)
    {
      if(a > c)
        a
      else
        c
    }
    else
    {
      if(b > c)
        b
      else
        c
    }
  }

  def minAll(a: Int, b:Int, c:Int): Int = {
    if(a < b)
    {
      if(c < a)
        c
      else
        a
    }
    else
    {
      if(c < b)
        c
      else
        b
    }
  }

  def resampleToByte(t: Tile, maxOfAllBandsAllImages: Int): Tile = {
    val byteTile = ByteArrayTile.empty(t.cols, t.rows)
    t.foreach { (col, row, z) =>
      val v = if(isData(z))
        (z / maxOfAllBandsAllImages) * 255
      else 0

      byteTile.set(col, row, z)
    }
    byteTile
  }

  def main(args: Array[String]) : Unit = {
    val dirRed = new File(args(0))
    val dirGreen = new File(args(1))
    val dirBlue = new File(args(2))

    val fileListRed = dirRed.listFiles.filter(_.isFile).toList.toArray
    val fileListGreen = dirGreen.listFiles.filter(_.isFile).toList.toArray
    val fileListBlue = dirBlue.listFiles.filter(_.isFile).toList.toArray

    val numImages = fileListRed.length
    val multiBands = Array.ofDim[MultiBandTile](numImages)

    val crs = SingleBandGeoTiff(fileListRed(0).toString).crs
    val extent = SingleBandGeoTiff(fileListRed(0).toString).extent

    cfor(0)(_ < numImages, _ + 1) { i =>
//      val red = SingleBandGeoTiff(fileListRed(i).toString).tile.rescale(0, 255).convert(TypeByte)
//      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile.rescale(0, 255).convert(TypeByte)
//      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile.rescale(0, 255).convert(TypeByte)

//      val red = SingleBandGeoTiff(fileListRed(i).toString).tile.rescale(0, 255)
//      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile.rescale(0, 255)
//      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile.rescale(0, 255)

      val red = SingleBandGeoTiff(fileListRed(i).toString).tile
      val green = SingleBandGeoTiff(fileListGreen(i).toString).tile
      val blue = SingleBandGeoTiff(fileListBlue(i).toString).tile
      multiBands(i) = ArrayMultiBandTile(Array(red, green, blue))
    }
    
    val cloudless = cloudRemovalMultiBand(multiBands, extent, crs)

    val maxOfAllBands = maxAll(maxValueBands(0), maxValueBands(1), maxValueBands(2))
    val minOfAllBands = minAll(minValueBands(0), minValueBands(1), minValueBands(2))

    val cloudlessRedByte = resampleToByte(cloudless.band(0), maxOfAllBands)
    val cloudlessGreenByte = resampleToByte(cloudless.band(1), maxOfAllBands)
    val cloudlessBlueByte = resampleToByte(cloudless.band(2), maxOfAllBands)

    val cloudlessByte = ArrayMultiBandTile(cloudlessRedByte, cloudlessGreenByte, cloudlessBlueByte)

    val imRows = cloudless.rows
    val imCols = cloudless.cols
    val rgb = IntArrayTile(Array.ofDim[Int](imCols * imRows), imCols, imRows)

    cfor(0)(_ < imRows, _ + 1) { row =>
      cfor(0)(_ < imCols, _ + 1) { col =>
        var v = 0
        v = {
            val r = cloudlessByte.band(0).get(col, row)
            val g = cloudlessByte.band(1).get(col, row)
            val b = cloudlessByte.band(2).get(col, row)
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
    rgb.renderPng.write("/tmp/cloudlessimagergb.png")

//    GeoTiffWriter.write(SingleBandGeoTiff(cloudless.band(0), extent, crs), "/tmp/cloudlessimagered.tif")
//    GeoTiffWriter.write(SingleBandGeoTiff(cloudless.band(1), extent, crs), "/tmp/cloudlessimagegreen.tif")
//    GeoTiffWriter.write(SingleBandGeoTiff(cloudless.band(2), extent, crs), "/tmp/cloudlessimageblue.tif")
//    GeoTiffWriter.write(MultiBandGeoTiff(cloudless, extent, crs), "/tmp/cloudlessimagergb.tif")
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