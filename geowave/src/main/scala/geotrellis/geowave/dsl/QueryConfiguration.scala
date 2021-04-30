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

package geotrellis.geowave.dsl

import geotrellis.geowave.adapter.TypeName
import geotrellis.geowave.api.SQueryBuilder
import geotrellis.geowave.conf.StoreConfiguration
import org.locationtech.geowave.core.store.api.{DataStore, Query, Writer}
import org.locationtech.geowave.core.store.query.constraints.BasicQueryByClass
import org.locationtech.geowave.core.store.query.constraints.BasicQueryByClass.ConstraintsByClass
import org.locationtech.geowave.core.store.query.filter.BasicQueryFilter

trait QueryConfiguration {
  def compareOp: BasicQueryFilter.BasicQueryCompareOperation
  def constraints: Option[ConstraintsByClass]
  def query: Query[_] = {
    val q = SQueryBuilder
      .newBuilder
      .addTypeName(typeName.value)
      .indexName(indexName)

    constraints.fold(q.build)(c => q.constraints(new BasicQueryByClass(c, compareOp)).build)
  }

  def indexName: String
  def typeName: TypeName
  def namespace: Option[String]
  def dataStore: DataStore = StoreConfiguration.getDataStore(namespace)
  def writer[T]: Writer[T] = dataStore.createWriter[T](typeName.value)
}