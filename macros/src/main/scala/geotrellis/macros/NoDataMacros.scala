package geotrellis.macros

import scala.reflect.macros.Context

object NoDataMacros {
  def isNoDataByte_impl(ct: Context)(b: ct.Expr[Byte]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$b == Byte.MinValue""")
  }

  def isNoDataShort_impl(ct: Context)(s: ct.Expr[Short]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$s == Short.MinValue""")
  }

  def isNoDataInt_impl(ct: Context)(i: ct.Expr[Int]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$i == Int.MinValue""")
  }

  def isNoDataFloat_impl(ct: Context)(f: ct.Expr[Float]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""java.lang.Float.isNaN($f)""")
  }

  def isNoDataDouble_impl(ct: Context)(d: ct.Expr[Double]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""java.lang.Double.isNaN($d)""")
  }

  def isDataByte_impl(ct: Context)(b: ct.Expr[Byte]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$b != Byte.MinValue""")
  }

  def isDataShort_impl(ct: Context)(s: ct.Expr[Short]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$s != Short.MinValue""")
  }

  def isDataInt_impl(ct: Context)(i: ct.Expr[Int]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""$i != Int.MinValue""")
  }

  def isDataFloat_impl(ct: Context)(f: ct.Expr[Float]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""!java.lang.Float.isNaN($f)""")
  }

  def isDataDouble_impl(ct: Context)(d: ct.Expr[Double]): ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""!java.lang.Double.isNaN($d)""")
  }

}
