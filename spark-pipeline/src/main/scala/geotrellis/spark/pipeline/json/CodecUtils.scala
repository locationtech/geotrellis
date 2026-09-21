/*
 * Copyright 2026 Azavea
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

package geotrellis.spark.pipeline.json

import io.circe.{Decoder, HCursor}

private[pipeline] object CodecUtils {
  /**
   * Falls back to `default` only when the field is absent, which is what
   * `Configuration.withDefaults` did. A field that is present but null still decodes normally,
   * so `{"end_zoom": null}` stays `None` while `{}` picks up the declared default.
   */
  def orDefault[A: Decoder](c: HCursor, key: String, default: => A): Decoder.Result[A] =
    c.downField(key).success.fold[Decoder.Result[A]](Right(default))(_.as[A])
}
