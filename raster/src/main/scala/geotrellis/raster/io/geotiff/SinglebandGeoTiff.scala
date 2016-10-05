package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader.GeoTiffReader
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

import spire.syntax.cfor._

case class SinglebandGeoTiff(
  val tile: Tile,
  val extent: Extent,
  val crs: CRS,
  val tags: Tags,
  val options: GeoTiffOptions
) extends GeoTiff[Tile] {
  val cellType = tile.cellType

  def mapTile(f: Tile => Tile): SinglebandGeoTiff =
    SinglebandGeoTiff(f(tile), extent, crs, tags, options)

  def imageData: GeoTiffImageData =
    tile match {
      case gtt: GeoTiffTile => gtt
      case _ => tile.toGeoTiffTile(options)
    }

  def crop(subExtent: Extent): SinglebandGeoTiff = {
    val raster: Raster[Tile] =
      this.raster.crop(subExtent)

    SinglebandGeoTiff(raster, subExtent, this.crs)
  }
  
  def cropIterator(subExtent: Extent): Iterator[(Extent, SinglebandGeoTiff)] =
    cropIterator(subExtent, true)

  def cropIterator(subExtent: Extent,
    fill: Boolean): Iterator[(Extent, SinglebandGeoTiff)] = {

    val unroundedColIterator =
      if (fill)
        extent.width.toFloat / subExtent.width
      else
        extent.width / subExtent.width

    val unroundedRowIterator =
      if (fill)
        extent.height.toFloat / subExtent.height
      else
        extent.height / subExtent.height

    val colIterator =
      math.ceil(unroundedColIterator - (unroundedColIterator % 0.001)).toInt
    
    val rowIterator =
        math.ceil(unroundedRowIterator - (unroundedRowIterator % 0.001)).toInt

    val arr =
      if (fill)
        Array.ofDim[(Extent, SinglebandGeoTiff)](colIterator * rowIterator)
      else
        Array.ofDim[(Extent, SinglebandGeoTiff)](unroundedColIterator.toInt * unroundedRowIterator.toInt)
    
    var counter = 0

    /*
     * This is needed as sometimes extents will be off by very small
     * margins (ie 1 * 10E-10). Therefore, this function will prevent
     * values from not being seen as equal if they contain this small margin
     * of difference.
     */
    def ~=(x: Double, y: Double, tolerance: Double): Boolean =
      if ((x - y).abs < tolerance) true else false

    cfor(0)(_ < rowIterator,  _ + 1){ row =>
      cfor(0)(_ < colIterator, _ + 1) { col =>

        val colMin =
          if (subExtent.xmin >= 0)
            col * subExtent.width
          else
            (col * subExtent.width) + subExtent.xmin

        val rowMin =
          if (subExtent.ymin >= 0)
            row * subExtent.height
          else
            (row * subExtent.height) + subExtent.ymin

        val colMax = math.min(colMin + subExtent.width, extent.xmax)
        val rowMax = math.min(rowMin + subExtent.height, extent.ymax)
        val e = Extent(colMin, rowMin, colMax, rowMax)
        
        if (fill || ~=(subExtent.area, e.area, 0.0001)) { 
          arr(counter) = (e, crop(e))
          counter += 1
        }
      }
    }
    arr.toIterator
  }
}

object SinglebandGeoTiff {

  def apply(
    tile: Tile,
    extent: Extent,
    crs: CRS
  ): SinglebandGeoTiff =
    new SinglebandGeoTiff(tile, extent, crs, Tags.empty, GeoTiffOptions.DEFAULT)

  /** Read a single-band GeoTIFF file from a byte array.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes)

  /** Read a single-band GeoTIFF file from a byte array.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(bytes: Array[Byte], decompress: Boolean, streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, decompress, streaming)

  /** Read a single-band GeoTIFF file from the file at the given path.
    * The GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path)

  def apply(path: String, e: Extent): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, e)
  
  def apply(path: String, e: Option[Extent]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, e)
  
  /** Read a single-band GeoTIFF file from the file at the given path.
    * If decompress = true, the GeoTIFF will be fully decompressed and held in memory.
    */
  def apply(path: String, decompress: Boolean, streaming: Boolean): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, decompress, streaming)

  /** Read a single-band GeoTIFF file from the file at a given path.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false, false)

  /** Read a single-band GeoTIFF file from a byte array.
    * The tile data will remain tiled/striped and compressed in the TIFF format.
    */
  def compressed(bytes: Array[Byte]): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(bytes, false, false)

  def streaming(path: String): SinglebandGeoTiff =
    GeoTiffReader.readSingleband(path, false, true)

  implicit def singlebandGeoTiffToTile(sbg: SinglebandGeoTiff): Tile =
    sbg.tile

  implicit def singlebandGeoTiffToRaster(sbg: SinglebandGeoTiff): SinglebandRaster =
    sbg.raster

  implicit def singlebandGeoTiffToProjectedRaster(sbg: SinglebandGeoTiff): ProjectedRaster[Tile] =
    sbg.projectedRaster
}
