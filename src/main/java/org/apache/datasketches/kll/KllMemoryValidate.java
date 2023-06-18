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

package org.apache.datasketches.kll;

import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.EMPTYBIT_AND_PREINTS;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.EMPTYBIT_AND_SER_VER;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.EMPTYBIT_AND_SINGLEFORMAT;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.INVALID_PREINTS;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.SINGLEBIT_AND_PREINTS;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.SINGLEBIT_AND_SER_VER;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.SRC_NOT_KLL;
import static org.apache.datasketches.kll.KllMemoryValidate.MemoryInputError.memoryValidateThrow;
import static org.apache.datasketches.kll.KllPreambleUtil.DATA_START_ADR;
import static org.apache.datasketches.kll.KllPreambleUtil.DATA_START_ADR_SINGLE_ITEM;
import static org.apache.datasketches.kll.KllPreambleUtil.PREAMBLE_INTS_EMPTY_SINGLE;
import static org.apache.datasketches.kll.KllPreambleUtil.PREAMBLE_INTS_FULL;
import static org.apache.datasketches.kll.KllPreambleUtil.SERIAL_VERSION_EMPTY_FULL;
import static org.apache.datasketches.kll.KllPreambleUtil.SERIAL_VERSION_SINGLE;
import static org.apache.datasketches.kll.KllPreambleUtil.SERIAL_VERSION_UPDATABLE;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryEmptyFlag;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryFamilyID;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryFlags;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryK;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryLevelZeroSortedFlag;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryM;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryMinK;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryN;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryNumLevels;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemoryPreInts;
import static org.apache.datasketches.kll.KllPreambleUtil.getMemorySerVer;
import static org.apache.datasketches.kll.KllSketch.SketchType.DOUBLES_SKETCH;
import static org.apache.datasketches.kll.KllSketch.SketchType.FLOATS_SKETCH;

import org.apache.datasketches.common.Family;
import org.apache.datasketches.common.SketchesArgumentException;
import org.apache.datasketches.kll.KllSketch.SketchType;
import org.apache.datasketches.memory.Memory;
import org.apache.datasketches.memory.WritableMemory;

/**
 * This class performs all the error checking of an incoming Memory object and extracts the key fields in the process.
 * This is used by all KLL sketches that read or import Memory objects.
 *
 * @author lrhodes
 *
 */
final class KllMemoryValidate {
  // first 8 bytes of preamble
  final int preInts;
  final int serVer;
  final int familyID;
  final int flags;
  final int k;
  final int m;
  //last byte is unused
  
  //From SerVer
  final boolean serialVersionEmptyFull;
  final boolean singleItemFormat;
  private boolean serialVersionUpdatable;
  
  //Flag bits:
  final boolean empty;
  final boolean level0Sorted;

  // depending on the layout, the next 8-16 bytes of the preamble, may be derived by assumption.
  // For example, if the layout is compact & empty, n = 0, if compact and single, n = 1. 
  long n;        //8 bytes (if present)
  int minK;      //2 bytes (if present)
  int numLevels; //1 byte  (if present)
  //unused byte
  int[] levelsArr; //starts at byte 20, adjusted to include top index here
  
  // derived. Not available for generic
  int sketchBytes = 0;
  private int typeBytes = 0;
  
  KllMemoryValidate(final Memory srcMem, final SketchType sketchType) {
    preInts = getMemoryPreInts(srcMem);
    serVer = getMemorySerVer(srcMem);
    familyID = getMemoryFamilyID(srcMem);
    if (familyID != Family.KLL.getID()) { memoryValidateThrow(SRC_NOT_KLL, familyID); }
    flags = getMemoryFlags(srcMem);
    k = getMemoryK(srcMem);
    m = getMemoryM(srcMem);
    
    KllHelper.checkM(m);
    KllHelper.checkK(k, m);
    //flags
    empty = getMemoryEmptyFlag(srcMem);
    level0Sorted  = getMemoryLevelZeroSortedFlag(srcMem);
    //from serVer
    serialVersionEmptyFull = serVer == SERIAL_VERSION_EMPTY_FULL;
    singleItemFormat = serVer == SERIAL_VERSION_SINGLE;
    serialVersionUpdatable = serVer == SERIAL_VERSION_UPDATABLE;
    
    if (sketchType == DOUBLES_SKETCH) { typeBytes = Double.BYTES; }
    else if (sketchType == FLOATS_SKETCH) { typeBytes = Float.BYTES; }
    //else { typeBytes = 0; }  //TODO

    if (serialVersionUpdatable) { updatableMemFormatValidate((WritableMemory) srcMem); }
    else { compactMemoryValidate(srcMem); }
  }

  private void compactMemoryValidate(final Memory srcMem) { //FOR HEAPIFY.  NOT UPDATABLE
    if (empty && singleItemFormat) { memoryValidateThrow(EMPTYBIT_AND_SINGLEFORMAT, flags); }
    final int sw = (empty ? 1 : 0) | (singleItemFormat ? 4 : 0);

    switch (sw) {
      case 0: { //FULL_COMPACT
        if (preInts != PREAMBLE_INTS_FULL) { memoryValidateThrow(INVALID_PREINTS, preInts); }
        if (serVer != SERIAL_VERSION_EMPTY_FULL) { memoryValidateThrow(EMPTYBIT_AND_SER_VER, serVer); }
        n = getMemoryN(srcMem);
        minK = getMemoryMinK(srcMem);
        numLevels = getMemoryNumLevels(srcMem);

        // Get Levels Arr and add the last element
        levelsArr = new int[numLevels + 1];
        srcMem.getIntArray(DATA_START_ADR, levelsArr, 0, numLevels); //copies all except the last one
        final int capacityItems = KllHelper.computeTotalItemCapacity(k, m, numLevels);
        levelsArr[numLevels] = capacityItems; //load the last one

        final int retainedItems = (levelsArr[numLevels] - levelsArr[0]);
        sketchBytes = DATA_START_ADR + numLevels * Integer.BYTES + 2 * typeBytes + retainedItems * typeBytes;
        break;
      }
      case 1: { //EMPTY_COMPACT
        if (preInts != PREAMBLE_INTS_EMPTY_SINGLE) { memoryValidateThrow(EMPTYBIT_AND_PREINTS, preInts); }
        if (serVer != SERIAL_VERSION_EMPTY_FULL) { memoryValidateThrow(EMPTYBIT_AND_SER_VER, serVer); }
        n = 0;           //assumed
        minK = k;        //assumed
        numLevels = 1;   //assumed
        levelsArr = new int[] {k, k};
        sketchBytes = DATA_START_ADR_SINGLE_ITEM;
        break;
      }
      case 4: { //SINGLE_COMPACT
        if (preInts != PREAMBLE_INTS_EMPTY_SINGLE) { memoryValidateThrow(SINGLEBIT_AND_PREINTS, preInts); }
        if (serVer != SERIAL_VERSION_SINGLE) { memoryValidateThrow(SINGLEBIT_AND_SER_VER, serVer); }
        n = 1;           //assumed
        minK = k;        //assumed
        numLevels = 1;   //assumed
        levelsArr = new int[] {k - 1, k};
        sketchBytes = DATA_START_ADR_SINGLE_ITEM + typeBytes;
        break;
      }
      default: break; //can not happen
    }
  }

  private void updatableMemFormatValidate(final WritableMemory wSrcMem) {
    if (preInts != PREAMBLE_INTS_FULL) { memoryValidateThrow(INVALID_PREINTS, preInts); }
    n = getMemoryN(wSrcMem);
    minK = getMemoryMinK(wSrcMem);
    numLevels = getMemoryNumLevels(wSrcMem);

    levelsArr = new int[numLevels + 1];
    wSrcMem.getIntArray(DATA_START_ADR, levelsArr, 0, numLevels + 1);

    final int capacity = levelsArr[numLevels];

    sketchBytes =
        DATA_START_ADR + levelsArr.length * Integer.BYTES + 2 * typeBytes + capacity * typeBytes;
  }

  enum MemoryInputError {
    SRC_NOT_KLL("FamilyID Field must be: " + Family.KLL.getID() + ", NOT: "),
    EMPTYBIT_AND_PREINTS("Empty Bit: 1 -> PreInts: " + PREAMBLE_INTS_EMPTY_SINGLE + ", NOT: "),
    EMPTYBIT_AND_SER_VER("Empty Bit: 1 -> SerVer: " + SERIAL_VERSION_EMPTY_FULL + ", NOT: "),
    SINGLEBIT_AND_SER_VER("Single Item Bit: 1 -> SerVer: " + SERIAL_VERSION_SINGLE + ", NOT: "),
    SINGLEBIT_AND_PREINTS("Single Item Bit: 1 -> PreInts: " + PREAMBLE_INTS_EMPTY_SINGLE + ", NOT: "),
    INVALID_PREINTS("PreInts Must Be: " + PREAMBLE_INTS_FULL + ", NOT: "),
    EMPTYBIT_AND_SINGLEFORMAT("Empty flag bit and SingleItem Format cannot both be true. Flags: ");

    private String msg;

    private MemoryInputError(final String msg) {
      this.msg = msg;
    }

    private String getMessage() {
      return msg;
    }

    final static void memoryValidateThrow(final MemoryInputError errType, final int errVal) {
      throw new SketchesArgumentException(errType.getMessage() + errVal);
    }

  }

}
