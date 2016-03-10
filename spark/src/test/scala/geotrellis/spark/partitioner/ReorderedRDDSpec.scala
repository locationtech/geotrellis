package geotrellis.spark.partitioner

import geotrellis.spark._
import geotrellis.spark.io.index.zcurve._
import org.apache.spark.Partitioner
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.scalatest._

object Implicits {
  implicit object TestPartitioner extends PartitionerIndex[GridKey] {
    private val zCurveIndex = new ZGridKeyIndex(KeyBounds(GridKey(0, 0), GridKey(100, 100)))

    def rescale(key: GridKey): GridKey =
      GridKey(key.col/2, key.row/2)

    override def toIndex(key: GridKey): Long =
      zCurveIndex.toIndex(rescale(key))

    override def indexRanges(r: (GridKey, GridKey)): Seq[(Long, Long)] =
      zCurveIndex.indexRanges((rescale(r._1), rescale(r._2)))
  }
}

class ReorderedRDDSpec extends FunSpec with Matchers with TestEnvironment {
  import Implicits._

  val bounds1 = KeyBounds(GridKey(0,0), GridKey(10,10))
  val part1 = SpacePartitioner(bounds1)
  val rdd1: RDD[(GridKey, Int)] = sc.parallelize {
    for {
      col <- 0 to 10
      row <- 0 to 10
    } yield (GridKey(col, row), col + row)
  }.partitionBy(part1)


  val bounds2 = KeyBounds(GridKey(5,5), GridKey(15,15))
  val part2 = SpacePartitioner(bounds2)
  val rdd2: RDD[(GridKey, Int)] = sc.parallelize {
    for {
      col <- 5 to 15
      row <- 5 to 15
    } yield (GridKey(col, row), col + row)
  }.partitionBy(part2)

  it("should reorder partitions"){
    val res = new ReorderedSpaceRDD(rdd1, SpacePartitioner(bounds2))
    res.collect() should not be empty
  }

  it("should reorder to empty"){
    val res = new ReorderedSpaceRDD(rdd1, SpacePartitioner[GridKey](EmptyBounds))
    res.collect() shouldBe empty
  }

  val partEmpty = SpacePartitioner[GridKey](EmptyBounds)
  val rddEmpty = sc.emptyRDD[(GridKey, Int)].partitionBy(partEmpty)

  it("should reorder from empty"){
    val res = new ReorderedSpaceRDD(rddEmpty, part1)
    for (part <- res.getPartitions) {
      part.asInstanceOf[ReorderedPartition].parentPartition should be (None)
    }
    res.collect() shouldBe empty
  }
}
