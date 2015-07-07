package geotrellis.raster.io.geotiff.reader

import geotrellis.raster.{Tile, TypeInt}
import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff.compression.Decompressor
import geotrellis.raster.io.geotiff.writer.GeoTiffWriter
import geotrellis.raster.io.geotiff._
import geotrellis.testkit.TestEngine

import org.scalatest.FunSpec

class CloudRemoval extends FunSpec
    with GeoTiffTestUtils
    with TestEngine{

  /**** New tests ****/

  describe("Cloud removal from a Landsat geotiff") {

    /*
    it("should read/write 32-bit landsat 8 data correctly - Failing") {

      val imPathBlue = "/home/kamikaze/gsoc/cropped_images/image00/B2_cropped.TIF"
      val imPathGreen = "/home/kamikaze/gsoc/cropped_images/image00/B3_cropped.TIF"
      val imPathRed = "/home/kamikaze/gsoc/cropped_images/image00/B4_cropped.TIF"
      //val imPath = "/media/Media/GeoTIFF/image28/B2.TIF"
      val writePathBlue = "/home/kamikaze/gsoc/landsat8test_blue_cropped.TIF"
      val writePathGreen = "/home/kamikaze/gsoc/landsat8test_green_cropped.TIF"
      val writePathRed = "/home/kamikaze/gsoc/landsat8test_red_cropped.TIF"

      val image1 = SingleBandGeoTiff(imPathBlue)
      val image2 = SingleBandGeoTiff(imPathGreen)
      val image3 = SingleBandGeoTiff(imPathRed)
      //val newimage = SingleBandGeoTiff(writePath)
      //println(image.tile.findMinMax)
      //println(image.tile.cellType)
      //println(newimage.findMinMax)
      //println(newimage.cellType)
      //assertEqual(image.tile, newimage.tile)
      //image1.write(writePathBlue)
      //image2.write(writePathGreen)
      //image3.write(writePathRed)
      //GeoTiffWriter.write(SingleBandGeoTiff(image.convert(TypeInt), image.extent, image.crs), writePath)
      GeoTiffWriter.write(image1, writePathBlue)
      GeoTiffWriter.write(image2, writePathGreen)
      GeoTiffWriter.write(image3, writePathRed)
    }*/

    /*it("should extract cloud mask - Test 4") {

      val imagePath = "/home/kamikaze/gsoc/image2/LT51240292009256IKR00_B"
      val writePath = "/home/kamikaze/gsoc/cloud_image2_test4.tif"

      var image : SingleBandGeoTiff = SingleBandGeoTiff(imagePath + 1 + ".TIF")
      val imTiles = new Array[Tile](3)

      for (i <- 0 to 2)
      {
        image = SingleBandGeoTiff(imagePath + (i+1).toString + ".TIF")
        imTiles(i) = image.tile.convert(TypeInt).map(b => if(isNoData(b)) 256 else if(b < 0) b + 255 else b ).convert(TypeByte)
      }

      println(imTiles(0).findMinMax)

      // Thresholds for cloud identification
      val band1threshold = 200
      val band2threshold = 130
      val band6threshold = 100

      def isCloud(col: Int, row: Int): Boolean = {
        val pixelValues = (for(k <- 0 to 2) yield imTiles(k).get(col, row)).toArray
        (pixelValues(0) > band1threshold) && (pixelValues(1) > band2threshold)
      }

      val cloudTile = imTiles(0).map((col, row, x) => if(isCloud(col, row)) 255 else 0)

      GeoTiffWriter.write(SingleBandGeoTiff(cloudTile, image.extent, image.crs), writePath)

    }*/

    it("should identify cloud pixels") {
      val basePath = "/home/kamikaze/gsoc/cropped_images/"
      val writePath = "/home/kamikaze/gsoc/cloud_images/"
      val band = "/B2_cropped.TIF"

      val image: SingleBandGeoTiff = SingleBandGeoTiff(basePath + "image00/B2_cropped.TIF")
      var cloudTile: Tile = image.tile
      val imRows = image.tile.rows
      val imCols = image.tile.cols
      val num_images = 47
      //val checkArray = new Array[Array[Int](2)](imRows*imCols)
      var less: Int = 0
      var more: Int = 0
      val threshold = 15000
      var len: Int = 0

      val checkArray = Array.ofDim[Boolean](imCols, imRows)

      val imTiles = new Array[Tile](num_images)

      for (i <- 0 to 46)
      {
        println(i)
        imTiles(i) = SingleBandGeoTiff(basePath + "image" + "%02d".format(i) + band).tile
        //println(imTiles(i).findMinMax)
        //cloudTile = imTiles(i).map((col, row, x) => if(x > 20000) 65535 else 0)
        GeoTiffWriter.write(SingleBandGeoTiff(imTiles(i), image.extent, image.crs), writePath + "image" + "%02d".format(i) + ".TIF")
      }


      for (i <- 0 to imCols-1) {
        for(j <- 0 to imRows-1) {
          len = imTiles.filter(tile => tile.get(i, j) > threshold).length
          //print(len)
          checkArray(i)(j) = len > 0
          //println(checkArray(i)(j))
        }
      }

      for (i <- 0 to 46)
      {
        println(i)
        cloudTile = imTiles(i).map((col, row, x) => if(checkArray(col)(row) && x > threshold) 65535 else 0)
        GeoTiffWriter.write(SingleBandGeoTiff(cloudTile, image.extent, image.crs), writePath + "image" + "%02d".format(i) + ".TIF")
      }
    }

/*
    it("should sort and assign color values - Test 3") {
      val basePath = "/home/kamikaze/gsoc/cropped_images/"
      //val basePath = "/media/Media/GeoTIFF/"
      val band = "/B2_cropped.TIF"
      //val band = "/B2.TIF"

      //val image = SingleBandGeoTiff(basePath + "image28/B2.TIF")
      val image: SingleBandGeoTiff = SingleBandGeoTiff(basePath + "image00/B2_cropped.TIF")
      val imTiles = new Array[Tile](46)
      var num: String = "0"
      //println(image.cellType)
      //image.tile.convert


      for (i <- 1 to 46)
        {
          println(i)
          imTiles(i-1) = SingleBandGeoTiff(basePath + "image" + "%02d".format(i) + band).tile
          //println(imTiles(i).findMinMax)
        }

      val dummyTile = imTiles(0)
      println(dummyTile.findMinMax)
      val minTile = dummyTile.map((col, row, x) => imTiles.minBy(x => x.get(col, row)).get(col, row))
      //image = image.mapTile(t => t.map((col, row, x) => imTiles.minBy(x => x.get(col, row)).get(col, row)))
      //println(image.findMinMax)
      GeoTiffWriter.write(SingleBandGeoTiff(minTile, image.extent, image.crs), "/home/kamikaze/gsoc/new_mask_blue_int32.TIF")

    }
*/

    /*it("should extract a cloud mask from cloudy image - Test 2") {

      //val basePath = "/media/Media/GeoTIFF"
      val basePath = "/home/kamikaze/gsoc/"
      val imagePath = "image1/LT51240292006264IKR00_B"

      val images = new Array[SingleBandGeoTiff](3)
      val imTiles = new Array[Tile](3)

      for (i <- 0 to 2)
      {
        images(i) = SingleBandGeoTiff(basePath + imagePath + (i+1).toString + ".TIF")
        imTiles(i) = images(i).tile.convert(TypeInt).map(b => if(isNoData(b)) 256 else if(b < 0) b + 255 else b ).convert(TypeByte)
      }

      //print(imTiles(0).cellType)
      //val rows = imTiles.maxBy(x => x.rows).rows
      //val cols = imTiles.maxBy(x => x.cols).cols

      //println(rows)
      //println(cols)

      println(imTiles(0).findMinMax)

      //val tile = images(0).tile

      // Thresholds for cloud identification
      val band1threshold = 200
      val band6threshold = 100

      //val pixelValues = new Array[Int](7)
      //var count = 0


      def isCloud(col: Int, row: Int): Boolean = {
        val pixelValues = (for(k <- 0 to 2) yield imTiles(k).get(col, row)).toArray
        pixelValues(0) > 200
      }

      /*def isShadow(col: Int, row: Int): Boolean = {
        val pixelValues = (for(k <- 0 to 6) yield imTiles(k).get(col, row)).toArray
        (pixelValues(0) <= band1threshold) && (pixelValues(4) < band5threshold) && (pixelValues(4) < (bandRatio*pixelValues(1)))
      }*/

      val cloudTile = imTiles(0).map((col, row, x) => if(isCloud(col, row)) 255 else 0)
      //val shadowTile = imTiles(0).map((col, row, x) => if(isShadow(col, row)) 255 else 0)

      GeoTiffWriter.write(SingleBandGeoTiff(cloudTile, images(0).extent, images(0).crs), basePath + "cloud_image_test2.tif")

    }*/


    /*
    it("should extract a cloud mask from the image/s specified - Test 1") {

      /* Reading Multiband image */
      /*
      val image = MultiBandGeoTiff(basePath + "image1/combined.tif")
      println(image.bandCount)
      */

      val imagePath = "/home/kamikaze/gsoc/image2/LT51240292009256IKR00_B"
      val writePath = "/home/kamikaze/gsoc/cloud_image2_test1.tif"

      var image : SingleBandGeoTiff = SingleBandGeoTiff(imagePath + 1 + ".TIF")
      val imTiles = new Array[Tile](7)

      for (i <- 0 to 6)
      {
        image = SingleBandGeoTiff(imagePath + (i+1).toString + ".TIF")
        imTiles(i) = image.tile.convert(TypeInt).map(b => if(isNoData(b)) 256 else if(b < 0) b + 255 else b ).convert(TypeByte)
      }

      //print(imTiles(0).cellType)
      //val rows = imTiles.maxBy(x => x.rows).rows
      //val cols = imTiles.maxBy(x => x.cols).cols

      //println(rows)
      //println(cols)

      println(imTiles(0).findMinMax)

      //val tile = images(0).tile

      // Thresholds for cloud identification
      val band1threshold = 200
      val band6threshold = 100

      // Thresholds for cloud shadow identification
      val band5threshold = 55
      val bandRatio = 1.2

      //val pixelValues = new Array[Int](7)
      var count = 0


      def isCloud(col: Int, row: Int): Boolean = {
        val pixelValues = (for(k <- 0 to 6) yield imTiles(k).get(col, row)).toArray
        (pixelValues(0) > band1threshold) && (pixelValues(5) < band6threshold) && (pixelValues(5) <= pixelValues.min)
      }

      /*def isShadow(col: Int, row: Int): Boolean = {
        val pixelValues = (for(k <- 0 to 6) yield imTiles(k).get(col, row)).toArray
        (pixelValues(0) <= band1threshold) && (pixelValues(4) < band5threshold) && (pixelValues(4) < (bandRatio*pixelValues(1)))
      }*/

      val cloudTile = imTiles(0).map((col, row, x) => if(isCloud(col, row)) 255 else 0)
      //val shadowTile = imTiles(0).map((col, row, x) => if(isShadow(col, row)) 255 else 0)

      GeoTiffWriter.write(SingleBandGeoTiff(cloudTile, image.extent, image.crs), writePath)
      //GeoTiffWriter.write(SingleBandGeoTiff(shadowTile, images(0).extent, images(0).crs), basePath + "shadow_image2.tif")

      //println(images(0).tile.findMinMax)


      //tile.map((a, b, c) => b)

      //val cloudimages = new Array[SingleBandGeoTiff](7)

      /* Checking for band1 threshold */
      //val cloudTile = images(0).tile.map(b =>
      //  if(b > 200) {
      //    255} else 0)

      /*Checking for band6 threshold */


      //GeoTiffWriter.write(image, basePath + "converted_image.TIF")
      //val imRaster = image.raster

      //val imTile1 = image.band(0)
      //val imTile2 = image.band(1)
      //val imTile3 = image.band(2)
      //val imTile4 = image.band(3)
      //val xtile = imTile.convert(TypeInt)
      //println(xtile.cellTy


      //val tile = imTile.convert(TypeByte)map { b => if(isNoData(b)) 128 else b.toByte & 0xFF }
      //val tile = imTile.convert(TypeInt).map { b => if(isNoData(b)) 256 else if(b < 0) b + 255 else b  }.convert(TypeByte)
      //val newTile = tile.convert(TypeByte)

      //val newTile = tile.map(b => if(b > 150) 255 else 0)
      //GeoTiffWriter.write(SingleBandGeoTiff(newTile, image.extent, image.crs), basePath + "binarized_image.tif")
      //val (x, y) = tile.findMinMax
      //println(tile.cellType)
      //var countInt = 0

      /*val tempList = for {
        i <- 0 to imRaster.rows
        j <- 0 to imRaster.cols
        if (imRaster.get(i, j) == 32767)
      } yield (i, j)
      */

      //println(tile.size)
      //println(imRaster.get(7590, 7760))
      //println(tile.rows)
      //println(tile.cols)
      //imRaster.foreach { z:Int => if(z < countInt) countInt = z }
      //println(x)
      //println(y)

      //GeoTiffWriter.write(image, basePath + "converted_image.tif")

    }*/

  }
}