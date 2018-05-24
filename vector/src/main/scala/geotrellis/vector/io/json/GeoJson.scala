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

package geotrellis.vector.io.json

import io.circe._
import io.circe.parser.{parse => circeParse}
import cats.syntax.either._

/** An object whose methods parse string representations as GeoJson */
object GeoJson {

  /** Parse a string as Json */
  def parse[T: Decoder](json: String) =
    circeParse(json).valueOr(throw _).as[T].valueOr(throw _)

  /** Parse a file's contents as Json */
  def fromFile[T: Decoder](path: String) = {
    val src = scala.io.Source.fromFile(path)
    val txt =
      try {
        src.mkString
      } finally {
        src.close
      }
    parse[T](txt)
  }
}
