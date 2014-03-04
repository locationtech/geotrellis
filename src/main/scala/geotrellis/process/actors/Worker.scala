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

package geotrellis.process.actors

import akka.actor._
import akka.routing._
import scala.concurrent.Await
import scala.concurrent.duration._

import geotrellis._
import geotrellis.process._

/**
 * Workers are responsible for evaluating an operation. However, if the
 * operation in question returns a step result that requires asynchronous callbacks, 
 * the work will be off-loaded to a StepAggregator.
 *
 * Thus, in practice workers only ever do work on simple operations.
 */
private[actors]
case class Worker(val serverContext: ServerContext) extends Actor {
  // Workers themselves don't have direct children. If the operation in
  // question has a step result requiring child operations be executed 
  // asynchronously, it will be processed by a StepAggregator
  // instead, who will be responsible for constructing the response (including
  // history).

  // Actor event loop
  def receive = {
    case RunOperation(op, pos, client) => {
      val handler = new ResultHandler(serverContext,client,pos)
      val history = History(op,serverContext.externalId)

      try {
        handler.handleResult(op.run(),history)
      } catch {
        case e:Throwable => {
          val error = StepError.fromException(e)
          System.err.println("Operation failed, with exception: " + 
            s"${e}\n\nStack trace:\n${error.trace}\n", error.msg,error.trace)
          handler.handleResult(error,history)
        }
      }
    }
    case x => sys.error(s"worker got unknown msg: $x")
  }
}
