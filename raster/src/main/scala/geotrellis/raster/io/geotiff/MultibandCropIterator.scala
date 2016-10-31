package geotrellis.raster.io.geotiff

import geotrellis.util.ByteReader
import geotrellis.raster._

/**
 * An extension of [[CropIterator]], this subclass works specifically with
 * multibandGeoTiffs.
 *
 * @param geoTiff: A [[MultibandGeoTiff]] file.
 * @param widnowedCols: An Int that is max col size of the sub-tiles.
 * @param widnowedRows: An Int that is max row size of the sub-tiles.
 * @return: An [[Iterator]] that conatins [[MultibandGeoTiff]]s.
 *
 */
class MultibandCropIterator(geoTiff: MultibandGeoTiff,
  windowedCols: Int,
  windowedRows: Int) extends CropIterator(geoTiff, windowedCols, windowedRows) {

  def next: MultibandGeoTiff = {
    if (hasNext) {
      if (colCount + 1 > colIterations)
        adjustValues
      
      val result: MultibandGeoTiff = geoTiff.crop(colMin, rowMin, colMax, rowMax)
      adjustValues
      result
    } else {
      throw new Error("Iterator is empty")
    }
  }
}

/** the companion class of [[MultibandCropIterator]] */
object MultibandCropIterator {
  def apply(path: String, dimensions: (Int, Int)): MultibandCropIterator =
    apply(MultibandGeoTiff.streaming(path), dimensions)

  def apply(path: String, colMax: Int, rowMax: Int): MultibandCropIterator =
    apply(MultibandGeoTiff.streaming(path), colMax, rowMax)

  def apply(byteReader: ByteReader, dimensions: (Int, Int)): MultibandCropIterator =
    apply(MultibandGeoTiff.streaming(byteReader), dimensions)
  
  def apply(byteReader: ByteReader, colMax: Int, rowMax: Int): MultibandCropIterator =
    apply(MultibandGeoTiff.streaming(byteReader), colMax, rowMax)

  def apply(geoTiff: MultibandGeoTiff, dimensions: (Int, Int)): MultibandCropIterator =
    apply(geoTiff: MultibandGeoTiff, dimensions._1, dimensions._2)

  def apply(geoTiff: MultibandGeoTiff, colMax: Int, rowMax: Int): MultibandCropIterator =
    new MultibandCropIterator(geoTiff: MultibandGeoTiff, colMax, rowMax)
}
