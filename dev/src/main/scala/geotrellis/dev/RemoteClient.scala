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

class RemoteClientApplication extends Bootable {
  val server = new Server("remoteServer", Catalog.empty("client"))
  val router = server.system.actorOf(
      Props[ServerActor].withRouter(FromConfig),
      name = "clusterRouter")

  def startup() {
  }

  def shutdown() {
    server.shutdown()
  }
}


object RemoteClient {
  def main(args: Array[String]) {
    println("Attempting to connect to cluster.")
    if (args.nonEmpty) System.setProperty("akka.remote.netty.port", args(0))

    val app = new RemoteClientApplication
    val server = app.server

    Cluster(server.system) registerOnMemberUp {
      println("Joined cluster.")

      val raster = io.LoadRaster("mtsthelens_tiled_cached")
      val op = stat.GetHistogram(raster)
      op.limit = 5000

      while(true) {
        println(" == Sending op for remote execution.")
        val start = System.currentTimeMillis
        val result = server.run(op.dispatch(app.router))
        val elapsed = System.currentTimeMillis - start
        println(s" ==== completed.  elapsed time: $elapsed\n")

        println(s" == executing operation locally.")
        val start2 = System.currentTimeMillis
        val result2 = server.run(op)
        val elapsed2 = System.currentTimeMillis - start2
        println(s" ==== raw time for execution: $elapsed2\n\n")

        Thread.sleep(200)
      } 
    }
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
