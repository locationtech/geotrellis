package geotrellis.process

/**
 * CalculationResult contains an operation's results.
 *
 * This could include the resulting value the operation produced, an error
 * that prevented the operation from completing, and the history of the
 * operation.
 */
sealed trait CalculationResult[+T]

/**
 * Internal version to include Inlined;
 * outside match statements shouldn't get warned about 
 * not handling an Inlined case since they should never leak outside.
 */
private[process] 
sealed trait InternalCalculationResult[+T]

/**
 * CalculationResult for an operation which was a literal argument.
 *
 * Instances of Inlined should never leak out of the actor world. E.g. messages
 * sent to clients in the GeoTrellis world should either be Complete or Failure.
 *
 * Inlined exists because these arguments don't have useful history, and
 * Calculations need to distinguish them from Complete results (which were
 * calculated operations with history).
 */
private[process]
case class Inlined[T](value:T) extends InternalCalculationResult[T]

/**
 * CalculationResult for a successful operation.
 */
case class Complete[T](value:T, history:History) extends CalculationResult[T]
                                                    with InternalCalculationResult[T]

/**
 * CalculationResult for a failed operation.
 */
case class Error(message:String, history:History) extends CalculationResult[Nothing]
                                                     with InternalCalculationResult[Nothing]
