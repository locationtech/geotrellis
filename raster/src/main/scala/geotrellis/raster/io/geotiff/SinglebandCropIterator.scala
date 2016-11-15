package geotrellis.raster.io.geotiff

import geotrellis.util.ByteReader
import geotrellis.raster._

/**
 * An extension of [[CropIterator]], this subclass works specifically with
 * multibandGeoTiffs.
 *
 * @param geoTiff: A [[SinglebandGeoTiff]] file.
 * @param widnowedCols: An Int that is max col size of the sub-tiles.
 * @param widnowedRows: An Int that is max row size of the sub-tiles.
 * @return: An [[Iterator]] that conatins [[SinglebandGeoTiff]]s.
 *
 */
class SinglebandCropIterator(geoTiff: SinglebandGeoTiff,
  windowedCols: Int,
  windowedRows: Int) extends CropIterator(geoTiff, windowedCols, windowedRows) {

  def next: SinglebandGeoTiff = {
    if (hasNext) {
      if (colCount + 1 > colIterations)
        adjustValues

      val result: SinglebandGeoTiff = geoTiff.crop(colMin, rowMin, colMax, rowMax)
      adjustValues
      result
    } else {
      throw new Error("Iterator is empty")
    }
  }
}

/** the companion class of [[SinglebandCropIterator]] */
object SinglebandCropIterator {
  def apply(path: String, dimensions: (Int, Int)): SinglebandCropIterator =
    apply(SinglebandGeoTiff.streaming(path), dimensions)

  def apply(path: String, colMax: Int, rowMax: Int): SinglebandCropIterator =
    apply(SinglebandGeoTiff.streaming(path), colMax, rowMax)

  def apply(byteReader: ByteReader, dimensions: (Int, Int)): SinglebandCropIterator =
    apply(SinglebandGeoTiff.streaming(byteReader), dimensions)
  
  def apply(byteReader: ByteReader, colMax: Int, rowMax: Int): SinglebandCropIterator =
    apply(SinglebandGeoTiff.streaming(byteReader), colMax, rowMax)

  def apply(geoTiff: SinglebandGeoTiff, dimensions: (Int, Int)): SinglebandCropIterator =
    apply(geoTiff: SinglebandGeoTiff, dimensions._1, dimensions._2)

  def apply(geoTiff: SinglebandGeoTiff, colMax: Int, rowMax: Int): SinglebandCropIterator =
    new SinglebandCropIterator(geoTiff: SinglebandGeoTiff, colMax, rowMax)
}
