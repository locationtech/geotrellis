package geotrellis.spark.etl

import geotrellis.raster.CellGrid
import geotrellis.raster.crop.CropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.raster.stitch.Stitcher
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.tiling._
import geotrellis.spark.pyramid._
import geotrellis.spark.tiling._
import geotrellis.util._
import geotrellis.vector._
import geotrellis.spark.etl.config._

import org.apache.spark._
import org.apache.spark.rdd.RDD
import com.typesafe.scalalogging.LazyLogging

import scala.reflect._
import scala.reflect.runtime.universe._

object Etl {
  val defaultModules = Array(s3.S3Module, hadoop.HadoopModule, file.FileModule, accumulo.AccumuloModule, cassandra.CassandraModule, hbase.HBaseModule)

  type SaveAction[K, V, M] = (AttributeStore, Writer[LayerId, RDD[(K, V)] with Metadata[M]], LayerId, RDD[(K, V)] with Metadata[M]) => Unit

  object SaveAction {
    def DEFAULT[K, V, M] = {
      (_: AttributeStore, writer: Writer[LayerId, RDD[(K, V)] with Metadata[M]], layerId: LayerId, rdd: RDD[(K, V)] with Metadata[M]) =>
        writer.write(layerId, rdd)
    }
  }

  def ingest[
    I: Component[?, ProjectedExtent]: TypeTag: ? => TilerKeyMethods[I, K],
    K: SpatialComponent: Boundable: TypeTag,
    V <: CellGrid: TypeTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](
     args: Seq[String], modules: Seq[TypedModule] = Etl.defaultModules
   )(implicit sc: SparkContext) = {
    implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
    implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

    EtlConf(args) foreach { conf =>
      /* parse command line arguments */
      val etl = Etl(conf, modules)
      /* load source tiles using input module specified */
      val sourceTiles = etl.load[I, V]
      /* perform the reprojection and mosaicing step to fit tiles to LayoutScheme specified */
      val (zoom, tiled) = etl.tile(sourceTiles)
      /* save and optionally pyramid the mosaiced layer */
      etl.save[K, V](LayerId(etl.input.name, zoom), tiled)
    }
  }
}

case class Etl(conf: EtlConf, @transient modules: Seq[TypedModule] = Etl.defaultModules) extends LazyLogging {
  import Etl.SaveAction

  val input  = conf.input
  val output = conf.output

  def scheme: Either[LayoutScheme, LayoutDefinition] = {
    if (output.layoutScheme.nonEmpty) {
      val scheme = output.getLayoutScheme
      logger.info(scheme.toString)
      Left(scheme)
    } else if (output.layoutExtent.nonEmpty) {
      val layout = output.getLayoutDefinition
      logger.info(layout.toString)
      Right(layout)
    } else
      sys.error("Either layoutScheme or layoutExtent with cellSize must be provided")
  }

  @transient val combinedModule = modules reduce (_ union _)

  /**
    * Loads RDD of rasters using the input module specified in the arguments.
    * This RDD will contain rasters as they are stored, possibly overlapping and not conforming to any tile layout.
    *
    * @tparam I Input key type
    * @tparam V Input raster value type
    */
  def load[I: Component[?, ProjectedExtent]: TypeTag, V <: CellGrid: TypeTag]()(implicit sc: SparkContext): RDD[(I, V)] = {
    val plugin = {
      val plugins = combinedModule.findSubclassOf[InputPlugin[I, V]]
      if(plugins.isEmpty) sys.error(s"Unable to find input module for input key type '${typeTag[I].tpe.toString}' and tile type '${typeTag[V].tpe.toString}'")
      plugins.find(_.suitableFor(input.backend.`type`.name, input.format)).getOrElse(sys.error(s"Unable to find input module of type '${input.backend.`type`.name}' for format `${input.format} " +
        s"for input key type '${typeTag[I].tpe.toString}' and tile type '${typeTag[V].tpe.toString}'"))
    }

    // clip in dest crs
    input.clip.fold(plugin(conf))(extent => plugin(conf).filter { case (k, _) =>
      val pe = k.getComponent[ProjectedExtent]
      output.getCrs.fold(extent.contains(pe.extent))(crs => extent.contains(pe.extent.reproject(pe.crs, crs)))
    })
  }

  /**
    * Tiles RDD of arbitrary rasters to conform to a layout scheme or definition provided in the arguments.
    * First metadata will be collected over input rasters to determine the overall extent, common crs, and resolution.
    * This information will be used to select a LayoutDefinition if LayoutScheme is provided in the arguments.
    *
    * The tiling step will use this LayoutDefinition to cut input rasters into chunks that conform to the layout.
    * If multiple rasters contribute to single target tile their values will be merged cell by cell.
    *
    * The timing of the reproject steps depends on the method chosen.
    * BufferedReproject must be performed after the tiling step because it leans on SpatialComponent to identify neighboring
    * tiles and sample their edge pixels. This method is the default and produces the best results.
    *
    * PerTileReproject method will be performed before the tiling step, on source tiles. When using this method the
    * reproject logic does not have access to pixels past the tile boundary and will see them as NODATA.
    * However, this approach does not require all source tiles to share a projection.
    *
    * @param rdd    RDD of source rasters
    * @param method Resampling method to be used when merging raster chunks in tiling step
    */
  def tile[
    I: Component[?, ProjectedExtent]: (? => TilerKeyMethods[I, K]),
    V <: CellGrid: Stitcher: ClassTag: (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]):
    (? => TileReprojectMethods[V]): (? => CropMethods[V]),
    K: SpatialComponent: Boundable: ClassTag
  ](rdd: RDD[(I, V)], method: ResampleMethod = NearestNeighbor)(implicit sc: SparkContext): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    val targetCellType = output.cellType
    val destCrs = output.getCrs.get

    def adjustCellType(md: TileLayerMetadata[K]) =
      md.copy(cellType = targetCellType.getOrElse(md.cellType))

    output.reprojectMethod match {
      case PerTileReproject =>
        val reprojected = rdd.reproject(destCrs)
        val (zoom: Int, md: TileLayerMetadata[K]) = scheme match {
          case Left(layoutScheme) => output.maxZoom match {
            case Some(zoom) =>  reprojected.collectMetadata(destCrs, output.tileSize, zoom)
            case _ => reprojected.collectMetadata(layoutScheme)
          }
          case Right(layoutDefinition) => reprojected.collectMetadata(layoutDefinition)
        }
        val amd = adjustCellType(md)
        val tilerOptions = Tiler.Options(resampleMethod = method, partitioner = new HashPartitioner(rdd.partitions.length))
        zoom -> ContextRDD(reprojected.tileToLayout[K](amd, tilerOptions), amd)

      case BufferedReproject =>
        val (_, md) = output.maxZoom match {
          case Some(zoom) => rdd.collectMetadata(destCrs, output.tileSize, zoom)
          case _ => rdd.collectMetadata(FloatingLayoutScheme(output.tileSize))
        }
        val amd = adjustCellType(md)
        // Keep the same number of partitions after tiling.
        val tilerOptions = Tiler.Options(resampleMethod = method, partitioner = new HashPartitioner(rdd.partitions.length))
        val tiled = ContextRDD(rdd.tileToLayout[K](amd, tilerOptions), amd)
        scheme match {
          case Left(layoutScheme) =>
            tiled.reproject(destCrs, layoutScheme, method)
          case Right(layoutDefinition) =>
            tiled.reproject(destCrs, layoutDefinition, method)
        }
    }
  }

  /**
    * Saves provided RDD to an output module specified by the ETL arguments.
    * This step may perform two to one pyramiding until zoom level 1 is reached.
    *
    * @param id          Layout ID to b
    * @param rdd         Tiled raster RDD with TileLayerMetadata
    * @param saveAction  Function to be called for saving. Defaults to writing the layer.
    *                    This gives the caller an oppurtunity to modify the layer before writing,
    *                    or to save additional attributes in the attributes store.
    *
    * @tparam K  Key type with SpatialComponent corresponding LayoutDefinition
    * @tparam V  Tile raster with cells from single tile in LayoutDefinition
    */
  def save[
    K: SpatialComponent: TypeTag,
    V <: CellGrid: TypeTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]
  ](
    id: LayerId,
    rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]],
    saveAction: SaveAction[K, V, TileLayerMetadata[K]] = SaveAction.DEFAULT[K, V, TileLayerMetadata[K]]
  ): Unit = {
    implicit def classTagK = ClassTag(typeTag[K].mirror.runtimeClass(typeTag[K].tpe)).asInstanceOf[ClassTag[K]]
    implicit def classTagV = ClassTag(typeTag[V].mirror.runtimeClass(typeTag[V].tpe)).asInstanceOf[ClassTag[V]]

    val outputPlugin =
      combinedModule
        .findSubclassOf[OutputPlugin[K, V, TileLayerMetadata[K]]]
        .find { _.suitableFor(output.backend.`type`.name) }
        .getOrElse(sys.error(s"Unable to find output module of type '${output.backend.`type`.name}'"))

    def savePyramid(zoom: Int, rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]): Unit = {
      val currentId = id.copy(zoom = zoom)
      outputPlugin(currentId, rdd, conf, saveAction)

      scheme match {
        case Left(s) =>
          if (output.pyramid && zoom >= 1) {
            val (nextLevel, nextRdd) = Pyramid.up(rdd, s, zoom, output.getPyramidOptions)
            savePyramid(nextLevel, nextRdd)
          }
        case Right(_) =>
          if (output.pyramid)
            logger.error("Pyramiding only supported with layoutScheme, skipping pyramid step")
      }
    }

    savePyramid(id.zoom, rdd)
    logger.info("Done")
  }
}
