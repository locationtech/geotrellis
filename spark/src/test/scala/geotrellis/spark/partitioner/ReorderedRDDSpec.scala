package geotrellis.spark.partitioner

import geotrellis.spark.{KeyBounds, SpatialKey, OnlyIfCanRunSpark}
import org.apache.spark.Partitioner
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.scalatest._

class ReorderedRDDSpec extends FunSpec with Matchers with OnlyIfCanRunSpark {

  val rdd1: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 0 to 10
      row <- 0 to 10
    } yield (SpatialKey(col, row), col + row)
  }
  val bounds1 = KeyBounds(SpatialKey(0,0), SpatialKey(10,10))
  val pr1 = SpaceRDD(rdd1, SpacePartitioner(bounds1, 2))

  val rdd2: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 5 to 15
      row <- 5 to 15
    } yield (SpatialKey(col, row), col + row)
  }
  val bounds2 = KeyBounds(SpatialKey(5,5), SpatialKey(15,15))
  val pr2 = SpaceRDD(rdd2, SpacePartitioner(bounds2, 2))

  val rddEmpty = sc.emptyRDD[(SpatialKey, Int)]
  val prEmpty = SpaceRDD(rddEmpty, SpacePartitioner[SpatialKey]())

//  it("should reorder partitions"){
//    val res = new ReorderedSpaceRDD(pr1, pr2.part)
//    res.collect()
//  }
//
//  it("should reorder to empty"){
//    val res = new ReorderedSpaceRDD(pr1, SpacePartitioner[SpatialKey]())
//    res.collect()
//  }

  it("emptyRDD?"){
    val rdd = sc.emptyRDD[Int]
    info(rdd.getPartitions.toList.toString)
    rdd.collect()
  }

  it("should reorder from empty"){
    val res = new ReorderedSpaceRDD(prEmpty, pr1.part)
    info(res.getPartitions.toList.toString)
    res.collect()

  }
}