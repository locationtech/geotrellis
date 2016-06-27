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

  /** A set of the [[GeoTiffSegments]] that intersect the windowedGridBounds. */
  val intersectingSegments: Set[Int] = {
    val segments = Set[Int]()
    val windowCoords = windowedGridBounds.coords

    cfor(0)(_ < windowedGridBounds.size, _ + 1) { i =>
      segments += segmentLayout.getSegmentIndex(windowCoords(i)._1, windowCoords(i)._2)
    }
    segments
  }

 val size = intersectingSegments.size

 private val tileLayout = segmentLayout.tileLayout

 val cols = {
   if (tiffTags.hasStripStorage) {
     tiffTags.cols
   } else {
     val layout = ListBuffer[Int]()
     for (segment <- intersectingSegments) {
       layout += segment % tileLayout.layoutCols
     }
     val largestCol = layout.groupBy(identity).maxBy(_._2.size)._1

     if (largestCol == tileLayout.layoutCols)
       tiffTags.cols
     else if (largestCol != 0)
       tileLayout.tileCols * largestCol
     else
       tileLayout.tileCols
   }
 }

 val rows = {
   if (tiffTags.bandCount == 1 || tiffTags.hasStripStorage) {
     var i = 0
     for (segment <- intersectingSegments) {
       i += tiffTags.rowsInSegment(segment)
     }
     i
   } else {
     (tiffTags.tileTags.tileLength.get * size).toInt
   }
 }
}
