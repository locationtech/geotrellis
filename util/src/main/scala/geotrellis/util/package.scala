package geotrellis

package object util {
  implicit def identityComponent[T]: Component[T, T] =
    Component(v => v, (_, v) => v)

  implicit def getIdentityComponent[C, T <: C]: GetComponent[T, C] =
    GetComponent(v => v)

  implicit def setIdentityComponent[T, C <: T]: SetComponent[T, C] =
    SetComponent((_, v) => v)

  /** A sugar method for getting a component of an object that has
    * an implicitly defined lens into a component of that object
    * with a specific type.
    */
  implicit class withGetComponentMethods[T](val self: T) extends MethodExtensions[T] {
    def getComponent[C]()(implicit component: GetComponent[T, C]): C =
      component.get(self)
  }

  /** A sugar method for setting a component of an object that has
    * an implicitly defined lens into a component of that object
    * with a specific type.
    */
  implicit class withSetComponentMethods[T](val self: T) extends MethodExtensions[T] {
    def setComponent[C](value: C)(implicit component: SetComponent[T, C]): T =
      component.set(self, value)
  }
}
