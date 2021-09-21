/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.datasketches.hash;

import org.apache.datasketches.memory.Memory;

/**
 * The XxHash is a fast, non-cryptographic, 64-bit hash function that has
 * excellent avalanche and 2-way bit independence properties.
 * This java version used the C++ version and the OpenHFT/Zero-Allocation-Hashing implementation
 * referenced below as inspiration.
 *
 * <p>The C++ source repository:
 * <a href="https://github.com/Cyan4973/xxHash">
 * https://github.com/Cyan4973/xxHash</a>. It has a BSD 2-Clause License:
 * <a href="http://www.opensource.org/licenses/bsd-license.php">
 * http://www.opensource.org/licenses/bsd-license.php</a>   See LICENSE.
 *
 * <p>Portions of this code were adapted from
 * <a href="https://github.com/OpenHFT/Zero-Allocation-Hashing/blob/master/src/main/java/net/openhft/hashing/XxHash.java">
 * OpenHFT/Zero-Allocation-Hashing</a>, which has an Apache 2 license as does this site. See LICENSE.
 *
 * @author Lee Rhodes
 */
public class XxHash {

  /**
   * Compute the hash of the given Memory object.
   * @param mem The given Memory object
   * @param offsetBytes Starting at this offset in bytes
   * @param lengthBytes Continuing for this number of bytes
   * @param seed use this seed for the hash function
   * @return return the resulting 64-bit hash value.
   */
  public static long hash(final Memory mem, final long offsetBytes, final long lengthBytes,
      final long seed) {
    return mem.xxHash64(offsetBytes, lengthBytes, seed);
  }

  /**
   * Returns a 64-bit hash.
   * @param in a long
   * @param seed A long valued seed.
   * @return the hash
   */
  public static long hash(final long in, final long seed) {
    return org.apache.datasketches.memory.XxHash.hashLong(in, seed);
  }

}
