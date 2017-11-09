package geotrellis.spark.cog

import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.raster._
import geotrellis.raster.io.geotiff.{ArraySegmentBytes, BandInterleave, BandType, GeoTiffMultibandTile, GeoTiffOptions, GeoTiffSegmentLayout, GeoTiffTile, Striped, Tiled}
import geotrellis.raster.merge._
import geotrellis.raster.resample._
import geotrellis.raster.prototype._
import geotrellis.raster.split.Split
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.pyramid.Pyramid.{Options => PyramidOptions}
import geotrellis.util._
import geotrellis.vector.Extent
import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import spire.syntax.cfor.cfor

import scala.reflect.ClassTag

object COGLayer {
  def apply[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(startZoom: Int, endZoom: Int, layoutScheme: LayoutScheme): GeoTiffMultibandTile = {

    val levels: Stream[(Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]])] =
      Pyramid.levelStream[K, V, TileLayerMetadata[K]](rdd, layoutScheme, startZoom, endZoom, PyramidOptions.DEFAULT)

    val options: GeoTiffOptions = ???

    levels.foreach { case (z, r) =>
      val sourceLayout = r.metadata
      val bandType = BandType.forCellType(sourceLayout.cellType)
      val layoutCols = sourceLayout.layoutCols
      val layoutRows = sourceLayout.layoutRows

      val segmentLayout = GeoTiffSegmentLayout(layoutCols, layoutRows, options.storageMethod, BandInterleave, bandType)

      val segmentCount = segmentLayout.tileLayout.layoutCols * segmentLayout.tileLayout.layoutRows
      val compressor = options.compression.createCompressor(segmentCount)

      val segmentBytes = Array.ofDim[Array[Byte]](segmentCount)
      val segmentTiles: Array[Tile] = r.map(_._2.asInstanceOf[Tile]).collect()

      cfor(0)(_ < segmentCount, _ + 1) { i =>
        val bytes = segmentTiles(i).toBytes
        segmentBytes(i) = compressor.compress(bytes, i)
      }

      GeoTiffTile(new ArraySegmentBytes(segmentBytes), compressor.createDecompressor, segmentLayout, options.compression, sourceLayout.cellType)
    }


    null
  }
}
