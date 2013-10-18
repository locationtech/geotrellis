package geotrellis.raster

import geotrellis._

/**
 * This class is a lazy wrapper for any RasterData. It's only function is to
 * defer functions like map/mapIfSet/combine to produce other lazy instances.
 */
// final class LazyArrayWrapper(data: ArrayRasterData)
//   extends LazyRasterData with Wrapper {
//   def copy = this
//   def underlying = data
//   override def toArray = data.toArray
//   override def toArrayDouble = data.toArrayDouble

//   final def apply(i: Int) = underlying.apply(i)
//   final def applyDouble(i: Int) = underlying.applyDouble(i)

//   def foreach(f: Int => Unit) = data.foreach(f)
//   def map(f: Int => Int) = LazyMap(underlying, f)
//   def mapIfSet(f: Int => Int) = LazyMapIfSet(underlying, f)
//   def combine(other: RasterData)(f: (Int, Int) => Int) = other match {
//     case a: ArrayRasterData => LazyCombine(underlying, a, f)
//     case o                  => o.combine(underlying)((z2, z1) => f(z1, z2))
//   }

//   def foreachDouble(f: Double => Unit) = data.foreachDouble(f)
//   def mapDouble(f: Double => Double) = LazyMapDouble(underlying, f)
//   def mapIfSetDouble(f: Double => Double) = LazyMapIfSetDouble(underlying, f)
//   def combineDouble(other: RasterData)(f: (Double, Double) => Double) = other match {
//     case a: ArrayRasterData => LazyCombineDouble(underlying, a, f)
//     case o                  => o.combineDouble(underlying)((z2, z1) => f(z1, z2))
//   }
// }

// object LazyArrayWrapper {
//   def apply(data: ArrayRasterData): LazyRasterData = data match {
//     case d: LazyRasterData => d
//     case d                 => new LazyArrayWrapper(d)
//   }
// }
