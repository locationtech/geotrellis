package geotrellis.pointcloud.pipeline

sealed trait WriterType extends ExprType { val `type` = "writers" }

object WriterTypes {
  case object bpf extends WriterType
  case object derivative extends WriterType
  case object gdal extends WriterType
  case object geowave extends WriterType
  case object las extends WriterType
  case object matlab extends WriterType
  case object nitf extends WriterType
  case object `null` extends WriterType
  case object oci extends WriterType
  case object optech extends WriterType
  case object p2g extends WriterType
  case object pcd extends WriterType
  case object pgpointcloud extends WriterType
  case object pclvisualizer extends WriterType
  case object ply extends WriterType
  case object rialto extends WriterType
  case object sbet extends WriterType
  case object sqlite extends WriterType
  case object text extends WriterType

  lazy val all = List(
    bpf, derivative, gdal, geowave, las, matlab, nitf, oci, optech,
    pcd, pgpointcloud, pclvisualizer, p2g, ply, rialto, sbet, sqlite, text
  )
}