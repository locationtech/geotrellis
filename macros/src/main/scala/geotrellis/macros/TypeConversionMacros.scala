package geotrellis.macros

import scala.reflect.macros.Context

object TypeConversionMacros {

  def b2s_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Short] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Byte.MinValue) { Short.MinValue } else { n.toShort } }""")
  }

  def b2i_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Int] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Byte.MinValue) { Int.MinValue } else { n.toInt } }""")
  }

  def b2f_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Float] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Byte.MinValue) { Float.NaN } else { $n.toFloat } }""")
  }

  def b2d_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Double] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Byte.MinValue) { Double.NaN } else { n.toDouble } }""")
  }


  def ub2s_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Short] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toByte) { Short.MinValue } else { (n & 0xFF).toShort } }""")
  }

  def ub2i_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Int] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toByte) { Int.MinValue } else { n & 0xFF } }""")
  }

  def ub2f_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Float] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toByte) { Float.NaN } else { (n & 0xFF).toFloat } }""")
  }

  def ub2d_impl(c: Context)(n: c.Expr[Byte]): c.Expr[Double] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toByte) { Double.NaN } else { (n & 0xFF).toDouble } }""")
  }


  def s2b_impl(c: Context)(n: c.Expr[Short]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Short.MinValue) { Byte.MinValue } else { n.toByte } }""")
  }

  def s2i_impl(c: Context)(n: c.Expr[Short]): c.Expr[Int] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Short.MinValue) { Int.MinValue } else { n.toInt } }""")
  }

  def s2f_impl(c: Context)(n: c.Expr[Short]): c.Expr[Float] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Short.MinValue) { Float.NaN } else { n.toFloat } }""")
  }

  def s2d_impl(c: Context)(n: c.Expr[Short]): c.Expr[Double] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Short.MinValue) { Double.NaN } else { n.toDouble } }""")
  }


  def us2b_impl(c: Context)(n: c.Expr[Short]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toShort) { Byte.MinValue } else { (n & 0xFFFF).toByte } }""")
  }

  def us2i_impl(c: Context)(n: c.Expr[Short]): c.Expr[Int] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toShort) { Int.MinValue } else { n & 0xFFFF } }""")
  }

  def us2f_impl(c: Context)(n: c.Expr[Short]): c.Expr[Float] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toShort) { Float.NaN } else { (n & 0xFFFF).toFloat } }""")
  }

  def us2d_impl(c: Context)(n: c.Expr[Short]): c.Expr[Double] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == 0.toShort) { Double.NaN } else { (n & 0xFFFF).toDouble } }""")
  }


  def i2b_impl(c: Context)(n: c.Expr[Int]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Int.MinValue) { Byte.MinValue } else { n.toByte } }""")
  }

  def i2ub_impl(c: Context)(n: c.Expr[Int]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Int.MinValue) { 0.toByte } else { n.toByte } }""")
  }

  def i2s_impl(c: Context)(n: c.Expr[Int]): c.Expr[Short] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Int.MinValue) { Short.MinValue } else { n.toShort } }""")
  }

  def i2us_impl(c: Context)(n: c.Expr[Int]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Int.MinValue) { 0.toShort } else { n.toByte } }""")
  }

  def i2f_impl(c: Context)(n: c.Expr[Int]): c.Expr[Float] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Int.MinValue) { Float.NaN } else { n.toFloat } }""")
  }

  def i2d_impl(c: Context)(n: c.Expr[Int]): c.Expr[Double] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(n == Int.MinValue) { Double.NaN } else { n.toDouble } }""")
  }


  def f2b_impl(c: Context)(n: c.Expr[Float]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Float.isNaN(n)) { Byte.MinValue } else { n.toByte } }""")
  }

  def f2s_impl(c: Context)(n: c.Expr[Float]): c.Expr[Short] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Float.isNaN(n)) { Short.MinValue } else { n.toShort } }""")
  }

  def f2i_impl(c: Context)(n: c.Expr[Float]): c.Expr[Int] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Float.isNaN(n)) { Int.MinValue } else { n.toInt } }""")
  }

  def f2d_impl(c: Context)(n: c.Expr[Float]): c.Expr[Double] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Float.isNaN(n)) { Double.NaN } else { n.toDouble } }""")
  }


  def d2b_impl(c: Context)(n: c.Expr[Double]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Double.isNaN(n)) { Byte.MinValue } else { n.toByte } }""")
  }

  def d2ub_impl(c: Context)(n: c.Expr[Double]): c.Expr[Byte] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Double.isNaN(n)) { 0.toByte } else { n.toByte } }""")
  }

  def d2s_impl(c: Context)(n: c.Expr[Double]): c.Expr[Short] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Double.isNaN(n)) { Short.MinValue } else { n.toShort } }""")
  }

  def d2us_impl(c: Context)(n: c.Expr[Double]): c.Expr[Short] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Double.isNaN(n)) { 0.toShort } else { n.toShort } }""")
  }

  def d2i_impl(c: Context)(n: c.Expr[Double]): c.Expr[Int] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Double.isNaN(n)) { Int.MinValue } else { n.toInt } }""")
  }

  def d2f_impl(c: Context)(n: c.Expr[Double]): c.Expr[Float] = {
    import c.universe._
    c.Expr(q"""{ val n = $n ; if(java.lang.Double.isNaN(n)) { Float.NaN } else { n.toFloat } }""")
  }

}
