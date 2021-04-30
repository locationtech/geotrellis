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

import cats.data.Validated
import cats.effect.Sync
import cats.syntax.either._
import io.circe.Json
import io.circe.schema.Schema

trait JsonValidator[T] {
  def validate(json: Json): Either[JsonValidatorErrors, T]
  def parseAndValidate(str: String): Either[JsonValidatorErrors, T] =
    io.circe.parser.parse(str).leftMap(JsonValidatorErrors(_)).flatMap(validate)
}

object JsonValidator {
  private def readJson(resource: String): Json = {
    val stream = getClass.getResourceAsStream(resource)
    try {
      val lines = scala.io.Source.fromInputStream(stream).getLines()
      val json = lines.mkString(" ")
      io.circe.parser.parse(json).valueOr(throw _)
    } finally stream.close()
  }

  private lazy val messageHeaderSchema  = Schema.load(readJson("/json/message.schema.json"))
  private lazy val indexParametersSchema  = Schema.load(readJson("/json/index-message.schema.json"))
  private lazy val ingestParametersSchema = Schema.load(readJson("/json/ingest-message.schema.json"))
  private lazy val deleteParametersSchema = Schema.load(readJson("/json/delete-message.schema.json"))

  private def validateSchema(schema: Schema, json: Json): Validated[JsonValidatorErrors, Unit] = schema.validate(json).leftMap(JsonValidatorErrors(_))
  def validateMessageHeader(json: Json): Validated[JsonValidatorErrors, Unit] = validateSchema(messageHeaderSchema, json)
  def validateIngestParameters(json: Json): Validated[JsonValidatorErrors, Unit] = validateSchema(ingestParametersSchema, json)
  def validateIndexParameters(json: Json): Validated[JsonValidatorErrors, Unit] = validateSchema(indexParametersSchema, json)
  def validateDeleteParameters(json: Json): Validated[JsonValidatorErrors, Unit] = validateSchema(deleteParametersSchema, json)

  def apply[T](str: String)(implicit validator: JsonValidator[T]) = validator
  def parse[T: JsonValidator](str: String) = apply(str).parseAndValidate(str)
  def parseF[F[_]: Sync, T: JsonValidator](str: String) = Sync[F].fromEither(apply(str).parseAndValidate(str))
  def parseUnsafe[T: JsonValidator](str: String) = parse[T](str).valueOr(throw _)
  def validate[T](json: Json)(implicit validator: JsonValidator[T]) = validator.validate(json)
  def validateUnsafe[T: JsonValidator](json: Json) = validate[T](json).valueOr(throw _)
}
