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
        val arr = iter.toSeq.sortWith(_._1 < _._1).map(_._2._2).toArray // TODO: fix
        (key, ArrayTile(arr, tileCols, tileRows))
    }

    new RasterRDD(tileRDD, graphRDD.metaData)
  }

  private lazy val getVertexIdByColAndRow: (Long, Long) => VertexId = {
    val metaData = graphRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val (layoutCols, layoutRows) = (gridBounds.width - 1, gridBounds.height - 1)
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)

    (col: Long, row: Long) => {
      val layoutCol = col / tileCols
      val layoutRow = row / tileRows

      val tileCol = col % tileCols
      val tileRow = row % tileRows

      ((layoutRow * tileRows + tileRow)
        * tileCols * layoutCols + layoutCol * tileCols + tileCol)
    }
  }

  private lazy val getColAndRowFromVertexId: (VertexId) => (Long, Long) = {
    val metaData = graphRDD.metaData
    val gridBounds = metaData.gridBounds
    val tileLayout = metaData.tileLayout

    val totalCols = tileLayout.tileCols * (gridBounds.width - 1)

    (vertexId: VertexId) => (vertexId % totalCols, vertexId / totalCols)
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
      .map(seq => {
        val doubleCoordinates = seq.map(vertexId => {
          val (c, r) = getColAndRowFromVertexId(vertexId)
          (c.toDouble, r.toDouble)
        })

        Line(doubleCoordinates)
      })

}
