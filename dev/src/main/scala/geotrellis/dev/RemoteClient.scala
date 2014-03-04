/*******************************************************************************
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

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
