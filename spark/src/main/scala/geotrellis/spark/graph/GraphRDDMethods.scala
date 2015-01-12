package geotrellis.spark.graph

import geotrellis.spark._
import geotrellis.spark.graph.op._

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

  val _sc: SpatialComponent[K]

  def toRaster: RasterRDD[K] = {
    val tileLayout = graphRDD.metaData.tileLayout
    val (tileCols, tileRows) = (tileLayout.tileCols, tileLayout.tileRows)

    def createCombiner(v: (VertexId, Double)) = DList(v)

    def mergeValue(f: DList[(VertexId, Double)], v: (VertexId, Double)) = v +: f

    def mergeCombiners(
      f: DList[(VertexId, Double)],
      s: DList[(VertexId, Double)]) = f ++ s

    val tileRDD: RDD[(K, Tile)] = graphRDD.vertices.map {
      case (vertexId, (key, value)) => (key, (vertexId, value))
    }.combineByKey(createCombiner, mergeValue, mergeCombiners)
    .map { case(key, iter) =>
        val in = iter.toList.toArray.sortWith(_._1 < _._1)
        val out = Array.ofDim[Double](in.size)
        cfor(0)(_  < in.size, _ + 1) { i =>
          out(i) = in(i)._2
        }

        (key, ArrayTile(out, tileCols, tileRows))
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
