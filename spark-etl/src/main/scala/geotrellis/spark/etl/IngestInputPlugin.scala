package geotrellis.spark.etl

import geotrellis.proj4.CRS
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.raster.{CellType, Tile, CellGrid}
import geotrellis.spark.reproject._
import geotrellis.spark._
import geotrellis.spark.ingest._
import geotrellis.spark.tiling.{LayoutDefinition, LayoutScheme}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag

abstract class IngestInputPlugin[I: IngestKey, K: ClassTag](implicit tiler: Tiler[I, K, Tile]) extends InputPlugin[K] {
  def source(props: Parameters)(implicit sc: SparkContext): RDD[(I, V)]

  def apply(
    lvl: StorageLevel,
    crs: CRS, scheme: Either[LayoutScheme, LayoutDefinition],
    targetCellType: Option[CellType],
    props: Parameters)
  (implicit sc: SparkContext): (Int, RDD[(K, Tile)] with Metadata[RasterMetaData]) = {

    val sourceTiles = source(props).reproject(crs).persist(lvl)
    val (zoom, rasterMetaData) = scheme match {
      case Left(layoutScheme) =>
        val (zoom, rmd) = RasterMetaData.fromRdd(sourceTiles, crs, layoutScheme) { key => key.projectedExtent.extent }
        targetCellType match {
          case None => zoom -> rmd
          case Some(ct) => zoom -> rmd.copy(cellType = ct)
        }

      case Right(layoutDefinition) =>
        0 -> RasterMetaData(
          crs = crs,
          cellType = targetCellType.get,
          extent = layoutDefinition.extent,
          layout = layoutDefinition
        )
    }
    val tiles = sourceTiles.tile[K](rasterMetaData, NearestNeighbor)
    zoom -> ContextRDD(tiles, rasterMetaData)
  }
}
