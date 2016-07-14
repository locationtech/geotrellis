package geotrellis.raster.io.geotiff

import geotrellis.util._
import geotrellis.vector.Extent
import geotrellis.raster._
import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._

import java.nio.ByteBuffer
import scala.collection.mutable._
import monocle.syntax.apply._
import spire.syntax.cfor._

/**
 * This class contains information on the inputted winodwed section of
 * a GeoTiff.
 *
 * @param extent: The desired [[Extent]] of the windowed area
 * @param storageMethod: The [[StorageMethod]] of the target GeoTiff
 * @param tifftags: The [[TiffTags]] of the target GeoTiff
 * @return A new instance of WindowedGeoTiff
 */
case class WindowedGeoTiff(extent: Extent, storageMethod: StorageMethod, tiffTags: TiffTags) {

  val segmentLayout =
    GeoTiffSegmentLayout(tiffTags.cols, tiffTags.rows, storageMethod, tiffTags.bandType)

  /** The [[GridBounds]] of the inputted extent */
  val windowedGridBounds: GridBounds = {
    val tileLayout = segmentLayout.tileLayout
    val rasterExtent = RasterExtent(tiffTags.extent, tiffTags.cols, tiffTags.rows)

    rasterExtent.gridBoundsFor(extent)
  }

  /** A list of the [[GeoTiffSegments]] that intersect the windowedGridBounds. */
  val intersectingSegments: List[Int] = {
    val segments = ListBuffer[Int]()

    cfor(0)(_ < tiffTags.segmentCount, _ + 1) { i =>
      val segmentTransform = segmentLayout.getSegmentTransform(i)

      val colStart =
        tiffTags.bandType match {
          case BitBandType => segmentTransform.bitIndexToCol(0)
          case _ => segmentTransform.indexToCol(0)
        }
      val rowStart =
        tiffTags.bandType match {
          case BitBandType => segmentTransform.bitIndexToRow(0)
          case _ => segmentTransform.indexToRow(0)
        }

      val (cols, rows) = segmentLayout.getSegmentDimensions(i)
      val segmentGridBounds = GridBounds(colStart, rowStart, colStart + cols, rowStart + rows)

      if (windowedGridBounds.intersects(segmentGridBounds))
        segments += i
    }
    segments.toList
  }
}
