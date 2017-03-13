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

package geotrellis.spark.etl.config

import geotrellis.spark.etl.config.json._

import org.apache.spark.SparkContext
import com.github.fge.jackson.JsonLoader
import spray.json._

trait BaseEtlConf extends ConfigParse {
  val help = """
               |geotrellis-etl
               |
               |Usage: geotrellis-etl [options]
               |
               |  --input <value>
               |        input is a non-empty String property
               |  --output <value>
               |        output is a non-empty String property
               |  --backend-profiles <value>
               |        backend-profiles is a non-empty String property
               |  --help
               |        prints this usage text
             """.stripMargin

  val requiredFields = Set('input, 'output, 'backendProfiles)

  val backendProfilesSchema = schemaFactory.getJsonSchema(JsonLoader.fromResource("/backend-profiles-schema.json"))
  val inputSchema           = schemaFactory.getJsonSchema(JsonLoader.fromResource("/input-schema.json"))
  val outputSchema          = schemaFactory.getJsonSchema(JsonLoader.fromResource("/output-schema.json"))

  def nextOption(map: Map[Symbol, String], list: Seq[String]): Map[Symbol, String] =
    list.toList match {
      case Nil => map
      case "--input" :: value :: tail =>
        nextOption(map ++ Map('input -> value), tail)
      case "--backend-profiles" :: value :: tail =>
        nextOption(map ++ Map('backendProfiles -> value), tail)
      case "--output" :: value :: tail =>
        nextOption(map ++ Map('output -> value), tail)
      case "--help" :: tail => {
        println(help)
        sys.exit(1)
      }
      case option :: tail => {
        println(s"Unknown option ${option}")
        println(help)
        sys.exit(1)
      }
    }

  def apply(args: Seq[String])(implicit sc: SparkContext) = {
    val m = parse(args)

    if(m.keySet != requiredFields) {
      println(s"missing required field(s): ${(requiredFields -- m.keySet).mkString(", ")}, use --help command to get additional information about input options.")
      sys.exit(1)
    }

    val(backendProfiles, input, output) = (m('backendProfiles), m('input), m('output))

    val inputValidation           = inputSchema.validate(JsonLoader.fromString(input), true)
    val backendProfilesValidation = backendProfilesSchema.validate(JsonLoader.fromString(backendProfiles), true)
    val outputValidation          = outputSchema.validate(JsonLoader.fromString(output), true)

    if(!inputValidation.isSuccess || !backendProfilesValidation.isSuccess || !outputValidation.isSuccess) {
      if(!inputValidation.isSuccess) {
        println("input validation error:")
        println(inputValidation)
      }
      if(!backendProfilesValidation.isSuccess) {
        println("backendProfiles validation error:")
        println(backendProfilesValidation)
      }
      if(!outputValidation.isSuccess) {
        println("output validation error:")
        println(outputValidation)
      }
      sys.exit(1)
    }

    val backendProfilesParsed = backendProfiles.parseJson.convertTo[Map[String, BackendProfile]]
    val inputsParsed = InputsFormat(backendProfilesParsed).read(input.parseJson)
    val outputParsed = OutputFormat(backendProfilesParsed).read(output.parseJson)

    inputsParsed.map(new EtlConf(_, outputParsed))
  }
}
