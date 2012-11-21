package geotrellis.raster.op.focal

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
