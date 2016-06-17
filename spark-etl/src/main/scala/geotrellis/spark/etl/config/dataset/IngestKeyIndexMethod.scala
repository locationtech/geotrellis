package geotrellis.spark.etl.config.dataset

import geotrellis.spark.io.index._

case class IngestKeyIndexMethod(
  `type`: String,
  timeTag: Option[String],
  timeFormat: Option[String],
  temporalResolution: Option[Int]
) {
  private def _getKeyIndexMethod: KeyIndexMethod[_] = (`type`, temporalResolution) match {
    case ("rowmajor", None)    => RowMajorKeyIndexMethod
    case ("hilbert", None)     => HilbertKeyIndexMethod
    case ("hilbert", Some(tr)) => HilbertKeyIndexMethod(tr.toInt)
    case ("zorder", None)      => ZCurveKeyIndexMethod
    case ("zorder", Some(tr))  => ZCurveKeyIndexMethod.byMilliseconds(tr)
    case _                     => throw new Exception("unsupported keyIndexMethod definition")
  }

  def getKeyIndexMethod[K] = _getKeyIndexMethod.asInstanceOf[KeyIndexMethod[K]]
}
