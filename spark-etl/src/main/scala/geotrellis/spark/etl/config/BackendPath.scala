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

import io.circe._
import io.circe.Decoder.Result
import cats.syntax.either._

import scala.util.matching.Regex

sealed trait BackendPath
case class S3Path(url: String, bucket: String, prefix: String) extends BackendPath {
  override def toString = url
}
case class AccumuloPath(table: String) extends BackendPath {
  override def toString = table
}
case class HBasePath(table: String) extends BackendPath {
  override def toString = table
}
case class CassandraPath(keyspace: String, table: String) extends BackendPath {
  override def toString = s"${keyspace}.${table}"
}
case class HadoopPath(path: String) extends BackendPath {
  override def toString = path
}
case class UserDefinedPath(path: String) extends BackendPath {
  override def toString = path
}

object BackendPath {
  implicit val backendPathEncoder: Encoder[BackendPath] =
    Encoder.encodeString.contramap[BackendPath] { _.toString }

  case class BackendPathDecoder(bt: BackendType) extends Decoder[BackendPath] {
    val idRx = "[A-Z0-9]{20}"
    val keyRx = "[a-zA-Z0-9+/]+={0,2}"
    val slug = "[a-zA-Z0-9-.]+"
    val S3UrlRx = new Regex(s"""s3://(?:($idRx):($keyRx)@)?($slug)/{0,1}(.*)""", "aws_id", "aws_key", "bucket", "prefix")

    def apply(c: HCursor): Result[BackendPath] = {
      c.as[String].flatMap { path =>
        Right(bt match {
          case AccumuloType  => AccumuloPath(path)
          case HBaseType     => HBasePath(path)
          case CassandraType => {
            val List(keyspace, table) = path.split("\\.").toList
            CassandraPath(keyspace, table)
          }
          case S3Type => {
            val S3UrlRx(_, _, bucket, prefix) = path
            Map("bucket" -> bucket, "key" -> prefix)
            S3Path(path, bucket, prefix)
          }
          case HadoopType | FileType          => HadoopPath(path)
          case UserDefinedBackendType(s)      => UserDefinedPath(path)
          case UserDefinedBackendInputType(s) => UserDefinedPath(path)
        })
      }
    }
  }
}
