/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark

import geotrellis.util._

import scala.util.{Failure, Success, Try}
import java.time.ZonedDateTime

import geotrellis.layers.LayerId

package object util {
  implicit class TryOption[T](option: Option[T]) {
    def mapNone(exception: => Throwable) = option match {
      case Some(t) => Success(t)
      case None    => Failure(exception)
    }
  }

  implicit class withLayerIdUtilMethods(val self: LayerId) extends MethodExtensions[LayerId] {
    def createTemporaryId(): LayerId = self.copy(name = s"${self.name}-${ZonedDateTime.now.toInstant.toEpochMilli}")
  }
}
