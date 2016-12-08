package geotrellis.proj4.io.wkt

case class Authority(name: String, code: String) extends Leaf

case class ToWgs84(values:List[Double]) extends Leaf

case class Parameter(name: String, value: Number) extends Leaf

case class Axis(name: String, direction: String) extends Leaf

case class TwinAxis(first: Axis, second: Axis) extends Leaf

case class Spheroid(name: String, semiMajorAxis: AnyVal, flatteningInverse: Double, authority: Option[Authority]) extends Tree

case class Datum(name: String, spheroid: Spheroid, towsg: Option[ToWgs84], authority: Option[Authority]) extends Tree

case class Unit(name: String, conversionFact: Number, authority: Option[Authority]) extends Tree

case class PrimeM(name: String, longitude: Double, authority: Option[Authority]) extends Tree

case class VertDatum(name: String, datumType: Number, authority: Option[Authority]) extends Tree

case class LocalDatum(name: String, datumType: Number, authority: Option[Authority]) extends Tree

case class Geogcs(name: String, datum: Datum, primeM: PrimeM, angularUnit: Unit, axes: Option[List[Axis]], authority: Option[Authority]) extends Tree

case class Projection(name: String, authority: Option[Authority]) extends Tree

case class Projcs(name: String, geogcs: Geogcs, projection: Projection, params: Option[List[Parameter]], linearUnit: Unit, twinAxis: Option[TwinAxis], authority: Option[Authority]) extends Tree

case class VertCS(name: String, vertDatum: VertDatum, unit: Unit, axis: Option[Axis], authority: Option[Authority]) extends Tree

case class LocalCS(name: String, localDatum: LocalDatum, unit: Unit, axisList: List[Axis], authority: Option[Authority]) extends Tree

case class Geoccs(name: String, datum: Datum, primeM: PrimeM, unit: Unit, axis: Option[List[Axis]], authority: Option[Authority]) extends Tree

case class CompDCS(name: String, head: Any, tail: Any, authority: Option[Authority]) extends Tree

trait Leaf extends Tree

case class Node (label: String, children: Set[Tree] = Set()) extends Tree

trait Tree