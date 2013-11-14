package geotrellis.spark

import org.apache.spark._
import SparkContext._

object Main {
  def main(args:Array[String]) = {
    val sc =  new SparkContext("local", "test")
    try {
      val rdd = sc.parallelize(Seq(1,2,3,34,44))

      val seq = rdd.count

      println(s"Wow! ${seq}")
    } finally { 
      sc.stop 
    }
  }
}
