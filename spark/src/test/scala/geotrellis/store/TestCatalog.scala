package geotrellis.store

import geotrellis.layer.{LayoutDefinition, SpatialKey}
import geotrellis.raster.geotiff.GeoTiffRasterSource
import geotrellis.raster.{ArrayMultibandTile, MultibandTile}
import geotrellis.spark.store.file.FileLayerWriter
import geotrellis.spark._
import geotrellis.store.file.FileAttributeStore
import geotrellis.store.index.ZCurveKeyIndexMethod

import org.apache.spark.SparkContext

import java.io.File

object TestCatalog {
  def resourcesPath(path: String): String = s"${new File("").getAbsolutePath}/spark/src/test/resources/$path"

  val filePath = resourcesPath("vlm/aspect-tiled.tif")
  val multibandOutputPath = resourcesPath("vlm/catalog")
  val singlebandOutputPath = resourcesPath("vlm/single_band_catalog")

  def fullPath(path: String) = new java.io.File(path).getAbsolutePath

  def createMultiband(implicit sc: SparkContext): Unit = {
    // Create the attributes store that will tell us information about our catalog.
    val attributeStore = FileAttributeStore(multibandOutputPath)

    // Create the writer that we will use to store the tiles in the local catalog.
    val writer = FileLayerWriter(attributeStore)

    val rs = GeoTiffRasterSource(TestCatalog.filePath)
    rs.resolutions.sortBy(_.cellSize.resolution).zipWithIndex.foreach { case (rasterExtent, index) =>
      val layout = LayoutDefinition(rasterExtent, tileSize = 256)

      val rdd: MultibandTileLayerRDD[SpatialKey] =
        RasterSourceRDD.spatial(List(rs.resampleToGrid(layout)), layout)
          .withContext( tiledd =>
            // the tiles are actually `PaddedTile`, this forces them to be ArrayTile
            tiledd.mapValues { mb: MultibandTile => ArrayMultibandTile(mb.bands.map(_.toArrayTile))}
          )

      val id = LayerId("landsat", index)
      writer.write(id, rdd, ZCurveKeyIndexMethod)
    }
  }

  def createSingleband(implicit sc: SparkContext): Unit = {
    // Create the attributes store that will tell us information about our catalog.
    val attributeStore = FileAttributeStore(singlebandOutputPath)

    // Create the writer that we will use to store the tiles in the local catalog.
    val writer = FileLayerWriter(attributeStore)

    val rs = GeoTiffRasterSource(TestCatalog.filePath)
    rs.resolutions.sortBy(_.cellSize.resolution).zipWithIndex.foreach { case (rasterExtent, index) =>
      val layout = LayoutDefinition(rasterExtent, tileSize = 256)

      val rdd: TileLayerRDD[SpatialKey] =
        RasterSourceRDD.spatial(List(rs.resampleToGrid(layout)), layout)
          .withContext( tiledd =>
            tiledd.mapValues { mb: MultibandTile =>
              ArrayMultibandTile(mb.bands.map(_.toArrayTile)).band(0)  // Get only first band
            }
          )

      val id = LayerId("landsat", index)
      writer.write(id, rdd, ZCurveKeyIndexMethod)
    }
  }
}
