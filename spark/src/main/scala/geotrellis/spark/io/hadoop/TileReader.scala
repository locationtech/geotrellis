package geotrellis.spark.io.hadoop

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.index._
import geotrellis.raster._

import org.apache.spark.SparkContext

trait TileReader[K] {
  /** @param keyBounds KeyBounds[K] for the single tile we'd like to pull */
  def read(
    catalogConfig: HadoopRasterCatalogConfig,
    layerMetaData: HadoopLayerMetaData,
    index: KeyIndex[K],
    keyBounds: KeyBounds[K]
  )(implicit sc: SparkContext): Tile
}
