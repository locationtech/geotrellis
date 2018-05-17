/*
 * Copyright 2018 Azavea
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

package geotrellis.spark.io.hadoop.geotiff

import geotrellis.vector.ProjectedExtent

trait CollectionAttributeStore[T] extends AttributeStore[Seq, T]
trait IteratorAttributeStore[T] extends AttributeStore[Iterator, T]

/** Layer that works with Metadata Index ?? */
trait AttributeStore[M[_], T] {
  /**
    * The only one query that has to be implemented with this interface
    * We are going to check this theory by implementing PSQL AttributeStore
    * */
  def query(layerName: Option[String], extent: Option[ProjectedExtent]): M[T]

  def query(layerName: String, extent: ProjectedExtent): M[T] =
    query(Some(layerName), Some(extent))

  def query(layerName: String): M[T] =
    query(Some(layerName), None)

  def query(extent: ProjectedExtent): M[T] =
    query(None, Some(extent))

  def query: M[T] = query(None, None)
}
