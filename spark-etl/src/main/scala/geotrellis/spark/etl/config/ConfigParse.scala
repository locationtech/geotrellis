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

import com.networknt.schema.JsonSchemaFactory
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path
import org.apache.spark.SparkContext

trait ConfigParse {
  val help: String
  val requiredFields: Set[Symbol]

  val schemaFactory = JsonSchemaFactory.getInstance()

  def getJson(filePath: String, conf: Configuration): String = {
    val path = new Path(filePath)
    val fs = path.getFileSystem(conf)
    val is = fs.open(path)
    val json = scala.io.Source.fromInputStream(is).getLines.mkString(" ")
    is.close(); fs.close(); json
  }

  def jsonNodeFromString(content: String): JsonNode = new ObjectMapper().readTree(content)

  def nextOption(map: Map[Symbol, String], list: Seq[String]): Map[Symbol, String]

  def parse(args: Seq[String])(implicit sc: SparkContext) =
    nextOption(Map(), args).map { case (key, value) => key -> getJson(value, sc.hadoopConfiguration) }
}
