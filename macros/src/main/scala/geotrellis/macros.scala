/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis

//import language.experimental.macros
import scala.reflect.macros.Context

object Macros {
  def isNoDataByte_impl(ct:Context)(b:ct.Expr[Byte]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$b == Byte.MinValue""")
  }

  def isNoDataShort_impl(ct:Context)(s:ct.Expr[Short]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$s == Short.MinValue""")
  }

  def isNoDataInt_impl(ct:Context)(i:ct.Expr[Int]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$i == Int.MinValue""")
  }

  def isNoDataFloat_impl(ct:Context)(f:ct.Expr[Float]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""java.lang.Float.isNaN($f)""")
  }

  def isNoDataDouble_impl(ct:Context)(d:ct.Expr[Double]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""java.lang.Double.isNaN($d)""")
  }

  def isDataByte_impl(ct:Context)(b:ct.Expr[Byte]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$b != Byte.MinValue""")
  }

  def isDataShort_impl(ct:Context)(s:ct.Expr[Short]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$s != Short.MinValue""")
  }

  def isDataInt_impl(ct:Context)(i:ct.Expr[Int]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$i != Int.MinValue""")
  }

  def isDataFloat_impl(ct:Context)(f:ct.Expr[Float]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""!java.lang.Float.isNaN($f)""")
  }

  def isDataDouble_impl(ct:Context)(d:ct.Expr[Double]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""!java.lang.Double.isNaN($d)""")
  }

}
