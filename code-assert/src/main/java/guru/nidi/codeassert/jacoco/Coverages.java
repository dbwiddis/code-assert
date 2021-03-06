/*
 * Copyright © 2015 Stefan Niederhauser (nidin@gmx.ch)
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
package guru.nidi.codeassert.jacoco;

import java.util.*;
import java.util.stream.Stream;

public class Coverages {
    final Set<Coverage> coverages = new TreeSet<>();
    final Map<String, Coverage> perPackage = new TreeMap<>();
    Coverage global;

    public void add(Coverage coverage) {
        coverages.add(coverage);
        final Coverage pc = perPackage.get(coverage.pack);
        perPackage.put(coverage.pack, pc == null ? coverage.withClazz("") : pc.combined(coverage));
        global = global == null ? coverage : global.combined(coverage);
    }

    Stream<Coverage> asStream() {
        return Stream.concat(Stream.concat(
                Stream.of(global), perPackage.values().stream()), coverages.stream());
    }
}
