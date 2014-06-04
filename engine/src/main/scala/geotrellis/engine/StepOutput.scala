/*
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
 */

package geotrellis

import geotrellis.process._

/**
 * When run, Operations will return a StepOutput. This will either indicate a
 * complete result (Result), an error (StepError), or indicate that it
 * needs other work performed asynchronously before it can continue.
 */
sealed trait StepOutput[+T]

case class Result[+T](value: T) extends StepOutput[T]
case class StepError(msg: String, trace: String) extends StepOutput[Nothing]
case class StepRequiresAsync[+T](args: Args, cb: Callback[T]) extends StepOutput[T]
case class AndThen[+T](op: Operation[T]) extends StepOutput[T]
case class LayerResult[+T](loadFunc: LayerLoader => T) extends StepOutput[T]

object StepError { 
  def fromException(e: Throwable) = { 
    val msg = e.getMessage
    val trace = e.getStackTrace.map(_.toString).mkString("\n") 
    StepError(msg, trace) 
  } 
} 
