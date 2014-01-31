package geotrellis.feature.benchmark

import geotrellis.feature.check.jts.Generators

import org.scalacheck.{Prop,Test,Gen,Arbitrary}

import com.vividsolutions.jts.geom._

import java.lang.System.currentTimeMillis
import scala.collection.mutable

object Bench {
  def bench[T](times:Int,body: T => Unit, v:T):Long = {
    var i = 0
    val start = currentTimeMillis()
    while(i < times) {
      body(v)
      i += 1
    }
    val duration = currentTimeMillis() - start
    duration
  }

  def bench[T1,T2](times:Int,body: (T1,T2)=> Unit, v1:T1,v2:T2):Long = {
    var i = 0
    val start = currentTimeMillis()
    while(i < times) {
      body(v1,v2)
      i += 1
    }
    val duration = currentTimeMillis() - start
    duration
  }

  def benchmark[T,K](gen:Gen[T])(f:T=>K)(body:T=>Unit): List[(K,Int,Long)] = {
    val params = Test.Parameters.default
    import params._

    val genPrms = new Gen.Parameters.Default { override val rng = params.rng }
    
    val iterations = math.ceil(minSuccessfulTests)
    val sizeStep = (maxSize-minSize) / iterations

    var n = 0 
    var skipped = 0

    val results = mutable.ListBuffer[(K,Int,Long)]()
    while(n < iterations && skipped < iterations) {
      val size = (minSize: Double) + (sizeStep * n)
      val propPrms = genPrms.resize(size.round.toInt)
      gen(propPrms) match {
        case None => skipped += 1
        case Some(value) =>
          val key = f(value)
          for (times <- Seq(1,10,50)) yield {
            if(n == 0) // warmup
              bench(times,body,value)
            else 
              results += ((key,times,bench(times,body,value)))
          }
      }
    }
    results.toList
  }

  def benchmark[T1,T2,K](gen1:Gen[T1],gen2:Gen[T2])(f:(T1,T2)=>K)(body:(T1,T2)=>Unit):List[(K,Int,Long)] = {
    val params = Test.Parameters.default
    import params._

    val genPrms = new Gen.Parameters.Default { override val rng = params.rng }
    
    val iterations = math.ceil(minSuccessfulTests)
    val sizeStep = (maxSize-minSize) / iterations

    var n = 0 
    var skipped = 0

    val results = mutable.ListBuffer[(K,Int,Long)]()
    while(n < iterations && skipped < iterations) {
      val size = (minSize: Double) + (sizeStep * n)
      val propPrms = genPrms.resize(size.round.toInt)
      gen1(propPrms) match {
        case None => skipped += 1
        case Some(value1) =>
          gen2(propPrms) match {
            case None => skipped += 1
            case Some(value2) =>
              val key = f(value1,value2)
              for (times <- Seq(1,2)) yield {
                if(n == 0) // warmup
                  bench(times,body,value1,value2)
                else
                  results += ((key,times,bench(times,body,value1,value2)))
              }
              n += 1
          }
      }
    }
    results.toList
  }

  def benchmarkArb[T,K](f:T=>K)(body:T=>Unit)(implicit arb:Arbitrary[T]): List[(K,Int,Long)] = 
    benchmark(arb.arbitrary)(f)(body)

  def benchmarkArb[T1,T2,K](f:(T1,T2)=>K)(body:(T1,T2)=>Unit)(implicit arb1:Arbitrary[T1],arb2:Arbitrary[T2]): List[(K,Int,Long)] = 
    benchmark(arb1.arbitrary,arb2.arbitrary)(f)(body)
}

object MultiBenchmark {
  import Generators._
  import Bench._

  def countMulti(m:MultiLineString):Int = m.getNumGeometries

  def do1() = {
    val results = 
      benchmarkArb((ml:MultiLineString,p:Point) => countMulti(ml))({ (ml: MultiLineString,p:Point) =>
        p.intersection(ml)//.intersection(p)
      })

    val (times,duration) =
      results
        .map { case (count,times,duration) => (count*times,duration) }
        .reduce { (t1,t2) => (t1._1+t2._1,t1._2+t2._2) }
    println(s"Average for line string point intersection: ${times/duration.toDouble}")
  }

  def do2() = {
    val results =
      benchmarkArb((ml:List[LineString],p:Point) => ml.size)({ (ml: List[LineString],p:Point) =>
        val v = 
          ml.map(_.intersection(p)).find(!_.isEmpty) match {
            case Some(p) => p
            case None => null
          }
      })

    val (times,duration) =
      results
        .map { case (count,times,duration) => (count*times,duration) }
        .reduce { (t1,t2) => (t1._1+t2._1,t1._2+t2._2) }
    println(s"Average for line string Seq point intersection: ${times/duration.toDouble}")
  }

  lazy val genListLineString:Gen[List[LineString]] = 
    Gen.choose(1,20).flatMap(Gen.containerOfN[List,LineString](_,genLineString))
  lazy val arbListLineString:Arbitrary[List[LineString]] = 
    Arbitrary(genListLineString)

  def main(args:Array[String]):Unit = {
    do1()
//    do2()
  }
}
