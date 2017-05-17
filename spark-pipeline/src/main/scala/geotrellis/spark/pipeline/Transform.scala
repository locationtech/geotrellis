package geotrellis.spark.pipeline

import geotrellis.proj4.CRS
import geotrellis.raster.{CellGrid, CellSize, CellType}
import geotrellis.raster.crop.CropMethods
import geotrellis.raster.merge.TileMergeMethods
import geotrellis.raster.prototype.TilePrototypeMethods
import geotrellis.raster.reproject.TileReprojectMethods
import geotrellis.raster.resample.{NearestNeighbor, PointResampleMethod, ResampleMethod}
import geotrellis.raster.stitch.Stitcher
import geotrellis.spark.tiling.{FloatingLayoutScheme, LayoutDefinition, LayoutLevel, LayoutScheme, TilerKeyMethods, ZoomedLayoutScheme}
import geotrellis.spark._
import geotrellis.spark.{Boundable, Metadata, SpatialComponent}
import geotrellis.util.Component
import geotrellis.vector.ProjectedExtent
import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag
import scala.util.Try

trait Transform extends PipelineExpr

trait TransformHomo {

}

/** Rename Inputs into groups */
case class TransformGroup(
  tags: List[String],
  tag: String,
  `type`: String = "transform.group"
) extends Transform {
  def eval[I, V](labeledRDDs: List[(String, RDD[(I, V)])]): List[(String, RDD[(I, V)])] = {
    val (grdds, rdds) = labeledRDDs.partition { case (l, _) => tags.contains(l) }
    grdds.map { case (_, rdd) => tag -> rdd } ::: rdds
  }
}

/** Merge inputs into a single Multiband RDD */
/*case class TransformMerge(
  tags: List[String],
  tag: String,
  `type`: String = "transform.merge"
) extends Transform {
  def eval[I, V, V2](rdds: List[RDD[(I, V)]]): List[RDD[(I, V2)]] = null
}*/

case class TransformMap(
  func: String, // function name
  tag: Option[String] = None,
  `type`: String = "transform.map"
) extends Transform {
  def eval[I, V](rdd: RDD[(I, V)]): RDD[(I, V)] = {
    //Class.forName(func).newInstance().asInstanceOf[PipelineFunction]
    null
  }
}

case class TransformPerTileReproject(
  crs: String,
  `type`: String = "transform.reproject.per-tile"
) extends Transform {
  def getCRS = Try(CRS.fromName(crs)) getOrElse CRS.fromString(crs)

  def eval[
    I: Component[?, ProjectedExtent],
    V <: CellGrid: (? => TileReprojectMethods[V])
  ](rdd: RDD[(I, V)]): RDD[(I, V)] = rdd.reproject(getCRS)
}

case class TransformBufferedReproject(
  crs: String,
  resampleMethod: PointResampleMethod = NearestNeighbor,
  maxZoom: Option[Int] = None,
  `type`: String = "transform.reproject.buffered"
) extends Transform {
  def getCRS = Try(CRS.fromName(crs)) getOrElse CRS.fromString(crs)

  def eval[
    K: SpatialComponent: Boundable: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => TileReprojectMethods[V]): (? => CropMethods[V]): (? => TileMergeMethods[V]): (? => TilePrototypeMethods[V])
  ](rdd: RDD[(K, V)] with Metadata[TileLayerMetadata[K]])(scheme: Either[LayoutScheme, LayoutDefinition]): (Int, RDD[(K, V)] with Metadata[TileLayerMetadata[K]]) = {
    (scheme, maxZoom) match {
      case (Left(layoutScheme: ZoomedLayoutScheme), Some(mz)) =>
        val LayoutLevel(zoom, layoutDefinition) = layoutScheme.levelForZoom(mz)
        zoom -> rdd.reproject(getCRS, layoutDefinition, resampleMethod)._2

      case (Left(layoutScheme), _) =>
        rdd.reproject(getCRS, layoutScheme, resampleMethod)

      case (Right(layoutDefinition), _) =>
        rdd.reproject(getCRS, layoutDefinition, resampleMethod)
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
      val (_, md) = rdd.collectMetadata(FloatingLayoutScheme(tileSize.get))
      md.copy(cellType = cellType.getOrElse(md.cellType))
    }
    ContextRDD(withTilerMethods(rdd).tileToLayout[K](md, resampleMethod), md)
  }
}
