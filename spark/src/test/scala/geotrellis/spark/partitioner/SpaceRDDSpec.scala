package geotrellis.spark.partitioner

import geotrellis.spark.{KeyBounds, SpatialKey, OnlyIfCanRunSpark}
import org.apache.spark.Partitioner
import org.apache.spark.rdd.{PairRDDFunctions, RDD}
import org.scalatest._

class SpaceRDDSpec extends FunSpec with Matchers with OnlyIfCanRunSpark {
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

  val rdd3: RDD[(SpatialKey, Int)] = sc.parallelize {
    for {
      col <- 20 to 25
      row <- 20 to 25
    } yield (SpatialKey(col, row), col + row)
  }
  val bounds3 = KeyBounds(SpatialKey(20,20), SpatialKey(25,25))
  val pr3 = SpaceRDD(rdd3, SpacePartitioner(bounds3, 2))

  val rddEmpty = sc.emptyRDD[(SpatialKey, Int)]
  val prEmpty = SpaceRDD(rddEmpty, SpacePartitioner[SpatialKey]())

  def maxPartitionSize(rdd: RDD[_]): Int = {
    rdd.mapPartitions(it => Iterator(it.size)).collect().max
  }

  it("find default partitioner"){
    info(pr1.partitioner.toString)
    info(pr2.partitioner.toString)
    info(Partitioner.defaultPartitioner(pr1, pr2).toString)
  }

  it("left join correctly") {
    val res = pr1.leftOuterJoin(pr2)
    val expected = new PairRDDFunctions(rdd1).leftOuterJoin(pr2)

    info(s"PairRDDFunctions partitioner: ${expected.partitioner}")
    info(s"SpaceRDD join partitioner: ${res.partitioner}")
    res.partitioner should be (pr1.partitioner)
    res.collect() sameElements expected.collect()
    maxPartitionSize(res) should be <= 4
  }

  it("left join with non intersecting SpaceRDD") {
    val res = pr1.leftOuterJoin(pr3)
    val expected = pr1.mapValues(v => (v, None))

    res.collect() sameElements expected.collect()
  }

  it("left join to empty SpaceRDD") {
    val res = prEmpty.leftOuterJoin(pr3)
    val records =res.collect()
    records sameElements Array[(SpatialKey, Int)]()
    info("records: " + records.length)
  }

  it("inner join correctly") {
    val res = pr1.join(pr2)
    val expected = new PairRDDFunctions(rdd1).join(pr2)

    info(s"PairRDDFunctions partitioner: ${expected.partitioner}")
    info(s"SpaceRDD join partitioner: ${res.partitioner}")
    res.partitioner should be equals Some(SpacePartitioner(KeyBounds(SpatialKey(5,5), SpatialKey(10,10)), 2))
    res.collect() sameElements expected.collect()
    maxPartitionSize(res) should be <= 4
  }

  it("inner join non intersecting SpaceRDD") {
    val res = pr1.join(pr3)
    val records =res.collect()
    records sameElements Array[(SpatialKey, Int)]()
    info("records: " + records.length)
  }

}
