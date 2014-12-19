package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.spark.graph.op._

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

  def shortestPath(sources: Seq[(Int, Int)]): GraphRDD[K] = {
    val metaData = graphRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val layoutCols = gridBounds.width - 1
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)
    val area = tileCols * tileRows

    def getOffsetByColAndRow(col: Long, row: Long) =
      area * layoutCols * row + area * col

    def getVertexIdByColAndRow(col: Long, row: Long) = {
      val tc = col / tileCols
      val tr = row / tileRows

      val offset = getOffsetByColAndRow(tc, tr)
      offset + (row % tileRows) * tileCols + col % tileCols
    }

    ShortestPath(graphRDD, sources.map { case((c, r)) =>
      getVertexIdByColAndRow(c, r)
    })
  }

}
