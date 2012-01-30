package trellis.run

import akka.kernel.Bootable
import scala.util.Random

import com.typesafe.config.ConfigFactory
import akka.actor.{ ActorRef, Props, Actor, ActorSystem }
import trellis.operation._
import trellis.process.{Run,OperationResult}
import trellis._

class RemoteClientApplication extends Bootable {
  val system = ActorSystem("RemoteClientApplication", ConfigFactory.load.getConfig("remoteClient"))
  val actor = system.actorOf(Props[RemoteClientActor], "remoteClientActor")
  val remoteActor = system.actorFor("akka://RemoteServerApplication@192.168.16.41:2552/user/remoteServer")

  def sendRemote(op: Run) = {
    actor ! (remoteActor, op)
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

    val cols = 512
    val rows = 512

    val e = Extent(0.0, 0.0, width, height)
    val re = RasterExtent(e, width / cols, height / rows, cols, rows)

    val data = Array.ofDim[Int](cols * rows)
    import scala.util.Random

    for (i <- 0 until cols * rows) data(i) = Random.nextInt() % 1000000

    val r1 = IntRaster(Array.fill(cols * rows)(3), cols, rows, re)
    println("Started Lookup Application")
    while (true) {
      val msg = Run(AddConstant(r1, 3))

      app.sendRemote(msg)

      Thread.sleep(50)
    }
  }
}
