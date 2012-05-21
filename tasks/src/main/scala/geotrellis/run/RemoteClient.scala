package geotrellis.run

import akka.kernel.Bootable
import scala.util.Random

import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import geotrellis.operation._
import geotrellis.process._
import geotrellis._

class RemoteClientApplication extends Bootable {


  val system = ActorSystem("RemoteClientApplication", ConfigFactory.load.getConfig("remoteClient"))
  val server = new Server("client", Catalog.empty("client"))
  val actor = system.actorOf(Props(new ServerActor("client", server)), "remoteClientActor")

  server.actor = actor
  server.system = system


  //val system = ActorSystem("RemoteClientApplication", ConfigFactory.load.getConfig("remoteClient"))
  //val actor = system.actorOf(Props[RemoteClientActor], "remoteClientActor")
  val remoteActor = system.actorFor("akka://RemoteServerApplication@192.168.16.41:2552/user/remoteServer")


  def sendRemote(op: Run) = {
    actor ! (remoteActor, op)
  }

  def runRemote(op:Operation[_]) {
    val remoteOp = op.dispatch(remoteActor)
    println("About to run operation w/ remote dispatch")
    val start = System.currentTimeMillis 
    val result = server.run(remoteOp)
    val elapsed = System.currentTimeMillis - start
    println("Finished running operation w/ remote dispatch.")
    println("Finished request: elapsed time: %d".format(elapsed))
  }

  def startup() {
  }

  def shutdown() {
    system.shutdown()
  }
}

class RemoteClientActor extends Actor {
  var startTime = 0.0
  
  def receive = {
    case (actor: ActorRef, op: Run) => { 
      startTime = System.currentTimeMillis
      actor ! op
    }
    case a:OperationResult[_] => {
      println("Result in: %f".format( System.currentTimeMillis - startTime) )
      println("Response was: " + a.toString()  )
    }
  }
}

object RemoteClient {
  def main(args: Array[String]) {
    val app = new RemoteClientApplication

    val height = 10.0
    val width = 10.0

    val cols = 256
    val rows = 256

    val e = Extent(0.0, 0.0, width, height)
    val re = RasterExtent(e, width / cols, height / rows, cols, rows)

    val data = Array.ofDim[Int](cols * rows)
    import scala.util.Random

    for (i <- 0 until cols * rows) data(i) = Random.nextInt() % 1000000

    val r1 = IntRaster(Array.fill(cols * rows)(3), re)
    println("CLIENT: Started application.")
    while (true) {
      val remoteOp = AddConstant(r1,3)
      app.runRemote(remoteOp)
    }
  }
}
