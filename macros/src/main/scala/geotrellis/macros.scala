//package geotrellis

import language.experimental.macros
import scala.reflect.macros.Context

package object geotrellis {
  // Keep constant values in sync with macro functions
  @inline final val byteNODATA = Byte.MinValue 
  @inline final val shortNODATA = Short.MinValue
  @inline final val NODATA = Int.MinValue

  def isNoData(b:Byte):Boolean = macro isNoDataByte_impl
  def isNoData(s:Short):Boolean = macro isNoDataShort_impl
  def isNoData(i:Int):Boolean = macro isNoDataInt_impl
  def isNoData(f:Float):Boolean = macro isNoDataFloat_impl
  def isNoData(d:Double):Boolean = macro isNoDataDouble_impl

  def isData(b:Byte):Boolean = macro isDataByte_impl
  def isData(s:Short):Boolean = macro isDataShort_impl
  def isData(i:Int):Boolean = macro isDataInt_impl
  def isData(f:Float):Boolean = macro isDataFloat_impl
  def isData(d:Double):Boolean = macro isDataDouble_impl

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
    ct.Expr(q"""java.lang.Float($f)""")
  }

  def isNoDataDouble_impl(ct:Context)(d:ct.Expr[Double]):ct.Expr[Boolean] = {
    import ct.universe._
    ct.Expr(q"""java.lang.Double($d)""")
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
