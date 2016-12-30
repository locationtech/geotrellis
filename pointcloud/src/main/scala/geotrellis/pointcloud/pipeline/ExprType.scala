package geotrellis.pointcloud.pipeline

trait ExprType {
  val `type`: String
  lazy val name = s"${`type`}.${this.getClass.getName.split("\\$").last}"

  override def toString = name
}
