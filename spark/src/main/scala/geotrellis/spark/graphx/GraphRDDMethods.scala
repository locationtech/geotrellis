package geotrellis.spark.graphx

import geotrellis.spark._
import geotrellis.spark.graphx.lib._

import geotrellis.raster._

import org.apache.spark.graphx._
import org.apache.spark.rdd.RDD

import reflect.ClassTag

trait GraphRDDMethods[K] {

  implicit val keyClassTag: ClassTag[K]

  val graphRDD: GraphRDD[K]

  val _sc: SpatialComponent[K]

  def toRaster: RasterRDD[K] = {
    val tileLayout = graphRDD.metaData.tileLayout
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)

    val tileRDD: RDD[(K, Tile)] = graphRDD.vertices
      .groupBy { case(vertexId, (key, value)) => key }
      .map { case(key, iter) =>
        val arr = iter.toSeq.sortWith(_._1 < _._1).map(_._2._2).toArray
        (key, ArrayTile(arr, tileCols, tileRows))
    }

    new RasterRDD(tileRDD, graphRDD.metaData)
  }

  def shortestPath(sources: Seq[(Int, Int)]): GraphRDD[K] =
    ShortestPath(graphRDD, sources.map { case((c, r)) => c.toLong * r.toLong } )

}
