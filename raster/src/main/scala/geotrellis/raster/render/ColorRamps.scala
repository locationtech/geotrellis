/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.render

object ColorRamps extends MatplotLibColorRamps {

  /** Blue to orange color ramp. */
  final def BlueToOrange =
    ColorRamp(
      0x2586ABFF, 0x4EA3C8FF, 0x7FB8D4FF, 0xADD8EAFF,
      0xC8E1E7FF, 0xEDECEAFF, 0xF0E7BBFF, 0xF5CF7DFF,
      0xF9B737FF, 0xE68F2DFF, 0xD76B27FF
    )

  /** Light yellow to orange color ramp. */
  final def LightYellowToOrange =
    ColorRamp(
      0x118C8CFF, 0x429D91FF, 0x61AF96FF, 0x75C59BFF,
      0xA2CF9FFF, 0xC5DAA3FF, 0xE6E5A7FF, 0xE3D28FFF,
      0xE0C078FF, 0xDDAD62FF, 0xD29953FF, 0xCA8746FF,
      0xC2773BFF
    )

  /** Blue to red color ramp. */
  final def BlueToRed =
    ColorRamp(
      0x2791C3FF, 0x5DA1CAFF, 0x83B2D1FF, 0xA8C5D8FF,
      0xCCDBE0FF, 0xE9D3C1FF, 0xDCAD92FF, 0xD08B6CFF,
      0xC66E4BFF, 0xBD4E2EFF
    )

  /** Green to red-orange color ramp. */
  final def GreenToRedOrange =
    ColorRamp(
      0x569543FF, 0x9EBD4DFF, 0xBBCA7AFF, 0xD9E2B2FF,
      0xE4E7C4FF, 0xE6D6BEFF, 0xE3C193FF, 0xDFAC6CFF,
      0xDB9842FF, 0xB96230FF
    )

  /** Light to dark (sunset) color ramp. */
  final def LightToDarkSunset =
    ColorRamp(
      0xFFFFFFFF, 0xFBEDD1FF, 0xF7E0A9FF, 0xEFD299FF,
      0xE8C58BFF, 0xE0B97EFF, 0xF2924DFF, 0xC97877FF,
      0x946196FF, 0x2AB7D6FF, 0x474040FF
    )

  /** Light to dark (green) color ramp. */
  final def LightToDarkGreen =
    ColorRamp(
      0xE8EDDBFF, 0xDCE8D4FF, 0xBEDBADFF, 0xA0CF88FF,
      0x81C561FF, 0x4BAF48FF, 0x1CA049FF, 0x3A6D35FF
    )

  /** Yellow to red color ramp, for use in heatmaps. */
  final def HeatmapYellowToRed =
    ColorRamp(
      0xF7DA22FF, 0xECBE1DFF, 0xE77124FF, 0xD54927FF,
      0xCF3A27FF, 0xA33936FF, 0x7F182AFF, 0x68101AFF
    )

  /** Blue to yellow to red spectrum color ramp, for use in heatmaps. */
  final def HeatmapBlueToYellowToRedSpectrum =
    ColorRamp(
      0x2A2E7FFF, 0x3D5AA9FF, 0x4698D3FF, 0x39C6F0FF,
      0x76C9B3FF, 0xA8D050FF, 0xF6EB14FF, 0xFCB017FF,
      0xF16022FF, 0xEE2C24FF, 0x7D1416FF
    )

  /** Dark red to yellow white color ramp, for use in heatmaps. */
  final def HeatmapDarkRedToYellowWhite =
    ColorRamp(
      0x68101AFF, 0x7F182AFF, 0xA33936FF, 0xCF3A27FF,
      0xD54927FF, 0xE77124FF, 0xECBE1DFF, 0xF7DA22FF,
      0xF6EDB1FF, 0xFFFFFFFF
    )

  /** Light purple to dark purple to white, for use in heatmaps. */
  final def HeatmapLightPurpleToDarkPurpleToWhite =
    ColorRamp(
      0xA52278FF, 0x993086FF, 0x8C3C97FF, 0x6D328AFF,
      0x4E2B81FF, 0x3B264BFF, 0x180B11FF, 0xFFFFFFFF
    )

  /** Bold Land Use color ramp, for use in land use classification. */
  final def ClassificationBoldLandUse =
    ColorRamp(
      0xB29CC3FF, 0x4F8EBBFF, 0x8F9238FF, 0xC18437FF,
      0xB5D6B1FF, 0xD378A6FF, 0xD4563CFF, 0xF9BE47FF
    )

  /** Muted terrain color ramp, for use in classification. */
  final def ClassificationMutedTerrain =
    ColorRamp(
      0xCEE1E8FF, 0x7CBCB5FF, 0x82B36DFF, 0x94C279FF,
      0xD1DE8DFF, 0xEDECC3FF, 0xCCAFB4FF, 0xC99884FF
    )

  /** Constructs a greyscale [[ColorRamp]] with `stops` discrete values. */
  def greyscale(stops: Int): ColorRamp = {
    val colors = (0 to stops)
      .map(i => {
        val c = java.awt.Color.HSBtoRGB(0f, 0f, i / stops.toFloat)
        (c << 8) | 0xFF // Add alpha channel.
      })
    ColorRamp(colors)
  }
}

/*
 * Matplotlib color ramps by Nathaniel J. Smith, Stefan van der Walt,
 * and (in the case of viridis) Eric Firing.
 *
 * See https://github.com/BIDS/colormap/blob/master/colormaps.py
 *
 * Released under the CC0 license/public domain dedication.
 */
trait MatplotLibColorRamps {
  final def Magma =
    ColorRamp(
      0x000004FF, 0x010005FF, 0x010106FF, 0x010108FF,
      0x020109FF, 0x02020BFF, 0x02020DFF, 0x03030FFF,
      0x030312FF, 0x040414FF, 0x050416FF, 0x060518FF,
      0x06051AFF, 0x07061CFF, 0x08071EFF, 0x090720FF,
      0x0A0822FF, 0x0B0924FF, 0x0C0926FF, 0x0D0A29FF,
      0x0E0B2BFF, 0x100B2DFF, 0x110C2FFF, 0x120D31FF,
      0x130D34FF, 0x140E36FF, 0x150E38FF, 0x160F3BFF,
      0x180F3DFF, 0x19103FFF, 0x1A1042FF, 0x1C1044FF,
      0x1D1147FF, 0x1E1149FF, 0x20114BFF, 0x21114EFF,
      0x221150FF, 0x241253FF, 0x251255FF, 0x271258FF,
      0x29115AFF, 0x2A115CFF, 0x2C115FFF, 0x2D1161FF,
      0x2F1163FF, 0x311165FF, 0x331067FF, 0x341069FF,
      0x36106BFF, 0x38106CFF, 0x390F6EFF, 0x3B0F70FF,
      0x3D0F71FF, 0x3F0F72FF, 0x400F74FF, 0x420F75FF,
      0x440F76FF, 0x451077FF, 0x471078FF, 0x491078FF,
      0x4A1079FF, 0x4C117AFF, 0x4E117BFF, 0x4F127BFF,
      0x51127CFF, 0x52137CFF, 0x54137DFF, 0x56147DFF,
      0x57157EFF, 0x59157EFF, 0x5A167EFF, 0x5C167FFF,
      0x5D177FFF, 0x5F187FFF, 0x601880FF, 0x621980FF,
      0x641A80FF, 0x651A80FF, 0x671B80FF, 0x681C81FF,
      0x6A1C81FF, 0x6B1D81FF, 0x6D1D81FF, 0x6E1E81FF,
      0x701F81FF, 0x721F81FF, 0x732081FF, 0x752181FF,
      0x762181FF, 0x782281FF, 0x792282FF, 0x7B2382FF,
      0x7C2382FF, 0x7E2482FF, 0x802582FF, 0x812581FF,
      0x832681FF, 0x842681FF, 0x862781FF, 0x882781FF,
      0x892881FF, 0x8B2981FF, 0x8C2981FF, 0x8E2A81FF,
      0x902A81FF, 0x912B81FF, 0x932B80FF, 0x942C80FF,
      0x962C80FF, 0x982D80FF, 0x992D80FF, 0x9B2E7FFF,
      0x9C2E7FFF, 0x9E2F7FFF, 0xA02F7FFF, 0xA1307EFF,
      0xA3307EFF, 0xA5317EFF, 0xA6317DFF, 0xA8327DFF,
      0xAA337DFF, 0xAB337CFF, 0xAD347CFF, 0xAE347BFF,
      0xB0357BFF, 0xB2357BFF, 0xB3367AFF, 0xB5367AFF,
      0xB73779FF, 0xB83779FF, 0xBA3878FF, 0xBC3978FF,
      0xBD3977FF, 0xBF3A77FF, 0xC03A76FF, 0xC23B75FF,
      0xC43C75FF, 0xC53C74FF, 0xC73D73FF, 0xC83E73FF,
      0xCA3E72FF, 0xCC3F71FF, 0xCD4071FF, 0xCF4070FF,
      0xD0416FFF, 0xD2426FFF, 0xD3436EFF, 0xD5446DFF,
      0xD6456CFF, 0xD8456CFF, 0xD9466BFF, 0xDB476AFF,
      0xDC4869FF, 0xDE4968FF, 0xDF4A68FF, 0xE04C67FF,
      0xE24D66FF, 0xE34E65FF, 0xE44F64FF, 0xE55064FF,
      0xE75263FF, 0xE85362FF, 0xE95462FF, 0xEA5661FF,
      0xEB5760FF, 0xEC5860FF, 0xED5A5FFF, 0xEE5B5EFF,
      0xEF5D5EFF, 0xF05F5EFF, 0xF1605DFF, 0xF2625DFF,
      0xF2645CFF, 0xF3655CFF, 0xF4675CFF, 0xF4695CFF,
      0xF56B5CFF, 0xF66C5CFF, 0xF66E5CFF, 0xF7705CFF,
      0xF7725CFF, 0xF8745CFF, 0xF8765CFF, 0xF9785DFF,
      0xF9795DFF, 0xF97B5DFF, 0xFA7D5EFF, 0xFA7F5EFF,
      0xFA815FFF, 0xFB835FFF, 0xFB8560FF, 0xFB8761FF,
      0xFC8961FF, 0xFC8A62FF, 0xFC8C63FF, 0xFC8E64FF,
      0xFC9065FF, 0xFD9266FF, 0xFD9467FF, 0xFD9668FF,
      0xFD9869FF, 0xFD9A6AFF, 0xFD9B6BFF, 0xFE9D6CFF,
      0xFE9F6DFF, 0xFEA16EFF, 0xFEA36FFF, 0xFEA571FF,
      0xFEA772FF, 0xFEA973FF, 0xFEAA74FF, 0xFEAC76FF,
      0xFEAE77FF, 0xFEB078FF, 0xFEB27AFF, 0xFEB47BFF,
      0xFEB67CFF, 0xFEB77EFF, 0xFEB97FFF, 0xFEBB81FF,
      0xFEBD82FF, 0xFEBF84FF, 0xFEC185FF, 0xFEC287FF,
      0xFEC488FF, 0xFEC68AFF, 0xFEC88CFF, 0xFECA8DFF,
      0xFECC8FFF, 0xFECD90FF, 0xFECF92FF, 0xFED194FF,
      0xFED395FF, 0xFED597FF, 0xFED799FF, 0xFED89AFF,
      0xFDDA9CFF, 0xFDDC9EFF, 0xFDDEA0FF, 0xFDE0A1FF,
      0xFDE2A3FF, 0xFDE3A5FF, 0xFDE5A7FF, 0xFDE7A9FF,
      0xFDE9AAFF, 0xFDEBACFF, 0xFCECAEFF, 0xFCEEB0FF,
      0xFCF0B2FF, 0xFCF2B4FF, 0xFCF4B6FF, 0xFCF6B8FF,
      0xFCF7B9FF, 0xFCF9BBFF, 0xFCFBBDFF, 0xFCFDBFFF
    )

  final val Inferno =
    ColorRamp(
      0x000004FF, 0x010005FF, 0x010106FF, 0x010108FF,
      0x02010AFF, 0x02020CFF, 0x02020EFF, 0x030210FF,
      0x040312FF, 0x040314FF, 0x050417FF, 0x060419FF,
      0x07051BFF, 0x08051DFF, 0x09061FFF, 0x0A0722FF,
      0x0B0724FF, 0x0C0826FF, 0x0D0829FF, 0x0E092BFF,
      0x10092DFF, 0x110A30FF, 0x120A32FF, 0x140B34FF,
      0x150B37FF, 0x160B39FF, 0x180C3CFF, 0x190C3EFF,
      0x1B0C41FF, 0x1C0C43FF, 0x1E0C45FF, 0x1F0C48FF,
      0x210C4AFF, 0x230C4CFF, 0x240C4FFF, 0x260C51FF,
      0x280B53FF, 0x290B55FF, 0x2B0B57FF, 0x2D0B59FF,
      0x2F0A5BFF, 0x310A5CFF, 0x320A5EFF, 0x340A5FFF,
      0x360961FF, 0x380962FF, 0x390963FF, 0x3B0964FF,
      0x3D0965FF, 0x3E0966FF, 0x400A67FF, 0x420A68FF,
      0x440A68FF, 0x450A69FF, 0x470B6AFF, 0x490B6AFF,
      0x4A0C6BFF, 0x4C0C6BFF, 0x4D0D6CFF, 0x4F0D6CFF,
      0x510E6CFF, 0x520E6DFF, 0x540F6DFF, 0x550F6DFF,
      0x57106EFF, 0x59106EFF, 0x5A116EFF, 0x5C126EFF,
      0x5D126EFF, 0x5F136EFF, 0x61136EFF, 0x62146EFF,
      0x64156EFF, 0x65156EFF, 0x67166EFF, 0x69166EFF,
      0x6A176EFF, 0x6C186EFF, 0x6D186EFF, 0x6F196EFF,
      0x71196EFF, 0x721A6EFF, 0x741A6EFF, 0x751B6EFF,
      0x771C6DFF, 0x781C6DFF, 0x7A1D6DFF, 0x7C1D6DFF,
      0x7D1E6DFF, 0x7F1E6CFF, 0x801F6CFF, 0x82206CFF,
      0x84206BFF, 0x85216BFF, 0x87216BFF, 0x88226AFF,
      0x8A226AFF, 0x8C2369FF, 0x8D2369FF, 0x8F2469FF,
      0x902568FF, 0x922568FF, 0x932667FF, 0x952667FF,
      0x972766FF, 0x982766FF, 0x9A2865FF, 0x9B2964FF,
      0x9D2964FF, 0x9F2A63FF, 0xA02A63FF, 0xA22B62FF,
      0xA32C61FF, 0xA52C60FF, 0xA62D60FF, 0xA82E5FFF,
      0xA92E5EFF, 0xAB2F5EFF, 0xAD305DFF, 0xAE305CFF,
      0xB0315BFF, 0xB1325AFF, 0xB3325AFF, 0xB43359FF,
      0xB63458FF, 0xB73557FF, 0xB93556FF, 0xBA3655FF,
      0xBC3754FF, 0xBD3853FF, 0xBF3952FF, 0xC03A51FF,
      0xC13A50FF, 0xC33B4FFF, 0xC43C4EFF, 0xC63D4DFF,
      0xC73E4CFF, 0xC83F4BFF, 0xCA404AFF, 0xCB4149FF,
      0xCC4248FF, 0xCE4347FF, 0xCF4446FF, 0xD04545FF,
      0xD24644FF, 0xD34743FF, 0xD44842FF, 0xD54A41FF,
      0xD74B3FFF, 0xD84C3EFF, 0xD94D3DFF, 0xDA4E3CFF,
      0xDB503BFF, 0xDD513AFF, 0xDE5238FF, 0xDF5337FF,
      0xE05536FF, 0xE15635FF, 0xE25734FF, 0xE35933FF,
      0xE45A31FF, 0xE55C30FF, 0xE65D2FFF, 0xE75E2EFF,
      0xE8602DFF, 0xE9612BFF, 0xEA632AFF, 0xEB6429FF,
      0xEB6628FF, 0xEC6726FF, 0xED6925FF, 0xEE6A24FF,
      0xEF6C23FF, 0xEF6E21FF, 0xF06F20FF, 0xF1711FFF,
      0xF1731DFF, 0xF2741CFF, 0xF3761BFF, 0xF37819FF,
      0xF47918FF, 0xF57B17FF, 0xF57D15FF, 0xF67E14FF,
      0xF68013FF, 0xF78212FF, 0xF78410FF, 0xF8850FFF,
      0xF8870EFF, 0xF8890CFF, 0xF98B0BFF, 0xF98C0AFF,
      0xF98E09FF, 0xFA9008FF, 0xFA9207FF, 0xFA9407FF,
      0xFB9606FF, 0xFB9706FF, 0xFB9906FF, 0xFB9B06FF,
      0xFB9D07FF, 0xFC9F07FF, 0xFCA108FF, 0xFCA309FF,
      0xFCA50AFF, 0xFCA60CFF, 0xFCA80DFF, 0xFCAA0FFF,
      0xFCAC11FF, 0xFCAE12FF, 0xFCB014FF, 0xFCB216FF,
      0xFCB418FF, 0xFBB61AFF, 0xFBB81DFF, 0xFBBA1FFF,
      0xFBBC21FF, 0xFBBE23FF, 0xFAC026FF, 0xFAC228FF,
      0xFAC42AFF, 0xFAC62DFF, 0xF9C72FFF, 0xF9C932FF,
      0xF9CB35FF, 0xF8CD37FF, 0xF8CF3AFF, 0xF7D13DFF,
      0xF7D340FF, 0xF6D543FF, 0xF6D746FF, 0xF5D949FF,
      0xF5DB4CFF, 0xF4DD4FFF, 0xF4DF53FF, 0xF4E156FF,
      0xF3E35AFF, 0xF3E55DFF, 0xF2E661FF, 0xF2E865FF,
      0xF2EA69FF, 0xF1EC6DFF, 0xF1ED71FF, 0xF1EF75FF,
      0xF1F179FF, 0xF2F27DFF, 0xF2F482FF, 0xF3F586FF,
      0xF3F68AFF, 0xF4F88EFF, 0xF5F992FF, 0xF6FA96FF,
      0xF8FB9AFF, 0xF9FC9DFF, 0xFAFDA1FF, 0xFCFFA4FF
    )

  final val Plasma =
    ColorRamp(
      0x0D0887FF, 0x100788FF, 0x130789FF, 0x16078AFF,
      0x19068CFF, 0x1B068DFF, 0x1D068EFF, 0x20068FFF,
      0x220690FF, 0x240691FF, 0x260591FF, 0x280592FF,
      0x2A0593FF, 0x2C0594FF, 0x2E0595FF, 0x2F0596FF,
      0x310597FF, 0x330597FF, 0x350498FF, 0x370499FF,
      0x38049AFF, 0x3A049AFF, 0x3C049BFF, 0x3E049CFF,
      0x3F049CFF, 0x41049DFF, 0x43039EFF, 0x44039EFF,
      0x46039FFF, 0x48039FFF, 0x4903A0FF, 0x4B03A1FF,
      0x4C02A1FF, 0x4E02A2FF, 0x5002A2FF, 0x5102A3FF,
      0x5302A3FF, 0x5502A4FF, 0x5601A4FF, 0x5801A4FF,
      0x5901A5FF, 0x5B01A5FF, 0x5C01A6FF, 0x5E01A6FF,
      0x6001A6FF, 0x6100A7FF, 0x6300A7FF, 0x6400A7FF,
      0x6600A7FF, 0x6700A8FF, 0x6900A8FF, 0x6A00A8FF,
      0x6C00A8FF, 0x6E00A8FF, 0x6F00A8FF, 0x7100A8FF,
      0x7201A8FF, 0x7401A8FF, 0x7501A8FF, 0x7701A8FF,
      0x7801A8FF, 0x7A02A8FF, 0x7B02A8FF, 0x7D03A8FF,
      0x7E03A8FF, 0x8004A8FF, 0x8104A7FF, 0x8305A7FF,
      0x8405A7FF, 0x8606A6FF, 0x8707A6FF, 0x8808A6FF,
      0x8A09A5FF, 0x8B0AA5FF, 0x8D0BA5FF, 0x8E0CA4FF,
      0x8F0DA4FF, 0x910EA3FF, 0x920FA3FF, 0x9410A2FF,
      0x9511A1FF, 0x9613A1FF, 0x9814A0FF, 0x99159FFF,
      0x9A169FFF, 0x9C179EFF, 0x9D189DFF, 0x9E199DFF,
      0xA01A9CFF, 0xA11B9BFF, 0xA21D9AFF, 0xA31E9AFF,
      0xA51F99FF, 0xA62098FF, 0xA72197FF, 0xA82296FF,
      0xAA2395FF, 0xAB2494FF, 0xAC2694FF, 0xAD2793FF,
      0xAE2892FF, 0xB02991FF, 0xB12A90FF, 0xB22B8FFF,
      0xB32C8EFF, 0xB42E8DFF, 0xB52F8CFF, 0xB6308BFF,
      0xB7318AFF, 0xB83289FF, 0xBA3388FF, 0xBB3488FF,
      0xBC3587FF, 0xBD3786FF, 0xBE3885FF, 0xBF3984FF,
      0xC03A83FF, 0xC13B82FF, 0xC23C81FF, 0xC33D80FF,
      0xC43E7FFF, 0xC5407EFF, 0xC6417DFF, 0xC7427CFF,
      0xC8437BFF, 0xC9447AFF, 0xCA457AFF, 0xCB4679FF,
      0xCC4778FF, 0xCC4977FF, 0xCD4A76FF, 0xCE4B75FF,
      0xCF4C74FF, 0xD04D73FF, 0xD14E72FF, 0xD24F71FF,
      0xD35171FF, 0xD45270FF, 0xD5536FFF, 0xD5546EFF,
      0xD6556DFF, 0xD7566CFF, 0xD8576BFF, 0xD9586AFF,
      0xDA5A6AFF, 0xDA5B69FF, 0xDB5C68FF, 0xDC5D67FF,
      0xDD5E66FF, 0xDE5F65FF, 0xDE6164FF, 0xDF6263FF,
      0xE06363FF, 0xE16462FF, 0xE26561FF, 0xE26660FF,
      0xE3685FFF, 0xE4695EFF, 0xE56A5DFF, 0xE56B5DFF,
      0xE66C5CFF, 0xE76E5BFF, 0xE76F5AFF, 0xE87059FF,
      0xE97158FF, 0xE97257FF, 0xEA7457FF, 0xEB7556FF,
      0xEB7655FF, 0xEC7754FF, 0xED7953FF, 0xED7A52FF,
      0xEE7B51FF, 0xEF7C51FF, 0xEF7E50FF, 0xF07F4FFF,
      0xF0804EFF, 0xF1814DFF, 0xF1834CFF, 0xF2844BFF,
      0xF3854BFF, 0xF3874AFF, 0xF48849FF, 0xF48948FF,
      0xF58B47FF, 0xF58C46FF, 0xF68D45FF, 0xF68F44FF,
      0xF79044FF, 0xF79143FF, 0xF79342FF, 0xF89441FF,
      0xF89540FF, 0xF9973FFF, 0xF9983EFF, 0xF99A3EFF,
      0xFA9B3DFF, 0xFA9C3CFF, 0xFA9E3BFF, 0xFB9F3AFF,
      0xFBA139FF, 0xFBA238FF, 0xFCA338FF, 0xFCA537FF,
      0xFCA636FF, 0xFCA835FF, 0xFCA934FF, 0xFDAB33FF,
      0xFDAC33FF, 0xFDAE32FF, 0xFDAF31FF, 0xFDB130FF,
      0xFDB22FFF, 0xFDB42FFF, 0xFDB52EFF, 0xFEB72DFF,
      0xFEB82CFF, 0xFEBA2CFF, 0xFEBB2BFF, 0xFEBD2AFF,
      0xFEBE2AFF, 0xFEC029FF, 0xFDC229FF, 0xFDC328FF,
      0xFDC527FF, 0xFDC627FF, 0xFDC827FF, 0xFDCA26FF,
      0xFDCB26FF, 0xFCCD25FF, 0xFCCE25FF, 0xFCD025FF,
      0xFCD225FF, 0xFBD324FF, 0xFBD524FF, 0xFBD724FF,
      0xFAD824FF, 0xFADA24FF, 0xF9DC24FF, 0xF9DD25FF,
      0xF8DF25FF, 0xF8E125FF, 0xF7E225FF, 0xF7E425FF,
      0xF6E626FF, 0xF6E826FF, 0xF5E926FF, 0xF5EB27FF,
      0xF4ED27FF, 0xF3EE27FF, 0xF3F027FF, 0xF2F227FF,
      0xF1F426FF, 0xF1F525FF, 0xF0F724FF, 0xF0F921FF
    )

  final val Viridis =
    ColorRamp(
      0x440154FF, 0x440256FF, 0x450457FF, 0x450559FF,
      0x46075AFF, 0x46085CFF, 0x460A5DFF, 0x460B5EFF,
      0x470D60FF, 0x470E61FF, 0x471063FF, 0x471164FF,
      0x471365FF, 0x481467FF, 0x481668FF, 0x481769FF,
      0x48186AFF, 0x481A6CFF, 0x481B6DFF, 0x481C6EFF,
      0x481D6FFF, 0x481F70FF, 0x482071FF, 0x482173FF,
      0x482374FF, 0x482475FF, 0x482576FF, 0x482677FF,
      0x482878FF, 0x482979FF, 0x472A7AFF, 0x472C7AFF,
      0x472D7BFF, 0x472E7CFF, 0x472F7DFF, 0x46307EFF,
      0x46327EFF, 0x46337FFF, 0x463480FF, 0x453581FF,
      0x453781FF, 0x453882FF, 0x443983FF, 0x443A83FF,
      0x443B84FF, 0x433D84FF, 0x433E85FF, 0x423F85FF,
      0x424086FF, 0x424186FF, 0x414287FF, 0x414487FF,
      0x404588FF, 0x404688FF, 0x3F4788FF, 0x3F4889FF,
      0x3E4989FF, 0x3E4A89FF, 0x3E4C8AFF, 0x3D4D8AFF,
      0x3D4E8AFF, 0x3C4F8AFF, 0x3C508BFF, 0x3B518BFF,
      0x3B528BFF, 0x3A538BFF, 0x3A548CFF, 0x39558CFF,
      0x39568CFF, 0x38588CFF, 0x38598CFF, 0x375A8CFF,
      0x375B8DFF, 0x365C8DFF, 0x365D8DFF, 0x355E8DFF,
      0x355F8DFF, 0x34608DFF, 0x34618DFF, 0x33628DFF,
      0x33638DFF, 0x32648EFF, 0x32658EFF, 0x31668EFF,
      0x31678EFF, 0x31688EFF, 0x30698EFF, 0x306A8EFF,
      0x2F6B8EFF, 0x2F6C8EFF, 0x2E6D8EFF, 0x2E6E8EFF,
      0x2E6F8EFF, 0x2D708EFF, 0x2D718EFF, 0x2C718EFF,
      0x2C728EFF, 0x2C738EFF, 0x2B748EFF, 0x2B758EFF,
      0x2A768EFF, 0x2A778EFF, 0x2A788EFF, 0x29798EFF,
      0x297A8EFF, 0x297B8EFF, 0x287C8EFF, 0x287D8EFF,
      0x277E8EFF, 0x277F8EFF, 0x27808EFF, 0x26818EFF,
      0x26828EFF, 0x26828EFF, 0x25838EFF, 0x25848EFF,
      0x25858EFF, 0x24868EFF, 0x24878EFF, 0x23888EFF,
      0x23898EFF, 0x238A8DFF, 0x228B8DFF, 0x228C8DFF,
      0x228D8DFF, 0x218E8DFF, 0x218F8DFF, 0x21908DFF,
      0x21918CFF, 0x20928CFF, 0x20928CFF, 0x20938CFF,
      0x1F948CFF, 0x1F958BFF, 0x1F968BFF, 0x1F978BFF,
      0x1F988BFF, 0x1F998AFF, 0x1F9A8AFF, 0x1E9B8AFF,
      0x1E9C89FF, 0x1E9D89FF, 0x1F9E89FF, 0x1F9F88FF,
      0x1FA088FF, 0x1FA188FF, 0x1FA187FF, 0x1FA287FF,
      0x20A386FF, 0x20A486FF, 0x21A585FF, 0x21A685FF,
      0x22A785FF, 0x22A884FF, 0x23A983FF, 0x24AA83FF,
      0x25AB82FF, 0x25AC82FF, 0x26AD81FF, 0x27AD81FF,
      0x28AE80FF, 0x29AF7FFF, 0x2AB07FFF, 0x2CB17EFF,
      0x2DB27DFF, 0x2EB37CFF, 0x2FB47CFF, 0x31B57BFF,
      0x32B67AFF, 0x34B679FF, 0x35B779FF, 0x37B878FF,
      0x38B977FF, 0x3ABA76FF, 0x3BBB75FF, 0x3DBC74FF,
      0x3FBC73FF, 0x40BD72FF, 0x42BE71FF, 0x44BF70FF,
      0x46C06FFF, 0x48C16EFF, 0x4AC16DFF, 0x4CC26CFF,
      0x4EC36BFF, 0x50C46AFF, 0x52C569FF, 0x54C568FF,
      0x56C667FF, 0x58C765FF, 0x5AC864FF, 0x5CC863FF,
      0x5EC962FF, 0x60CA60FF, 0x63CB5FFF, 0x65CB5EFF,
      0x67CC5CFF, 0x69CD5BFF, 0x6CCD5AFF, 0x6ECE58FF,
      0x70CF57FF, 0x73D056FF, 0x75D054FF, 0x77D153FF,
      0x7AD151FF, 0x7CD250FF, 0x7FD34EFF, 0x81D34DFF,
      0x84D44BFF, 0x86D549FF, 0x89D548FF, 0x8BD646FF,
      0x8ED645FF, 0x90D743FF, 0x93D741FF, 0x95D840FF,
      0x98D83EFF, 0x9BD93CFF, 0x9DD93BFF, 0xA0DA39FF,
      0xA2DA37FF, 0xA5DB36FF, 0xA8DB34FF, 0xAADC32FF,
      0xADDC30FF, 0xB0DD2FFF, 0xB2DD2DFF, 0xB5DE2BFF,
      0xB8DE29FF, 0xBADE28FF, 0xBDDF26FF, 0xC0DF25FF,
      0xC2DF23FF, 0xC5E021FF, 0xC8E020FF, 0xCAE11FFF,
      0xCDE11DFF, 0xD0E11CFF, 0xD2E21BFF, 0xD5E21AFF,
      0xD8E219FF, 0xDAE319FF, 0xDDE318FF, 0xDFE318FF,
      0xE2E418FF, 0xE5E419FF, 0xE7E419FF, 0xEAE51AFF,
      0xECE51BFF, 0xEFE51CFF, 0xF1E51DFF, 0xF4E61EFF,
      0xF6E620FF, 0xF8E621FF, 0xFBE723FF, 0xFDE725FF
    )
}
