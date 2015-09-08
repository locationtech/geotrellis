package geotrellis.raster.io.geotiff.tags

import CommonPublicValues._

import scala.collection.immutable.HashMap

object ModelTypes {

  val ModelTypeProjected = 1
  val ModelTypeGeographic = 2

}

object PrimeMeridianTypes {

  val PM_Greenwich = 8901
  val PM_Lisbon = 8902
  val PM_Paris = 8903
  val PM_Bogota = 8904
  val PM_Madrid = 8905
  val PM_Rome = 8906
  val PM_Bern = 8907
  val PM_Jakarta = 8908
  val PM_Ferro = 8909
  val PM_Brussels = 8910
  val PM_Stockholm = 8911

}

object AngularUnitTypes {

  val Angular_Radian = 9101
  val Angular_Degree = 9102
  val Angular_Arc_Minute = 9103
  val Angular_Arc_Second = 9104
  val Angular_Grad = 9105
  val Angular_Gon = 9106
  val Angular_DMS = 9107
  val Angular_DMS_Hemisphere = 9108

}

object DatumTypes {

  val DatumE_Airy1830 = 6001
  val DatumE_AiryModified1849 = 6002
  val DatumE_AustralianNationalSpheroid = 6003
  val DatumE_Bessel1841 = 6004
  val DatumE_BesselModified = 6005
  val DatumE_BesselNamibia = 6006
  val DatumE_Clarke1858 = 6007
  val DatumE_Clarke1866 = 6008
  val DatumE_Clarke1866Michigan = 6009
  val DatumE_Clarke1880_Benoit = 6010
  val DatumE_Clarke1880_IGN = 6011
  val DatumE_Clarke1880_RGS = 6012
  val DatumE_Clarke1880_Arc = 6013
  val DatumE_Clarke1880_SGA1922 = 6014
  val DatumE_Everest1830_1937Adjustment = 6015
  val DatumE_Everest1830_1967Definition = 6016
  val DatumE_Everest1830_1975Definition = 6017
  val DatumE_Everest1830Modified = 6018
  val DatumE_GRS1980 = 6019
  val DatumE_Helmert1906 = 6020
  val DatumE_IndonesianNationalSpheroid = 6021
  val DatumE_International1924 = 6022
  val DatumE_International1967 = 6023
  val DatumE_Krassowsky1960 = 6024
  val DatumE_NWL9D = 6025
  val DatumE_NWL10D = 6026
  val DatumE_Plessis1817 = 6027
  val DatumE_Struve1860 = 6028
  val DatumE_WarOffice = 6029
  val DatumE_WGS84 = 6030
  val DatumE_GEM10C = 6031
  val DatumE_OSU86F = 6032
  val DatumE_OSU91A = 6033
  val DatumE_Clarke1880 = 6034
  val DatumE_Sphere = 6035
  val Datum_Adindan = 6201
  val Datum_Australian_Geodetic_Datum_1966 = 6202
  val Datum_Australian_Geodetic_Datum_1984 = 6203
  val Datum_Ain_el_Abd_1970 = 6204
  val Datum_Afgooye = 6205
  val Datum_Agadez = 6206
  val Datum_Lisbon = 6207
  val Datum_Aratu = 6208
  val Datum_Arc_1950 = 6209
  val Datum_Arc_1960 = 6210
  val Datum_Batavia = 6211
  val Datum_Barbados = 6212
  val Datum_Beduaram = 6213
  val Datum_Beijing_1954 = 6214
  val Datum_Reseau_National_Belge_1950 = 6215
  val Datum_Bermuda_1957 = 6216
  val Datum_Bern_1898 = 6217
  val Datum_Bogota = 6218
  val Datum_Bukit_Rimpah = 6219
  val Datum_Camacupa = 6220
  val Datum_Campo_Inchauspe = 6221
  val Datum_Cape = 6222
  val Datum_Carthage = 6223
  val Datum_Chua = 6224
  val Datum_Corrego_Alegre = 6225
  val Datum_Cote_d_Ivoire = 6226
  val Datum_Deir_ez_Zor = 6227
  val Datum_Douala = 6228
  val Datum_Egypt_1907 = 6229
  val Datum_European_Datum_1950 = 6230
  val Datum_European_Datum_1987 = 6231
  val Datum_Fahud = 6232
  val Datum_Gandajika_1970 = 6233
  val Datum_Garoua = 6234
  val Datum_Guyane_Francaise = 6235
  val Datum_Hu_Tzu_Shan = 6236
  val Datum_Hungarian_Datum_1972 = 6237
  val Datum_Indonesian_Datum_1974 = 6238
  val Datum_Indian_1954 = 6239
  val Datum_Indian_1975 = 6240
  val Datum_Jamaica_1875 = 6241
  val Datum_Jamaica_1969 = 6242
  val Datum_Kalianpur = 6243
  val Datum_Kandawala = 6244
  val Datum_Kertau = 6245
  val Datum_Kuwait_Oil_Company = 6246
  val Datum_La_Canoa = 6247
  val Datum_Provisional_S_American_Datum_1956 = 6248
  val Datum_Lake = 6249
  val Datum_Leigon = 6250
  val Datum_Liberia_1964 = 6251
  val Datum_Lome = 6252
  val Datum_Luzon_1911 = 6253
  val Datum_Hito_XVIII_1963 = 6254
  val Datum_Herat_North = 6255
  val Datum_Mahe_1971 = 6256
  val Datum_Makassar = 6257
  val Datum_European_Reference_System_1989 = 6258
  val Datum_Malongo_1987 = 6259
  val Datum_Manoca = 6260
  val Datum_Merchich = 6261
  val Datum_Massawa = 6262
  val Datum_Minna = 6263
  val Datum_Mhast = 6264
  val Datum_Monte_Mario = 6265
  val Datum_M_poraloko = 6266
  val Datum_North_American_Datum_1927 = 6267
  val Datum_NAD_Michigan = 6268
  val Datum_North_American_Datum_1983 = 6269
  val Datum_Nahrwan_1967 = 6270
  val Datum_Naparima_1972 = 6271
  val Datum_New_Zealand_Geodetic_Datum_1949 = 6272
  val Datum_NGO_1948 = 6273
  val Datum_Datum_73 = 6274
  val Datum_Nouvelle_Triangulation_Francaise = 6275
  val Datum_NSWC_9Z_2 = 6276
  val Datum_OSGB_1936 = 6277
  val Datum_OSGB_1970_SN = 6278
  val Datum_OS_SN_1980 = 6279
  val Datum_Padang_1884 = 6280
  val Datum_Palestine_1923 = 6281
  val Datum_Pointe_Noire = 6282
  val Datum_Geocentric_Datum_of_Australia_1994 = 6283
  val Datum_Pulkovo_1942 = 6284
  val Datum_Qatar = 6285
  val Datum_Qatar_1948 = 6286
  val Datum_Qornoq = 6287
  val Datum_Loma_Quintana = 6288
  val Datum_Amersfoort = 6289
  val Datum_RT38 = 6290
  val Datum_South_American_Datum_1969 = 6291
  val Datum_Sapper_Hill_1943 = 6292
  val Datum_Schwarzeck = 6293
  val Datum_Segora = 6294
  val Datum_Serindung = 6295
  val Datum_Sudan = 6296
  val Datum_Tananarive_1925 = 6297
  val Datum_Timbalai_1948 = 6298
  val Datum_TM65 = 6299
  val Datum_TM75 = 6300
  val Datum_Tokyo = 6301
  val Datum_Trinidad_1903 = 6302
  val Datum_Trucial_Coast_1948 = 6303
  val Datum_Voirol_1875 = 6304
  val Datum_Voirol_Unifie_1960 = 6305
  val Datum_Bern_1938 = 6306
  val Datum_Nord_Sahara_1959 = 6307
  val Datum_Stockholm_1938 = 6308
  val Datum_Yacare = 6309
  val Datum_Yoff = 6310
  val Datum_Zanderij = 6311
  val Datum_Militar_Geographische_Institut = 6312
  val Datum_Reseau_National_Belge_1972 = 6313
  val Datum_Deutsche_Hauptdreiecksnetz = 6314
  val Datum_Conakry_1905 = 6315
  val Datum_WGS72 = 6322
  val Datum_WGS72_Transit_Broadcast_Ephemeris = 6324
  val Datum_WGS84 = 6326
  val Datum_Ancienne_Triangulation_Francaise = 6901
  val Datum_Nord_de_Guerre = 6902

}

object ProjectedLinearUnits {

  val LinearMeterCode = 9001
  val LinearMeter = "m"

  val LinearFootCode = 9002
  val LinearFoot = "ft"

  val LinearFootUSCode = 9003
  val LinearFootUS = "us-ft"

  val LinearFootIndianCode = 9006
  val LinearFootIndian = "ind-ft"

  val LinearLinkCode = 9007
  val LinearLink = "link"

  val LinearYardIndianCode = 9013
  val LinearYardIndian = "ind-yd"

  val LinearFathomCode = 9014
  val LinearFathom = "fath"

  val LinearMileInternationalNauticalCode = 9015
  val LinearMileInternationalNautical = "kmi"

  val projectedLinearUnitsMap = HashMap[Int, String](
    LinearMeterCode -> LinearMeter,
    LinearFootCode -> LinearFoot,
    LinearFootUSCode -> LinearFootUS,
    LinearFootIndianCode -> LinearFootIndian,
    LinearLinkCode -> LinearLink,
    LinearYardIndianCode -> LinearYardIndian,
    LinearFathomCode -> LinearFathom,
    LinearMileInternationalNauticalCode -> LinearMileInternationalNautical
  )

  val reversedProjectedLinearUnitsMap = HashMap[String, Int](
    LinearMeter -> LinearMeterCode,
    LinearFoot -> LinearFootCode,
    LinearFootUS -> LinearFootUSCode,
    LinearFootIndian -> LinearFootIndianCode,
    LinearLink ->LinearLinkCode,
    LinearYardIndian -> LinearYardIndianCode,
    LinearFathom -> LinearFathomCode,
    LinearMileInternationalNautical -> LinearMileInternationalNauticalCode
  )

}

object MapSystems {

  val MapSys_UTM_North = -9001
  val MapSys_UTM_South = -9002
  val MapSys_State_Plane_27 = -9003
  val MapSys_State_Plane_83 = -9004
}

object CoordinateTransformTypes {

  val CT_TransverseMercator = 1
  val CT_TransvMercator_Modified_Alaska = 2
  val CT_ObliqueMercator = 3
  val CT_ObliqueMercator_Laborde = 4
  val CT_ObliqueMercator_Rosenmund = 5
  val CT_ObliqueMercator_Spherical = 6
  val CT_Mercator = 7
  val CT_LambertConfConic_2SP = 8
  val CT_LambertConfConic = CT_LambertConfConic_2SP
  val CT_LambertConfConic_1SP = 9
  val CT_LambertConfConic_Helmert = CT_LambertConfConic_1SP
  val CT_LambertAzimEqualArea = 10
  val CT_AlbersEqualArea = 11
  val CT_AzimuthalEquidistant = 12
  val CT_EquidistantConic = 13
  val CT_Stereographic = 14
  val CT_PolarStereographic = 15
  val CT_ObliqueStereographic = 16
  val CT_Equirectangular = 17
  val CT_CassiniSoldner = 18
  val CT_Gnomonic = 19
  val CT_MillerCylindrical = 20
  val CT_Orthographic = 21
  val CT_Polyconic = 22
  val CT_Robinson = 23
  val CT_Sinusoidal = 24
  val CT_VanDerGrinten = 25
  val CT_NewZealandMapGrid = 26
  val CT_TransvMercator_SouthOriented = 27
  val CT_CylindricalEqualArea = 28
  val CT_HotineObliqueMercatorAzimuthCenter = 9815
  val CT_SouthOrientedGaussConformal = CT_TransvMercator_SouthOriented
  val CT_AlaskaConformal = CT_TransvMercator_Modified_Alaska
  val CT_TransvEquidistCylindrical = CT_CassiniSoldner
  val CT_ObliqueMercator_Hotine = CT_ObliqueMercator
  val CT_SwissObliqueCylindrical = CT_ObliqueMercator_Rosenmund
  val CT_GaussBoaga = CT_TransverseMercator
  val CT_GaussKruger = CT_TransverseMercator

  val projMethodToCTProjMethodMap = HashMap[Int, Int](
    9801 -> CT_LambertConfConic_1SP,
    9802 -> CT_LambertConfConic_2SP,
    9803 -> CT_LambertConfConic_2SP,
    9804 -> CT_Mercator,
    9805 -> CT_Mercator,
    9841 -> CT_Mercator,
    1024 -> CT_Mercator,
    9806 -> CT_CassiniSoldner,
    9807 -> CT_TransverseMercator,
    9808 -> CT_TransvMercator_SouthOriented,
    9809 -> CT_ObliqueStereographic,
    9810 -> CT_PolarStereographic,
    9829 -> CT_PolarStereographic,
    9811 -> CT_NewZealandMapGrid,
    9812 -> CT_ObliqueMercator,
    9813 -> CT_ObliqueMercator_Laborde,
    9814 -> CT_ObliqueMercator_Rosenmund,
    9815 -> CT_HotineObliqueMercatorAzimuthCenter,
    9816 -> UserDefinedCPV,
    9820 -> CT_LambertAzimEqualArea,
    1027 -> CT_LambertAzimEqualArea,
    9822 -> CT_AlbersEqualArea,
    9834 -> CT_CylindricalEqualArea
  )

}

object EllipsoidTypes {

  val Ellipse_Airy_1830 = 7001
  val Ellipse_Airy_Modified_1849 = 7002
  val Ellipse_Australian_National_Spheroid = 7003
  val Ellipse_Bessel_1841 = 7004
  val Ellipse_Bessel_Modified = 7005
  val Ellipse_Bessel_Namibia = 7006
  val Ellipse_Clarke_1858 = 7007
  val Ellipse_Clarke_1866 = 7008
  val Ellipse_Clarke_1866_Michigan = 7009
  val Ellipse_Clarke_1880_Benoit = 7010
  val Ellipse_Clarke_1880_IGN = 7011
  val Ellipse_Clarke_1880_RGS = 7012
  val Ellipse_Clarke_1880_Arc = 7013
  val Ellipse_Clarke_1880_SGA_1922 = 7014
  val Ellipse_Everest_1830_1937_Adjustment = 7015
  val Ellipse_Everest_1830_1967_Definition = 7016
  val Ellipse_Everest_1830_1975_Definition = 7017
  val Ellipse_Everest_1830_Modified = 7018
  val Ellipse_GRS_1980 = 7019
  val Ellipse_Helmert_1906 = 7020
  val Ellipse_Indonesian_National_Spheroid = 7021
  val Ellipse_International_1924 = 7022
  val Ellipse_International_1967 = 7023
  val Ellipse_Krassowsky_1940 = 7024
  val Ellipse_NWL_9D = 7025
  val Ellipse_NWL_10D = 7026
  val Ellipse_Plessis_1817 = 7027
  val Ellipse_Struve_1860 = 7028
  val Ellipse_War_Office = 7029
  val Ellipse_WGS_84 = 7030
  val Ellipse_GEM_10C = 7031
  val Ellipse_OSU86F = 7032
  val Ellipse_OSU91A = 7033
  val Ellipse_Clarke_1880 = 7034
  val Ellipse_Sphere = 7035

}

object GeographicCSTypes {

  val GCS_Adindan = 4201
  val GCS_AGD66 = 4202
  val GCS_AGD84 = 4203
  val GCS_Ain_el_Abd = 4204
  val GCS_Afgooye = 4205
  val GCS_Agadez = 4206
  val GCS_Lisbon = 4207
  val GCS_Aratu = 4208
  val GCS_Arc_1950 = 4209
  val GCS_Arc_1960 = 4210
  val GCS_Batavia = 4211
  val GCS_Barbados = 4212
  val GCS_Beduaram = 4213
  val GCS_Beijing_1954 = 4214
  val GCS_Belge_1950 = 4215
  val GCS_Bermuda_1957 = 4216
  val GCS_Bern_1898 = 4217
  val GCS_Bogota = 4218
  val GCS_Bukit_Rimpah = 4219
  val GCS_Camacupa = 4220
  val GCS_Campo_Inchauspe = 4221
  val GCS_Cape = 4222
  val GCS_Carthage = 4223
  val GCS_Chua = 4224
  val GCS_Corrego_Alegre = 4225
  val GCS_Cote_d_Ivoire = 4226
  val GCS_Deir_ez_Zor = 4227
  val GCS_Douala = 4228
  val GCS_Egypt_1907 = 4229
  val GCS_ED50 = 4230
  val GCS_ED87 = 4231
  val GCS_Fahud = 4232
  val GCS_Gandajika_1970 = 4233
  val GCS_Garoua = 4234
  val GCS_Guyane_Francaise = 4235
  val GCS_Hu_Tzu_Shan = 4236
  val GCS_HD72 = 4237
  val GCS_ID74 = 4238
  val GCS_Indian_1954 = 4239
  val GCS_Indian_1975 = 4240
  val GCS_Jamaica_1875 = 4241
  val GCS_JAD69 = 4242
  val GCS_Kalianpur = 4243
  val GCS_Kandawala = 4244
  val GCS_Kertau = 4245
  val GCS_KOC = 4246
  val GCS_La_Canoa = 4247
  val GCS_PSAD56 = 4248
  val GCS_Lake = 4249
  val GCS_Leigon = 4250
  val GCS_Liberia_1964 = 4251
  val GCS_Lome = 4252
  val GCS_Luzon_1911 = 4253
  val GCS_Hito_XVIII_1963 = 4254
  val GCS_Herat_North = 4255
  val GCS_Mahe_1971 = 4256
  val GCS_Makassar = 4257
  val GCS_EUREF89 = 4258
  val GCS_Malongo_1987 = 4259
  val GCS_Manoca = 4260
  val GCS_Merchich = 4261
  val GCS_Massawa = 4262
  val GCS_Minna = 4263
  val GCS_Mhast = 4264
  val GCS_Monte_Mario = 4265
  val GCS_M_poraloko = 4266
  val GCS_NAD27 = 4267
  val GCS_NAD_Michigan = 4268
  val GCS_NAD83 = 4269
  val GCS_Nahrwan_1967 = 4270
  val GCS_Naparima_1972 = 4271
  val GCS_GD49 = 4272
  val GCS_NGO_1948 = 4273
  val GCS_Datum_73 = 4274
  val GCS_NTF = 4275
  val GCS_NSWC_9Z_2 = 4276
  val GCS_OSGB_1936 = 4277
  val GCS_OSGB70 = 4278
  val GCS_OS_SN80 = 4279
  val GCS_Padang = 4280
  val GCS_Palestine_1923 = 4281
  val GCS_Pointe_Noire = 4282
  val GCS_GDA94 = 4283
  val GCS_Pulkovo_1942 = 4284
  val GCS_Qatar = 4285
  val GCS_Qatar_1948 = 4286
  val GCS_Qornoq = 4287
  val GCS_Loma_Quintana = 4288
  val GCS_Amersfoort = 4289
  val GCS_RT38 = 4290
  val GCS_SAD69 = 4291
  val GCS_Sapper_Hill_1943 = 4292
  val GCS_Schwarzeck = 4293
  val GCS_Segora = 4294
  val GCS_Serindung = 4295
  val GCS_Sudan = 4296
  val GCS_Tananarive = 4297
  val GCS_Timbalai_1948 = 4298
  val GCS_TM65 = 4299
  val GCS_TM75 = 4300
  val GCS_Tokyo = 4301
  val GCS_Trinidad_1903 = 4302
  val GCS_TC_1948 = 4303
  val GCS_Voirol_1875 = 4304
  val GCS_Voirol_Unifie = 4305
  val GCS_Bern_1938 = 4306
  val GCS_Nord_Sahara_1959 = 4307
  val GCS_Stockholm_1938 = 4308
  val GCS_Yacare = 4309
  val GCS_Yoff = 4310
  val GCS_Zanderij = 4311
  val GCS_MGI = 4312
  val GCS_Belge_1972 = 4313
  val GCS_DHDN = 4314
  val GCS_Conakry_1905 = 4315
  val GCS_WGS_72 = 4322
  val GCS_WGS_72BE = 4324
  val GCS_WGS_84 = 4326
  val GCS_Bern_1898_Bern = 4801
  val GCS_Bogota_Bogota = 4802
  val GCS_Lisbon_Lisbon = 4803
  val GCS_Makassar_Jakarta = 4804
  val GCS_MGI_Ferro = 4805
  val GCS_Monte_Mario_Rome = 4806
  val GCS_NTF_Paris = 4807
  val GCS_Padang_Jakarta = 4808
  val GCS_Belge_1950_Brussels = 4809
  val GCS_Tananarive_Paris = 4810
  val GCS_Voirol_1875_Paris = 4811
  val GCS_Voirol_Unifie_Paris = 4812
  val GCS_Batavia_Jakarta = 4813
  val GCS_ATF_Paris = 4901
  val GCS_NDG_Paris = 4902
  val GCSE_Airy1830 = 4001
  val GCSE_AiryModified1849 = 4002
  val GCSE_AustralianNationalSpheroid = 4003
  val GCSE_Bessel1841 = 4004
  val GCSE_BesselModified = 4005
  val GCSE_BesselNamibia = 4006
  val GCSE_Clarke1858 = 4007
  val GCSE_Clarke1866 = 4008
  val GCSE_Clarke1866Michigan = 4009
  val GCSE_Clarke1880_Benoit = 4010
  val GCSE_Clarke1880_IGN = 4011
  val GCSE_Clarke1880_RGS = 4012
  val GCSE_Clarke1880_Arc = 4013
  val GCSE_Clarke1880_SGA1922 = 4014
  val GCSE_Everest1830_1937Adjustment = 4015
  val GCSE_Everest1830_1967Definition = 4016
  val GCSE_Everest1830_1975Definition = 4017
  val GCSE_Everest1830Modified = 4018
  val GCSE_GRS1980 = 4019
  val GCSE_Helmert1906 = 4020
  val GCSE_IndonesianNationalSpheroid = 4021
  val GCSE_International1924 = 4022
  val GCSE_International1967 = 4023
  val GCSE_Krassowsky1940 = 4024
  val GCSE_NWL9D = 4025
  val GCSE_NWL10D = 4026
  val GCSE_Plessis1817 = 4027
  val GCSE_Struve1860 = 4028
  val GCSE_WarOffice = 4029
  val GCSE_WGS84 = 4030
  val GCSE_GEM10C = 4031
  val GCSE_OSU86F = 4032
  val GCSE_OSU91A = 4033
  val GCSE_Clarke1880 = 4034
  val GCSE_Sphere = 4035

}

object EPSGProjectionTypes {

  val PCS_Hjorsey_1955_Lambert =  3053
  val PCS_ISN93_Lambert_1993 =  3057
  val PCS_ETRS89_Poland_CS2000_zone_5 = 2176
  val PCS_ETRS89_Poland_CS2000_zone_6 = 2177
  val PCS_ETRS89_Poland_CS2000_zone_7 = 2177
  val PCS_ETRS89_Poland_CS2000_zone_8 = 2178
  val PCS_ETRS89_Poland_CS92 = 2180
  val PCS_GGRS87_Greek_Grid = 2100
  val PCS_KKJ_Finland_zone_1 = 2391
  val PCS_KKJ_Finland_zone_2 = 2392
  val PCS_KKJ_Finland_zone_3 = 2393
  val PCS_KKJ_Finland_zone_4 = 2394
  val PCS_RT90_2_5_gon_W = 2400
  val PCS_Lietuvos_Koordinoei_Sistema_1994 = 2600
  val PCS_Estonian_Coordinate_System_of_1992 = 3300
  val PCS_HD72_EOV = 23700
  val PCS_Dealul_Piscului_1970_Stereo_70 = 31700
  val PCS_Adindan_UTM_zone_37N = 20137
  val PCS_Adindan_UTM_zone_38N = 20138
  val PCS_AGD66_AMG_zone_48 = 20248
  val PCS_AGD66_AMG_zone_49 = 20249
  val PCS_AGD66_AMG_zone_50 = 20250
  val PCS_AGD66_AMG_zone_51 = 20251
  val PCS_AGD66_AMG_zone_52 = 20252
  val PCS_AGD66_AMG_zone_53 = 20253
  val PCS_AGD66_AMG_zone_54 = 20254
  val PCS_AGD66_AMG_zone_55 = 20255
  val PCS_AGD66_AMG_zone_56 = 20256
  val PCS_AGD66_AMG_zone_57 = 20257
  val PCS_AGD66_AMG_zone_58 = 20258
  val PCS_AGD84_AMG_zone_48 = 20348
  val PCS_AGD84_AMG_zone_49 = 20349
  val PCS_AGD84_AMG_zone_50 = 20350
  val PCS_AGD84_AMG_zone_51 = 20351
  val PCS_AGD84_AMG_zone_52 = 20352
  val PCS_AGD84_AMG_zone_53 = 20353
  val PCS_AGD84_AMG_zone_54 = 20354
  val PCS_AGD84_AMG_zone_55 = 20355
  val PCS_AGD84_AMG_zone_56 = 20356
  val PCS_AGD84_AMG_zone_57 = 20357
  val PCS_AGD84_AMG_zone_58 = 20358
  val PCS_Ain_el_Abd_UTM_zone_37N = 20437
  val PCS_Ain_el_Abd_UTM_zone_38N = 20438
  val PCS_Ain_el_Abd_UTM_zone_39N = 20439
  val PCS_Ain_el_Abd_Bahrain_Grid = 20499
  val PCS_Afgooye_UTM_zone_38N = 20538
  val PCS_Afgooye_UTM_zone_39N = 20539
  val PCS_Lisbon_Portugese_Grid = 20700
  val PCS_Aratu_UTM_zone_22S = 20822
  val PCS_Aratu_UTM_zone_23S = 20823
  val PCS_Aratu_UTM_zone_24S = 20824
  val PCS_Arc_1950_Lo13 = 20973
  val PCS_Arc_1950_Lo15 = 20975
  val PCS_Arc_1950_Lo17 = 20977
  val PCS_Arc_1950_Lo19 = 20979
  val PCS_Arc_1950_Lo21 = 20981
  val PCS_Arc_1950_Lo23 = 20983
  val PCS_Arc_1950_Lo25 = 20985
  val PCS_Arc_1950_Lo27 = 20987
  val PCS_Arc_1950_Lo29 = 20989
  val PCS_Arc_1950_Lo31 = 20991
  val PCS_Arc_1950_Lo33 = 20993
  val PCS_Arc_1950_Lo35 = 20995
  val PCS_Batavia_NEIEZ = 21100
  val PCS_Batavia_UTM_zone_48S = 21148
  val PCS_Batavia_UTM_zone_49S = 21149
  val PCS_Batavia_UTM_zone_50S = 21150
  val PCS_Beijing_Gauss_zone_13 = 21413
  val PCS_Beijing_Gauss_zone_14 = 21414
  val PCS_Beijing_Gauss_zone_15 = 21415
  val PCS_Beijing_Gauss_zone_16 = 21416
  val PCS_Beijing_Gauss_zone_17 = 21417
  val PCS_Beijing_Gauss_zone_18 = 21418
  val PCS_Beijing_Gauss_zone_19 = 21419
  val PCS_Beijing_Gauss_zone_20 = 21420
  val PCS_Beijing_Gauss_zone_21 = 21421
  val PCS_Beijing_Gauss_zone_22 = 21422
  val PCS_Beijing_Gauss_zone_23 = 21423
  val PCS_Beijing_Gauss_13N = 21473
  val PCS_Beijing_Gauss_14N = 21474
  val PCS_Beijing_Gauss_15N = 21475
  val PCS_Beijing_Gauss_16N = 21476
  val PCS_Beijing_Gauss_17N = 21477
  val PCS_Beijing_Gauss_18N = 21478
  val PCS_Beijing_Gauss_19N = 21479
  val PCS_Beijing_Gauss_20N = 21480
  val PCS_Beijing_Gauss_21N = 21481
  val PCS_Beijing_Gauss_22N = 21482
  val PCS_Beijing_Gauss_23N = 21483
  val PCS_Belge_Lambert_50 = 21500
  val PCS_Bern_1898_Swiss_Old = 21790
  val PCS_Bogota_UTM_zone_17N = 21817
  val PCS_Bogota_UTM_zone_18N = 21818
  val PCS_Bogota_Colombia_3W = 21891
  val PCS_Bogota_Colombia_Bogota = 21892
  val PCS_Bogota_Colombia_3E = 21893
  val PCS_Bogota_Colombia_6E = 21894
  val PCS_Camacupa_UTM_32S = 22032
  val PCS_Camacupa_UTM_33S = 22033
  val PCS_C_Inchauspe_Argentina_1 = 22191
  val PCS_C_Inchauspe_Argentina_2 = 22192
  val PCS_C_Inchauspe_Argentina_3 = 22193
  val PCS_C_Inchauspe_Argentina_4 = 22194
  val PCS_C_Inchauspe_Argentina_5 = 22195
  val PCS_C_Inchauspe_Argentina_6 = 22196
  val PCS_C_Inchauspe_Argentina_7 = 22197
  val PCS_Carthage_UTM_zone_32N = 22332
  val PCS_Carthage_Nord_Tunisie = 22391
  val PCS_Carthage_Sud_Tunisie = 22392
  val PCS_Corrego_Alegre_UTM_23S = 22523
  val PCS_Corrego_Alegre_UTM_24S = 22524
  val PCS_Douala_UTM_zone_32N = 22832
  val PCS_Egypt_1907_Red_Belt = 22992
  val PCS_Egypt_1907_Purple_Belt = 22993
  val PCS_Egypt_1907_Ext_Purple = 22994
  val PCS_ED50_UTM_zone_28N = 23028
  val PCS_ED50_UTM_zone_29N = 23029
  val PCS_ED50_UTM_zone_30N = 23030
  val PCS_ED50_UTM_zone_31N = 23031
  val PCS_ED50_UTM_zone_32N = 23032
  val PCS_ED50_UTM_zone_33N = 23033
  val PCS_ED50_UTM_zone_34N = 23034
  val PCS_ED50_UTM_zone_35N = 23035
  val PCS_ED50_UTM_zone_36N = 23036
  val PCS_ED50_UTM_zone_37N = 23037
  val PCS_ED50_UTM_zone_38N = 23038
  val PCS_Fahud_UTM_zone_39N = 23239
  val PCS_Fahud_UTM_zone_40N = 23240
  val PCS_Garoua_UTM_zone_33N = 23433
  val PCS_ID74_UTM_zone_46N = 23846
  val PCS_ID74_UTM_zone_47N = 23847
  val PCS_ID74_UTM_zone_48N = 23848
  val PCS_ID74_UTM_zone_49N = 23849
  val PCS_ID74_UTM_zone_50N = 23850
  val PCS_ID74_UTM_zone_51N = 23851
  val PCS_ID74_UTM_zone_52N = 23852
  val PCS_ID74_UTM_zone_53N = 23853
  val PCS_ID74_UTM_zone_46S = 23886
  val PCS_ID74_UTM_zone_47S = 23887
  val PCS_ID74_UTM_zone_48S = 23888
  val PCS_ID74_UTM_zone_49S = 23889
  val PCS_ID74_UTM_zone_50S = 23890
  val PCS_ID74_UTM_zone_51S = 23891
  val PCS_ID74_UTM_zone_52S = 23892
  val PCS_ID74_UTM_zone_53S = 23893
  val PCS_ID74_UTM_zone_54S = 23894
  val PCS_Indian_1954_UTM_47N = 23947
  val PCS_Indian_1954_UTM_48N = 23948
  val PCS_Indian_1975_UTM_47N = 24047
  val PCS_Indian_1975_UTM_48N = 24048
  val PCS_Jamaica_1875_Old_Grid = 24100
  val PCS_JAD69_Jamaica_Grid = 24200
  val PCS_Kalianpur_India_0 = 24370
  val PCS_Kalianpur_India_I = 24371
  val PCS_Kalianpur_India_IIa = 24372
  val PCS_Kalianpur_India_IIIa = 24373
  val PCS_Kalianpur_India_IVa = 24374
  val PCS_Kalianpur_India_IIb = 24382
  val PCS_Kalianpur_India_IIIb = 24383
  val PCS_Kalianpur_India_IVb = 24384
  val PCS_Kertau_Singapore_Grid = 24500
  val PCS_Kertau_UTM_zone_47N = 24547
  val PCS_Kertau_UTM_zone_48N = 24548
  val PCS_La_Canoa_UTM_zone_20N = 24720
  val PCS_La_Canoa_UTM_zone_21N = 24721
  val PCS_PSAD56_UTM_zone_18N = 24818
  val PCS_PSAD56_UTM_zone_19N = 24819
  val PCS_PSAD56_UTM_zone_20N = 24820
  val PCS_PSAD56_UTM_zone_21N = 24821
  val PCS_PSAD56_UTM_zone_17S = 24877
  val PCS_PSAD56_UTM_zone_18S = 24878
  val PCS_PSAD56_UTM_zone_19S = 24879
  val PCS_PSAD56_UTM_zone_20S = 24880
  val PCS_PSAD56_Peru_west_zone = 24891
  val PCS_PSAD56_Peru_central = 24892
  val PCS_PSAD56_Peru_east_zone = 24893
  val PCS_Leigon_Ghana_Grid = 25000
  val PCS_Lome_UTM_zone_31N = 25231
  val PCS_Luzon_Philippines_I = 25391
  val PCS_Luzon_Philippines_II = 25392
  val PCS_Luzon_Philippines_III = 25393
  val PCS_Luzon_Philippines_IV = 25394
  val PCS_Luzon_Philippines_V = 25395
  val PCS_Makassar_NEIEZ = 25700
  val PCS_Malongo_1987_UTM_32S = 25932
  val PCS_Merchich_Nord_Maroc = 26191
  val PCS_Merchich_Sud_Maroc = 26192
  val PCS_Merchich_Sahara = 26193
  val PCS_Massawa_UTM_zone_37N = 26237
  val PCS_Minna_UTM_zone_31N = 26331
  val PCS_Minna_UTM_zone_32N = 26332
  val PCS_Minna_Nigeria_West = 26391
  val PCS_Minna_Nigeria_Mid_Belt = 26392
  val PCS_Minna_Nigeria_East = 26393
  val PCS_Mhast_UTM_zone_32S = 26432
  val PCS_Monte_Mario_Italy_1 = 26591
  val PCS_Monte_Mario_Italy_2 = 26592
  val PCS_M_poraloko_UTM_32N = 26632
  val PCS_M_poraloko_UTM_32S = 26692
  val PCS_NAD27_UTM_zone_3N = 26703
  val PCS_NAD27_UTM_zone_4N = 26704
  val PCS_NAD27_UTM_zone_5N = 26705
  val PCS_NAD27_UTM_zone_6N = 26706
  val PCS_NAD27_UTM_zone_7N = 26707
  val PCS_NAD27_UTM_zone_8N = 26708
  val PCS_NAD27_UTM_zone_9N = 26709
  val PCS_NAD27_UTM_zone_10N = 26710
  val PCS_NAD27_UTM_zone_11N = 26711
  val PCS_NAD27_UTM_zone_12N = 26712
  val PCS_NAD27_UTM_zone_13N = 26713
  val PCS_NAD27_UTM_zone_14N = 26714
  val PCS_NAD27_UTM_zone_15N = 26715
  val PCS_NAD27_UTM_zone_16N = 26716
  val PCS_NAD27_UTM_zone_17N = 26717
  val PCS_NAD27_UTM_zone_18N = 26718
  val PCS_NAD27_UTM_zone_19N = 26719
  val PCS_NAD27_UTM_zone_20N = 26720
  val PCS_NAD27_UTM_zone_21N = 26721
  val PCS_NAD27_UTM_zone_22N = 26722
  val PCS_NAD27_Alabama_East = 26729
  val PCS_NAD27_Alabama_West = 26730
  val PCS_NAD27_Alaska_zone_1 = 26731
  val PCS_NAD27_Alaska_zone_2 = 26732
  val PCS_NAD27_Alaska_zone_3 = 26733
  val PCS_NAD27_Alaska_zone_4 = 26734
  val PCS_NAD27_Alaska_zone_5 = 26735
  val PCS_NAD27_Alaska_zone_6 = 26736
  val PCS_NAD27_Alaska_zone_7 = 26737
  val PCS_NAD27_Alaska_zone_8 = 26738
  val PCS_NAD27_Alaska_zone_9 = 26739
  val PCS_NAD27_Alaska_zone_10 = 26740
  val PCS_NAD27_California_I = 26741
  val PCS_NAD27_California_II = 26742
  val PCS_NAD27_California_III = 26743
  val PCS_NAD27_California_IV = 26744
  val PCS_NAD27_California_V = 26745
  val PCS_NAD27_California_VI = 26746
  val PCS_NAD27_California_VII = 26747
  val PCS_NAD27_Arizona_East = 26748
  val PCS_NAD27_Arizona_Central = 26749
  val PCS_NAD27_Arizona_West = 26750
  val PCS_NAD27_Arkansas_North = 26751
  val PCS_NAD27_Arkansas_South = 26752
  val PCS_NAD27_Colorado_North = 26753
  val PCS_NAD27_Colorado_Central = 26754
  val PCS_NAD27_Colorado_South = 26755
  val PCS_NAD27_Connecticut = 26756
  val PCS_NAD27_Delaware = 26757
  val PCS_NAD27_Florida_East = 26758
  val PCS_NAD27_Florida_West = 26759
  val PCS_NAD27_Florida_North = 26760
  val PCS_NAD27_Hawaii_zone_1 = 26761
  val PCS_NAD27_Hawaii_zone_2 = 26762
  val PCS_NAD27_Hawaii_zone_3 = 26763
  val PCS_NAD27_Hawaii_zone_4 = 26764
  val PCS_NAD27_Hawaii_zone_5 = 26765
  val PCS_NAD27_Georgia_East = 26766
  val PCS_NAD27_Georgia_West = 26767
  val PCS_NAD27_Idaho_East = 26768
  val PCS_NAD27_Idaho_Central = 26769
  val PCS_NAD27_Idaho_West = 26770
  val PCS_NAD27_Illinois_East = 26771
  val PCS_NAD27_Illinois_West = 26772
  val PCS_NAD27_Indiana_East = 26773
  val PCS_NAD27_BLM_14N_feet = 26774
  val PCS_NAD27_Indiana_West = 26774
  val PCS_NAD27_BLM_15N_feet = 26775
  val PCS_NAD27_Iowa_North = 26775
  val PCS_NAD27_BLM_16N_feet = 26776
  val PCS_NAD27_Iowa_South = 26776
  val PCS_NAD27_BLM_17N_feet = 26777
  val PCS_NAD27_Kansas_North = 26777
  val PCS_NAD27_Kansas_South = 26778
  val PCS_NAD27_Kentucky_North = 26779
  val PCS_NAD27_Kentucky_South = 26780
  val PCS_NAD27_Louisiana_North = 26781
  val PCS_NAD27_Louisiana_South = 26782
  val PCS_NAD27_Maine_East = 26783
  val PCS_NAD27_Maine_West = 26784
  val PCS_NAD27_Maryland = 26785
  val PCS_NAD27_Massachusetts = 26786
  val PCS_NAD27_Massachusetts_Is = 26787
  val PCS_NAD27_Michigan_North = 26788
  val PCS_NAD27_Michigan_Central = 26789
  val PCS_NAD27_Michigan_South = 26790
  val PCS_NAD27_Minnesota_North = 26791
  val PCS_NAD27_Minnesota_Cent = 26792
  val PCS_NAD27_Minnesota_South = 26793
  val PCS_NAD27_Mississippi_East = 26794
  val PCS_NAD27_Mississippi_West = 26795
  val PCS_NAD27_Missouri_East = 26796
  val PCS_NAD27_Missouri_Central = 26797
  val PCS_NAD27_Missouri_West = 26798
  val PCS_NAD_Michigan_Michigan_East = 26801
  val PCS_NAD_Michigan_Michigan_Old_Central = 26802
  val PCS_NAD_Michigan_Michigan_West = 26803
  val PCS_NAD83_UTM_zone_3N = 26903
  val PCS_NAD83_UTM_zone_4N = 26904
  val PCS_NAD83_UTM_zone_5N = 26905
  val PCS_NAD83_UTM_zone_6N = 26906
  val PCS_NAD83_UTM_zone_7N = 26907
  val PCS_NAD83_UTM_zone_8N = 26908
  val PCS_NAD83_UTM_zone_9N = 26909
  val PCS_NAD83_UTM_zone_10N = 26910
  val PCS_NAD83_UTM_zone_11N = 26911
  val PCS_NAD83_UTM_zone_12N = 26912
  val PCS_NAD83_UTM_zone_13N = 26913
  val PCS_NAD83_UTM_zone_14N = 26914
  val PCS_NAD83_UTM_zone_15N = 26915
  val PCS_NAD83_UTM_zone_16N = 26916
  val PCS_NAD83_UTM_zone_17N = 26917
  val PCS_NAD83_UTM_zone_18N = 26918
  val PCS_NAD83_UTM_zone_19N = 26919
  val PCS_NAD83_UTM_zone_20N = 26920
  val PCS_NAD83_UTM_zone_21N = 26921
  val PCS_NAD83_UTM_zone_22N = 26922
  val PCS_NAD83_UTM_zone_23N = 26923
  val PCS_NAD83_Alabama_East = 26929
  val PCS_NAD83_Alabama_West = 26930
  val PCS_NAD83_Alaska_zone_1 = 26931
  val PCS_NAD83_Alaska_zone_2 = 26932
  val PCS_NAD83_Alaska_zone_3 = 26933
  val PCS_NAD83_Alaska_zone_4 = 26934
  val PCS_NAD83_Alaska_zone_5 = 26935
  val PCS_NAD83_Alaska_zone_6 = 26936
  val PCS_NAD83_Alaska_zone_7 = 26937
  val PCS_NAD83_Alaska_zone_8 = 26938
  val PCS_NAD83_Alaska_zone_9 = 26939
  val PCS_NAD83_Alaska_zone_10 = 26940
  val PCS_NAD83_California_1 = 26941
  val PCS_NAD83_California_2 = 26942
  val PCS_NAD83_California_3 = 26943
  val PCS_NAD83_California_4 = 26944
  val PCS_NAD83_California_5 = 26945
  val PCS_NAD83_California_6 = 26946
  val PCS_NAD83_Arizona_East = 26948
  val PCS_NAD83_Arizona_Central = 26949
  val PCS_NAD83_Arizona_West = 26950
  val PCS_NAD83_Arkansas_North = 26951
  val PCS_NAD83_Arkansas_South = 26952
  val PCS_NAD83_Colorado_North = 26953
  val PCS_NAD83_Colorado_Central = 26954
  val PCS_NAD83_Colorado_South = 26955
  val PCS_NAD83_Connecticut = 26956
  val PCS_NAD83_Delaware = 26957
  val PCS_NAD83_Florida_East = 26958
  val PCS_NAD83_Florida_West = 26959
  val PCS_NAD83_Florida_North = 26960
  val PCS_NAD83_Hawaii_zone_1 = 26961
  val PCS_NAD83_Hawaii_zone_2 = 26962
  val PCS_NAD83_Hawaii_zone_3 = 26963
  val PCS_NAD83_Hawaii_zone_4 = 26964
  val PCS_NAD83_Hawaii_zone_5 = 26965
  val PCS_NAD83_Georgia_East = 26966
  val PCS_NAD83_Georgia_West = 26967
  val PCS_NAD83_Idaho_East = 26968
  val PCS_NAD83_Idaho_Central = 26969
  val PCS_NAD83_Idaho_West = 26970
  val PCS_NAD83_Illinois_East = 26971
  val PCS_NAD83_Illinois_West = 26972
  val PCS_NAD83_Indiana_East = 26973
  val PCS_NAD83_Indiana_West = 26974
  val PCS_NAD83_Iowa_North = 26975
  val PCS_NAD83_Iowa_South = 26976
  val PCS_NAD83_Kansas_North = 26977
  val PCS_NAD83_Kansas_South = 26978
  val PCS_NAD83_Kentucky_North = 2205
  val PCS_NAD83_Kentucky_South = 26980
  val PCS_NAD83_Louisiana_North = 26981
  val PCS_NAD83_Louisiana_South = 26982
  val PCS_NAD83_Maine_East = 26983
  val PCS_NAD83_Maine_West = 26984
  val PCS_NAD83_Maryland = 26985
  val PCS_NAD83_Massachusetts = 26986
  val PCS_NAD83_Massachusetts_Is = 26987
  val PCS_NAD83_Michigan_North = 26988
  val PCS_NAD83_Michigan_Central = 26989
  val PCS_NAD83_Michigan_South = 26990
  val PCS_NAD83_Minnesota_North = 26991
  val PCS_NAD83_Minnesota_Cent = 26992
  val PCS_NAD83_Minnesota_South = 26993
  val PCS_NAD83_Mississippi_East = 26994
  val PCS_NAD83_Mississippi_West = 26995
  val PCS_NAD83_Missouri_East = 26996
  val PCS_NAD83_Missouri_Central = 26997
  val PCS_NAD83_Missouri_West = 26998
  val PCS_Nahrwan_1967_UTM_38N = 27038
  val PCS_Nahrwan_1967_UTM_39N = 27039
  val PCS_Nahrwan_1967_UTM_40N = 27040
  val PCS_Naparima_UTM_20N = 27120
  val PCS_GD49_NZ_Map_Grid = 27200
  val PCS_GD49_North_Island_Grid = 27291
  val PCS_GD49_South_Island_Grid = 27292
  val PCS_Datum_73_UTM_zone_29N = 27429
  val PCS_ATF_Nord_de_Guerre = 27500
  val PCS_NTF_France_I = 27581
  val PCS_NTF_France_II = 27582
  val PCS_NTF_France_III = 27583
  val PCS_NTF_Nord_France = 27591
  val PCS_NTF_Centre_France = 27592
  val PCS_NTF_Sud_France = 27593
  val PCS_British_National_Grid = 27700
  val PCS_Point_Noire_UTM_32S = 28232
  val PCS_GDA94_MGA_zone_48 = 28348
  val PCS_GDA94_MGA_zone_49 = 28349
  val PCS_GDA94_MGA_zone_50 = 28350
  val PCS_GDA94_MGA_zone_51 = 28351
  val PCS_GDA94_MGA_zone_52 = 28352
  val PCS_GDA94_MGA_zone_53 = 28353
  val PCS_GDA94_MGA_zone_54 = 28354
  val PCS_GDA94_MGA_zone_55 = 28355
  val PCS_GDA94_MGA_zone_56 = 28356
  val PCS_GDA94_MGA_zone_57 = 28357
  val PCS_GDA94_MGA_zone_58 = 28358
  val PCS_Pulkovo_Gauss_zone_4 = 28404
  val PCS_Pulkovo_Gauss_zone_5 = 28405
  val PCS_Pulkovo_Gauss_zone_6 = 28406
  val PCS_Pulkovo_Gauss_zone_7 = 28407
  val PCS_Pulkovo_Gauss_zone_8 = 28408
  val PCS_Pulkovo_Gauss_zone_9 = 28409
  val PCS_Pulkovo_Gauss_zone_10 = 28410
  val PCS_Pulkovo_Gauss_zone_11 = 28411
  val PCS_Pulkovo_Gauss_zone_12 = 28412
  val PCS_Pulkovo_Gauss_zone_13 = 28413
  val PCS_Pulkovo_Gauss_zone_14 = 28414
  val PCS_Pulkovo_Gauss_zone_15 = 28415
  val PCS_Pulkovo_Gauss_zone_16 = 28416
  val PCS_Pulkovo_Gauss_zone_17 = 28417
  val PCS_Pulkovo_Gauss_zone_18 = 28418
  val PCS_Pulkovo_Gauss_zone_19 = 28419
  val PCS_Pulkovo_Gauss_zone_20 = 28420
  val PCS_Pulkovo_Gauss_zone_21 = 28421
  val PCS_Pulkovo_Gauss_zone_22 = 28422
  val PCS_Pulkovo_Gauss_zone_23 = 28423
  val PCS_Pulkovo_Gauss_zone_24 = 28424
  val PCS_Pulkovo_Gauss_zone_25 = 28425
  val PCS_Pulkovo_Gauss_zone_26 = 28426
  val PCS_Pulkovo_Gauss_zone_27 = 28427
  val PCS_Pulkovo_Gauss_zone_28 = 28428
  val PCS_Pulkovo_Gauss_zone_29 = 28429
  val PCS_Pulkovo_Gauss_zone_30 = 28430
  val PCS_Pulkovo_Gauss_zone_31 = 28431
  val PCS_Pulkovo_Gauss_zone_32 = 28432
  val PCS_Pulkovo_Gauss_4N = 28464
  val PCS_Pulkovo_Gauss_5N = 28465
  val PCS_Pulkovo_Gauss_6N = 28466
  val PCS_Pulkovo_Gauss_7N = 28467
  val PCS_Pulkovo_Gauss_8N = 28468
  val PCS_Pulkovo_Gauss_9N = 28469
  val PCS_Pulkovo_Gauss_10N = 28470
  val PCS_Pulkovo_Gauss_11N = 28471
  val PCS_Pulkovo_Gauss_12N = 28472
  val PCS_Pulkovo_Gauss_13N = 28473
  val PCS_Pulkovo_Gauss_14N = 28474
  val PCS_Pulkovo_Gauss_15N = 28475
  val PCS_Pulkovo_Gauss_16N = 28476
  val PCS_Pulkovo_Gauss_17N = 28477
  val PCS_Pulkovo_Gauss_18N = 28478
  val PCS_Pulkovo_Gauss_19N = 28479
  val PCS_Pulkovo_Gauss_20N = 28480
  val PCS_Pulkovo_Gauss_21N = 28481
  val PCS_Pulkovo_Gauss_22N = 28482
  val PCS_Pulkovo_Gauss_23N = 28483
  val PCS_Pulkovo_Gauss_24N = 28484
  val PCS_Pulkovo_Gauss_25N = 28485
  val PCS_Pulkovo_Gauss_26N = 28486
  val PCS_Pulkovo_Gauss_27N = 28487
  val PCS_Pulkovo_Gauss_28N = 28488
  val PCS_Pulkovo_Gauss_29N = 28489
  val PCS_Pulkovo_Gauss_30N = 28490
  val PCS_Pulkovo_Gauss_31N = 28491
  val PCS_Pulkovo_Gauss_32N = 28492
  val PCS_Qatar_National_Grid = 28600
  val PCS_RD_Netherlands_Old = 28991
  val PCS_RD_Netherlands_New = 28992
  val PCS_SAD69_UTM_zone_18N = 29118
  val PCS_SAD69_UTM_zone_19N = 29119
  val PCS_SAD69_UTM_zone_20N = 29120
  val PCS_SAD69_UTM_zone_21N = 29121
  val PCS_SAD69_UTM_zone_22N = 29122
  val PCS_SAD69_UTM_zone_17S = 29177
  val PCS_SAD69_UTM_zone_18S = 29178
  val PCS_SAD69_UTM_zone_19S = 29179
  val PCS_SAD69_UTM_zone_20S = 29180
  val PCS_SAD69_UTM_zone_21S = 29181
  val PCS_SAD69_UTM_zone_22S = 29182
  val PCS_SAD69_UTM_zone_23S = 29183
  val PCS_SAD69_UTM_zone_24S = 29184
  val PCS_SAD69_UTM_zone_25S = 29185
  val PCS_Sapper_Hill_UTM_20S = 29220
  val PCS_Sapper_Hill_UTM_21S = 29221
  val PCS_Schwarzeck_UTM_33S = 29333
  val PCS_Sudan_UTM_zone_35N = 29635
  val PCS_Sudan_UTM_zone_36N = 29636
  val PCS_Tananarive_Laborde = 29700
  val PCS_Tananarive_UTM_38S = 29738
  val PCS_Tananarive_UTM_39S = 29739
  val PCS_Timbalai_1948_Borneo = 29800
  val PCS_Timbalai_1948_UTM_49N = 29849
  val PCS_Timbalai_1948_UTM_50N = 29850
  val PCS_TM65_Irish_Nat_Grid = 29900
  val PCS_Trinidad_1903_Trinidad = 30200
  val PCS_TC_1948_UTM_zone_39N = 30339
  val PCS_TC_1948_UTM_zone_40N = 30340
  val PCS_Voirol_N_Algerie_ancien = 30491
  val PCS_Voirol_S_Algerie_ancien = 30492
  val PCS_Voirol_Unifie_N_Algerie = 30591
  val PCS_Voirol_Unifie_S_Algerie = 30592
  val PCS_Bern_1938_Swiss_New = 30600
  val PCS_Nord_Sahara_UTM_29N = 30729
  val PCS_Nord_Sahara_UTM_30N = 30730
  val PCS_Nord_Sahara_UTM_31N = 30731
  val PCS_Nord_Sahara_UTM_32N = 30732
  val PCS_Yoff_UTM_zone_28N = 31028
  val PCS_Zanderij_UTM_zone_21N = 31121
  val PCS_MGI_Austria_West = 31291
  val PCS_MGI_Austria_Central = 31292
  val PCS_MGI_Austria_East = 31293
  val PCS_Belge_Lambert_72 = 31300
  val PCS_DHDN_Germany_zone_1 = 31491
  val PCS_DHDN_Germany_zone_2 = 31492
  val PCS_DHDN_Germany_zone_3 = 31493
  val PCS_DHDN_Germany_zone_4 = 31494
  val PCS_DHDN_Germany_zone_5 = 31495
  val PCS_NAD27_Montana_North = 32001
  val PCS_NAD27_Montana_Central = 32002
  val PCS_NAD27_Montana_South = 32003
  val PCS_NAD27_Nebraska_North = 32005
  val PCS_NAD27_Nebraska_South = 32006
  val PCS_NAD27_Nevada_East = 32007
  val PCS_NAD27_Nevada_Central = 32008
  val PCS_NAD27_Nevada_West = 32009
  val PCS_NAD27_New_Hampshire = 32010
  val PCS_NAD27_New_Jersey = 32011
  val PCS_NAD27_New_Mexico_East = 32012
  val PCS_NAD27_New_Mexico_Cent = 32013
  val PCS_NAD27_New_Mexico_West = 32014
  val PCS_NAD27_New_York_East = 32015
  val PCS_NAD27_New_York_Central = 32016
  val PCS_NAD27_New_York_West = 32017
  val PCS_NAD27_New_York_Long_Is = 32018
  val PCS_NAD27_North_Carolina = 32019
  val PCS_NAD27_North_Dakota_N = 32020
  val PCS_NAD27_North_Dakota_S = 32021
  val PCS_NAD27_Ohio_North = 32022
  val PCS_NAD27_Ohio_South = 32023
  val PCS_NAD27_Oklahoma_North = 32024
  val PCS_NAD27_Oklahoma_South = 32025
  val PCS_NAD27_Oregon_North = 32026
  val PCS_NAD27_Oregon_South = 32027
  val PCS_NAD27_Pennsylvania_N = 32028
  val PCS_NAD27_Pennsylvania_S = 32029
  val PCS_NAD27_Rhode_Island = 32030
  val PCS_NAD27_South_Carolina_N = 32031
  val PCS_NAD27_South_Carolina_S = 32033
  val PCS_NAD27_South_Dakota_N = 32034
  val PCS_NAD27_South_Dakota_S = 32035
  val PCS_NAD27_Tennessee = 2204
  val PCS_NAD27_Texas_North = 32037
  val PCS_NAD27_Texas_North_Cen = 32038
  val PCS_NAD27_Texas_Central = 32039
  val PCS_NAD27_Texas_South_Cen = 32040
  val PCS_NAD27_Texas_South = 32041
  val PCS_NAD27_Utah_North = 32042
  val PCS_NAD27_Utah_Central = 32043
  val PCS_NAD27_Utah_South = 32044
  val PCS_NAD27_Vermont = 32045
  val PCS_NAD27_Virginia_North = 32046
  val PCS_NAD27_Virginia_South = 32047
  val PCS_NAD27_Washington_North = 32048
  val PCS_NAD27_Washington_South = 32049
  val PCS_NAD27_West_Virginia_N = 32050
  val PCS_NAD27_West_Virginia_S = 32051
  val PCS_NAD27_Wisconsin_North = 32052
  val PCS_NAD27_Wisconsin_Cen = 32053
  val PCS_NAD27_Wisconsin_South = 32054
  val PCS_NAD27_Wyoming_East = 32055
  val PCS_NAD27_Wyoming_E_Cen = 32056
  val PCS_NAD27_Wyoming_W_Cen = 32057
  val PCS_NAD27_Wyoming_West = 32058
  val PCS_NAD27_Puerto_Rico = 32059
  val PCS_NAD27_St_Croix = 32060
  val PCS_NAD83_Montana = 32100
  val PCS_NAD83_Nebraska = 32104
  val PCS_NAD83_Nevada_East = 32107
  val PCS_NAD83_Nevada_Central = 32108
  val PCS_NAD83_Nevada_West = 32109
  val PCS_NAD83_New_Hampshire = 32110
  val PCS_NAD83_New_Jersey = 32111
  val PCS_NAD83_New_Mexico_East = 32112
  val PCS_NAD83_New_Mexico_Cent = 32113
  val PCS_NAD83_New_Mexico_West = 32114
  val PCS_NAD83_New_York_East = 32115
  val PCS_NAD83_New_York_Central = 32116
  val PCS_NAD83_New_York_West = 32117
  val PCS_NAD83_New_York_Long_Is = 32118
  val PCS_NAD83_North_Carolina = 32119
  val PCS_NAD83_North_Dakota_N = 32120
  val PCS_NAD83_North_Dakota_S = 32121
  val PCS_NAD83_Ohio_North = 32122
  val PCS_NAD83_Ohio_South = 32123
  val PCS_NAD83_Oklahoma_North = 32124
  val PCS_NAD83_Oklahoma_South = 32125
  val PCS_NAD83_Oregon_North = 32126
  val PCS_NAD83_Oregon_South = 32127
  val PCS_NAD83_Pennsylvania_N = 32128
  val PCS_NAD83_Pennsylvania_S = 32129
  val PCS_NAD83_Rhode_Island = 32130
  val PCS_NAD83_South_Carolina = 32133
  val PCS_NAD83_South_Dakota_N = 32134
  val PCS_NAD83_South_Dakota_S = 32135
  val PCS_NAD83_Tennessee = 32136
  val PCS_NAD83_Texas_North = 32137
  val PCS_NAD83_Texas_North_Cen = 32138
  val PCS_NAD83_Texas_Central = 32139
  val PCS_NAD83_Texas_South_Cen = 32140
  val PCS_NAD83_Texas_South = 32141
  val PCS_NAD83_Utah_North = 32142
  val PCS_NAD83_Utah_Central = 32143
  val PCS_NAD83_Utah_South = 32144
  val PCS_NAD83_Vermont = 32145
  val PCS_NAD83_Virginia_North = 32146
  val PCS_NAD83_Virginia_South = 32147
  val PCS_NAD83_Washington_North = 32148
  val PCS_NAD83_Washington_South = 32149
  val PCS_NAD83_West_Virginia_N = 32150
  val PCS_NAD83_West_Virginia_S = 32151
  val PCS_NAD83_Wisconsin_North = 32152
  val PCS_NAD83_Wisconsin_Cen = 32153
  val PCS_NAD83_Wisconsin_South = 32154
  val PCS_NAD83_Wyoming_East = 32155
  val PCS_NAD83_Wyoming_E_Cen = 32156
  val PCS_NAD83_Wyoming_W_Cen = 32157
  val PCS_NAD83_Wyoming_West = 32158
  val PCS_NAD83_Puerto_Rico_Virgin_Is = 32161
  val PCS_WGS72_UTM_zone_1N = 32201
  val PCS_WGS72_UTM_zone_2N = 32202
  val PCS_WGS72_UTM_zone_3N = 32203
  val PCS_WGS72_UTM_zone_4N = 32204
  val PCS_WGS72_UTM_zone_5N = 32205
  val PCS_WGS72_UTM_zone_6N = 32206
  val PCS_WGS72_UTM_zone_7N = 32207
  val PCS_WGS72_UTM_zone_8N = 32208
  val PCS_WGS72_UTM_zone_9N = 32209
  val PCS_WGS72_UTM_zone_10N = 32210
  val PCS_WGS72_UTM_zone_11N = 32211
  val PCS_WGS72_UTM_zone_12N = 32212
  val PCS_WGS72_UTM_zone_13N = 32213
  val PCS_WGS72_UTM_zone_14N = 32214
  val PCS_WGS72_UTM_zone_15N = 32215
  val PCS_WGS72_UTM_zone_16N = 32216
  val PCS_WGS72_UTM_zone_17N = 32217
  val PCS_WGS72_UTM_zone_18N = 32218
  val PCS_WGS72_UTM_zone_19N = 32219
  val PCS_WGS72_UTM_zone_20N = 32220
  val PCS_WGS72_UTM_zone_21N = 32221
  val PCS_WGS72_UTM_zone_22N = 32222
  val PCS_WGS72_UTM_zone_23N = 32223
  val PCS_WGS72_UTM_zone_24N = 32224
  val PCS_WGS72_UTM_zone_25N = 32225
  val PCS_WGS72_UTM_zone_26N = 32226
  val PCS_WGS72_UTM_zone_27N = 32227
  val PCS_WGS72_UTM_zone_28N = 32228
  val PCS_WGS72_UTM_zone_29N = 32229
  val PCS_WGS72_UTM_zone_30N = 32230
  val PCS_WGS72_UTM_zone_31N = 32231
  val PCS_WGS72_UTM_zone_32N = 32232
  val PCS_WGS72_UTM_zone_33N = 32233
  val PCS_WGS72_UTM_zone_34N = 32234
  val PCS_WGS72_UTM_zone_35N = 32235
  val PCS_WGS72_UTM_zone_36N = 32236
  val PCS_WGS72_UTM_zone_37N = 32237
  val PCS_WGS72_UTM_zone_38N = 32238
  val PCS_WGS72_UTM_zone_39N = 32239
  val PCS_WGS72_UTM_zone_40N = 32240
  val PCS_WGS72_UTM_zone_41N = 32241
  val PCS_WGS72_UTM_zone_42N = 32242
  val PCS_WGS72_UTM_zone_43N = 32243
  val PCS_WGS72_UTM_zone_44N = 32244
  val PCS_WGS72_UTM_zone_45N = 32245
  val PCS_WGS72_UTM_zone_46N = 32246
  val PCS_WGS72_UTM_zone_47N = 32247
  val PCS_WGS72_UTM_zone_48N = 32248
  val PCS_WGS72_UTM_zone_49N = 32249
  val PCS_WGS72_UTM_zone_50N = 32250
  val PCS_WGS72_UTM_zone_51N = 32251
  val PCS_WGS72_UTM_zone_52N = 32252
  val PCS_WGS72_UTM_zone_53N = 32253
  val PCS_WGS72_UTM_zone_54N = 32254
  val PCS_WGS72_UTM_zone_55N = 32255
  val PCS_WGS72_UTM_zone_56N = 32256
  val PCS_WGS72_UTM_zone_57N = 32257
  val PCS_WGS72_UTM_zone_58N = 32258
  val PCS_WGS72_UTM_zone_59N = 32259
  val PCS_WGS72_UTM_zone_60N = 32260
  val PCS_WGS72_UTM_zone_1S = 32301
  val PCS_WGS72_UTM_zone_2S = 32302
  val PCS_WGS72_UTM_zone_3S = 32303
  val PCS_WGS72_UTM_zone_4S = 32304
  val PCS_WGS72_UTM_zone_5S = 32305
  val PCS_WGS72_UTM_zone_6S = 32306
  val PCS_WGS72_UTM_zone_7S = 32307
  val PCS_WGS72_UTM_zone_8S = 32308
  val PCS_WGS72_UTM_zone_9S = 32309
  val PCS_WGS72_UTM_zone_10S = 32310
  val PCS_WGS72_UTM_zone_11S = 32311
  val PCS_WGS72_UTM_zone_12S = 32312
  val PCS_WGS72_UTM_zone_13S = 32313
  val PCS_WGS72_UTM_zone_14S = 32314
  val PCS_WGS72_UTM_zone_15S = 32315
  val PCS_WGS72_UTM_zone_16S = 32316
  val PCS_WGS72_UTM_zone_17S = 32317
  val PCS_WGS72_UTM_zone_18S = 32318
  val PCS_WGS72_UTM_zone_19S = 32319
  val PCS_WGS72_UTM_zone_20S = 32320
  val PCS_WGS72_UTM_zone_21S = 32321
  val PCS_WGS72_UTM_zone_22S = 32322
  val PCS_WGS72_UTM_zone_23S = 32323
  val PCS_WGS72_UTM_zone_24S = 32324
  val PCS_WGS72_UTM_zone_25S = 32325
  val PCS_WGS72_UTM_zone_26S = 32326
  val PCS_WGS72_UTM_zone_27S = 32327
  val PCS_WGS72_UTM_zone_28S = 32328
  val PCS_WGS72_UTM_zone_29S = 32329
  val PCS_WGS72_UTM_zone_30S = 32330
  val PCS_WGS72_UTM_zone_31S = 32331
  val PCS_WGS72_UTM_zone_32S = 32332
  val PCS_WGS72_UTM_zone_33S = 32333
  val PCS_WGS72_UTM_zone_34S = 32334
  val PCS_WGS72_UTM_zone_35S = 32335
  val PCS_WGS72_UTM_zone_36S = 32336
  val PCS_WGS72_UTM_zone_37S = 32337
  val PCS_WGS72_UTM_zone_38S = 32338
  val PCS_WGS72_UTM_zone_39S = 32339
  val PCS_WGS72_UTM_zone_40S = 32340
  val PCS_WGS72_UTM_zone_41S = 32341
  val PCS_WGS72_UTM_zone_42S = 32342
  val PCS_WGS72_UTM_zone_43S = 32343
  val PCS_WGS72_UTM_zone_44S = 32344
  val PCS_WGS72_UTM_zone_45S = 32345
  val PCS_WGS72_UTM_zone_46S = 32346
  val PCS_WGS72_UTM_zone_47S = 32347
  val PCS_WGS72_UTM_zone_48S = 32348
  val PCS_WGS72_UTM_zone_49S = 32349
  val PCS_WGS72_UTM_zone_50S = 32350
  val PCS_WGS72_UTM_zone_51S = 32351
  val PCS_WGS72_UTM_zone_52S = 32352
  val PCS_WGS72_UTM_zone_53S = 32353
  val PCS_WGS72_UTM_zone_54S = 32354
  val PCS_WGS72_UTM_zone_55S = 32355
  val PCS_WGS72_UTM_zone_56S = 32356
  val PCS_WGS72_UTM_zone_57S = 32357
  val PCS_WGS72_UTM_zone_58S = 32358
  val PCS_WGS72_UTM_zone_59S = 32359
  val PCS_WGS72_UTM_zone_60S = 32360
  val PCS_WGS72BE_UTM_zone_1N = 32401
  val PCS_WGS72BE_UTM_zone_2N = 32402
  val PCS_WGS72BE_UTM_zone_3N = 32403
  val PCS_WGS72BE_UTM_zone_4N = 32404
  val PCS_WGS72BE_UTM_zone_5N = 32405
  val PCS_WGS72BE_UTM_zone_6N = 32406
  val PCS_WGS72BE_UTM_zone_7N = 32407
  val PCS_WGS72BE_UTM_zone_8N = 32408
  val PCS_WGS72BE_UTM_zone_9N = 32409
  val PCS_WGS72BE_UTM_zone_10N = 32410
  val PCS_WGS72BE_UTM_zone_11N = 32411
  val PCS_WGS72BE_UTM_zone_12N = 32412
  val PCS_WGS72BE_UTM_zone_13N = 32413
  val PCS_WGS72BE_UTM_zone_14N = 32414
  val PCS_WGS72BE_UTM_zone_15N = 32415
  val PCS_WGS72BE_UTM_zone_16N = 32416
  val PCS_WGS72BE_UTM_zone_17N = 32417
  val PCS_WGS72BE_UTM_zone_18N = 32418
  val PCS_WGS72BE_UTM_zone_19N = 32419
  val PCS_WGS72BE_UTM_zone_20N = 32420
  val PCS_WGS72BE_UTM_zone_21N = 32421
  val PCS_WGS72BE_UTM_zone_22N = 32422
  val PCS_WGS72BE_UTM_zone_23N = 32423
  val PCS_WGS72BE_UTM_zone_24N = 32424
  val PCS_WGS72BE_UTM_zone_25N = 32425
  val PCS_WGS72BE_UTM_zone_26N = 32426
  val PCS_WGS72BE_UTM_zone_27N = 32427
  val PCS_WGS72BE_UTM_zone_28N = 32428
  val PCS_WGS72BE_UTM_zone_29N = 32429
  val PCS_WGS72BE_UTM_zone_30N = 32430
  val PCS_WGS72BE_UTM_zone_31N = 32431
  val PCS_WGS72BE_UTM_zone_32N = 32432
  val PCS_WGS72BE_UTM_zone_33N = 32433
  val PCS_WGS72BE_UTM_zone_34N = 32434
  val PCS_WGS72BE_UTM_zone_35N = 32435
  val PCS_WGS72BE_UTM_zone_36N = 32436
  val PCS_WGS72BE_UTM_zone_37N = 32437
  val PCS_WGS72BE_UTM_zone_38N = 32438
  val PCS_WGS72BE_UTM_zone_39N = 32439
  val PCS_WGS72BE_UTM_zone_40N = 32440
  val PCS_WGS72BE_UTM_zone_41N = 32441
  val PCS_WGS72BE_UTM_zone_42N = 32442
  val PCS_WGS72BE_UTM_zone_43N = 32443
  val PCS_WGS72BE_UTM_zone_44N = 32444
  val PCS_WGS72BE_UTM_zone_45N = 32445
  val PCS_WGS72BE_UTM_zone_46N = 32446
  val PCS_WGS72BE_UTM_zone_47N = 32447
  val PCS_WGS72BE_UTM_zone_48N = 32448
  val PCS_WGS72BE_UTM_zone_49N = 32449
  val PCS_WGS72BE_UTM_zone_50N = 32450
  val PCS_WGS72BE_UTM_zone_51N = 32451
  val PCS_WGS72BE_UTM_zone_52N = 32452
  val PCS_WGS72BE_UTM_zone_53N = 32453
  val PCS_WGS72BE_UTM_zone_54N = 32454
  val PCS_WGS72BE_UTM_zone_55N = 32455
  val PCS_WGS72BE_UTM_zone_56N = 32456
  val PCS_WGS72BE_UTM_zone_57N = 32457
  val PCS_WGS72BE_UTM_zone_58N = 32458
  val PCS_WGS72BE_UTM_zone_59N = 32459
  val PCS_WGS72BE_UTM_zone_60N = 32460
  val PCS_WGS72BE_UTM_zone_1S = 32501
  val PCS_WGS72BE_UTM_zone_2S = 32502
  val PCS_WGS72BE_UTM_zone_3S = 32503
  val PCS_WGS72BE_UTM_zone_4S = 32504
  val PCS_WGS72BE_UTM_zone_5S = 32505
  val PCS_WGS72BE_UTM_zone_6S = 32506
  val PCS_WGS72BE_UTM_zone_7S = 32507
  val PCS_WGS72BE_UTM_zone_8S = 32508
  val PCS_WGS72BE_UTM_zone_9S = 32509
  val PCS_WGS72BE_UTM_zone_10S = 32510
  val PCS_WGS72BE_UTM_zone_11S = 32511
  val PCS_WGS72BE_UTM_zone_12S = 32512
  val PCS_WGS72BE_UTM_zone_13S = 32513
  val PCS_WGS72BE_UTM_zone_14S = 32514
  val PCS_WGS72BE_UTM_zone_15S = 32515
  val PCS_WGS72BE_UTM_zone_16S = 32516
  val PCS_WGS72BE_UTM_zone_17S = 32517
  val PCS_WGS72BE_UTM_zone_18S = 32518
  val PCS_WGS72BE_UTM_zone_19S = 32519
  val PCS_WGS72BE_UTM_zone_20S = 32520
  val PCS_WGS72BE_UTM_zone_21S = 32521
  val PCS_WGS72BE_UTM_zone_22S = 32522
  val PCS_WGS72BE_UTM_zone_23S = 32523
  val PCS_WGS72BE_UTM_zone_24S = 32524
  val PCS_WGS72BE_UTM_zone_25S = 32525
  val PCS_WGS72BE_UTM_zone_26S = 32526
  val PCS_WGS72BE_UTM_zone_27S = 32527
  val PCS_WGS72BE_UTM_zone_28S = 32528
  val PCS_WGS72BE_UTM_zone_29S = 32529
  val PCS_WGS72BE_UTM_zone_30S = 32530
  val PCS_WGS72BE_UTM_zone_31S = 32531
  val PCS_WGS72BE_UTM_zone_32S = 32532
  val PCS_WGS72BE_UTM_zone_33S = 32533
  val PCS_WGS72BE_UTM_zone_34S = 32534
  val PCS_WGS72BE_UTM_zone_35S = 32535
  val PCS_WGS72BE_UTM_zone_36S = 32536
  val PCS_WGS72BE_UTM_zone_37S = 32537
  val PCS_WGS72BE_UTM_zone_38S = 32538
  val PCS_WGS72BE_UTM_zone_39S = 32539
  val PCS_WGS72BE_UTM_zone_40S = 32540
  val PCS_WGS72BE_UTM_zone_41S = 32541
  val PCS_WGS72BE_UTM_zone_42S = 32542
  val PCS_WGS72BE_UTM_zone_43S = 32543
  val PCS_WGS72BE_UTM_zone_44S = 32544
  val PCS_WGS72BE_UTM_zone_45S = 32545
  val PCS_WGS72BE_UTM_zone_46S = 32546
  val PCS_WGS72BE_UTM_zone_47S = 32547
  val PCS_WGS72BE_UTM_zone_48S = 32548
  val PCS_WGS72BE_UTM_zone_49S = 32549
  val PCS_WGS72BE_UTM_zone_50S = 32550
  val PCS_WGS72BE_UTM_zone_51S = 32551
  val PCS_WGS72BE_UTM_zone_52S = 32552
  val PCS_WGS72BE_UTM_zone_53S = 32553
  val PCS_WGS72BE_UTM_zone_54S = 32554
  val PCS_WGS72BE_UTM_zone_55S = 32555
  val PCS_WGS72BE_UTM_zone_56S = 32556
  val PCS_WGS72BE_UTM_zone_57S = 32557
  val PCS_WGS72BE_UTM_zone_58S = 32558
  val PCS_WGS72BE_UTM_zone_59S = 32559
  val PCS_WGS72BE_UTM_zone_60S = 32560
  val PCS_WGS84_UTM_zone_1N = 32601
  val PCS_WGS84_UTM_zone_2N = 32602
  val PCS_WGS84_UTM_zone_3N = 32603
  val PCS_WGS84_UTM_zone_4N = 32604
  val PCS_WGS84_UTM_zone_5N = 32605
  val PCS_WGS84_UTM_zone_6N = 32606
  val PCS_WGS84_UTM_zone_7N = 32607
  val PCS_WGS84_UTM_zone_8N = 32608
  val PCS_WGS84_UTM_zone_9N = 32609
  val PCS_WGS84_UTM_zone_10N = 32610
  val PCS_WGS84_UTM_zone_11N = 32611
  val PCS_WGS84_UTM_zone_12N = 32612
  val PCS_WGS84_UTM_zone_13N = 32613
  val PCS_WGS84_UTM_zone_14N = 32614
  val PCS_WGS84_UTM_zone_15N = 32615
  val PCS_WGS84_UTM_zone_16N = 32616
  val PCS_WGS84_UTM_zone_17N = 32617
  val PCS_WGS84_UTM_zone_18N = 32618
  val PCS_WGS84_UTM_zone_19N = 32619
  val PCS_WGS84_UTM_zone_20N = 32620
  val PCS_WGS84_UTM_zone_21N = 32621
  val PCS_WGS84_UTM_zone_22N = 32622
  val PCS_WGS84_UTM_zone_23N = 32623
  val PCS_WGS84_UTM_zone_24N = 32624
  val PCS_WGS84_UTM_zone_25N = 32625
  val PCS_WGS84_UTM_zone_26N = 32626
  val PCS_WGS84_UTM_zone_27N = 32627
  val PCS_WGS84_UTM_zone_28N = 32628
  val PCS_WGS84_UTM_zone_29N = 32629
  val PCS_WGS84_UTM_zone_30N = 32630
  val PCS_WGS84_UTM_zone_31N = 32631
  val PCS_WGS84_UTM_zone_32N = 32632
  val PCS_WGS84_UTM_zone_33N = 32633
  val PCS_WGS84_UTM_zone_34N = 32634
  val PCS_WGS84_UTM_zone_35N = 32635
  val PCS_WGS84_UTM_zone_36N = 32636
  val PCS_WGS84_UTM_zone_37N = 32637
  val PCS_WGS84_UTM_zone_38N = 32638
  val PCS_WGS84_UTM_zone_39N = 32639
  val PCS_WGS84_UTM_zone_40N = 32640
  val PCS_WGS84_UTM_zone_41N = 32641
  val PCS_WGS84_UTM_zone_42N = 32642
  val PCS_WGS84_UTM_zone_43N = 32643
  val PCS_WGS84_UTM_zone_44N = 32644
  val PCS_WGS84_UTM_zone_45N = 32645
  val PCS_WGS84_UTM_zone_46N = 32646
  val PCS_WGS84_UTM_zone_47N = 32647
  val PCS_WGS84_UTM_zone_48N = 32648
  val PCS_WGS84_UTM_zone_49N = 32649
  val PCS_WGS84_UTM_zone_50N = 32650
  val PCS_WGS84_UTM_zone_51N = 32651
  val PCS_WGS84_UTM_zone_52N = 32652
  val PCS_WGS84_UTM_zone_53N = 32653
  val PCS_WGS84_UTM_zone_54N = 32654
  val PCS_WGS84_UTM_zone_55N = 32655
  val PCS_WGS84_UTM_zone_56N = 32656
  val PCS_WGS84_UTM_zone_57N = 32657
  val PCS_WGS84_UTM_zone_58N = 32658
  val PCS_WGS84_UTM_zone_59N = 32659
  val PCS_WGS84_UTM_zone_60N = 32660
  val PCS_WGS84_UTM_zone_1S = 32701
  val PCS_WGS84_UTM_zone_2S = 32702
  val PCS_WGS84_UTM_zone_3S = 32703
  val PCS_WGS84_UTM_zone_4S = 32704
  val PCS_WGS84_UTM_zone_5S = 32705
  val PCS_WGS84_UTM_zone_6S = 32706
  val PCS_WGS84_UTM_zone_7S = 32707
  val PCS_WGS84_UTM_zone_8S = 32708
  val PCS_WGS84_UTM_zone_9S = 32709
  val PCS_WGS84_UTM_zone_10S = 32710
  val PCS_WGS84_UTM_zone_11S = 32711
  val PCS_WGS84_UTM_zone_12S = 32712
  val PCS_WGS84_UTM_zone_13S = 32713
  val PCS_WGS84_UTM_zone_14S = 32714
  val PCS_WGS84_UTM_zone_15S = 32715
  val PCS_WGS84_UTM_zone_16S = 32716
  val PCS_WGS84_UTM_zone_17S = 32717
  val PCS_WGS84_UTM_zone_18S = 32718
  val PCS_WGS84_UTM_zone_19S = 32719
  val PCS_WGS84_UTM_zone_20S = 32720
  val PCS_WGS84_UTM_zone_21S = 32721
  val PCS_WGS84_UTM_zone_22S = 32722
  val PCS_WGS84_UTM_zone_23S = 32723
  val PCS_WGS84_UTM_zone_24S = 32724
  val PCS_WGS84_UTM_zone_25S = 32725
  val PCS_WGS84_UTM_zone_26S = 32726
  val PCS_WGS84_UTM_zone_27S = 32727
  val PCS_WGS84_UTM_zone_28S = 32728
  val PCS_WGS84_UTM_zone_29S = 32729
  val PCS_WGS84_UTM_zone_30S = 32730
  val PCS_WGS84_UTM_zone_31S = 32731
  val PCS_WGS84_UTM_zone_32S = 32732
  val PCS_WGS84_UTM_zone_33S = 32733
  val PCS_WGS84_UTM_zone_34S = 32734
  val PCS_WGS84_UTM_zone_35S = 32735
  val PCS_WGS84_UTM_zone_36S = 32736
  val PCS_WGS84_UTM_zone_37S = 32737
  val PCS_WGS84_UTM_zone_38S = 32738
  val PCS_WGS84_UTM_zone_39S = 32739
  val PCS_WGS84_UTM_zone_40S = 32740
  val PCS_WGS84_UTM_zone_41S = 32741
  val PCS_WGS84_UTM_zone_42S = 32742
  val PCS_WGS84_UTM_zone_43S = 32743
  val PCS_WGS84_UTM_zone_44S = 32744
  val PCS_WGS84_UTM_zone_45S = 32745
  val PCS_WGS84_UTM_zone_46S = 32746
  val PCS_WGS84_UTM_zone_47S = 32747
  val PCS_WGS84_UTM_zone_48S = 32748
  val PCS_WGS84_UTM_zone_49S = 32749
  val PCS_WGS84_UTM_zone_50S = 32750
  val PCS_WGS84_UTM_zone_51S = 32751
  val PCS_WGS84_UTM_zone_52S = 32752
  val PCS_WGS84_UTM_zone_53S = 32753
  val PCS_WGS84_UTM_zone_54S = 32754
  val PCS_WGS84_UTM_zone_55S = 32755
  val PCS_WGS84_UTM_zone_56S = 32756
  val PCS_WGS84_UTM_zone_57S = 32757
  val PCS_WGS84_UTM_zone_58S = 32758
  val PCS_WGS84_UTM_zone_59S = 32759
  val PCS_WGS84_UTM_zone_60S = 32760

}

object GDALEPSGProjectionTypes {

  val Proj_Stereo_70 = 19926
  val Proj_Alabama_CS27_East = 10101
  val Proj_Alabama_CS27_West = 10102
  val Proj_Alabama_CS83_East = 10131
  val Proj_Alabama_CS83_West = 10132
  val Proj_Arizona_Coordinate_System_east = 10201
  val Proj_Arizona_Coordinate_System_Central = 10202
  val Proj_Arizona_Coordinate_System_west = 10203
  val Proj_Arizona_CS83_east = 10231
  val Proj_Arizona_CS83_Central = 10232
  val Proj_Arizona_CS83_west = 10233
  val Proj_Arkansas_CS27_North = 10301
  val Proj_Arkansas_CS27_South = 10302
  val Proj_Arkansas_CS83_North = 10331
  val Proj_Arkansas_CS83_South = 10332
  val Proj_California_CS27_I = 10401
  val Proj_California_CS27_II = 10402
  val Proj_California_CS27_III = 10403
  val Proj_California_CS27_IV = 10404
  val Proj_California_CS27_V = 10405
  val Proj_California_CS27_VI = 10406
  val Proj_California_CS27_VII = 10407
  val Proj_California_CS83_1 = 10431
  val Proj_California_CS83_2 = 10432
  val Proj_California_CS83_3 = 10433
  val Proj_California_CS83_4 = 10434
  val Proj_California_CS83_5 = 10435
  val Proj_California_CS83_6 = 10436
  val Proj_Colorado_CS27_North = 10501
  val Proj_Colorado_CS27_Central = 10502
  val Proj_Colorado_CS27_South = 10503
  val Proj_Colorado_CS83_North = 10531
  val Proj_Colorado_CS83_Central = 10532
  val Proj_Colorado_CS83_South = 10533
  val Proj_Connecticut_CS27 = 10600
  val Proj_Connecticut_CS83 = 10630
  val Proj_Delaware_CS27 = 10700
  val Proj_Delaware_CS83 = 10730
  val Proj_Florida_CS27_East = 10901
  val Proj_Florida_CS27_West = 10902
  val Proj_Florida_CS27_North = 10903
  val Proj_Florida_CS83_East = 10931
  val Proj_Florida_CS83_West = 10932
  val Proj_Florida_CS83_North = 10933
  val Proj_Georgia_CS27_East = 11001
  val Proj_Georgia_CS27_West = 11002
  val Proj_Georgia_CS83_East = 11031
  val Proj_Georgia_CS83_West = 11032
  val Proj_Idaho_CS27_East = 11101
  val Proj_Idaho_CS27_Central = 11102
  val Proj_Idaho_CS27_West = 11103
  val Proj_Idaho_CS83_East = 11131
  val Proj_Idaho_CS83_Central = 11132
  val Proj_Idaho_CS83_West = 11133
  val Proj_Illinois_CS27_East = 11201
  val Proj_Illinois_CS27_West = 11202
  val Proj_Illinois_CS83_East = 11231
  val Proj_Illinois_CS83_West = 11232
  val Proj_Indiana_CS27_East = 11301
  val Proj_Indiana_CS27_West = 11302
  val Proj_Indiana_CS83_East = 11331
  val Proj_Indiana_CS83_West = 11332
  val Proj_Iowa_CS27_North = 11401
  val Proj_Iowa_CS27_South = 11402
  val Proj_Iowa_CS83_North = 11431
  val Proj_Iowa_CS83_South = 11432
  val Proj_Kansas_CS27_North = 11501
  val Proj_Kansas_CS27_South = 11502
  val Proj_Kansas_CS83_North = 11531
  val Proj_Kansas_CS83_South = 11532
  val Proj_Kentucky_CS27_North = 11601
  val Proj_Kentucky_CS27_South = 11602
  val Proj_Kentucky_CS83_North = 15303
  val Proj_Kentucky_CS83_South = 11632
  val Proj_Louisiana_CS27_North = 11701
  val Proj_Louisiana_CS27_South = 11702
  val Proj_Louisiana_CS83_North = 11731
  val Proj_Louisiana_CS83_South = 11732
  val Proj_Maine_CS27_East = 11801
  val Proj_Maine_CS27_West = 11802
  val Proj_Maine_CS83_East = 11831
  val Proj_Maine_CS83_West = 11832
  val Proj_Maryland_CS27 = 11900
  val Proj_Maryland_CS83 = 11930
  val Proj_Massachusetts_CS27_Mainland = 12001
  val Proj_Massachusetts_CS27_Island = 12002
  val Proj_Massachusetts_CS83_Mainland = 12031
  val Proj_Massachusetts_CS83_Island = 12032
  val Proj_Michigan_State_Plane_East = 12101
  val Proj_Michigan_State_Plane_Old_Central = 12102
  val Proj_Michigan_State_Plane_West = 12103
  val Proj_Michigan_CS27_North = 12111
  val Proj_Michigan_CS27_Central = 12112
  val Proj_Michigan_CS27_South = 12113
  val Proj_Michigan_CS83_North = 12141
  val Proj_Michigan_CS83_Central = 12142
  val Proj_Michigan_CS83_South = 12143
  val Proj_Minnesota_CS27_North = 12201
  val Proj_Minnesota_CS27_Central = 12202
  val Proj_Minnesota_CS27_South = 12203
  val Proj_Minnesota_CS83_North = 12231
  val Proj_Minnesota_CS83_Central = 12232
  val Proj_Minnesota_CS83_South = 12233
  val Proj_Mississippi_CS27_East = 12301
  val Proj_Mississippi_CS27_West = 12302
  val Proj_Mississippi_CS83_East = 12331
  val Proj_Mississippi_CS83_West = 12332
  val Proj_Missouri_CS27_East = 12401
  val Proj_Missouri_CS27_Central = 12402
  val Proj_Missouri_CS27_West = 12403
  val Proj_Missouri_CS83_East = 12431
  val Proj_Missouri_CS83_Central = 12432
  val Proj_Missouri_CS83_West = 12433
  val Proj_Montana_CS27_North = 12501
  val Proj_Montana_CS27_Central = 12502
  val Proj_Montana_CS27_South = 12503
  val Proj_Montana_CS83 = 12530
  val Proj_Nebraska_CS27_North = 12601
  val Proj_Nebraska_CS27_South = 12602
  val Proj_Nebraska_CS83 = 12630
  val Proj_Nevada_CS27_East = 12701
  val Proj_Nevada_CS27_Central = 12702
  val Proj_Nevada_CS27_West = 12703
  val Proj_Nevada_CS83_East = 12731
  val Proj_Nevada_CS83_Central = 12732
  val Proj_Nevada_CS83_West = 12733
  val Proj_New_Hampshire_CS27 = 12800
  val Proj_New_Hampshire_CS83 = 12830
  val Proj_New_Jersey_CS27 = 12900
  val Proj_New_Jersey_CS83 = 12930
  val Proj_New_Mexico_CS27_East = 13001
  val Proj_New_Mexico_CS27_Central = 13002
  val Proj_New_Mexico_CS27_West = 13003
  val Proj_New_Mexico_CS83_East = 13031
  val Proj_New_Mexico_CS83_Central = 13032
  val Proj_New_Mexico_CS83_West = 13033
  val Proj_New_York_CS27_East = 13101
  val Proj_New_York_CS27_Central = 13102
  val Proj_New_York_CS27_West = 13103
  val Proj_New_York_CS27_Long_Island = 13104
  val Proj_New_York_CS83_East = 13131
  val Proj_New_York_CS83_Central = 13132
  val Proj_New_York_CS83_West = 13133
  val Proj_New_York_CS83_Long_Island = 13134
  val Proj_North_Carolina_CS27 = 13200
  val Proj_North_Carolina_CS83 = 13230
  val Proj_North_Dakota_CS27_North = 13301
  val Proj_North_Dakota_CS27_South = 13302
  val Proj_North_Dakota_CS83_North = 13331
  val Proj_North_Dakota_CS83_South = 13332
  val Proj_Ohio_CS27_North = 13401
  val Proj_Ohio_CS27_South = 13402
  val Proj_Ohio_CS83_North = 13431
  val Proj_Ohio_CS83_South = 13432
  val Proj_Oklahoma_CS27_North = 13501
  val Proj_Oklahoma_CS27_South = 13502
  val Proj_Oklahoma_CS83_North = 13531
  val Proj_Oklahoma_CS83_South = 13532
  val Proj_Oregon_CS27_North = 13601
  val Proj_Oregon_CS27_South = 13602
  val Proj_Oregon_CS83_North = 13631
  val Proj_Oregon_CS83_South = 13632
  val Proj_Pennsylvania_CS27_North = 13701
  val Proj_Pennsylvania_CS27_South = 13702
  val Proj_Pennsylvania_CS83_North = 13731
  val Proj_Pennsylvania_CS83_South = 13732
  val Proj_Rhode_Island_CS27 = 13800
  val Proj_Rhode_Island_CS83 = 13830
  val Proj_South_Carolina_CS27_North = 13901
  val Proj_South_Carolina_CS27_South = 13902
  val Proj_South_Carolina_CS83 = 13930
  val Proj_South_Dakota_CS27_North = 14001
  val Proj_South_Dakota_CS27_South = 14002
  val Proj_South_Dakota_CS83_North = 14031
  val Proj_South_Dakota_CS83_South = 14032
  val Proj_Tennessee_CS27 = 15302
  val Proj_Tennessee_CS83 = 14130
  val Proj_Texas_CS27_North = 14201
  val Proj_Texas_CS27_North_Central = 14202
  val Proj_Texas_CS27_Central = 14203
  val Proj_Texas_CS27_South_Central = 14204
  val Proj_Texas_CS27_South = 14205
  val Proj_Texas_CS83_North = 14231
  val Proj_Texas_CS83_North_Central = 14232
  val Proj_Texas_CS83_Central = 14233
  val Proj_Texas_CS83_South_Central = 14234
  val Proj_Texas_CS83_South = 14235
  val Proj_Utah_CS27_North = 14301
  val Proj_Utah_CS27_Central = 14302
  val Proj_Utah_CS27_South = 14303
  val Proj_Utah_CS83_North = 14331
  val Proj_Utah_CS83_Central = 14332
  val Proj_Utah_CS83_South = 14333
  val Proj_Vermont_CS27 = 14400
  val Proj_Vermont_CS83 = 14430
  val Proj_Virginia_CS27_North = 14501
  val Proj_Virginia_CS27_South = 14502
  val Proj_Virginia_CS83_North = 14531
  val Proj_Virginia_CS83_South = 14532
  val Proj_Washington_CS27_North = 14601
  val Proj_Washington_CS27_South = 14602
  val Proj_Washington_CS83_North = 14631
  val Proj_Washington_CS83_South = 14632
  val Proj_West_Virginia_CS27_North = 14701
  val Proj_West_Virginia_CS27_South = 14702
  val Proj_West_Virginia_CS83_North = 14731
  val Proj_West_Virginia_CS83_South = 14732
  val Proj_Wisconsin_CS27_North = 14801
  val Proj_Wisconsin_CS27_Central = 14802
  val Proj_Wisconsin_CS27_South = 14803
  val Proj_Wisconsin_CS83_North = 14831
  val Proj_Wisconsin_CS83_Central = 14832
  val Proj_Wisconsin_CS83_South = 14833
  val Proj_Wyoming_CS27_East = 14901
  val Proj_Wyoming_CS27_East_Central = 14902
  val Proj_Wyoming_CS27_West_Central = 14903
  val Proj_Wyoming_CS27_West = 14904
  val Proj_Wyoming_CS83_East = 14931
  val Proj_Wyoming_CS83_East_Central = 14932
  val Proj_Wyoming_CS83_West_Central = 14933
  val Proj_Wyoming_CS83_West = 14934
  val Proj_Alaska_CS27_1 = 15001
  val Proj_Alaska_CS27_2 = 15002
  val Proj_Alaska_CS27_3 = 15003
  val Proj_Alaska_CS27_4 = 15004
  val Proj_Alaska_CS27_5 = 15005
  val Proj_Alaska_CS27_6 = 15006
  val Proj_Alaska_CS27_7 = 15007
  val Proj_Alaska_CS27_8 = 15008
  val Proj_Alaska_CS27_9 = 15009
  val Proj_Alaska_CS27_10 = 15010
  val Proj_Alaska_CS83_1 = 15031
  val Proj_Alaska_CS83_2 = 15032
  val Proj_Alaska_CS83_3 = 15033
  val Proj_Alaska_CS83_4 = 15034
  val Proj_Alaska_CS83_5 = 15035
  val Proj_Alaska_CS83_6 = 15036
  val Proj_Alaska_CS83_7 = 15037
  val Proj_Alaska_CS83_8 = 15038
  val Proj_Alaska_CS83_9 = 15039
  val Proj_Alaska_CS83_10 = 15040
  val Proj_Hawaii_CS27_1 = 15101
  val Proj_Hawaii_CS27_2 = 15102
  val Proj_Hawaii_CS27_3 = 15103
  val Proj_Hawaii_CS27_4 = 15104
  val Proj_Hawaii_CS27_5 = 15105
  val Proj_Hawaii_CS83_1 = 15131
  val Proj_Hawaii_CS83_2 = 15132
  val Proj_Hawaii_CS83_3 = 15133
  val Proj_Hawaii_CS83_4 = 15134
  val Proj_Hawaii_CS83_5 = 15135
  val Proj_Puerto_Rico_CS27 = 15201
  val Proj_St_Croix = 15202
  val Proj_Puerto_Rico_Virgin_Is = 15230
  val Proj_BLM_14N_feet = 15914
  val Proj_BLM_15N_feet = 15915
  val Proj_BLM_16N_feet = 15916
  val Proj_BLM_17N_feet = 15917
  val Proj_UTM_zone_1N =   16001
  val Proj_UTM_zone_2N =   16002
  val Proj_UTM_zone_3N =   16003
  val Proj_UTM_zone_4N =   16004
  val Proj_UTM_zone_5N =   16005
  val Proj_UTM_zone_6N =   16006
  val Proj_UTM_zone_7N =   16007
  val Proj_UTM_zone_8N =   16008
  val Proj_UTM_zone_9N =   16009
  val Proj_UTM_zone_10N =  16010
  val Proj_UTM_zone_11N =  16011
  val Proj_UTM_zone_12N =  16012
  val Proj_UTM_zone_13N =  16013
  val Proj_UTM_zone_14N =  16014
  val Proj_UTM_zone_15N =  16015
  val Proj_UTM_zone_16N =  16016
  val Proj_UTM_zone_17N =  16017
  val Proj_UTM_zone_18N =  16018
  val Proj_UTM_zone_19N =  16019
  val Proj_UTM_zone_20N =  16020
  val Proj_UTM_zone_21N =  16021
  val Proj_UTM_zone_22N =  16022
  val Proj_UTM_zone_23N =  16023
  val Proj_UTM_zone_24N =  16024
  val Proj_UTM_zone_25N =  16025
  val Proj_UTM_zone_26N =  16026
  val Proj_UTM_zone_27N =  16027
  val Proj_UTM_zone_28N =  16028
  val Proj_UTM_zone_29N =  16029
  val Proj_UTM_zone_30N =  16030
  val Proj_UTM_zone_31N =  16031
  val Proj_UTM_zone_32N =  16032
  val Proj_UTM_zone_33N =  16033
  val Proj_UTM_zone_34N =  16034
  val Proj_UTM_zone_35N =  16035
  val Proj_UTM_zone_36N =  16036
  val Proj_UTM_zone_37N =  16037
  val Proj_UTM_zone_38N =  16038
  val Proj_UTM_zone_39N =  16039
  val Proj_UTM_zone_40N =  16040
  val Proj_UTM_zone_41N =  16041
  val Proj_UTM_zone_42N =  16042
  val Proj_UTM_zone_43N =  16043
  val Proj_UTM_zone_44N =  16044
  val Proj_UTM_zone_45N =  16045
  val Proj_UTM_zone_46N =  16046
  val Proj_UTM_zone_47N =  16047
  val Proj_UTM_zone_48N =  16048
  val Proj_UTM_zone_49N =  16049
  val Proj_UTM_zone_50N =  16050
  val Proj_UTM_zone_51N =  16051
  val Proj_UTM_zone_52N =  16052
  val Proj_UTM_zone_53N =  16053
  val Proj_UTM_zone_54N =  16054
  val Proj_UTM_zone_55N =  16055
  val Proj_UTM_zone_56N =  16056
  val Proj_UTM_zone_57N =  16057
  val Proj_UTM_zone_58N =  16058
  val Proj_UTM_zone_59N =  16059
  val Proj_UTM_zone_60N =  16060
  val Proj_UTM_zone_1S =   16101
  val Proj_UTM_zone_2S =   16102
  val Proj_UTM_zone_3S =   16103
  val Proj_UTM_zone_4S =   16104
  val Proj_UTM_zone_5S =   16105
  val Proj_UTM_zone_6S =   16106
  val Proj_UTM_zone_7S =   16107
  val Proj_UTM_zone_8S =   16108
  val Proj_UTM_zone_9S =   16109
  val Proj_UTM_zone_10S =  16110
  val Proj_UTM_zone_11S =  16111
  val Proj_UTM_zone_12S =  16112
  val Proj_UTM_zone_13S =  16113
  val Proj_UTM_zone_14S =  16114
  val Proj_UTM_zone_15S =  16115
  val Proj_UTM_zone_16S =  16116
  val Proj_UTM_zone_17S =  16117
  val Proj_UTM_zone_18S =  16118
  val Proj_UTM_zone_19S =  16119
  val Proj_UTM_zone_20S =  16120
  val Proj_UTM_zone_21S =  16121
  val Proj_UTM_zone_22S =  16122
  val Proj_UTM_zone_23S =  16123
  val Proj_UTM_zone_24S =  16124
  val Proj_UTM_zone_25S =  16125
  val Proj_UTM_zone_26S =  16126
  val Proj_UTM_zone_27S =  16127
  val Proj_UTM_zone_28S =  16128
  val Proj_UTM_zone_29S =  16129
  val Proj_UTM_zone_30S =  16130
  val Proj_UTM_zone_31S =  16131
  val Proj_UTM_zone_32S =  16132
  val Proj_UTM_zone_33S =  16133
  val Proj_UTM_zone_34S =  16134
  val Proj_UTM_zone_35S =  16135
  val Proj_UTM_zone_36S =  16136
  val Proj_UTM_zone_37S =  16137
  val Proj_UTM_zone_38S =  16138
  val Proj_UTM_zone_39S =  16139
  val Proj_UTM_zone_40S =  16140
  val Proj_UTM_zone_41S =  16141
  val Proj_UTM_zone_42S =  16142
  val Proj_UTM_zone_43S =  16143
  val Proj_UTM_zone_44S =  16144
  val Proj_UTM_zone_45S =  16145
  val Proj_UTM_zone_46S =  16146
  val Proj_UTM_zone_47S =  16147
  val Proj_UTM_zone_48S =  16148
  val Proj_UTM_zone_49S =  16149
  val Proj_UTM_zone_50S =  16150
  val Proj_UTM_zone_51S =  16151
  val Proj_UTM_zone_52S =  16152
  val Proj_UTM_zone_53S =  16153
  val Proj_UTM_zone_54S =  16154
  val Proj_UTM_zone_55S =  16155
  val Proj_UTM_zone_56S =  16156
  val Proj_UTM_zone_57S =  16157
  val Proj_UTM_zone_58S =  16158
  val Proj_UTM_zone_59S =  16159
  val Proj_UTM_zone_60S =  16160
  val Proj_Gauss_Kruger_zone_0 =  16200
  val Proj_Gauss_Kruger_zone_1 =  16201
  val Proj_Gauss_Kruger_zone_2 =  16202
  val Proj_Gauss_Kruger_zone_3 =  16203
  val Proj_Gauss_Kruger_zone_4 =  16204
  val Proj_Gauss_Kruger_zone_5 =  16205
  val Proj_Map_Grid_of_Australia_48 = 17348
  val Proj_Map_Grid_of_Australia_49 = 17349
  val Proj_Map_Grid_of_Australia_50 = 17350
  val Proj_Map_Grid_of_Australia_51 = 17351
  val Proj_Map_Grid_of_Australia_52 = 17352
  val Proj_Map_Grid_of_Australia_53 = 17353
  val Proj_Map_Grid_of_Australia_54 = 17354
  val Proj_Map_Grid_of_Australia_55 = 17355
  val Proj_Map_Grid_of_Australia_56 = 17356
  val Proj_Map_Grid_of_Australia_57 = 17357
  val Proj_Map_Grid_of_Australia_58 = 17358
  val Proj_Australian_Map_Grid_48 = 17448
  val Proj_Australian_Map_Grid_49 = 17449
  val Proj_Australian_Map_Grid_50 = 17450
  val Proj_Australian_Map_Grid_51 = 17451
  val Proj_Australian_Map_Grid_52 = 17452
  val Proj_Australian_Map_Grid_53 = 17453
  val Proj_Australian_Map_Grid_54 = 17454
  val Proj_Australian_Map_Grid_55 = 17455
  val Proj_Australian_Map_Grid_56 = 17456
  val Proj_Australian_Map_Grid_57 = 17457
  val Proj_Australian_Map_Grid_58 = 17458
  val Proj_Argentina_1 = 18031
  val Proj_Argentina_2 = 18032
  val Proj_Argentina_3 = 18033
  val Proj_Argentina_4 = 18034
  val Proj_Argentina_5 = 18035
  val Proj_Argentina_6 = 18036
  val Proj_Argentina_7 = 18037
  val Proj_Colombia_3W = 18051
  val Proj_Colombia_Bogota = 18052
  val Proj_Colombia_3E = 18053
  val Proj_Colombia_6E = 18054
  val Proj_Egypt_Red_Belt = 18072
  val Proj_Egypt_Purple_Belt = 18073
  val Proj_Extended_Purple_Belt = 18074
  val Proj_New_Zealand_North_Island_Nat_Grid = 18141
  val Proj_New_Zealand_South_Island_Nat_Grid = 18142
  val Proj_Bahrain_Grid = 19900
  val Proj_Netherlands_E_Indies_Equatorial = 19905
  val Proj_RSO_Borneo = 19912

}

object ProjectionTypesMap {

  import EPSGProjectionTypes._
  import GDALEPSGProjectionTypes._

  val UserDefinedProjectionType = 32767

  val projectionTypesMap = HashMap[Int, Int](
    PCS_NAD83_Alabama_East -> Proj_Alabama_CS83_East,
    PCS_NAD83_Alabama_West -> Proj_Alabama_CS83_West,
    PCS_NAD83_Alaska_zone_1 -> Proj_Alaska_CS83_1,
    PCS_NAD83_Alaska_zone_2 -> Proj_Alaska_CS83_2,
    PCS_NAD83_Alaska_zone_3 -> Proj_Alaska_CS83_3,
    PCS_NAD83_Alaska_zone_4 -> Proj_Alaska_CS83_4,
    PCS_NAD83_Alaska_zone_5 -> Proj_Alaska_CS83_5,
    PCS_NAD83_Alaska_zone_6 -> Proj_Alaska_CS83_6,
    PCS_NAD83_Alaska_zone_7 -> Proj_Alaska_CS83_7,
    PCS_NAD83_Alaska_zone_8 -> Proj_Alaska_CS83_8,
    PCS_NAD83_Alaska_zone_9 -> Proj_Alaska_CS83_9,
    PCS_NAD83_Alaska_zone_10 -> Proj_Alaska_CS83_10,
    PCS_NAD83_California_1 -> Proj_California_CS83_1,
    PCS_NAD83_California_2 -> Proj_California_CS83_2,
    PCS_NAD83_California_3 -> Proj_California_CS83_3,
    PCS_NAD83_California_4 -> Proj_California_CS83_4,
    PCS_NAD83_California_5 -> Proj_California_CS83_5,
    PCS_NAD83_California_6 -> Proj_California_CS83_6,
    PCS_NAD83_Arizona_East -> Proj_Arizona_CS83_east,
    PCS_NAD83_Arizona_Central -> Proj_Arizona_CS83_Central,
    PCS_NAD83_Arizona_West -> Proj_Arizona_CS83_west,
    PCS_NAD83_Arkansas_North -> Proj_Arkansas_CS83_North,
    PCS_NAD83_Arkansas_South -> Proj_Arkansas_CS83_South,
    PCS_NAD83_Colorado_North -> Proj_Colorado_CS83_North,
    PCS_NAD83_Colorado_Central -> Proj_Colorado_CS83_Central,
    PCS_NAD83_Colorado_South -> Proj_Colorado_CS83_South,
    PCS_NAD83_Connecticut -> Proj_Connecticut_CS83,
    PCS_NAD83_Delaware -> Proj_Delaware_CS83,
    PCS_NAD83_Florida_East -> Proj_Florida_CS83_East,
    PCS_NAD83_Florida_North -> Proj_Florida_CS83_North,
    PCS_NAD83_Florida_West -> Proj_Florida_CS83_West,
    PCS_NAD83_Hawaii_zone_1 -> Proj_Hawaii_CS83_1,
    PCS_NAD83_Hawaii_zone_2 -> Proj_Hawaii_CS83_2,
    PCS_NAD83_Hawaii_zone_3 -> Proj_Hawaii_CS83_3,
    PCS_NAD83_Hawaii_zone_4 -> Proj_Hawaii_CS83_4,
    PCS_NAD83_Hawaii_zone_5 -> Proj_Hawaii_CS83_5,
    PCS_NAD83_Georgia_East -> Proj_Georgia_CS83_East,
    PCS_NAD83_Georgia_West -> Proj_Georgia_CS83_West,
    PCS_NAD83_Idaho_East -> Proj_Idaho_CS83_East,
    PCS_NAD83_Idaho_Central -> Proj_Idaho_CS83_Central,
    PCS_NAD83_Idaho_West -> Proj_Idaho_CS83_West,
    PCS_NAD83_Illinois_East -> Proj_Illinois_CS83_East,
    PCS_NAD83_Illinois_West -> Proj_Illinois_CS83_West,
    PCS_NAD83_Indiana_East -> Proj_Indiana_CS83_East,
    PCS_NAD83_Indiana_West -> Proj_Indiana_CS83_West,
    PCS_NAD83_Iowa_North -> Proj_Iowa_CS83_North,
    PCS_NAD83_Iowa_South -> Proj_Iowa_CS83_South,
    PCS_NAD83_Kansas_North -> Proj_Kansas_CS83_North,
    PCS_NAD83_Kansas_South -> Proj_Kansas_CS83_South,
    PCS_NAD83_Kentucky_North -> Proj_Kentucky_CS83_North,
    PCS_NAD83_Kentucky_South -> Proj_Kentucky_CS83_South,
    PCS_NAD83_Louisiana_North -> Proj_Louisiana_CS83_North,
    PCS_NAD83_Louisiana_South -> Proj_Louisiana_CS83_South,
    PCS_NAD83_Maine_East -> Proj_Maine_CS83_East,
    PCS_NAD83_Maine_West -> Proj_Maine_CS83_West,
    PCS_NAD83_Maryland -> Proj_Maryland_CS83,
    PCS_NAD83_Massachusetts -> Proj_Massachusetts_CS83_Mainland,
    PCS_NAD83_Massachusetts_Is -> Proj_Massachusetts_CS83_Island,
    PCS_NAD83_Michigan_North -> Proj_Michigan_CS83_North,
    PCS_NAD83_Michigan_Central -> Proj_Michigan_CS83_Central,
    PCS_NAD83_Michigan_South -> Proj_Michigan_CS83_South,
    PCS_NAD83_Minnesota_North -> Proj_Minnesota_CS83_North,
    PCS_NAD83_Minnesota_Cent -> Proj_Minnesota_CS83_Central,
    PCS_NAD83_Minnesota_South -> Proj_Minnesota_CS83_South,
    PCS_NAD83_Mississippi_East -> Proj_Mississippi_CS83_East,
    PCS_NAD83_Mississippi_West -> Proj_Mississippi_CS83_West,
    PCS_NAD83_Missouri_East -> Proj_Missouri_CS83_East,
    PCS_NAD83_Missouri_Central -> Proj_Missouri_CS83_Central,
    PCS_NAD83_Missouri_West -> Proj_Missouri_CS83_West,
    PCS_NAD83_Montana -> Proj_Montana_CS83,
    PCS_NAD83_Nebraska -> Proj_Nebraska_CS83,
    PCS_NAD83_Nevada_East -> Proj_Nevada_CS83_East,
    PCS_NAD83_Nevada_Central -> Proj_Nevada_CS83_Central,
    PCS_NAD83_Nevada_West -> Proj_Nevada_CS83_West,
    PCS_NAD83_New_Hampshire -> Proj_New_Hampshire_CS83,
    PCS_NAD83_New_Jersey -> Proj_New_Jersey_CS83,
    PCS_NAD83_New_Mexico_East -> Proj_New_Mexico_CS83_East,
    PCS_NAD83_New_Mexico_Cent -> Proj_New_Mexico_CS83_Central,
    PCS_NAD83_New_Mexico_West -> Proj_New_Mexico_CS83_West,
    PCS_NAD83_New_York_East -> Proj_New_York_CS83_East,
    PCS_NAD83_New_York_Central -> Proj_New_York_CS83_Central,
    PCS_NAD83_New_York_West -> Proj_New_York_CS83_West,
    PCS_NAD83_New_York_Long_Is -> Proj_New_York_CS83_Long_Island,
    PCS_NAD83_North_Carolina -> Proj_North_Carolina_CS83,
    PCS_NAD83_North_Dakota_N -> Proj_North_Dakota_CS83_North,
    PCS_NAD83_North_Dakota_S -> Proj_North_Dakota_CS83_South,
    PCS_NAD83_Ohio_North -> Proj_Ohio_CS83_North,
    PCS_NAD83_Ohio_South -> Proj_Ohio_CS83_South,
    PCS_NAD83_Oklahoma_North -> Proj_Oklahoma_CS83_North,
    PCS_NAD83_Oklahoma_South -> Proj_Oklahoma_CS83_South,
    PCS_NAD83_Oregon_North -> Proj_Oregon_CS83_North,
    PCS_NAD83_Oregon_South -> Proj_Oregon_CS83_South,
    PCS_NAD83_Pennsylvania_N -> Proj_Pennsylvania_CS83_North,
    PCS_NAD83_Pennsylvania_S -> Proj_Pennsylvania_CS83_South,
    PCS_NAD83_Rhode_Island -> Proj_Rhode_Island_CS83,
    PCS_NAD83_South_Carolina -> Proj_South_Carolina_CS83,
    PCS_NAD83_South_Dakota_N -> Proj_South_Dakota_CS83_North,
    PCS_NAD83_South_Dakota_S -> Proj_South_Dakota_CS83_South,
    PCS_NAD83_Tennessee -> Proj_Tennessee_CS83,
    PCS_NAD83_Texas_North -> Proj_Texas_CS83_North,
    PCS_NAD83_Texas_North_Cen -> Proj_Texas_CS83_North_Central,
    PCS_NAD83_Texas_Central -> Proj_Texas_CS83_Central,
    PCS_NAD83_Texas_South_Cen -> Proj_Texas_CS83_South_Central,
    PCS_NAD83_Texas_South -> Proj_Texas_CS83_South,
    PCS_NAD83_Utah_North -> Proj_Utah_CS83_North,
    PCS_NAD83_Utah_Central -> Proj_Utah_CS83_Central,
    PCS_NAD83_Utah_South -> Proj_Utah_CS83_South,
    PCS_NAD83_Vermont -> Proj_Vermont_CS83,
    PCS_NAD83_Virginia_North -> Proj_Virginia_CS83_North,
    PCS_NAD83_Virginia_South -> Proj_Virginia_CS83_South,
    PCS_NAD83_Washington_North -> Proj_Washington_CS83_North,
    PCS_NAD83_Washington_South -> Proj_Washington_CS83_South,
    PCS_NAD83_West_Virginia_N -> Proj_West_Virginia_CS83_North,
    PCS_NAD83_West_Virginia_S -> Proj_West_Virginia_CS83_South,
    PCS_NAD83_Wisconsin_North -> Proj_Wisconsin_CS83_North,
    PCS_NAD83_Wisconsin_Cen -> Proj_Wisconsin_CS83_Central,
    PCS_NAD83_Wisconsin_South -> Proj_Wisconsin_CS83_South,
    PCS_NAD83_Wyoming_East -> Proj_Wyoming_CS83_East,
    PCS_NAD83_Wyoming_E_Cen -> Proj_Wyoming_CS83_East_Central,
    PCS_NAD83_Wyoming_W_Cen -> Proj_Wyoming_CS83_West_Central,
    PCS_NAD83_Wyoming_West -> Proj_Wyoming_CS83_West,
    PCS_NAD83_Puerto_Rico_Virgin_Is -> Proj_Puerto_Rico_Virgin_Is,
    PCS_NAD27_Alabama_East -> Proj_Alabama_CS27_East,
    PCS_NAD27_Alabama_West -> Proj_Alabama_CS27_West,
    PCS_NAD27_Alaska_zone_1 -> Proj_Alaska_CS27_1,
    PCS_NAD27_Alaska_zone_2 -> Proj_Alaska_CS27_2,
    PCS_NAD27_Alaska_zone_3 -> Proj_Alaska_CS27_3,
    PCS_NAD27_Alaska_zone_4 -> Proj_Alaska_CS27_4,
    PCS_NAD27_Alaska_zone_5 -> Proj_Alaska_CS27_5,
    PCS_NAD27_Alaska_zone_6 -> Proj_Alaska_CS27_6,
    PCS_NAD27_Alaska_zone_7 -> Proj_Alaska_CS27_7,
    PCS_NAD27_Alaska_zone_8 -> Proj_Alaska_CS27_8,
    PCS_NAD27_Alaska_zone_9 -> Proj_Alaska_CS27_9,
    PCS_NAD27_Alaska_zone_10 -> Proj_Alaska_CS27_10,
    PCS_NAD27_California_I -> Proj_California_CS27_I,
    PCS_NAD27_California_II -> Proj_California_CS27_II,
    PCS_NAD27_California_III -> Proj_California_CS27_III,
    PCS_NAD27_California_IV -> Proj_California_CS27_IV,
    PCS_NAD27_California_V -> Proj_California_CS27_V,
    PCS_NAD27_California_VI -> Proj_California_CS27_VI,
    PCS_NAD27_California_VII -> Proj_California_CS27_VII,
    PCS_NAD27_Arizona_East -> Proj_Arizona_Coordinate_System_east,
    PCS_NAD27_Arizona_Central -> Proj_Arizona_Coordinate_System_Central,
    PCS_NAD27_Arizona_West -> Proj_Arizona_Coordinate_System_west,
    PCS_NAD27_Arkansas_North -> Proj_Arkansas_CS27_North,
    PCS_NAD27_Arkansas_South -> Proj_Arkansas_CS27_South,
    PCS_NAD27_Colorado_North -> Proj_Colorado_CS27_North,
    PCS_NAD27_Colorado_Central -> Proj_Colorado_CS27_Central,
    PCS_NAD27_Colorado_South -> Proj_Colorado_CS27_South,
    PCS_NAD27_Connecticut -> Proj_Connecticut_CS27,
    PCS_NAD27_Delaware -> Proj_Delaware_CS27,
    PCS_NAD27_Florida_East -> Proj_Florida_CS27_East,
    PCS_NAD27_Florida_North -> Proj_Florida_CS27_North,
    PCS_NAD27_Florida_West -> Proj_Florida_CS27_West,
    PCS_NAD27_Hawaii_zone_1 -> Proj_Hawaii_CS27_1,
    PCS_NAD27_Hawaii_zone_2 -> Proj_Hawaii_CS27_2,
    PCS_NAD27_Hawaii_zone_3 -> Proj_Hawaii_CS27_3,
    PCS_NAD27_Hawaii_zone_4 -> Proj_Hawaii_CS27_4,
    PCS_NAD27_Hawaii_zone_5 -> Proj_Hawaii_CS27_5,
    PCS_NAD27_Georgia_East -> Proj_Georgia_CS27_East,
    PCS_NAD27_Georgia_West -> Proj_Georgia_CS27_West,
    PCS_NAD27_Idaho_East -> Proj_Idaho_CS27_East,
    PCS_NAD27_Idaho_Central -> Proj_Idaho_CS27_Central,
    PCS_NAD27_Idaho_West -> Proj_Idaho_CS27_West,
    PCS_NAD27_Illinois_East -> Proj_Illinois_CS27_East,
    PCS_NAD27_Illinois_West -> Proj_Illinois_CS27_West,
    PCS_NAD27_Indiana_East -> Proj_Indiana_CS27_East,
    PCS_NAD27_Indiana_West -> Proj_Indiana_CS27_West,
    PCS_NAD27_Iowa_North -> Proj_Iowa_CS27_North,
    PCS_NAD27_Iowa_South -> Proj_Iowa_CS27_South,
    PCS_NAD27_Kansas_North -> Proj_Kansas_CS27_North,
    PCS_NAD27_Kansas_South -> Proj_Kansas_CS27_South,
    PCS_NAD27_Kentucky_North -> Proj_Kentucky_CS27_North,
    PCS_NAD27_Kentucky_South -> Proj_Kentucky_CS27_South,
    PCS_NAD27_Louisiana_North -> Proj_Louisiana_CS27_North,
    PCS_NAD27_Louisiana_South -> Proj_Louisiana_CS27_South,
    PCS_NAD27_Maine_East -> Proj_Maine_CS27_East,
    PCS_NAD27_Maine_West -> Proj_Maine_CS27_West,
    PCS_NAD27_Maryland -> Proj_Maryland_CS27,
    PCS_NAD27_Massachusetts -> Proj_Massachusetts_CS27_Mainland,
    PCS_NAD27_Massachusetts_Is -> Proj_Massachusetts_CS27_Island,
    PCS_NAD27_Michigan_North -> Proj_Michigan_CS27_North,
    PCS_NAD27_Michigan_Central -> Proj_Michigan_CS27_Central,
    PCS_NAD27_Michigan_South -> Proj_Michigan_CS27_South,
    PCS_NAD27_Minnesota_North -> Proj_Minnesota_CS27_North,
    PCS_NAD27_Minnesota_Cent -> Proj_Minnesota_CS27_Central,
    PCS_NAD27_Minnesota_South -> Proj_Minnesota_CS27_South,
    PCS_NAD27_Mississippi_East -> Proj_Mississippi_CS27_East,
    PCS_NAD27_Mississippi_West -> Proj_Mississippi_CS27_West,
    PCS_NAD27_Missouri_East -> Proj_Missouri_CS27_East,
    PCS_NAD27_Missouri_Central -> Proj_Missouri_CS27_Central,
    PCS_NAD27_Missouri_West -> Proj_Missouri_CS27_West,
    PCS_NAD27_Montana_North -> Proj_Montana_CS27_North,
    PCS_NAD27_Montana_Central -> Proj_Montana_CS27_Central,
    PCS_NAD27_Montana_South -> Proj_Montana_CS27_South,
    PCS_NAD27_Nebraska_North -> Proj_Nebraska_CS27_North,
    PCS_NAD27_Nebraska_South -> Proj_Nebraska_CS27_South,
    PCS_NAD27_Nevada_East -> Proj_Nevada_CS27_East,
    PCS_NAD27_Nevada_Central -> Proj_Nevada_CS27_Central,
    PCS_NAD27_Nevada_West -> Proj_Nevada_CS27_West,
    PCS_NAD27_New_Hampshire -> Proj_New_Hampshire_CS27,
    PCS_NAD27_New_Jersey -> Proj_New_Jersey_CS27,
    PCS_NAD27_New_Mexico_East -> Proj_New_Mexico_CS27_East,
    PCS_NAD27_New_Mexico_Cent -> Proj_New_Mexico_CS27_Central,
    PCS_NAD27_New_Mexico_West -> Proj_New_Mexico_CS27_West,
    PCS_NAD27_New_York_East -> Proj_New_York_CS27_East,
    PCS_NAD27_New_York_Central -> Proj_New_York_CS27_Central,
    PCS_NAD27_New_York_West -> Proj_New_York_CS27_West,
    PCS_NAD27_New_York_Long_Is -> Proj_New_York_CS27_Long_Island,
    PCS_NAD27_North_Carolina -> Proj_North_Carolina_CS27,
    PCS_NAD27_North_Dakota_N -> Proj_North_Dakota_CS27_North,
    PCS_NAD27_North_Dakota_S -> Proj_North_Dakota_CS27_South,
    PCS_NAD27_Ohio_North -> Proj_Ohio_CS27_North,
    PCS_NAD27_Ohio_South -> Proj_Ohio_CS27_South,
    PCS_NAD27_Oklahoma_North -> Proj_Oklahoma_CS27_North,
    PCS_NAD27_Oklahoma_South -> Proj_Oklahoma_CS27_South,
    PCS_NAD27_Oregon_North -> Proj_Oregon_CS27_North,
    PCS_NAD27_Oregon_South -> Proj_Oregon_CS27_South,
    PCS_NAD27_Pennsylvania_N -> Proj_Pennsylvania_CS27_North,
    PCS_NAD27_Pennsylvania_S -> Proj_Pennsylvania_CS27_South,
    PCS_NAD27_Rhode_Island -> Proj_Rhode_Island_CS27,
    PCS_NAD27_South_Carolina_N -> Proj_South_Carolina_CS27_North,
    PCS_NAD27_South_Carolina_S -> Proj_South_Carolina_CS27_South,
    PCS_NAD27_South_Dakota_N -> Proj_South_Dakota_CS27_North,
    PCS_NAD27_South_Dakota_S -> Proj_South_Dakota_CS27_South,
    PCS_NAD27_Tennessee -> Proj_Tennessee_CS27,
    PCS_NAD27_Texas_North -> Proj_Texas_CS27_North,
    PCS_NAD27_Texas_North_Cen -> Proj_Texas_CS27_North_Central,
    PCS_NAD27_Texas_Central -> Proj_Texas_CS27_Central,
    PCS_NAD27_Texas_South_Cen -> Proj_Texas_CS27_South_Central,
    PCS_NAD27_Texas_South -> Proj_Texas_CS27_South,
    PCS_NAD27_Utah_North -> Proj_Utah_CS27_North,
    PCS_NAD27_Utah_Central -> Proj_Utah_CS27_Central,
    PCS_NAD27_Utah_South -> Proj_Utah_CS27_South,
    PCS_NAD27_Vermont -> Proj_Vermont_CS27,
    PCS_NAD27_Virginia_North -> Proj_Virginia_CS27_North,
    PCS_NAD27_Virginia_South -> Proj_Virginia_CS27_South,
    PCS_NAD27_Washington_North -> Proj_Washington_CS27_North,
    PCS_NAD27_Washington_South -> Proj_Washington_CS27_South,
    PCS_NAD27_West_Virginia_N -> Proj_West_Virginia_CS27_North,
    PCS_NAD27_West_Virginia_S -> Proj_West_Virginia_CS27_South,
    PCS_NAD27_Wisconsin_North -> Proj_Wisconsin_CS27_North,
    PCS_NAD27_Wisconsin_Cen -> Proj_Wisconsin_CS27_Central,
    PCS_NAD27_Wisconsin_South -> Proj_Wisconsin_CS27_South,
    PCS_NAD27_Wyoming_East -> Proj_Wyoming_CS27_East,
    PCS_NAD27_Wyoming_E_Cen -> Proj_Wyoming_CS27_East_Central,
    PCS_NAD27_Wyoming_W_Cen -> Proj_Wyoming_CS27_West_Central,
    PCS_NAD27_Wyoming_West -> Proj_Wyoming_CS27_West,
    PCS_NAD27_Puerto_Rico -> Proj_Puerto_Rico_CS27
  )

}
