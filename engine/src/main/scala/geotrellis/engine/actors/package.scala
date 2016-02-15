package geotrellis.engine

package object actors {
  @deprecated("geotrellis-engine has been deprecated", "7b92cb2")
  type Callback[+T] = (List[Any]) => StepOutput[T]
}
