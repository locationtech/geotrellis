package geotrellis.pointcloud.pipeline

sealed trait ReaderType extends ExprType { val `type` = "readers" }

object ReaderTypes {
  case object bpf extends ReaderType
  case object buffer extends ReaderType
  case object faux extends ReaderType
  case object gdal extends ReaderType
  case object geowave extends ReaderType
  case object greyhound extends ReaderType
  case object ilvis2 extends ReaderType
  case object las extends ReaderType
  case object mrsid extends ReaderType
  case object nitf extends ReaderType
  case object oci extends ReaderType
  case object optech extends ReaderType
  case object pcd extends ReaderType
  case object pgpointcloud extends ReaderType
  case object ply extends ReaderType
  case object pts extends ReaderType
  case object qfit extends ReaderType
  case object rxp extends ReaderType
  case object sbet extends ReaderType
  case object sqlite extends ReaderType
  case object text extends ReaderType
  case object tindex extends ReaderType
  case object terrasolid extends ReaderType
  case object icebridge extends ReaderType

  lazy val all = List(
    bpf, buffer, faux, gdal, geowave, greyhound, ilvis2, las, mrsid, nitf, oci,
    optech, pcd, pgpointcloud, ply, pts, qfit, rxp, sbet, sqlite, text, tindex,
    terrasolid, icebridge
  )
}