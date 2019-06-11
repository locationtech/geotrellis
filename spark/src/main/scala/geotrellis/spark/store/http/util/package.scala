package geotrellis.spark.store.http

import org.apache.commons.validator.routines.UrlValidator


package object util {
  final val SCHEMES = Array("http", "https")

  @transient private[util] lazy val urlValidator = new UrlValidator(SCHEMES, UrlValidator.ALLOW_LOCAL_URLS)
}
