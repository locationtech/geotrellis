/*
 * Copyright 2020 Azavea
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

package geotrellis.geowave.dsl.json

import cats.data.NonEmptyList
import io.circe.schema.ValidationError
import io.circe.{DecodingFailure, Error}

final case class JsonValidatorErrors(errors: NonEmptyList[Error]) extends Exception {
  def toList: List[Error] = errors.head :: errors.tail

  override def getMessage: String = errors.toList.map(_.getMessage).mkString("; ")
  override def fillInStackTrace(): Throwable = this
}

object JsonValidatorErrors {
  def apply(err: Error): JsonValidatorErrors = JsonValidatorErrors(NonEmptyList.of(err))
  def apply(nel: NonEmptyList[ValidationError])(implicit di: DummyImplicit): JsonValidatorErrors = JsonValidatorErrors(nel.map(e => DecodingFailure(e.getMessage, Nil)))
}