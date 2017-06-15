package geotrellis.spark.pipeline.json

import geotrellis.spark.pipeline._
import geotrellis.raster.crop.CropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject.TileReprojectMethods
import geotrellis.raster.reproject.Reproject.{Options => RasterReprojectOptions}
import geotrellis.raster.resample.{NearestNeighbor, PointResampleMethod}
import geotrellis.raster.stitch.Stitcher
import geotrellis.raster.{CellGrid, CellSize, CellType}
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutLevel, LayoutScheme, TilerKeyMethods, ZoomedLayoutScheme}
import geotrellis.spark.{Boundable, Metadata, SpatialComponent, _}
import geotrellis.util.Component
import geotrellis.vector.ProjectedExtent

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

trait Transform extends PipelineExpr

/** Rename Inputs into groups */
case class TransformGroup(
  `type`: String,
  tags: List[String],
  tag: String
) extends Transform

/** Merge inputs into a single Multiband RDD */
case class TransformMerge(
  `type`: String,
  tags: List[String],
  tag: String
) extends Transform

case class TransformMap(
  `type`: String,
  func: String, // function name
  tag: Option[String] = None
) extends Transform

case class TransformPerTileReproject(
  `type`: String,
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None
) extends Transform {
  def eval[I: Component[?, ProjectedExtent], V <: CellGrid: (? => TileReprojectMethods[V])](rdd: RDD[(I, V)]): RDD[(I, V)] = {
    (scheme, maxZoom) match {
      case (Left(layoutScheme: ZoomedLayoutScheme), Some(mz)) =>
        val LayoutLevel(zoom, layoutDefinition) = layoutScheme.levelForZoom(mz)
        rdd.reproject(this.getCRS, RasterReprojectOptions(method = resampleMethod, targetCellSize = Some(layoutDefinition.cellSize)))

      case _ => rdd.reproject(this.getCRS)
    }
  }
}

case class TransformBufferedReproject(
  `type`: String,
  crs: String,
  scheme: Either[LayoutScheme, LayoutDefinition],
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None
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

case class TransformTile(
  resampleMethod: PointResampleMethod = NearestNeighbor,
  //resampleMethod: String = "nearest-neighbor", // nearest-neighbor | bilinear | cubic-convolution | cubic-spline | lanczos,
  layoutScheme: String = "zoomed", // floating | zoomed
  tileSize: Option[Int] = None,
  cellSize: Option[CellSize] = None,
  cellType: Option[CellType] = None,
  partitions: Option[Int] = None,
  `type`: String = "transform.tile"
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
