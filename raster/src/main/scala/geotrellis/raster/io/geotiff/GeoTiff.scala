package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.utils._
import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import geotrellis.vector.Extent
import geotrellis.proj4._

import monocle.syntax._

case class GeoTiff(tags: Tags, data: Array[Array[Byte]]) {
  def band(i: Int): Tile = {
//    if(
  }

  def cols = (tags &|-> Tags._basicTags ^|-> BasicTags._imageWidth get)
  def rows = (tags &|-> Tags._basicTags ^|-> BasicTags._imageLength get)

  def extent: Extent = 
    (tags 
      &|-> Tags._geoTiffTags
      ^|-> GeoTiffTags._modelTransformation get
    ) match {
      case Some(trans) if (trans.validateAsMatrix && trans.size == 4 && trans(0).size == 4) => 
        transformationModelSpace(trans)
      case _ => 
        (tags
          &|-> Tags._geoTiffTags 
          ^|-> GeoTiffTags._modelTiePoints get
        ) match {
          case Some(tiePoints) if (!tiePoints.isEmpty) =>
            tiePointsModelSpace(
              tiePoints,
              (tags 
                &|-> Tags._geoTiffTags
                ^|-> GeoTiffTags._modelPixelScale get
              )
            )
      case _ => 
            Extent(0, 0, cols, rows)
    }
  }

  def crs: CRS = 
    proj4String match {
      case Some(s) => CRS.fromString(s)
      case None => LatLng
    }

  lazy val cellType: CellType = {
    val bitsPerSample = 
      (tags
        &|-> Tags._basicTags
        ^|-> BasicTags._bitsPerSample get)

    val sampleFormat = 
      (tags
        &|-> Tags._dataSampleFormatTags
        ^|-> DataSampleFormatTags._sampleFormat get)

    (bitsPerSample, sampleFormat) match {
      case (Some(bitsPerSampleArray), sampleFormatArray) if (bitsPerSampleArray.size > 0 && sampleFormatArray.size > 0) =>
        val bitsPerSample = bitsPerSampleArray(0)

        val sampleFormat = sampleFormatArray(0)

        import codes.SampleFormat._

        if (bitsPerSample == 1) TypeBit
        else if (bitsPerSample <= 8) TypeByte
        else if (bitsPerSample <= 16) TypeShort
        else if (bitsPerSample == 32 && sampleFormat == UnsignedInt || sampleFormat == SignedInt) TypeInt
        else if (bitsPerSample == 32 && sampleFormat == FloatingPoint) TypeFloat
        else if (bitsPerSample == 64 && sampleFormat == FloatingPoint) TypeDouble
        else {
          throw new MalformedGeoTiffException(
            s"bad/unsupported bitspersample or sampleformat: $bitsPerSample or $sampleFormat"
          )
        }

      case _ =>
        throw new MalformedGeoTiffException("no bitsPerSample values!")
    }
  }

  private def proj4String: Option[String] =
    try {
      reader.GeoTiffCSParser(tags).getProj4String
    } catch {
      case e: Exception => None
    }

  def getRasterBoundaries: Array[Pixel3D] = {
    val imageWidth = cols
    val imageLength = rows

    Array(
      Pixel3D(0, imageLength, 0),
      Pixel3D(imageWidth, 0, 0)
    )
  }

  private def transformationModelSpace(modelTransformation: Array[Array[Double]]) = {
    def matrixMult(pixel: Pixel3D) = Pixel3D.fromArray((modelTransformation *
      Array(Array(pixel.x, pixel.y, pixel.z, 1))).flatten.take(3))

    getExtentFromModelFunction(matrixMult)
  }

  private def getExtentFromModelFunction(func: Pixel3D => Pixel3D) = {
    val modelPixels = getRasterBoundaries.map(func)

    val (minX, minY) = (modelPixels(0).x, modelPixels(0).y)
    val (maxX, maxY) = (modelPixels(1).x, modelPixels(1).y)

    Extent(minX, minY, maxX, maxY)
  }

  private def tiePointsModelSpace(tiePoints: Array[(Pixel3D, Pixel3D)],
    pixelScaleOption: Option[(Double, Double, Double)]) =
    pixelScaleOption match {
      case Some(pixelScales) => {
        def modelFunc(pixel: Pixel3D) = {
          val (first, second) = tiePoints.head

          val scaleX = (pixel.x - first.x) * pixelScales._1
          val scaleY = (pixel.y - first.y) * pixelScales._2
          val scaleZ = (pixel.z - first.z) * pixelScales._3

          Pixel3D(scaleX + second.x, second.y - scaleY, scaleZ + second.z)
        }

        getExtentFromModelFunction(modelFunc)
      }
      case None => {
        val imageWidth = cols
        val imageLength = rows

        var minX = 0.0
        var minY = 0.0
        var maxX = 0.0
        var maxY = 0.0

        var i = 0
        while(i < 4) {
          val xt = if (i % 2 == 1) imageWidth - 1 else 0
          val yt = if (i >= 2) imageLength - 1 else 0

          val optPixel = tiePoints.filter(pixel => pixel._1.x == xt &&
            pixel._1.y == yt).map(_._2).headOption

          if (!optPixel.isEmpty) {
            val pixel = optPixel.get
            if (i == 0 || i == 1) maxY = pixel.y
            if (i == 0 || i == 2) minX = pixel.x
            if (i == 1 || i == 3) maxX = pixel.x
            if (i == 2 || i == 3) minY = pixel.y
          }

          i += 1
        }

        Extent(minX, minY, maxX, maxY)
      }
    }

}
