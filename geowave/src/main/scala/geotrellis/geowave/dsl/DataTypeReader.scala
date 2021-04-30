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

import java.net.URI

import cats.~>

/** DataTypeReader abstracts over the ability to read and split a data format during ingest.
 * The type encoding is controlled by the M type parameter.
 * It is therefore possible to read GeoTiff multiple ways by varying O and M
 */
trait DataTypeReader[F[_], O, R] { self =>
  def read(uri: URI, options: Option[O]): F[Iterator[R]]

  def mapK[G[_]](f: F ~> G): DataTypeReader[G, O, R] = new DataTypeReader[G, O, R] {
    def read(uri: URI, options: Option[O]): G[Iterator[R]] = f(self.read(uri, options))
  }
}