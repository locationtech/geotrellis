package geotrellis.pointcloud.pipeline

case class PipelineConstructor(list: List[PipelineExpr]) {
  def ~(e: PipelineExpr): PipelineConstructor = PipelineConstructor(list :+ e)
  def map[B](f: PipelineExpr => B): List[B] = list.map(f)
  def mapExpr(f: PipelineExpr => PipelineExpr): PipelineConstructor = PipelineConstructor(list.map(f))
  def tail: List[PipelineExpr] = list.tail
  def head: PipelineExpr = list.head
}
