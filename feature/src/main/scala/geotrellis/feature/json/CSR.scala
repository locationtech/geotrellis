package geotrellis.feature.json

/**
 * User: eugene
 * Date: 3/25/14
 */
abstract trait CSR
case class NoCSR() extends CSR
case class TextCSR() extends CSR

