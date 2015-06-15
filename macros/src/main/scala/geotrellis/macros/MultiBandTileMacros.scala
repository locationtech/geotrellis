package geotrellis.macros

import spire.macros.InlineUtil

import scala.reflect.macros.Context

trait MacroCombinableMultiBandTile[T] {
  def combineIntTileCombiner(combiner: IntTileCombiner3): T
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner3): T

  def combineIntTileCombiner(combiner: IntTileCombiner4): T
  def combineDoubleTileCombiner(combiner: DoubleTileCombiner4): T
}

trait IntTileCombiner3 {
  val b0: Int
  val b1: Int
  val b2: Int
  def apply(z1: Int, z2: Int, z3: Int): Int
}

trait DoubleTileCombiner3 {
  val b0: Int
  val b1: Int
  val b2: Int
  def apply(z1: Double, z2: Double, z3: Double): Double
}

trait IntTileCombiner4 {
  val b0: Int
  val b1: Int
  val b2: Int
  val b3: Int
  def apply(z1: Int, z2: Int, z3: Int, z4: Int): Int
}

trait DoubleTileCombiner4 {
  val b0: Int
  val b1: Int
  val b2: Int
  val b3: Int
  def apply(z1: Double, z2: Double, z3: Double, z4: Double): Double
}


object MultiBandTileMacros {
  def intCombine3_impl[T, MBT <: MacroCombinableMultiBandTile[T]](c: Context)
    (b0: c.Expr[Int], b1: c.Expr[Int], b2: c.Expr[Int])(f: c.Expr[(Int, Int, Int) => Int]): c.Expr[T] = {
    import c.universe._
    val self = c.Expr[MacroCombinableMultiBandTile[T]](c.prefix.tree)
    val tree = 
      q"""$self.combineIntTileCombiner(new geotrellis.macros.IntTileCombiner3 {
            val b0 = $b0 ; val b1 = $b1 ; val b2 = $b2
            def apply(z1: Int, z2: Int, z3: Int): Int = $f(z1, z2, z3) 
          })"""
    new InlineUtil[c.type](c).inlineAndReset[T](tree)
  }

  def doubleCombine3_impl[T, MBT <: MacroCombinableMultiBandTile[T]](c: Context)
    (b0: c.Expr[Int], b1: c.Expr[Int], b2: c.Expr[Int])(f: c.Expr[(Double, Double, Double) => Double]): c.Expr[T] = {
    import c.universe._
    val self = c.Expr[MacroCombinableMultiBandTile[T]](c.prefix.tree)
    val tree = 
      q"""$self.combineDoubleTileCombiner(new geotrellis.macros.DoubleTileCombiner3 {
            val b0 = $b0 ; val b1 = $b1 ; val b2 = $b2
            def apply(z1: Double, z2: Double, z3: Double): Double = $f(z1, z2, z3) 
          })"""
    new InlineUtil[c.type](c).inlineAndReset[T](tree)
  }

  def intCombine4_impl[T, MBT <: MacroCombinableMultiBandTile[T]](c: Context)(b0: c.Expr[Int], b1: c.Expr[Int], b2: c.Expr[Int], b3: c.Expr[Int])
    (f: c.Expr[(Int, Int, Int, Int) => Int]): c.Expr[T] = {
    import c.universe._
    val self = c.Expr[MacroCombinableMultiBandTile[T]](c.prefix.tree)
    val tree = 
      q"""$self.combineIntTileCombiner(new geotrellis.macros.IntTileCombiner4 {
            val b0 = $b0 ; val b1 = $b1 ; val b2 = $b2 ; val b3 = $b3
            def apply(z1: Int, z2: Int, z3: Int, z4: Int): Int = $f(z1, z2, z3, z4) 
          })"""
    new InlineUtil[c.type](c).inlineAndReset[T](tree)
  }

  def doubleCombine4_impl[T, MBT <: MacroCombinableMultiBandTile[T]](c: Context)
    (b0: c.Expr[Int], b1: c.Expr[Int], b2: c.Expr[Int], b3: c.Expr[Int])(f: c.Expr[(Double, Double, Double, Double) => Double]): c.Expr[T] = {
    import c.universe._
    val self = c.Expr[MacroCombinableMultiBandTile[T]](c.prefix.tree)
    val tree = 
      q"""$self.combineDoubleTileCombiner(new geotrellis.macros.DoubleTileCombiner4 {
            val b0 = $b0 ; val b1 = $b1 ; val b2 = $b2 ; val b3 = $b3
            def apply(z1: Double, z2: Double, z3: Double, z4: Double): Double = $f(z1, z2, z3, z4) 
          })"""
    new InlineUtil[c.type](c).inlineAndReset[T](tree)
  }
}
