package geotrellis.dev

import akka.kernel.Bootable
import akka.actor.{ Props, Actor, ActorSystem }
import com.typesafe.config.ConfigFactory

import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.routing.ConsistentHashingRouter
import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.HeapMetricsSelector
import akka.cluster.routing.ClusterRouterConfig
import akka.cluster.routing.ClusterRouterSettings
import akka.cluster.routing.AdaptiveLoadBalancingRouter
import akka.cluster.routing.SystemLoadAverageMetricsSelector

import geotrellis.process._


// Run 'RemoteServer' in different sbt terminals, like the following.
// ./sbt
// project dev
// run 2551  (to listen on port 2551)

// Each time you run remote server, use a distinct port.


class RemoteServerApplication extends Bootable {
  // The client will identify this server as a candidate for work
  // by id, which is set as "remoteServer" in the client's configuration.
  val id = "remoteServer"

  println()
  val f = new java.io.File( "../src/test/resources/catalog.json" ).getCanonicalPath
  println(f)
  //val f = "src/test/resources/catalog.json"
  val server = new Server(id, Catalog.fromPath(f))

  def startup() { }

  def shutdown() {
    server.shutdown()
  }
}

object RemoteServer {
  def main(args: Array[String]) {
    if (args.nonEmpty) System.setProperty("akka.remote.netty.tcp.port", args(0))
    new RemoteServerApplication
    println("Started GeoTrellis remote server.")
    println("Ready to receive messages.")
  }
}
