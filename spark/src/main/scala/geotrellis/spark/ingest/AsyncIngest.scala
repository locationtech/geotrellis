package geotrellis.spark.ingest

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample.{NearestNeighbor, ResampleMethod}
import geotrellis.spark._
import geotrellis.spark.pyramid.Pyramid
import geotrellis.spark.reproject._
import geotrellis.spark.tiling._
import geotrellis.vector._
import geotrellis.util._

import org.apache.spark.Partitioner
import org.apache.spark.rdd._
import org.apache.spark.storage.StorageLevel

import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

object AsyncIngest {
  /**
    * Represents the ingest process.
    * An ingest process produces a layer from a set of input rasters.
    *
    * The ingest process has the following steps:
    *
    *  - Reproject tiles to the desired CRS:  (CRS, RDD[(Extent, CRS), Tile)]) -> RDD[(Extent, Tile)]
    *  - Determine the appropriate layer meta data for the layer. (CRS, LayoutScheme, RDD[(Extent, Tile)]) -> LayerMetadata)
    *  - Resample the rasters into the desired tile format. RDD[(Extent, Tile)] => TileLayerRDD[K]
    *  - Optionally pyramid to top zoom level, calling sink at each level
    *
    * Ingesting is abstracted over the following variants:
    *  - The source of the input tiles, which are represented as an RDD of (T, Tile) tuples, where T: Component[?, ProjectedExtent]
    *  - The LayoutScheme which will be used to determine how to retile the input tiles.
    *
    * @param sourceTiles  RDD of tiles that have Extent and CRS
    * @param destCRS      CRS to be used by the output layer
    * @param layoutScheme LayoutScheme to be used by output layer
    * @param ec           ExecutionContext to run the futures on
    * @param pyramid      Pyramid up to level 1, sink function will be called for each level
    * @param cacheLevel   Storage level to use for RDD caching
    * @param sink         function that utilize the result of the ingest, assumed to force materialization of the RDD
    * @tparam T type of input tile key
    * @tparam K type of output tile key, must have SpatialComponent
    * @return a Future of list of throwables representing any errors that occurred from the sink method calls
    */
  def apply[T: ClassTag : ? => TilerKeyMethods[T, K] : Component[?, ProjectedExtent], K: SpatialComponent : Boundable : ClassTag](
      sourceTiles: RDD[(T, Tile)],
      destCRS: CRS,
      layoutScheme: LayoutScheme,
      ec: ExecutionContext,
      pyramid: Boolean = false,
      cacheLevel: StorageLevel = StorageLevel.NONE,
      resampleMethod: ResampleMethod = NearestNeighbor,
      partitioner: Option[Partitioner] = None,
      bufferSize: Option[Int] = None,
      maxZoom: Option[Int] = None,
      tileSize: Option[Int] = Some(256))
    (sink: (TileLayerRDD[K], Int) => Future[Unit]) : Future[List[Throwable]] = {
    implicit val executionContext: ExecutionContext = ec
    val (_, tileLayerMetadata) = (maxZoom, tileSize) match {
      case (Some(zoom), Some(tileSize)) => sourceTiles.collectMetadata(destCRS, tileSize, zoom)
      case _ => sourceTiles.collectMetadata(FloatingLayoutScheme(512))
    }
    val tiledRdd = sourceTiles.tileToLayout(tileLayerMetadata, resampleMethod).cache()

    val contextRdd = new ContextRDD(tiledRdd, tileLayerMetadata)
    val (zoom, tileLayerRdd) =
      bufferSize match {
        case Some(bs) => contextRdd.reproject(destCRS, layoutScheme, bs)
        case None => contextRdd.reproject(destCRS, layoutScheme)
      }

    tileLayerRdd.persist(cacheLevel)

    def sinkToErrorList(rdd: TileLayerRDD[K], zoom: Int): Future[List[Throwable]] =
      sink(rdd, zoom)
        .map(_ => List())
        .recover { case t =>
          List(t)
        }

    def buildPyramid(zoom: Int, rdd: TileLayerRDD[K]): List[(Int, TileLayerRDD[K], Future[List[Throwable]])] = {
      if (zoom >= 1) {
        rdd.persist(cacheLevel)
        // ensure that the future doesnt fail
        val future = sinkToErrorList(rdd, zoom)
        val pyramidLevel@(nextZoom, nextRdd) = Pyramid.up(rdd, layoutScheme, zoom, partitioner)
        (nextZoom, nextRdd, future) :: buildPyramid(nextZoom, nextRdd)
      } else {
        val future = sinkToErrorList(rdd, zoom)
        List((zoom, rdd, future))
      }
    }

    if (pyramid) {
      val results = buildPyramid(zoom, tileLayerRdd)
      val rdds = results.map(_._2)
      val futures = results.map(_._3)

      Future
        .sequence(futures)
        .map(_.flatten)
        .andThen { case _ =>
          rdds.foreach { rdd => rdd.unpersist(true) }
        }
    } else {
      sinkToErrorList(tileLayerRdd, zoom)
    }
  }
}
