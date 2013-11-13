package geotrellis.benchmark.oldfocal

import geotrellis._
import geotrellis.raster._
import scala.math._

object FocalOp {
  /**
   * Allows for easy definition of focal operations using the op syntax. See
   * geotrellis.raster.op.focal.Sum for an example.
   *
   * @param r Input Raster of the operation.
   * @param s FocalStrategyType that should be used.
   * @param n Neighborhood type (e.g. Square(2), Circle(5), etc)
   * @param dfn The FocalOpDefinition that defines the FocalOpData and FocalCalculation
   *            for this operation.
   */
  def getResult(r:Raster, s:FocalStrategyType, n:Neighborhood, dfn: FocalOpDefinition) = {
    dfn match {
      case d: IntFocalOpDefinition =>
	val strategy = FocalStrategy.get[Int](s,n)
	Result(strategy.handle(r, d.newData(r), () => d.newCalc))
      case d: DoubleFocalOpDefinition =>
	val strategy = FocalStrategy.get[Double](s,n)
        Result(strategy.handle(r, d.newData(r), () => d.newCalc))
      case d: MultiTypeFocalOpDefinition => 
	r.rasterType match {
	  case TypeBit | TypeByte | TypeShort | TypeInt =>
	    val strategy = FocalStrategy.get[Int](s,n)
	    Result(strategy.handle(r, d.newIntData(r), () => d.newIntCalc))
	  case _ =>
	    val strategy = FocalStrategy.get[Double](s, n)
	    Result(strategy.handle(r, d.newDoubleData(r), () => d.newDoubleCalc))
	}
    }
  }
  
  /** Convienence function that allows you to just supply a function that supplies
   *  a new FocalCalculation[Int] */
  def getResultInt(r:Raster, 
		 s:FocalStrategyType, 
		 n:Neighborhood, 
		 newCalcFunc: () => FocalCalculation[Int]):Result[Raster] = {
    val dfn = new IntFocalOpDefinition { def newCalc = newCalcFunc() }
    getResult(r,s,n,dfn)
  }

  /** Convienence function that allows you to just supply a function that supplies
   *  a new FocalCalculation[Double] */
  def getResultDouble(r:Raster, 
		 s:FocalStrategyType, 
		 n:Neighborhood, 
		 newCalcFunc: () => FocalCalculation[Double]):Result[Raster] = {
    val dfn = new DoubleFocalOpDefinition { def newCalc = newCalcFunc() }
    getResult(r,s,n,dfn)
  }
}

/**
 * Defines methods to create new FocalCalculations and FocalOpDatas
 * that define how a focal operation
 */
sealed trait FocalOpDefinition

trait IntFocalOpDefinition extends FocalOpDefinition {
  def newCalc: FocalCalculation[Int]
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Int or
   *  extend not matching the input raster */
  def newData(r: Raster): FocalOpData[Int] = IntFocalOpData(r.rasterExtent)
}

trait DoubleFocalOpDefinition extends FocalOpDefinition {
  def newCalc: FocalCalculation[Double]
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Double
   *  extend not matching the input raster */
  def newData(r: Raster): FocalOpData[Double] = DoubleFocalOpData(r.rasterExtent)
}

trait MultiTypeFocalOpDefinition extends FocalOpDefinition {
  def newIntCalc: FocalCalculation[Int]
  def newDoubleCalc: FocalCalculation[Double]
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Int or
   *  extend not matching the input raster */
  def newIntData(r: Raster): FocalOpData[Int] = IntFocalOpData(r.rasterExtent)
  /** Makes a new raster data holder for the result of the focal operation.
   *  Override if you want a RasterData of type other than Double or
   *  extend not matching the input raster */
  def newDoubleData(r: Raster): FocalOpData[Double] = DoubleFocalOpData(r.rasterExtent)
}

/**
 * Allows the operation writer to define the most optimal
 * cell-visiting strategy for the FocalStrategy to use,
 * so that the FocalCalculation may be designed with that
 * strategy in mind to maximize caching. */
sealed trait FocalStrategyType

/** Specifies an Aggregated strategy should be used */
case object Aggregated extends FocalStrategyType
/** Specifies the Default strategy should be used */
case object Default extends FocalStrategyType
/** Specifies a Sliding strategy should be used */
case object Sliding extends FocalStrategyType

