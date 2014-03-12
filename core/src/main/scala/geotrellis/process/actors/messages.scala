/**************************************************************************
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
 **************************************************************************/

package geotrellis.process.actors

import geotrellis._
import geotrellis.process._
import akka.actor._


/*******************************************************
 * External messages sent from GeoTrellis land (non-actors)
 *******************************************************
 */

/**
 * External message to compute the given operation and return result to sender.
 * Run child operations using default local dispatcher.
 */
case class Run(op:Operation[_])

/********************
 * Internal messages 
 ********************
 */

/**
 * Internal message to run the provided op and send the result to the client.
 */
private[actors] case class RunOperation[T](op: Operation[T], 
                                           pos: Int, 
                                           client: ActorRef)

/**
 * Internal message to compute the provided args (if necessary), invoke the
 * provided callback with the computed args, and send the result to the client.
 */
private[actors] case class RunCallback[T](args:Args, 
                                           pos:Int, 
                                           cb:Callback[T], 
                                           client:ActorRef, 
                                           history:History)

/**
 * Message used to send result values. Used internally.
 */
private[process] case class PositionedResult[T](result:InternalOperationResult[T], pos: Int)
