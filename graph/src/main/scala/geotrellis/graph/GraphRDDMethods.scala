package geotrellis.graph

import geotrellis.spark._
import geotrellis.graph.op._

import geotrellis.raster._

import geotrellis.vector.Line

import org.apache.spark.graphx._

import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD

import spire.syntax.cfor._

import scalaz._

import reflect.ClassTag

trait GraphRDDMethods[K] {

  implicit val keyClassTag: ClassTag[K]

  val graphRDD: GraphRDD[K]

  implicit val _sc: SpatialComponent[K]

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

  def toRaster: RDD[(K, Tile)] with Metadata[RasterMetaData] = {
    val metaData = graphRDD.metaData

    val tileLayout = metaData.tileLayout

    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)
    val tileSize = tileCols * tileRows

    val verticesGroupedByTile = graphRDD.vertices.map {
      case (vertexId, value) =>
        val (c, r) = getColAndRowFromVertexId(vertexId)
        ((c, r), value)
    }.groupBy {
      case ((c, r), value) => (c.toInt / tileCols, r.toInt / tileRows)
    }

    val keysAsPairRDD = graphRDD.keysRDD.map(k => {
      val SpatialKey(col, row) = k
      ((col, row), k)
    })

    val resRDD: RDD[(K, Tile)] = keysAsPairRDD
      .join(verticesGroupedByTile)
      .map { case(_, (key, iter)) =>
        val in = iter.toArray
        val tile = ArrayTile.empty(TypeDouble, tileCols, tileRows)
        cfor(0)(_  < in.size, _ + 1) { i =>
          val ((c, r), v) = in(i)
          val (tileCol, tileRow) = ((c % tileCols).toInt, (r % tileRows).toInt)
          val idx = tileRow * tileCols + tileCol

          tile.updateDouble(idx, v)
        }

        (key, tile)
    }

    (resRDD, metaData)
  }

  def shortestPath(sources: Seq[(Long, Long)]): GraphRDD[K] =
    ShortestPath(graphRDD, sources.map { case((c, r)) =>
      getVertexIdByColAndRow(c, r)
    })

  def shortestPath(source: (Long, Long), dest: (Long, Long)): Seq[Line] =
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

  def costDistance(points: Seq[(Int, Int)])(implicit dummy: DI): RasterRDD[K] =
    costDistance(points.map { case(c, r) => (c.toLong, r.toLong) })

  def costDistance(points: Seq[(Long, Long)]): RasterRDD[K] =
    CostDistance(graphRDD, points)

  def costDistanceWithPath(start: (Int, Int), dest: (Int, Int))
    (implicit dummy: DI): Seq[Line] =
    costDistanceWithPath(
      (start._1.toLong, start._2.toLong),
      (dest._1.toLong, dest._2.toLong)
    )

  def costDistanceWithPath(start: (Long, Long), dest: (Long, Long)): Seq[Line] =
    CostDistance(graphRDD, start, dest)

}
