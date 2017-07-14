package geotrellis.spark.pipeline.json.transform

import io.circe.generic.extras.ConfiguredJsonCodec

import geotrellis.raster.crop.CropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.raster.reproject.TileReprojectMethods
import geotrellis.raster.resample.{NearestNeighbor, PointResampleMethod}
import geotrellis.raster.stitch.Stitcher
import geotrellis.raster.{CellGrid, CellSize, CellType}
import geotrellis.spark.pipeline.json._
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutLevel, LayoutScheme, Tiler, TilerKeyMethods, ZoomedLayoutScheme}
import geotrellis.spark._
import geotrellis.util.Component
import geotrellis.vector.ProjectedExtent

import org.apache.spark.HashPartitioner
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

trait Transform extends PipelineExpr

/** Rename Inputs into groups */
@ConfiguredJsonCodec
case class Group(
  tags: List[String],
  tag: String,
  `type`: String = "transform.group"
) extends Transform

/** Merge inputs into a single Multiband RDD */
@ConfiguredJsonCodec
case class Merge(
  tags: List[String],
  tag: String,
  `type`: String = "transform.merge"
) extends Transform

@ConfiguredJsonCodec
case class Map(
  func: String, // function name
  tag: Option[String] = None,
  `type`: String = "transform.map"
) extends Transform

@ConfiguredJsonCodec
case class PerTileReproject(
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: String = "transform.reproject.per-tile"
) extends Transform {
  def eval[I: Component[?, ProjectedExtent], V <: CellGrid: (? => TileReprojectMethods[V])](rdd: RDD[(I, V)]): RDD[(I, V)] = {
    (scheme, maxZoom) match {
      case (Left(layoutScheme: ZoomedLayoutScheme), Some(mz)) =>
        val LayoutLevel(_, layoutDefinition) = layoutScheme.levelForZoom(mz)
        rdd.reproject(this.getCRS, RasterReprojectOptions(method = resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))

      case _ => rdd.reproject(this.getCRS)
    }
  }
}

@ConfiguredJsonCodec
case class BufferedReproject(
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: String = "transform.reproject.buffered"
) extends Transform {
  def eval[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    (scheme, maxZoom) match {
      case (Left(layoutScheme: ZoomedLayoutScheme), Some(mz)) =>
        val LayoutLevel(zoom, layoutDefinition) = layoutScheme.levelForZoom(mz)
        zoom -> rdd.reproject(this.getCRS, layoutDefinition, RasterReprojectOptions(method = resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))._2

      case (Left(layoutScheme), _) =>
        rdd.reproject(this.getCRS, layoutScheme, resampleMethod)

      case (Right(layoutDefinition), _) =>
        rdd.reproject(this.getCRS, layoutDefinition, resampleMethod)
    }
  }
}

@ConfiguredJsonCodec
case class TileToLayout(
  resampleMethod: PointResampleMethod = NearestNeighbor,
  tileSize: Option[Int] = None,
  cellSize: Option[CellSize] = None,
  cellType: Option[CellType] = None,
  partitions: Option[Int] = None,
  `type`: String = "transform.tile-to-layout"
) extends Transform {
  def eval[
    K: Boundable: SpatialComponent: ClassTag,
    I: Component[?, ProjectedExtent]: ? => TilerKeyMethods[I, K],
    V <: CellGrid: (? => TileReprojectMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]): ClassTag
  ](rdd: RDD[(I, V)]): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {
    val md = { // collecting floating metadata allows detecting upsampling
      val (_, md) = rdd.collectMetadata(FloatingLayoutScheme(tileSize.getOrElse(256)))
      md.copy(cellType = cellType.getOrElse(md.cellType))
    }
    ContextRDD(withTilerMethods(rdd).tileToLayout[K](md, resampleMethod), md)
  }
}

@ConfiguredJsonCodec
case class TileToLayoutWithZoom(
  resampleMethod: PointResampleMethod = NearestNeighbor,
  scheme: Either[LayoutScheme, LayoutDefinition],
  tileSize: Option[Int] = None,
  cellSize: Option[CellSize] = None,
  cellType: Option[CellType] = None,
  partitions: Option[Int] = None,
  maxZoom: Option[Int] = None,
  `type`: String = "transform.tile-to-layout-with-zoom"
) extends Transform {
  def eval[
    K: Boundable: SpatialComponent: ClassTag,
    I: Component[?, ProjectedExtent]: ? => TilerKeyMethods[I, K],
    V <: CellGrid: (? => TileReprojectMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V]): ClassTag
  ](rdd: RDD[(I, V)]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    /** Tile layers form some resolution and adjust partition count based on resolution difference */
    def resizingTileRDD(
      rdd: RDD[(I, V)],
      floatMD: TileLayerMetadata[K],
      targetLayout: LayoutDefinition
    ): RDD[(K, V)] with Metadata[TileLayerMetadata[K]] = {
      // rekey metadata to targetLayout
      val newSpatialBounds = KeyBounds(targetLayout.mapTransform(floatMD.extent))
      val tiledMD = floatMD.copy(
        bounds = floatMD.bounds.setSpatialBounds(newSpatialBounds),
        layout = targetLayout
      )

      // > 1 means we're upsampling during tiling process
      val resolutionRatio = floatMD.layout.cellSize.resolution / targetLayout.cellSize.resolution
      val tilerOptions = Tiler.Options(
        resampleMethod = resampleMethod,
        partitioner = new HashPartitioner(
          partitions = (math.pow(2, (resolutionRatio - 1) * 2) * rdd.partitions.length).toInt))

      val tiledRDD = rdd.tileToLayout[K](tiledMD, tilerOptions)
      ContextRDD(tiledRDD, tiledMD)
    }

    val floatMD = rdd.collectMetadata(FloatingLayoutScheme(tileSize.getOrElse(256)))._2

    scheme match {
      case Left(scheme: ZoomedLayoutScheme) if maxZoom.isDefined =>
        val LayoutLevel(zoom, layoutDefinition) = scheme.levelForZoom(maxZoom.get)
        zoom -> resizingTileRDD(rdd, floatMD, layoutDefinition)

      case Left(scheme) => // True for both FloatinglayoutScheme and ZoomedlayoutScheme
        val LayoutLevel(zoom, layoutDefinition) = scheme.levelFor(floatMD.extent, floatMD.cellSize)
        zoom -> resizingTileRDD(rdd, floatMD, layoutDefinition)

      case Right(layoutDefinition) =>
        0 -> resizingTileRDD(rdd, floatMD, layoutDefinition)
    }
  }
}
