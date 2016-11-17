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

package geotrellis.util

// --- //

/** A Functor definition that exposes its initial inner type.
  * This allows us to impose additional (implicit) constraints
  * on it, for instance a `SpatialComponent` that might be
  * required on `A` within the [[map]] function.
  */
trait Functor[F[_], A] extends MethodExtensions[F[A]]{
  /** Lift `f` into `F` and apply to `F[A]`. */
  def map[B](f: A => B): F[B]
}
