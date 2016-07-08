package geotrellis.spark.io.geowave

import geotrellis.geotools._
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.resample.NearestNeighbor
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.util._
import geotrellis.vector.Extent

import com.typesafe.scalalogging.slf4j._
import mil.nga.giat.geowave.adapter.raster.adapter.RasterDataAdapter
import mil.nga.giat.geowave.core.geotime.ingest._
import mil.nga.giat.geowave.core.store._
import mil.nga.giat.geowave.datastore.accumulo._
import org.apache.spark.rdd.RDD
import org.apache.spark.SparkContext
import org.geotools.coverage.grid.GridCoverage2D
import org.geotools.coverage.processing.CoverageProcessor
import org.geotools.coverage.processing.operation.Mosaic
import org.geotools.coverage.processing.operation.Mosaic.GridGeometryPolicy
import org.geotools.factory.{GeoTools, Hints}
import org.opengis.coverage.grid.GridCoverage
import org.opengis.parameter.ParameterValueGroup

import spray.json._

import scala.reflect._
import javax.media.jai.{ImageLayout, JAI}

object GeowaveLayerWriter extends LazyLogging {

  def write[
    K: ClassTag,
    V: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](
    coverageName: String,
    rdd: RDD[(K, V)] with Metadata[M],
    zookeeper: String,
    accumuloInstance: String,
    accumuloUser: String,
    accumuloPass: String,
    geowaveNamespace: String
  ): Unit = {
    val metadata = implicitly[ClassTag[K]].toString match {
      case "geotrellis.spark.SpatialKey" => rdd.metadata.asInstanceOf[TileLayerMetadata[SpatialKey]]
      case t: String => throw new Exception(s"Unsupported Key Type: $t")
    }
    val crs = metadata.crs
    val mt = metadata.mapTransform
    val cellType = metadata.cellType.toString
    val specimen = rdd.first

    rdd.mapPartitions({ partition =>
      val gwMetadata = new java.util.HashMap[String, String](); gwMetadata.put("cellType", cellType)

      /* Construct (Multiband|)Tile to GridCoverage2D conversion function */
      val fn: (((K, V)) => GridCoverage2D) = {
        specimen match {
          case (_ : SpatialKey, _: Tile) => { case (k: K, v: V) =>
            val extent = mt(k.asInstanceOf[SpatialKey]).reproject(crs, LatLng)
            val tile = v.asInstanceOf[Tile]
            ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
          }
          case (_ : SpatialKey, _: MultibandTile) => { case (k: K, v: V) =>
            val extent = mt(k.asInstanceOf[SpatialKey]).reproject(crs, LatLng)
            val tile = v.asInstanceOf[MultibandTile]
            ProjectedRaster(Raster(tile, extent), LatLng).toGridCoverage2D
          }
        }
      }

      /* Produce mosaic from all of the tiles in this partition */
      val sources = new java.util.ArrayList[GridCoverage2D]
      val retval = partition.map({ case kv => sources.add(fn(kv)); Unit }); retval.toList
      val processor = CoverageProcessor.getInstance(GeoTools.getDefaultHints())
      val param = processor.getOperation("Mosaic").getParameters()
      val hints = new Hints
      val imageLayout = new ImageLayout
      logger.info(s"partition size = ${sources.size}")
      param.parameter("Sources").setValue(sources)
      hints.put(JAI.KEY_IMAGE_LAYOUT, imageLayout)
      imageLayout.setTileHeight(256)
      imageLayout.setTileWidth(256)
      val image = processor.doOperation(param, hints).asInstanceOf[GridCoverage2D]

      /* Objects for writing into GeoWave */
      val basicOperations = new BasicAccumuloOperations(
        zookeeper,
        accumuloInstance,
        accumuloUser,
        accumuloPass,
        geowaveNamespace
      )
      val dataStore = new AccumuloDataStore(basicOperations)
      val index = (new SpatialDimensionalityTypeProvider.SpatialIndexBuilder).setAllTiers(true).createIndex()
      val adapter = new RasterDataAdapter(coverageName, gwMetadata, image, 256, true) // image only used for sample and color metadata, not data
      val indexWriter = dataStore.createWriter(adapter, index).asInstanceOf[IndexWriter[GridCoverage]]

      /* Write the mosaic into GeoWave */
      indexWriter.write(image)
      indexWriter.close
      retval
    }, preservesPartitioning = true).collect
  }
}

class GeowaveLayerWriter(val attributeStore: GeowaveAttributeStore)(implicit sc: SparkContext)
    extends LayerWriter[LayerId] with LazyLogging {

  protected def _write[
    K: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](
    layerId: LayerId,
    rdd: RDD[(K, V)] with Metadata[M],
    keyIndex: KeyIndex[K]
  ): Unit = {
    val LayerId(coverageName, zoom) = layerId
    val specimen = rdd.first

    // Perform checks
    if (zoom > 0) logger.warn("The zoom level is ignored because GeoWave does its own pyramiding")
    specimen._1 match {
      case _: SpatialKey =>
      case _ => throw new Exception(s"Unsupported Key Type: ${implicitly[ClassTag[K]].toString}")
    }
    specimen._2 match {
      case _: Tile =>
      case _: MultibandTile =>
      case _ => throw new Exception(s"Unsupported Value Type: ${implicitly[ClassTag[V]].toString}")
    }

    GeowaveLayerWriter.write(
      coverageName, rdd,
      attributeStore.zookeeper,
      attributeStore.accumuloInstance,
      attributeStore.accumuloUser,
      attributeStore.accumuloPass,
      attributeStore.geowaveNamespace
    )
  }

}
