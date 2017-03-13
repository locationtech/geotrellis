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

package geotrellis

package object util {
  implicit def identityComponent[T]: Component[T, T] =
    Component(v => v, (_, v) => v)

  implicit def getIdentityComponent[C, T <: C]: GetComponent[T, C] =
    GetComponent(v => v)

  implicit def setIdentityComponent[T, C <: T]: SetComponent[T, C] =
    SetComponent((_, v) => v)

  /** A sugar method for getting a component of an object that has
    * an implicitly defined lens into a component of that object
    * with a specific type.
    */
  implicit class withGetComponentMethods[T](val self: T) extends MethodExtensions[T] {
    def getComponent[C]()(implicit component: GetComponent[T, C]): C =
      component.get(self)
  }

  /** A sugar method for setting a component of an object that has
    * an implicitly defined lens into a component of that object
    * with a specific type.
    */
  implicit class withSetComponentMethods[T](val self: T) extends MethodExtensions[T] {
    def setComponent[C](value: C)(implicit component: SetComponent[T, C]): T =
      component.set(self, value)
  }
}
