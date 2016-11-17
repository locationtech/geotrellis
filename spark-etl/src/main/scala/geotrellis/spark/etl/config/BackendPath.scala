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
