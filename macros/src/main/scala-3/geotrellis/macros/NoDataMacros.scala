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

package geotrellis.macros

import scala.quoted.*

object NoDataMacros {
  def isNoDataByte_impl(b: Expr[Byte])(using Quotes): Expr[Boolean] = '{ $b == Byte.MinValue }
  def isNoDataShort_impl(s: Expr[Short])(using Quotes): Expr[Boolean] = '{ $s == Short.MinValue }
  def isNoDataInt_impl(i: Expr[Int])(using Quotes): Expr[Boolean] = '{ $i == Int.MinValue }
  def isNoDataFloat_impl(f: Expr[Float])(using Quotes): Expr[Boolean] = '{ java.lang.Float.isNaN($f) }
  def isNoDataDouble_impl(d: Expr[Double])(using Quotes): Expr[Boolean] = '{ java.lang.Double.isNaN($d) }

  def isDataByte_impl(b: Expr[Byte])(using Quotes): Expr[Boolean] = '{ $b != Byte.MinValue }
  def isDataShort_impl(s: Expr[Short])(using Quotes): Expr[Boolean] = '{ $s != Short.MinValue }
  def isDataInt_impl(i: Expr[Int])(using Quotes): Expr[Boolean] = '{ $i != Int.MinValue }
  def isDataFloat_impl(f: Expr[Float])(using Quotes): Expr[Boolean] = '{ !java.lang.Float.isNaN($f) }
  def isDataDouble_impl(d: Expr[Double])(using Quotes): Expr[Boolean] = '{ !java.lang.Double.isNaN($d) }
}
