package geotrellis.engine

package object actors {
  @deprecated("geotrellis-engine has been deprecated", "Geotrellis Version 0.10")
  type Callback[+T] = (List[Any]) => StepOutput[T]
}
