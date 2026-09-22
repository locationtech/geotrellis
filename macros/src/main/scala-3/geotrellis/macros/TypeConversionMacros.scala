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

object TypeConversionMacros {

  def b2ub_impl(n: Expr[Byte])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == Byte.MinValue) 0.toByte else v }
  def b2s_impl(n: Expr[Byte])(using Quotes): Expr[Short] = '{ val v = $n; if (v == Byte.MinValue) Short.MinValue else v.toShort }
  def b2us_impl(n: Expr[Byte])(using Quotes): Expr[Short] = '{ val v = $n; if (v == Byte.MinValue) 0.toShort else v.toShort }
  def b2i_impl(n: Expr[Byte])(using Quotes): Expr[Int] = '{ val v = $n; if (v == Byte.MinValue) Int.MinValue else v.toInt }
  def b2f_impl(n: Expr[Byte])(using Quotes): Expr[Float] = '{ val v = $n; if (v == Byte.MinValue) Float.NaN else v.toFloat }
  def b2d_impl(n: Expr[Byte])(using Quotes): Expr[Double] = '{ val v = $n; if (v == Byte.MinValue) Double.NaN else v.toDouble }

  def ub2b_impl(n: Expr[Byte])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == 0.toByte) Byte.MinValue else v }
  def ub2s_impl(n: Expr[Byte])(using Quotes): Expr[Short] = '{ val v = $n; if (v == 0.toByte) Short.MinValue else (v & 0xFF).toShort }
  def ub2us_impl(n: Expr[Byte])(using Quotes): Expr[Short] = '{ ($n & 0xFF).toShort }
  def ub2i_impl(n: Expr[Byte])(using Quotes): Expr[Int] = '{ val v = $n; if (v == 0.toByte) Int.MinValue else v & 0xFF }
  def ub2f_impl(n: Expr[Byte])(using Quotes): Expr[Float] = '{ val v = $n; if (v == 0.toByte) Float.NaN else (v & 0xFF).toFloat }
  def ub2d_impl(n: Expr[Byte])(using Quotes): Expr[Double] = '{ val v = $n; if (v == 0.toByte) Double.NaN else (v & 0xFF).toDouble }

  def s2b_impl(n: Expr[Short])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == Short.MinValue) Byte.MinValue else v.toByte }
  def s2ub_impl(n: Expr[Short])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == Short.MinValue) 0.toByte else v.toByte }
  def s2us_impl(n: Expr[Short])(using Quotes): Expr[Short] = '{ val v = $n; if (v == Short.MinValue) 0.toShort else v }
  def s2i_impl(n: Expr[Short])(using Quotes): Expr[Int] = '{ val v = $n; if (v == Short.MinValue) Int.MinValue else v.toInt }
  def s2f_impl(n: Expr[Short])(using Quotes): Expr[Float] = '{ val v = $n; if (v == Short.MinValue) Float.NaN else v.toFloat }
  def s2d_impl(n: Expr[Short])(using Quotes): Expr[Double] = '{ val v = $n; if (v == Short.MinValue) Double.NaN else v.toDouble }

  def us2b_impl(n: Expr[Short])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == 0.toByte) Byte.MinValue else v.toByte }
  def us2ub_impl(n: Expr[Short])(using Quotes): Expr[Byte] = '{ $n.toByte }
  def us2s_impl(n: Expr[Short])(using Quotes): Expr[Short] = '{ val v = $n; if (v == 0.toShort) Short.MinValue else v }
  def us2i_impl(n: Expr[Short])(using Quotes): Expr[Int] = '{ val v = $n; if (v == 0.toShort) Int.MinValue else v & 0xFFFF }
  def us2f_impl(n: Expr[Short])(using Quotes): Expr[Float] = '{ val v = $n; if (v == 0.toShort) Float.NaN else (v & 0xFFFF).toFloat }
  def us2d_impl(n: Expr[Short])(using Quotes): Expr[Double] = '{ val v = $n; if (v == 0.toShort) Double.NaN else (v & 0xFFFF).toDouble }

  def i2b_impl(n: Expr[Int])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == Int.MinValue) Byte.MinValue else v.toByte }
  def i2ub_impl(n: Expr[Int])(using Quotes): Expr[Byte] = '{ val v = $n; if (v == Int.MinValue) 0.toByte else v.toByte }
  def i2s_impl(n: Expr[Int])(using Quotes): Expr[Short] = '{ val v = $n; if (v == Int.MinValue) Short.MinValue else v.toShort }
  def i2us_impl(n: Expr[Int])(using Quotes): Expr[Short] = '{ val v = $n; if (v == Int.MinValue) 0.toShort else v.toShort }
  def i2f_impl(n: Expr[Int])(using Quotes): Expr[Float] = '{ val v = $n; if (v == Int.MinValue) Float.NaN else v.toFloat }
  def i2d_impl(n: Expr[Int])(using Quotes): Expr[Double] = '{ val v = $n; if (v == Int.MinValue) Double.NaN else v.toDouble }

  def f2b_impl(n: Expr[Float])(using Quotes): Expr[Byte] = '{ val v = $n; if (java.lang.Float.isNaN(v)) Byte.MinValue else v.toByte }
  def f2ub_impl(n: Expr[Float])(using Quotes): Expr[Byte] = '{ val v = $n; if (java.lang.Float.isNaN(v)) 0.toByte else v.toByte }
  def f2s_impl(n: Expr[Float])(using Quotes): Expr[Short] = '{ val v = $n; if (java.lang.Float.isNaN(v)) Short.MinValue else v.toShort }
  def f2us_impl(n: Expr[Float])(using Quotes): Expr[Short] = '{ val v = $n; if (java.lang.Float.isNaN(v)) 0.toShort else v.toShort }
  def f2i_impl(n: Expr[Float])(using Quotes): Expr[Int] = '{ val v = $n; if (java.lang.Float.isNaN(v)) Int.MinValue else v.toInt }
  def f2d_impl(n: Expr[Float])(using Quotes): Expr[Double] = '{ $n.toDouble }

  def d2b_impl(n: Expr[Double])(using Quotes): Expr[Byte] = '{ val v = $n; if (java.lang.Double.isNaN(v)) Byte.MinValue else v.toByte }
  def d2ub_impl(n: Expr[Double])(using Quotes): Expr[Byte] = '{ val v = $n; if (java.lang.Double.isNaN(v)) 0.toByte else v.toByte }
  def d2s_impl(n: Expr[Double])(using Quotes): Expr[Short] = '{ val v = $n; if (java.lang.Double.isNaN(v)) Short.MinValue else v.toShort }
  def d2us_impl(n: Expr[Double])(using Quotes): Expr[Short] = '{ val v = $n; if (java.lang.Double.isNaN(v)) 0.toShort else v.toShort }
  def d2i_impl(n: Expr[Double])(using Quotes): Expr[Int] = '{ val v = $n; if (java.lang.Double.isNaN(v)) Int.MinValue else v.toInt }
  def d2f_impl(n: Expr[Double])(using Quotes): Expr[Float] = '{ $n.toFloat }
}
