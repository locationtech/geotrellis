package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.spark.graph.op._

import geotrellis.raster._

import geotrellis.vector.Line

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

  private lazy val getVertexIdByColAndRow: (Long, Long) => VertexId = {
    val metaData = graphRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val layoutCols = gridBounds.width - 1
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)
    val area = tileCols * tileRows

    (col: Long, row: Long) => {
      val tc = col / tileCols
      val tr = row / tileRows

      val offset = area * layoutCols * tr + area * tc
      offset + (row % tileRows) * tileCols + col % tileCols
    }
  }

  private lazy val getColAndRowFromVertexId: (VertexId) => (Long, Long) = {
    val metaData = graphRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val layoutCols = gridBounds.width - 1
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)
    val area = tileCols * tileRows

    (vertexId: VertexId) => {
      val tileIndex = vertexId / area
      val tc = tileIndex % layoutCols
      val tr = tileIndex / layoutCols

      val (startCol, startRow) = (tc * tileCols, tr * layoutCols)

      val offset = area * tileIndex
      val diff = vertexId - offset
      val colInsideTile = diff % tileCols
      val rowInsideTile = diff / tileCols

      (startCol + colInsideTile, startRow + rowInsideTile)
    }
  }

  def shortestPath(sources: Seq[(Long, Long)]): GraphRDD[K] =
    ShortestPath(graphRDD, sources.map { case((c, r)) =>
      getVertexIdByColAndRow(c, r)
    })

  def shortestPath(source: (Long, Long), dest: (Long, Long)): Set[Line] =
    ShortestPath(
      graphRDD,
      getVertexIdByColAndRow(source._1, source._2),
      getVertexIdByColAndRow(dest._1, dest._2)
    )
      .map(_.map(vertexId => getColAndRowFromVertexId(vertexId)))
      .map(_.map { case(c, r) => (c.toDouble, r.toDouble) })
      .map(Line(_))

}
