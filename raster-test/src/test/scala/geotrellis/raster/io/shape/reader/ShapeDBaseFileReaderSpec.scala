package geotrellis.raster.io.shape.reader

import org.scalatest._

/**
  * Tests reading .dbf files.
  */
class ShapeDBaseFileReaderSpec extends FunSpec with Matchers {

  def read(path: String) = ShapeDBaseFileReader(path).read

  describe("should read shape DBase files correctly") {

    it("should read demographics.dbf correctly") {
      val dBaseFile = read("raster-test/data/shapefiles/demographics/demographics.dbf")

      testDemographicsMap(dBaseFile(0), 605, "410104003", "Nan Guan Jie Dao", 28894, 44931, 941)
      testDemographicsMap(dBaseFile(1), 754, "410103200", "Qi Li Yan Xiang", 55538, 87009, 2722)
      testDemographicsMap(dBaseFile(2), 335, "410122107", "Xie Zhuang Zhen", 11679, 21465, 1998)
      testDemographicsMap(dBaseFile(3), 498, "410122100", "Cheng Guan Zhen", 43912, 76611, 1718)
      testDemographicsMap(dBaseFile(4), 453, "410183108", "Bai Zhai Zhen", 30115, 57347, 4488)
      testDemographicsMap(dBaseFile(5), 518, "410181112", "Zhi Tian Zhen", 26521, 50551, 3226)
      testDemographicsMap(dBaseFile(6), 442, "410184108", "Guo Dian Zhen", 24751, 44268, 3865)
      testDemographicsMap(dBaseFile(7), 549, "410181113", "Hui Guo Zhen", 41674, 83092, 4726)
      testDemographicsMap(dBaseFile(8), 223, "410182105", "Si Shui Zhen", 12551, 22922, 1597)
      testDemographicsMap(dBaseFile(9), 189, "410185002", "Shao Lin Jie Dao", 9026, 26550, 1107)
      testDemographicsMap(dBaseFile(10), 515, "410181108", "Kang Dian Zhen", 24520, 46814, 2870)
      testDemographicsMap(dBaseFile(11), 280, "410181107", "Nan He Du Zhen", 11677, 21255, 1239)
      testDemographicsMap(dBaseFile(12), 708, "410185201", "Dong Jin Dian Xiang", 30004, 53239, 3483)
      testDemographicsMap(dBaseFile(13), 267, "410102002", "Jian She Lu Jie Dao", 19251, 31242, 496)
      testDemographicsMap(dBaseFile(14), 536, "410182110", "Jia Yu Zhen", 26656, 49568, 3553)
      testDemographicsMap(dBaseFile(15), 247, "410122202", "Dong Zhang Xiang", 12334, 23664, 2002)
      testDemographicsMap(dBaseFile(16), 670, "410181111", "Xi Cun Zhen", 30076, 59840, 3539)
      testDemographicsMap(dBaseFile(17), 552, "410102007", "Lv Dong Cun Jie Dao", 35513, 55469, 1030)
      testDemographicsMap(dBaseFile(18), 170, "410105200", "Yao Qiao Xiang", 14376, 25845, 2312)
      testDemographicsMap(dBaseFile(19), 455, "410181106", "Zhan Jie Zhen", 17717, 32857, 2043)
      testDemographicsMap(dBaseFile(20), 323, "410103009", "Fu Hua Jie Jie Dao", 50090, 78306, 1360)
      testDemographicsMap(dBaseFile(21), 346, "410182100", "Cheng Guan Zhen", 47518, 78704, 1783)
      testDemographicsMap(dBaseFile(22), 276, "410104007", "Long Hai Lu Jie Dao", 27802, 41567, 926)
      testDemographicsMap(dBaseFile(23), 142, "410105005", "Shi Qiao Jie Dao", 18429, 33380, 638)
      testDemographicsMap(dBaseFile(24), 233, "410185102", "Lu Dian Zhen", 14336, 25260, 2037)
      testDemographicsMap(dBaseFile(25), 227, "410183200", "Yuan Zhuang Xiang", 11316, 21557, 1623)
      testDemographicsMap(dBaseFile(26), 95, "410106002", "Xin An Xi Lu Jie Dao", 2302, 3469, 54)
      testDemographicsMap(dBaseFile(27), 451, "410103001", "Huai He Lu Jie Dao", 42725, 68935, 1307)
      testDemographicsMap(dBaseFile(28), 416, "410122201", "Da Meng Xiang", 25410, 47300, 4095)
      testDemographicsMap(dBaseFile(29), 408, "410122108", "Zhang Zhuang Zhen", 16104, 29369, 2646)
      testDemographicsMap(dBaseFile(30), 945, "410105100", "Ji Cheng Zhen", 63461, 98845, 3781)
      testDemographicsMap(dBaseFile(31), 42, "410103002", "Jie Fang Lu Jie Dao", 1835, 3123, 50)
      testDemographicsMap(dBaseFile(32), 359, "410183109", "Yue Cun Zhen", 23064, 42801, 2473)
      testDemographicsMap(dBaseFile(33), 152, "410122204", "Lu Yi Miao Xiang", 8504, 16247, 1409)
      testDemographicsMap(dBaseFile(34), 164, "410103003", "Ming Gong Lu Jie Dao", 12843, 19841, 332)
      testDemographicsMap(dBaseFile(35), 331, "410184101", "Xin Cun Zhen", 18652, 33479, 2559)
      testDemographicsMap(dBaseFile(36), 378, "410122209", "Yao Jia Xiang", 16097, 29944, 2763)
      testDemographicsMap(dBaseFile(37), 449, "410182104", "Wang Cun Zhen", 28888, 51525, 4224)
      testDemographicsMap(dBaseFile(38), 272, "410182106", "Gao Yang Zhen", 15516, 29167, 2325)
      testDemographicsMap(dBaseFile(39), 535, "410105006", "Nan Yang Lu Jie Dao", 39520, 61039, 1082)
      testDemographicsMap(dBaseFile(40), 313, "410184104", "Li He Zhen", 16781, 30298, 2470)
      testDemographicsMap(dBaseFile(41), 185, "410185001", "Song Yang Jie Dao", 35194, 61424, 1874)
      testDemographicsMap(dBaseFile(42), 683, "410182103", "Guang Wu Zhen", 45714, 80915, 6981)
      testDemographicsMap(dBaseFile(43), 272, "410104005", "Dong Da Jie Jie Dao", 15489, 23781, 448)
      testDemographicsMap(dBaseFile(44), 147, "410104004", "Cheng Dong Lu Jie Dao", 18514, 27535, 551)
      testDemographicsMap(dBaseFile(45), 405, "410181110", "Bei Shan Kou Zhen", 23005, 44315, 2937)
      testDemographicsMap(dBaseFile(46), 565, "410184103", "Guan Yin Si Zhen", 24788, 45076, 3607)
      testDemographicsMap(dBaseFile(47), 277, "410181116", "She Cun Zhen", 14245, 27220, 1169)
      testDemographicsMap(dBaseFile(48), 413, "410105007", "Nan Yang Xin Cun Jie Dao", 47205, 76694, 1370)
      testDemographicsMap(dBaseFile(49), 539, "410122206", "San Guan Miao Xiang", 20473, 38018, 3218)
      testDemographicsMap(dBaseFile(50), 448, "410183107", "Liu Zhai Zhen", 22918, 43397, 3255)
      testDemographicsMap(dBaseFile(51), 505, "410184200", "Cheng Guan Xiang", 41155, 70217, 3937)
      testDemographicsMap(dBaseFile(52), 1, "410104200", "Dong Cheng Xiang", 145, 193, 7)
      testDemographicsMap(dBaseFile(53), 730, "410105009", "Feng Chan Lu Jie Dao", 71258, 104263, 2249)
      testDemographicsMap(dBaseFile(54), 563, "410185204", "Shi Dao Xiang", 19754, 35868, 2991)
      testDemographicsMap(dBaseFile(55), 599, "410181001", "Xin Hua Jie Dao", 55103, 87683, 1633)
      testDemographicsMap(dBaseFile(56), 211, "410185105", "Xuan Hua Zhen", 12743, 22576, 1749)
      testDemographicsMap(dBaseFile(57), 119, "410185200", "Song Biao Xiang", 7411, 13635, 976)
      testDemographicsMap(dBaseFile(58), 178, "410182202", "Bei Mang Xiang", 7198, 13547, 1053)
      testDemographicsMap(dBaseFile(59), 611, "410185100", "Da Jin Dian Zhen", 30488, 54424, 4599)
      testDemographicsMap(dBaseFile(60), 390, "410122208", "Diao Jia Xiang", 17325, 32346, 3049)
      testDemographicsMap(dBaseFile(61), 347, "410181115", "Jia Jin Kou Zhen", 13505, 26141, 844)
      testDemographicsMap(dBaseFile(62), 167, "410106200", "Nie Zhai Xiang", 13436, 21357, 857)
      testDemographicsMap(dBaseFile(63), 515, "410183110", "Lai Ji Zhen", 32831, 59663, 3425)
      testDemographicsMap(dBaseFile(64), 321, "410122102", "Guan Du Zhen", 21424, 39313, 3380)
      testDemographicsMap(dBaseFile(65), 269, "410102101", "Shi Fo Zhen", 27422, 44250, 2421)
      testDemographicsMap(dBaseFile(66), 345, "410103008", "Jian Zhong Jie Jie Dao", 35030, 61379, 1050)
      testDemographicsMap(dBaseFile(67), 177, "410104001", "Bei Xia Jie Jie Dao", 16582, 25858, 539)
      testDemographicsMap(dBaseFile(68), 389, "410122205", "Ba Gang Xiang", 15028, 27364, 2516)
      testDemographicsMap(dBaseFile(69), 173, "410103100", "Ma Zhai Zhen", 15206, 28239, 1231)
      testDemographicsMap(dBaseFile(70), 479, "410185203", "Jun Zhao Xiang", 19861, 35877, 3404)
      testDemographicsMap(dBaseFile(71), 114, "410104008", "Zi Jing Shan Nan Lu Jie Dao", 12306, 17705, 361)
      testDemographicsMap(dBaseFile(72), 505, "410184106", "Xue Dian Zhen", 23849, 42802, 3526)
      testDemographicsMap(dBaseFile(73), 1, "410103202", "Long Hai Xiang", 119, 192, 3)
      testDemographicsMap(dBaseFile(74), 204, "410108103", "Mao Zhuang Zhen", 14293, 24686, 2060)
      testDemographicsMap(dBaseFile(75), 583, "410184109", "Long Hu Zhen", 33994, 61017, 4086)
      testDemographicsMap(dBaseFile(76), 492, "410185205", "Tang Zhuang Xiang", 21079, 36351, 3168)
      testDemographicsMap(dBaseFile(77), 597, "410185103", "Gao Cheng Zhen", 34377, 60509, 3415)
      testDemographicsMap(dBaseFile(78), 327, "410108100", "Lao Ya Chen Zhen", 23126, 37840, 1967)
      testDemographicsMap(dBaseFile(79), 239, "410104100", "Shi Ba Li He Zhen", 23873, 39185, 1615)
      testDemographicsMap(dBaseFile(80), 447, "410122203", "Liu Ji Xiang", 22570, 42170, 3780)
      testDemographicsMap(dBaseFile(81), 387, "410182200", "Cheng Guan Xiang", 29591, 52785, 4120)
      testDemographicsMap(dBaseFile(82), 562, "410184203", "Long Wang Xiang", 21037, 39834, 3440)
      testDemographicsMap(dBaseFile(83), 511, "410181104", "Da Yu Gou Zhen", 23370, 42928, 2090)
      testDemographicsMap(dBaseFile(84), 596, "410102001", "Lin Shan Zhai Jie Dao", 47179, 79095, 1348)
      testDemographicsMap(dBaseFile(85), 66, "410181103", "Zhu Lin Zhen", 3425, 6308, 157)
      testDemographicsMap(dBaseFile(86), 345, "410183103", "Ping Mo Zhen", 20903, 39668, 2360)
      testDemographicsMap(dBaseFile(87), 454, "410183105", "Gou Tang Zhen", 27454, 51513, 3217)
      testDemographicsMap(dBaseFile(88), 290, "410122207", "Feng Tang Xiang", 9739, 18370, 1555)
      testDemographicsMap(dBaseFile(89), 568, "410122109", "Huang Dian Zhen", 20388, 37970, 3430)
      testDemographicsMap(dBaseFile(90), 525, "410184107", "Meng Zhuang Zhen", 21937, 39896, 3616)
      testDemographicsMap(dBaseFile(91), 277, "410185003", "Zhong Yue Jie Dao", 11967, 21353, 1520)
      testDemographicsMap(dBaseFile(92), 465, "410182109", "Cui Miao Zhen", 26383, 48460, 3876)
      testDemographicsMap(dBaseFile(93), 154, "410181117", "Tao Yuan Zhen", 5858, 11184, 346)
      testDemographicsMap(dBaseFile(94), 326, "410102200", "Gou Zhao Xiang", 24868, 41616, 3085)
      testDemographicsMap(dBaseFile(95), 451, "410103201", "Hou Zhai Xiang", 27790, 48041, 3332)
      testDemographicsMap(dBaseFile(96), 225, "410185206", "Xu Zhuang Xiang", 14986, 26318, 1628)
      testDemographicsMap(dBaseFile(97), 461, "410183102", "Niu Dian Zhen", 30517, 53315, 3567)
      testDemographicsMap(dBaseFile(98), 431, "410181102", "Xiao Guan Zhen", 20684, 38666, 2264)
      testDemographicsMap(dBaseFile(99), 154, "410185202", "Bai Ping Xiang", 7975, 14468, 894)
      testDemographicsMap(dBaseFile(100), 105, "410122200", "Cang Zhai Xiang", 8166, 14752, 1340)
      testDemographicsMap(dBaseFile(101), 164, "410105004", "Du Ling Jie Dao", 12604, 21617, 334)
      testDemographicsMap(dBaseFile(102), 127, "410102201", "Da Gang Liu Xiang", 11309, 18206, 907)
      testDemographicsMap(dBaseFile(103), 789, "410102003", "Mian Fang Lu Jie Dao", 23116, 37768, 1030)
      testDemographicsMap(dBaseFile(104), 297, "410181101", "Xin Zhong Zhen", 11128, 21073, 1058)
      testDemographicsMap(dBaseFile(105), 641, "410184102", "Xin Dian Zhen", 27693, 50520, 4250)
      testDemographicsMap(dBaseFile(106), 633, "410105001", "Jing Ba Lu Jie Dao", 54366, 84585, 1932)
      testDemographicsMap(dBaseFile(107), 64, "410106003", "Kuang Shan Jie Dao", 1352, 2158, 56)
      testDemographicsMap(dBaseFile(108), 61, "410103010", "De Hua Jie Jie Dao", 3822, 5925, 126)
      testDemographicsMap(dBaseFile(109), 532, "410108102", "Gu Xing Zhen", 26937, 47018, 3686)
      testDemographicsMap(dBaseFile(110), 631, "410183104", "Chao Hua Zhen", 41966, 77551, 2957)
      testDemographicsMap(dBaseFile(111), 485, "410183111", "Qi Li Gang Zhen", 30292, 50946, 1676)
      testDemographicsMap(dBaseFile(112), 606, "410106001", "Ji Yuan Lu Jie Dao", 31889, 50030, 1111)
      testDemographicsMap(dBaseFile(113), 458, "410102004", "Qin Ling Lu Jie Dao", 17048, 27033, 658)
      testDemographicsMap(dBaseFile(114), 238, "410105003", "Ren Min Lu Jie Dao", 28689, 44581, 1023)
      testDemographicsMap(dBaseFile(115), 531, "410181114", "Lu Zhuang Zhen", 31854, 63568, 3915)
      testDemographicsMap(dBaseFile(116), 560, "410184202", "Ba Qian Xiang", 21585, 41596, 3457)
      testDemographicsMap(dBaseFile(117), 417, "410102100", "Xu Shui Zhen", 38784, 67460, 3556)
      testDemographicsMap(dBaseFile(118), 264, "410182107", "Xia Wo Zhen", 17882, 31916, 2412)
      testDemographicsMap(dBaseFile(119), 392, "410105008", "Wen Hua Lu Jie Dao", 87697, 156466, 1938)
      testDemographicsMap(dBaseFile(120), 160, "410183201", "Jian Shan Xiang", 5044, 9616, 762)
      testDemographicsMap(dBaseFile(121), 215, "410122104", "Wan Tan Zhen", 13979, 26241, 2214)
      testDemographicsMap(dBaseFile(122), 443, "410181100", "Mi He Zhen", 26314, 49858, 1814)
      testDemographicsMap(dBaseFile(123), 297, "410182201", "Gao Cun Xiang", 20146, 36699, 2965)
      testDemographicsMap(dBaseFile(124), 133, "410104006", "Er Li Gang Jie Dao", 14132, 20837, 423)
      testDemographicsMap(dBaseFile(125), 5, "410102202", "Zhong Yuan Xiang", 1579, 1811, 4)
      testDemographicsMap(dBaseFile(126), 625, "410184105", "He Zhuang Zhen", 31468, 56365, 3749)
      testDemographicsMap(dBaseFile(127), 387, "410102005", "Tong Bai Lu Jie Dao", 45551, 68813, 1546)
      testDemographicsMap(dBaseFile(128), 621, "410183101", "Mi Cun Zhen", 27235, 47785, 2628)
      testDemographicsMap(dBaseFile(129), 393, "410102008", "Ru He Lu Jie Dao", 37951, 58009, 1215)
      testDemographicsMap(dBaseFile(130), 148, "410184100", "Cheng Guan Zhen", 20257, 34238, 719)
      testDemographicsMap(dBaseFile(131), 539, "410102006", "San Guan Miao Jie Dao", 31454, 47628, 815)
      testDemographicsMap(dBaseFile(132), 290, "410103007", "Da Xue Lu Jie Dao", 35029, 59270, 859)
      testDemographicsMap(dBaseFile(133), 240, "410105002", "Hua Yuan Lu Jie Dao", 32370, 53630, 1096)
      testDemographicsMap(dBaseFile(134), 67, "410182203", "Miao Zi Xiang", 2842, 5033, 405)
      testDemographicsMap(dBaseFile(135), 736, "410185104", "Da Ye Zhen", 42338, 75749, 5269)
      testDemographicsMap(dBaseFile(136), 293, "410181105", "He Luo Zhen", 10902, 21448, 1467)
      testDemographicsMap(dBaseFile(137), 738, "410185101", "Ying Yang Zhen", 24370, 45484, 4111)
      testDemographicsMap(dBaseFile(138), 106, "410108001", "Liu Zhai Jie Dao", 9052, 13463, 273)
      testDemographicsMap(dBaseFile(139), 274, "410105101", "Liu Lin Zhen", 29236, 47844, 2226)
      testDemographicsMap(dBaseFile(140), 507, "410103006", "Wu Li Bao Jie Dao", 36823, 58375, 1093)
      testDemographicsMap(dBaseFile(141), 449, "410182101", "Qiao Lou Zhen", 25975, 46523, 3117)
      testDemographicsMap(dBaseFile(142), 499, "410122106", "Zheng An Zhen", 20959, 38194, 3518)
      testDemographicsMap(dBaseFile(143), 285, "410108101", "Hua Yuan Kou Zhen", 15337, 26845, 2224)
      testDemographicsMap(dBaseFile(144), 400, "410181109", "Xiao Yi Zhen", 23960, 42401, 1628)
      testDemographicsMap(dBaseFile(145), 417, "410104201", "Nan Cao Xiang", 31787, 56511, 3993)
      testDemographicsMap(dBaseFile(146), 324, "410122103", "Lang Cheng Gang Zhen", 17967, 33002, 2758)
      testDemographicsMap(dBaseFile(147), 231, "410104202", "Pu Tian Xiang", 17400, 30570, 2021)
      testDemographicsMap(dBaseFile(148), 182, "410182108", "Liu He Zhen", 11172, 20318, 1469)
      testDemographicsMap(dBaseFile(149), 259, "410122105", "Bai Sha Zhen", 22086, 39601, 3428)
      testDemographicsMap(dBaseFile(150), 263, "410103005", "Mi Feng Zhang Jie Dao", 21312, 33341, 664)
      testDemographicsMap(dBaseFile(151), 821, "410105102", "Miao Li Zhen", 47567, 69519, 1557)
      testDemographicsMap(dBaseFile(152), 439, "410182102", "Yu Long Zhen", 28099, 51758, 3535)
      testDemographicsMap(dBaseFile(153), 398, "410183100", "Cheng Guan Zhen", 59666, 99421, 3546)
      testDemographicsMap(dBaseFile(154), 270, "410104002", "Xi Da Jie Jie Dao", 12576, 19840, 392)
      testDemographicsMap(dBaseFile(155), 378, "410122101", "Han Si Zhen", 21865, 41117, 3676)
      testDemographicsMap(dBaseFile(156), 453, "410183106", "Da Kui Zhen", 31016, 57899, 3793)
      testDemographicsMap(dBaseFile(157), 276, "410184201", "Qian Hu Zhai Xiang", 10721, 19567, 1872)
      testDemographicsMap(dBaseFile(158), 52, "410103004", "Yi Ma Lu Jie Dao", 3472, 5324, 114)
      testDemographicsMap(dBaseFile(159), 719, "410183202", "Qu Liang Xiang", 35426, 66535, 5566)

      dBaseFile.size should be (160)
    }

    def testDemographicsMap(
      map: Map[String, ShapeDBaseRecord],
      lowIncome: Int,
      gbCode: String,
      ename: String,
      workingAge: Int,
      totalPop: Int,
      employment: Int) = {
      map.get("LowIncome") match {
        case Some(LongDBaseRecord(v)) => v should be (lowIncome)
        case _ => fail
      }

      map.get("gbcode") match {
        case Some(StringDBaseRecord(v)) => v should be (gbCode)
        case _ => fail
      }

      map.get("ename") match {
        case Some(StringDBaseRecord(v)) => v should be (ename)
        case _ => fail
      }

      map.get("WorkingAge") match {
        case Some(LongDBaseRecord(v)) => v should be (workingAge)
        case _ => fail
      }

      map.get("TotalPop") match {
        case Some(LongDBaseRecord(v)) => v should be (totalPop)
        case _ => fail
      }

      map.get("Employment") match {
        case Some(LongDBaseRecord(v)) => v should be (employment)
        case _ => fail
      }
    }

    // Since the file is so big, we just run a test
    // with some sample tests.
    it("should read countries.dbf correctly") {
      val dBaseFile = read("raster-test/data/shapefiles/countries/countries.dbf")
      dBaseFile.size should be(255)

      dBaseFile(0)("GU_A3") match {
        case StringDBaseRecord(s) => s should be("ABW")
        case _ => fail
      }

      dBaseFile(23)("NAME_LEN") match {
        case DoubleDBaseRecord(d) => d should be(8.0)
        case _ => fail
      }

      dBaseFile(41)("WOE_ID_EH") match {
        case DoubleDBaseRecord(d) => d should be(2.3424782E7)
        case _ => fail
      }

      dBaseFile(65)("WOE_NOTE") match {
        case StringDBaseRecord(s) => s should be("Exact WOE match as country")
        case _ => fail
      }

      dBaseFile(150)("GDP_MD_EST") match {
        case DoubleDBaseRecord(d) => d should be(9962)
        case _ => fail
      }

      dBaseFile(254)("SUBREGION") match {
        case StringDBaseRecord(s) => s should be("Eastern Africa")
        case _ => fail
      }
    }
  }
}
