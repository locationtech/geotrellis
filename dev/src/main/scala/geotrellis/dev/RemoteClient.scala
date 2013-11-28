package geotrellis.dev

import akka.kernel.Bootable
import scala.util.Random

import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import geotrellis.raster.op._
import geotrellis.process._
import geotrellis._
import geotrellis.raster._
import geotrellis.statistics.op._

import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.HeapMetricsSelector
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.SystemLoadAverageMetricsSelector
import akka.kernel.Bootable
import akka.actor.{ Props, Actor, ActorSystem }
import com.typesafe.config.ConfigFactory

import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.routing.ConsistentHashingRouter
import akka.routing.FromConfig
import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.HeapMetricsSelector
import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.SystemLoadAverageMetricsSelector

import akka.cluster.Cluster
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp

import geotrellis.process._
import akka.serialization._

import geotrellis.source._
import geotrellis.statistics._

class RemoteClientApplication extends Bootable {
  val server = new Server("remoteServer", Catalog.fromPath("../src/test/resources/catalog.json"))
  val router = server.getRouter()

  def startup() {
  }

  def shutdown() {
    server.shutdown()
  }
}


object RemoteClient {
  def main(args: Array[String]) {
    println("Attempting to connect to cluster.")
    if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))

    val app = new RemoteClientApplication
    val server = app.server

    Cluster(server.system) registerOnMemberUp {
      println("Joined cluster.")
      //oldTest(app)
      sourceTest(app)
      app.server.shutdown()
    }
  }


  def sourceTest(app:RemoteClientApplication):Unit = {
    val server = app.server

    val r = RasterSource("mtsthelens_tiled_cached")
      .localAdd(3)
      .tileHistograms
      .mapOp(MinFromHistogram(_))
      .distribute(app.router)
      .converge
      .map(seqInt => seqInt.reduce(math.min(_,_)))

    for (i <- 1 until 10) {
      server.run(r) match {
        case Complete(value,success) =>
          println(s"result: value")
          println(success.toString)
        case Error(msg, failure) =>
          println(msg)
          println(failure)
      }
    }
  }

  def oldTest(app:RemoteClientApplication):Unit = {
    val server = app.server
    val raster = io.LoadRaster("mtsthelens_tiled_cached")
    val op = stat.GetHistogram(raster)
    //op.limit = 5000 // TODO: Replace grouping of ops

    for(i <- 1 until 10) {
      println(" == Sending op for remote execution.")
      val start = System.currentTimeMillis

      server.run(op.dispatch(app.router)) match {
        case Complete(value,success) => println(success.toString)
        case Error(msg,failure) =>
          println(msg)
          println(failure)
      }
      val elapsed = System.currentTimeMillis - start
      println(s" ==== completed.  elapsed time: $elapsed\n")

      println(s" == executing operation locally.")
      val start2 = System.currentTimeMillis
      server.run(op) match {
        case Complete(value,success) => println(success.toString)
        case Error(msg, failure) =>
          println(msg)
          println(failure)
      }
      val elapsed2 = System.currentTimeMillis - start2
      println(s" ==== raw time for execution: $elapsed2\n\n")

      Thread.sleep(200)
    }
    server.shutdown()
  }

  def testSerialization(remoteOp:AnyRef, server:Server) {
    val serialization = SerializationExtension(server.system)
    val serializer = serialization.findSerializerFor(remoteOp)
    val bytes = serializer.toBinary(remoteOp)
    val back = serializer.fromBinary(bytes, manifest = None)
    assert(back == remoteOp)
  }
}

case class HelloWorldOp[String](s:Op[String]) extends Op1(s)({
  s => {
    println("Executing load tileset operation.")
    Result(s)
  }
})


  case class MinFromHistogram(h:Op[Histogram]) extends Op1(h)({
    (h) => Result(h.getMinValue)
  })

  case class FindMin(ints:Op[Seq[Int]]) extends Op1(ints)({
    (ints) => Result(ints.reduce(math.min(_,_)))
  })
