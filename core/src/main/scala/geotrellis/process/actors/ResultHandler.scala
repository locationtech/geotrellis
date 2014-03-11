/***
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
 ***/

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
class ResultHandler(serverContext:ServerContext,
                    client:ActorRef,
                    pos:Int) {
  // This method handles a given output. It will either return a result/error
  // to the client, or dispatch more asynchronous requests, as necessary.
  def handleResult[T](output:StepOutput[T],history:History) {
    output match {
      // ok, this operation completed and we have a value. so return it.
      case Result(value) =>
        val result = PositionedResult(Complete(value, history.withResult(value)), pos)
        client ! result

      // there was an error, so return that as well.
      case StepError(msg, trace) =>
        client ! PositionedResult(Error(msg, history.withError(msg,trace)), pos)

      // we need to do more work, so as the server to do it asynchronously.
      case StepRequiresAsync(args,cb) =>
        serverContext.serverRef ! RunCallback(args, pos, cb, client, history)

      // Execute the returned operation as the next step of this calculation.
      case AndThen(op) =>
         serverContext.serverRef ! RunOperation(op, pos, client)

      // This result needs to know how to load layer information.
      case LayerResult(f) =>
        val value = f(serverContext.layerLoader)
        val result = PositionedResult(Complete(value, history.withResult(value)),pos)
        client ! result

    }
  }
}
