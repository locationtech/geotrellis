package geotrellis.raster

trait Raster {
  type This <: Raster

  val cellType: CellType

  def convert(cellType: CellType): This

  def dualForeach(f: Int => Unit)(g: Double => Unit): Unit =
    if (cellType.isFloatingPoint) foreachDouble(g) else foreach(f)

  def foreach(f: Int=>Unit): Unit 

  def foreachDouble(f: Double=>Unit): Unit 

  def map(f: Int => Int): This
  def combine(r2: This)(f: (Int, Int) => Int): This

  def mapDouble(f: Double => Double): This
  def combineDouble(r2: This)(f: (Double, Double) => Double): This

  def mapIfSet(f: Int => Int): This =
    map { i =>
      if(isNoData(i)) i
      else f(i)
    }

  def mapIfSetDouble(f: Double => Double): This =
    mapDouble { d =>
      if(isNoData(d)) d
      else f(d)
    }

  def dualMap(f: Int => Int)(g: Double => Double): This =
    if (cellType.isFloatingPoint) mapDouble(g) else map(f)

  def dualMapIfSet(f: Int => Int)(g: Double => Double): This =
    if (cellType.isFloatingPoint) mapIfSetDouble(g) else mapIfSet(f)

  def dualCombine(r2: This)(f: (Int, Int) => Int)(g: (Double, Double) => Double): This =
    if (cellType.union(r2.cellType).isFloatingPoint) combineDouble(r2)(g) else combine(r2)(f)

}
