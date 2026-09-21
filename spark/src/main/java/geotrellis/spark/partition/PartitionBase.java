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

package geotrellis.spark.partition;

import org.apache.spark.Partition;

/**
 * Spark's Scala 2.13 `Partition` implements `equals` as `super.equals` and so declares an
 * abstract super-accessor, `org$apache$spark$Partition$$super$equals`. Scala 2 synthesises that
 * accessor in every implementing class; Scala 3 does not, and fails with "Member method equals
 * of mixin trait Partition is missing a concrete super implementation".
 *
 * Implementing the interface from Java supplies the accessor directly, and because this is a
 * class rather than a trait, subclasses inherit it instead of having to mix it in. `super.equals`
 * was `AnyRef.equals`, so reference equality is the faithful body. Subclasses that need value
 * semantics (or Spark's `hashCode == index`) override them as they did before.
 */
public abstract class PartitionBase implements Partition {
    @SuppressWarnings("checkstyle:MethodName")
    public boolean org$apache$spark$Partition$$super$equals(Object other) {
        return this == other;
    }

    @Override
    public boolean equals(Object other) {
        return this == other;
    }
}
