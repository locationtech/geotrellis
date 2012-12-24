package geotrellis.run

import akka.kernel.Bootable
import akka.actor.{ Props, Actor, ActorSystem }
import com.typesafe.config.ConfigFactory

import geotrellis.process._

class RemoteServerApplication extends Bootable {
  val system = ActorSystem("RemoteServerApplication", ConfigFactory.load.getConfig("remoteServer"))
  val server = new Server("foo", Catalog.empty("test"))

  val actor = system.actorOf(Props(new ServerActor("server", server)), "remoteServer")

  server.actor = actor
  server.system = system
 
  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}

object RemoteServer {
  def main(args: Array[String]) {
    new RemoteServerApplication
    println("Started Trellis remote server.")
    println("Ready to receive messages.")
  }
}
