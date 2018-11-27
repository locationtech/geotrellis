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

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import spray.json._

import scala.collection.JavaConverters._

trait BaseEtlConf extends ConfigParse with LazyLogging {
  private def loggerError(str: String, color: String = Console.RED) = logger.error(s"${color}${str}${Console.RESET}")

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

  val backendProfilesSchema = schemaFactory.getSchema(getClass.getResourceAsStream("/backend-profiles-schema.json"))
  val inputSchema           = schemaFactory.getSchema(getClass.getResourceAsStream("/input-schema.json"))
  val outputSchema          = schemaFactory.getSchema(getClass.getResourceAsStream("/output-schema.json"))

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
      loggerError(s"missing required field(s): ${(requiredFields -- m.keySet).mkString(", ")}, use --help command to get additional information about input options.")
      sys.exit(1)
    }

    val(backendProfiles, input, output) = (m('backendProfiles), m('input), m('output))

    val inputValidation           = inputSchema.validate(jsonNodeFromString(input))
    val backendProfilesValidation = backendProfilesSchema.validate(jsonNodeFromString(backendProfiles))
    val outputValidation          = outputSchema.validate(jsonNodeFromString(output))

    if(!inputValidation.isEmpty || !backendProfilesValidation.isEmpty || !outputValidation.isEmpty) {
      if(!inputValidation.isEmpty) {
        loggerError(s"input validation errors:")
        inputValidation.asScala.foreach(msg => loggerError(s" - ${msg.getMessage}"))
      }
      if(!backendProfilesValidation.isEmpty) {
        loggerError(s"backendProfiles validation error:")
        backendProfilesValidation.asScala.foreach(msg => loggerError(s" - ${msg.getMessage}"))
      }
      if(!outputValidation.isEmpty) {
        loggerError(s"output validation error:")
        outputValidation.asScala.foreach(msg => loggerError(s" - ${msg.getMessage}"))
      }
      sys.exit(1)
    }

    val backendProfilesParsed = backendProfiles.parseJson.convertTo[Map[String, BackendProfile]]
    val inputsParsed = InputsFormat(backendProfilesParsed).read(input.parseJson)
    val outputParsed = OutputFormat(backendProfilesParsed).read(output.parseJson)

    inputsParsed.map(new EtlConf(_, outputParsed))
  }
}
