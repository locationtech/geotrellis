package geotrellis.process.actors

import geotrellis._
import geotrellis.process._

import akka.actor._

/**
 * This class contains functionality to handle results from operations,
 * evaluating StepOutput, constructing OperationResults and sending them 
 * back to the client.
 */
private[actors]
class ResultHandler(server:Server,
                    client:ActorRef,
                    dispatcher:ActorRef,
                    pos:Int) {
  // This method handles a given output. It will either return a result/error
  // to the client, or dispatch more asynchronous requests, as necessary.
  def handleResult[T](output:StepOutput[T],history:History) {
    output match {
      // ok, this operation completed and we have a value. so return it.
      case Result(value) => {
        val (v,hist) = 
          value match {
            // case r:Raster => 
            //   val v = r.force
            //   val hist = history.withResult(value,forced=true)
            //   (v,hist)
            case _ => (value,history.withResult(value))
          }

        val result = OperationResult(Complete(v, hist), pos)

        client ! result
      }

      // Execute the returned operation as the next step of this calculation.
      case AndThen(op) => {
         server.actor ! RunOperation(op, pos, client, Some(dispatcher))
      }

      // there was an error, so return that as well.
      case StepError(msg, trace) => {
        client ! OperationResult(Error(msg, history.withError(msg,trace)), pos)
      }

      // we need to do more work, so as the server to do it asynchronously.
      case StepRequiresAsync(args,cb) => {
        server.actor ! RunCallback(args, pos, cb, client, dispatcher,history)
      }
    }
  }
}
