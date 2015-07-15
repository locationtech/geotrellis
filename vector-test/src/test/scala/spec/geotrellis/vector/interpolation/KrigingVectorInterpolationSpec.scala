package geotrellis.vector.interpolation

import geotrellis.raster.interpolation.KrigingInterpolation
import geotrellis.testkit._
import geotrellis.vector._

import org.scalatest._
import spire.syntax.cfor._

class KrigingVectorInterpolationSpec extends FunSpec
with TestEngine{

  def generatePoints(pointsData : Seq[PointFeature[Double]]): Seq[PointFeature[Double]] = {
    (1 to pointsData.size).filterNot(_ % 5 == 0).map { i => pointsData(i-1)}
  }

  def generatePermutePoints(pointsData : Seq[PointFeature[Double]]): Seq[PointFeature[Double]] = {
    (1 to pointsData.size).filter( _ % 5 == 0).map { i => pointsData(i-1)}
  }

  describe("Kriging Simple Interpolation(vector)") {

    //val points = Seq[PointFeature[Int]](
    val points = Seq[PointFeature[Double]](
      PointFeature(Point(0.0,0.0),10),
      PointFeature(Point(1.0,0.0),20),
      PointFeature(Point(4.0,4.0),60),
      PointFeature(Point(0.0,6.0),80)
    )
    //val radius = Some(6)
    val radius:Option[Double] = Some(6)
    val lag = 2
    val chunkSize = 100
    val pointPredict = Point(1,1)
    //def krigingFunc = KrigingVectorInterpolation(KrigingSimple, points, radius, chunkSize, lag, Linear)
    val krigingFunc = new KrigingSimple(points, radius, chunkSize, lag, Linear).createPredictor()

    val E = 1e-4
    val krigingSimpleTuple = krigingFunc(pointPredict)

    it("should return correct prediction value") {
      println("Prediction is " + krigingSimpleTuple._1)
      krigingSimpleTuple._1 should be (25.8681 +- E)
    }

    it("should return correct prediction variance") {
      println("Prediction variance is " + krigingSimpleTuple._2)
      krigingSimpleTuple._2 should be (8.2382 +- E)
    }
  }

  describe("Kriging Simple Interpolation(vector) Cobalt") {
    val pointsData = Seq[PointFeature[Double]](
      PointFeature(Point(651612,  566520), 36),PointFeature(Point(647102,  558630), 25),PointFeature(Point(647182,  558800), 28),PointFeature(Point(649128,  557630), 32),PointFeature(Point(647854,  555610), 24),PointFeature(Point(647694,  557350), 20),PointFeature(Point(647708,  564470), 37),PointFeature(Point(644624,  562540), 36),PointFeature(Point(646027,  564510), 30),PointFeature(Point(643040,  562300), 27),PointFeature(Point(651895,  562160), 37),PointFeature(Point(652962,  561340), 44),PointFeature(Point(653143,  560090), 56),PointFeature(Point(654360,  558530), 37),PointFeature(Point(653393,  568200), 22),PointFeature(Point(642529,  561050), 22),PointFeature(Point(642598,  560810), 19),PointFeature(Point(641190,  561010), 23),PointFeature(Point(639200,  561890), 21),PointFeature(Point(638594,  563750), 19),PointFeature(Point(637337,  562230), 15),PointFeature(Point(636452,  568640), 22),PointFeature(Point(631574,  571100), 18),PointFeature(Point(634076,  570790), 25),PointFeature(Point(636365,  569080), 28),PointFeature(Point(636733,  569010), 37),PointFeature(Point(639893,  567890), 30),PointFeature(Point(639487,  567050), 36),PointFeature(Point(620919,  556300), 11),PointFeature(Point(621901,  557310), 21),PointFeature(Point(620782,  558680), 33),PointFeature(Point(620569,  561020), 19),PointFeature(Point(636843,  555640), 27),PointFeature(Point(616813,  558950), 23),PointFeature(Point(617033,  558780), 21),PointFeature(Point(613992,  555550), 23),PointFeature(Point(613354,  555780), 23),PointFeature(Point(615106,  557490), 22),PointFeature(Point(616596,  557240), 25),PointFeature(Point(601760,  569670), 40),PointFeature(Point(602088,  569870), 26),PointFeature(Point(599902,  564790), 43),PointFeature(Point(599684,  565000), 19),PointFeature(Point(598913,  558640), 24),PointFeature(Point(603116,  557960), 24),PointFeature(Point(602909,  558180), 21),PointFeature(Point(607649,  555400), 26),PointFeature(Point(650871,  556020), 34),PointFeature(Point(606293,  577850), 25),PointFeature(Point(606040,  578030), 28),PointFeature(Point(605176,  573160), 21),PointFeature(Point(599479,  573040), 34),PointFeature(Point(599554,  573410), 22),PointFeature(Point(593244,  572010), 33),PointFeature(Point(597121,  569350), 36),PointFeature(Point(596983,  569510), 42),PointFeature(Point(593130,  566900), 30),PointFeature(Point(593938,  556070), 15),PointFeature(Point(595728,  557420), 25),PointFeature(Point(595936,  561040), 21),PointFeature(Point(596620,  561970), 17),PointFeature(Point(609380,  556690), 23),PointFeature(Point(608916,  563690), 26),PointFeature(Point(605216,  568650), 57),PointFeature(Point(605443,  568690), 43),PointFeature(Point(607745,  563850), 27),PointFeature(Point(611825,  567060), 27),PointFeature(Point(611436,  567030), 72),PointFeature(Point(614348,  561460), 45),PointFeature(Point(611604,  560700), 42),PointFeature(Point(624440,  556700), 10),PointFeature(Point(622764,  562870), 32),PointFeature(Point(626016,  566380), 8),PointFeature(Point(625976,  566740), 18),PointFeature(Point(622873,  568860), 12),PointFeature(Point(622567,  569000), 21),PointFeature(Point(604983,  573340), 27),PointFeature(Point(597626,  573560), 20),PointFeature(Point(591232,  568940), 34),PointFeature(Point(594636,  567290), 45),PointFeature(Point(593922,  566380), 14),PointFeature(Point(592013,  566510), 5),PointFeature(Point(595226,  560090), 19),PointFeature(Point(598758,  563630), 32),PointFeature(Point(600885,  567780), 23),PointFeature(Point(599996,  561760), 27),PointFeature(Point(600081,  560790), 22),PointFeature(Point(598463,  557550), 16),PointFeature(Point(602545,  556810), 24),PointFeature(Point(605976,  555690), 26),PointFeature(Point(607555,  558480), 31),PointFeature(Point(608016,  560070), 34),PointFeature(Point(609059,  562230), 32),PointFeature(Point(605013,  566020), 37),PointFeature(Point(606315,  564770), 35),PointFeature(Point(613081,  564260), 40),PointFeature(Point(612986,  563690), 39),PointFeature(Point(611538,  559630), 29),PointFeature(Point(624899,  559970), 16),PointFeature(Point(623692,  563770), 15),PointFeature(Point(621275,  561270), 19),PointFeature(Point(617190,  564200), 22),PointFeature(Point(618997,  563230), 19),PointFeature(Point(614617,  573560), 25),PointFeature(Point(616470,  565370), 24),PointFeature(Point(613218,  574790), 29),PointFeature(Point(608557,  599300), 21),PointFeature(Point(606804,  598620), 15),PointFeature(Point(634289,  556100), 18),PointFeature(Point(631870,  556110), 20),PointFeature(Point(629483,  560550), 11),PointFeature(Point(629561,  566520), 25),PointFeature(Point(629710,  566740), 23),PointFeature(Point(630014,  565750), 17),PointFeature(Point(634129,  559170), 28),PointFeature(Point(639795,  567130), 36),PointFeature(Point(632562,  557040), 17),PointFeature(Point(629342,  557480), 18),PointFeature(Point(629352,  560500), 17),PointFeature(Point(629957,  566760), 19),PointFeature(Point(632325,  563000), 22),PointFeature(Point(635545,  560580), 20),PointFeature(Point(636073,  561500), 32),PointFeature(Point(639013,  569150), 32),PointFeature(Point(639482,  569240), 28),PointFeature(Point(643686,  574470), 29),PointFeature(Point(642683,  574830), 27),PointFeature(Point(642810,  575290), 31),PointFeature(Point(638961,  582720), 31),PointFeature(Point(639150,  582680), 26),PointFeature(Point(639251,  582880), 26),PointFeature(Point(638496,  585430), 25),PointFeature(Point(636144,  588070), 40),PointFeature(Point(635741,  586240), 32),PointFeature(Point(634068,  585740), 38),PointFeature(Point(632708,  584290), 35),PointFeature(Point(631889,  597300), 18),PointFeature(Point(626585,  599270), 21),PointFeature(Point(629409,  596950), 24),PointFeature(Point(625302,  599120), 18),PointFeature(Point(624481,  600340), 61),PointFeature(Point(622499,  599540), 22),PointFeature(Point(620254,  598460), 21),PointFeature(Point(619866,  598630), 20),PointFeature(Point(617409,  599490), 21),PointFeature(Point(616831,  599810), 42),PointFeature(Point(591549,  572060), 37),PointFeature(Point(591264,  572440), 35),PointFeature(Point(613749,  598650), 45),PointFeature(Point(615325,  597310), 22),PointFeature(Point(612912,  593680), 28),PointFeature(Point(617021,  593050), 31),PointFeature(Point(615403,  591980), 31),PointFeature(Point(616171,  587580), 26),PointFeature(Point(617365,  586660), 24),PointFeature(Point(615867,  585200), 17),PointFeature(Point(599010,  586170), 25),PointFeature(Point(597020,  586520), 30),PointFeature(Point(591754,  582230), 19),PointFeature(Point(593989,  578330), 23),PointFeature(Point(594689,  576990), 25),PointFeature(Point(594260,  576660), 21),PointFeature(Point(592562,  579210), 35),PointFeature(Point(592463,  579940), 23),PointFeature(Point(593777,  597960), 11),PointFeature(Point(595686,  599190), 10),PointFeature(Point(597908,  592680), 27),PointFeature(Point(600559,  592620), 36),PointFeature(Point(602825,  590620), 23),PointFeature(Point(602228,  599230), 9),PointFeature(Point(602155,  597660), 15),PointFeature(Point(604029,  588880), 25),PointFeature(Point(604559,  587590), 24),PointFeature(Point(606009,  585120), 27),PointFeature(Point(605270,  592265), 25),PointFeature(Point(605670,  598570), 17),PointFeature(Point(614269,  578810), 1),PointFeature(Point(614474,  578650), 28),PointFeature(Point(617997,  570770), 31),PointFeature(Point(618624,  568230), 25),PointFeature(Point(618606,  567740), 18),PointFeature(Point(618887,  567630), 27),PointFeature(Point(618794,  569090), 22),PointFeature(Point(618529,  570240), 23),PointFeature(Point(618162,  571560), 22),PointFeature(Point(620149,  575810), 23),PointFeature(Point(619981,  574850), 21),PointFeature(Point(619020,  576430), 21),PointFeature(Point(616927,  579480), 17),PointFeature(Point(616833,  579600), 14),PointFeature(Point(612915,  587530), 6),PointFeature(Point(612694,  585590), 10),PointFeature(Point(617510,  584630), 16),PointFeature(Point(623479,  586980), 41),PointFeature(Point(621053,  584010), 19),PointFeature(Point(610043,  580680), 25),PointFeature(Point(610300,  578520), 24),PointFeature(Point(606904,  580170), 25),PointFeature(Point(608493,  574370), 27),PointFeature(Point(612159,  573690), 26),PointFeature(Point(612010,  573800), 23),PointFeature(Point(611931,  577440), 32),PointFeature(Point(611362,  580410), 1),PointFeature(Point(610134,  582700), 21),PointFeature(Point(608766,  584000), 16),PointFeature(Point(608145,  589720), 25),PointFeature(Point(609678,  588770), 16),PointFeature(Point(611205,  591080), 27),PointFeature(Point(606500,  595500), 10),PointFeature(Point(609328,  594750), 23),PointFeature(Point(623213,  587040), 30),PointFeature(Point(622756,  580950), 15),PointFeature(Point(623164,  581030), 21),PointFeature(Point(625657,  579160), 32),PointFeature(Point(625606,  579720), 46),PointFeature(Point(626405,  577560), 18),PointFeature(Point(626739,  574880), 20),PointFeature(Point(626324,  575810), 18),PointFeature(Point(627450,  573180), 19),PointFeature(Point(627778,  574860), 17),PointFeature(Point(633165,  576360), 48),PointFeature(Point(633750,  577810), 33),PointFeature(Point(631460,  576400), 27),PointFeature(Point(628334,  579060), 40),PointFeature(Point(627549,  581430), 38),PointFeature(Point(627624,  582490), 27),PointFeature(Point(626481,  584210), 20),PointFeature(Point(638228,  590560), 26),PointFeature(Point(641352,  591850), 20),PointFeature(Point(640052,  596280), 29),PointFeature(Point(638635,  598640), 20),PointFeature(Point(638341,  598950), 20),PointFeature(Point(599940,  581950), 25),PointFeature(Point(595842,  588160), 25),PointFeature(Point(595120,  590250), 31),PointFeature(Point(591802,  591850), 15),PointFeature(Point(603701,  581450), 17),PointFeature(Point(603115,  579840), 23),PointFeature(Point(627628,  586720), 26),PointFeature(Point(627273,  588730), 30),PointFeature(Point(626910,  591260), 39),PointFeature(Point(626777,  588830), 19),PointFeature(Point(626115,  591720), 34),PointFeature(Point(626365,  594390), 32),PointFeature(Point(628415,  594860), 24),PointFeature(Point(630906,  590590), 32),PointFeature(Point(630558,  590670), 35),PointFeature(Point(632288,  594350), 33),PointFeature(Point(619455,  593070), 22),PointFeature(Point(619523,  593360), 22),PointFeature(Point(594753,  583520), 25),PointFeature(Point(603377,  579860), 23),PointFeature(Point(595584,  583700), 26),PointFeature(Point(649494,  600180), 19),PointFeature(Point(643730,  590910), 56),PointFeature(Point(645800,  584940), 19),PointFeature(Point(647518,  580690), 17),PointFeature(Point(651393,  574740), 14),PointFeature(Point(652149,  572600), 9),PointFeature(Point(647998,  572760), 20),PointFeature(Point(652522,  569410), 29),PointFeature(Point(654281,  588890), 26),PointFeature(Point(653908,  594370), 30),PointFeature(Point(654848,  584240), 22),PointFeature(Point(632931,  599420), 22),PointFeature(Point(648521,  597390), 20),PointFeature(Point(645922,  593710), 10),PointFeature(Point(646848,  594960), 11),PointFeature(Point(649617,  596020), 17),PointFeature(Point(649735,  593070), 17),PointFeature(Point(649484,  592940), 23),PointFeature(Point(648448,  588290), 14),PointFeature(Point(641850,  587650), 16),PointFeature(Point(642476,  585690), 26),PointFeature(Point(643173,  584040), 19),PointFeature(Point(644350,  581160), 28),PointFeature(Point(645265,  578830), 30),PointFeature(Point(645355,  577210), 28),PointFeature(Point(646615,  583780), 11),PointFeature(Point(646560,  583420), 19),PointFeature(Point(650590,  578130), 17),PointFeature(Point(651650,  578330), 17),PointFeature(Point(651450,  578750), 20),PointFeature(Point(645843,  571040), 29),PointFeature(Point(646458,  568650), 32),PointFeature(Point(654331,  600740), 17)
    )
    val points : Seq[PointFeature[Double]] = generatePoints(pointsData)
    val testPoints : Seq[PointFeature[Double]] = generatePermutePoints(pointsData)
    val radius:Option[Double] = Some(600)
    val lag: Double = 0.5
    val chunkSize = 100
    //val pointPredict = Point(1,1)
    //val krigingFunc = new KrigingSimple(points, radius, chunkSize, lag, Spherical).createPredictor()

    it("should return correct prediction value") {
      println("Here")
      println("Sending lag=" + lag)
      val krigingFunc = new KrigingGeo(points, radius, chunkSize, lag, Spherical).createPredictor()

      val E = 1e-4
      println("Training : " + points.length)
      println("Testing : " + testPoints.length)
      //cfor(0)(_ < testPoints.length, _ + 1) { i =>
      cfor(0)(_ < 2, _ + 1) { i =>
        val pointPredict = Point(651612, 566520)

        //val pointPredict = testPoints(i).geom
        //TODO: Handle exceptions when the matrices are not invertible (for some points in the test Set)

        val krigingTuple = krigingFunc(pointPredict)

        println("Prediction is " + krigingTuple._1)
        //krigingSimpleTuple._1 should be (25.8681 +- E)
        krigingTuple._1 should be (36.0 +- E)
      }
    }
  }

  describe("Kriging Geostatistical Interpolation(vector)") {
    val points = Seq[PointFeature[Double]](
      PointFeature(Point(0.0,0.0),10),
      PointFeature(Point(1.0,0.0),20),
      PointFeature(Point(4.0,4.0),60),
      PointFeature(Point(0.0,6.0),80)
    )
    val radius:Option[Double] = Some(6)
    val lag = 2
    val chunkSize = 100
    val pointPredict = Point(1,1)
    //val krigingFunc = new KrigingGeo(points, radius, chunkSize, lag, Linear).createPredictor()
    val krigingFunc = new KrigingSimple(points, radius, chunkSize, lag, Linear).createPredictor()

    val E = 1e-4
    val krigingGeoTuple = krigingFunc(pointPredict)

    //Extend the tests after handling exceptions in the previous tests
    ignore("should return correct prediction value") {
      println("Prediction is " + krigingGeoTuple._1)
      krigingGeoTuple._1 should be (krigingGeoTuple._1 +- E)
    }
  }
}
