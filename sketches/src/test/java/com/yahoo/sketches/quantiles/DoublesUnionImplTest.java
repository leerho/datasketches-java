/*
 * Copyright 2015-16, Yahoo! Inc.
 * Licensed under the terms of the Apache License 2.0. See LICENSE file at the project root for terms.
 */

package com.yahoo.sketches.quantiles;

import static com.yahoo.sketches.quantiles.DirectUpdateDoublesSketchTest.buildAndLoadDQS;
import static com.yahoo.sketches.quantiles.HeapUpdateDoublesSketchTest.buildAndLoadQS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.yahoo.memory.Memory;
import com.yahoo.memory.WritableMemory;

import com.yahoo.sketches.SketchesArgumentException;

public class DoublesUnionImplTest {

  @Test
  public void checkUnion1() {
    DoublesSketch result;
    DoublesSketch qs1 = null;
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(); //virgin 256

    DoublesSketch qs0 = buildAndLoadQS(256, 500);
    union.update(qs0); //me = null, that = valid, exact
    result = union.getResult();
    assertEquals(result.getN(), 500);
    assertEquals(result.getK(), 256);

    union.reset();
    qs1 = buildAndLoadQS(256, 1000); //first 1000
    union.update(qs1); //me = null,  that = valid, OK

    //check copy   me = null,  that = valid
    result = union.getResult();
    assertEquals(result.getN(), 1000);
    assertEquals(result.getK(), 256);

    //check merge  me = valid, that = valid, both K's the same
    DoublesSketch qs2 = buildAndLoadQS(256, 1000, 1000); //add 1000
    union.update(qs2);
    result = union.getResult();
    assertEquals(result.getN(), 2000);
    assertEquals(result.getK(), 256);
  }

  @Test
  public void checkUnion1Direct() {
    DoublesSketch result;
    DoublesSketch qs1 = null;
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(); //virgin 256

    DoublesSketch qs0 = buildAndLoadDQS(256, 500);
    union.update(qs0); //me = null, that = valid, exact
    result = union.getResult();
    assertEquals(result.getN(), 500);
    assertEquals(result.getK(), 256);

    union.reset();
    qs1 = buildAndLoadDQS(256, 1000); //first 1000
    union.update(qs1); //me = null,  that = valid, OK

    //check copy   me = null,  that = valid
    result = union.getResult();
    assertEquals(result.getN(), 1000);
    assertEquals(result.getK(), 256);

    //check merge  me = valid, that = valid, both K's the same
    DoublesSketch qs2 = buildAndLoadDQS(256, 1000, 1000).compact(); //add 1000
    union.update(qs2);
    result = union.getResult();
    assertEquals(result.getN(), 2000);
    assertEquals(result.getK(), 256);
  }

  @Test
  public void checkUnion2() {
    DoublesSketch qs1 = buildAndLoadQS(256, 1000).compact();
    DoublesSketch qs2 = buildAndLoadQS(128, 1000);
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(); //virgin 256
    assertEquals(union.getEffectiveK(), 256);

    union.update(qs1);
    DoublesSketch res1 = union.getResult();
    //println(res1.toString());
    assertEquals(res1.getN(), 1000);
    assertEquals(res1.getK(), 256);

    union.update(qs2);
    DoublesSketch res2 = union.getResult();
    assertEquals(res2.getN(), 2000);
    assertEquals(res2.getK(), 128);
    assertEquals(union.getEffectiveK(), 128);
    println(union.toString());
  }

  @Test
  public void checkUnion2Direct() {
    DoublesSketch qs1 = buildAndLoadDQS(256, 1000);
    DoublesSketch qs2 = buildAndLoadDQS(128, 1000);
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(); //virgin 256
    assertEquals(union.getEffectiveK(), 256);

    union.update(qs1);
    DoublesSketch res1 = union.getResult();
    //println(res1.toString());
    assertEquals(res1.getN(), 1000);
    assertEquals(res1.getK(), 256);

    union.update(qs2);
    DoublesSketch res2 = union.getResult();
    assertEquals(res2.getN(), 2000);
    assertEquals(res2.getK(), 128);
    assertEquals(union.getEffectiveK(), 128);
    println(union.toString());
  }

  @Test
  public void checkUnion3() { //Union is direct, empty and with larger K than valid input
    int k1 = 128;
    int n1 = 2 * k1;
    int k2 = 256;
    int n2 = 2000;
    DoublesSketch sketchIn1 = buildAndLoadQS(k1, n1);
    int bytes = DoublesSketch.getUpdatableStorageBytes(k2, n2);//just for size
    WritableMemory mem = WritableMemory.wrap(new byte[bytes]);
    DoublesUnion union = DoublesUnion.builder().setMaxK(k2).build(mem); //virgin 256
    union.update(sketchIn1);
    assertEquals(union.getMaxK(), k2);
    assertEquals(union.getEffectiveK(), k1);
    DoublesSketch result = union.getResult();
    assertEquals(result.getMaxValue(), n1, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    assertEquals(result.getK(), k1);
  }

  @Test
  public void checkUnion3Direct() { //Union is direct, empty and with larger K than valid input
    int k1 = 128;
    int n1 = 2 * k1;
    int k2 = 256;
    int n2 = 2000;
    DoublesSketch sketchIn1 = buildAndLoadDQS(k1, n1);
    int bytes = DoublesSketch.getUpdatableStorageBytes(k2, n2);//just for size
    WritableMemory mem = WritableMemory.wrap(new byte[bytes]);
    DoublesUnion union = DoublesUnion.builder().setMaxK(k2).build(mem); //virgin 256
    union.update(sketchIn1);
    assertEquals(union.getMaxK(), k2);
    assertEquals(union.getEffectiveK(), k1);
    DoublesSketch result = union.getResult();
    assertEquals(result.getMaxValue(), n1, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    assertEquals(result.getK(), k1);
  }

  @Test
  public void checkUnion4() { //Union is direct, valid and with larger K than valid input
    int k1 = 8;
    int n1 = 2 * k1; //16
    int k2 = 4;
    int n2 = 2 * k2; //8
    int bytes = DoublesSketch.getUpdatableStorageBytes(256, 50);//just for size
    WritableMemory skMem = WritableMemory.wrap(new byte[bytes]);
    UpdateDoublesSketch sketchIn1 = DoublesSketch.builder().setK(k1).build(skMem);
    for (int i = 0; i < n1; i++) { sketchIn1.update(i + 1); }

    WritableMemory uMem = WritableMemory.wrap(new byte[bytes]);
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(uMem); //virgin 256
    //DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(); //virgin 256
    union.update(sketchIn1);
    assertEquals(union.getResult().getN(), n1);
    assertEquals(union.getMaxK(), 256);
    assertEquals(union.getEffectiveK(), k1);
    DoublesSketch result = union.getResult();
    assertEquals(result.getN(), 16);
    assertEquals(result.getMaxValue(), n1, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    assertEquals(result.getK(), k1);

    DoublesSketch sketchIn2 = buildAndLoadQS(k2, n2, 17);
    union.reset();
    union.update(sketchIn2);
    result = union.getResult();
    assertEquals(result.getMaxValue(), n2 + 17, 0.0);
    assertEquals(result.getMinValue(), 1.0 + 17, 0.0);
    println("\nFinal" + union.getResult().toString(true, true));
  }

  @Test
  public void checkUnion4Direct() { //Union is direct, valid and with larger K than valid input
    int k1 = 8;
    int n1 = 2 * k1; //16
    int k2 = 4;
    int n2 = 2 * k2; //8
    int bytes = DoublesSketch.getUpdatableStorageBytes(256, 50);//just for size
    WritableMemory skMem = WritableMemory.wrap(new byte[bytes]);
    UpdateDoublesSketch sketchIn1 = DoublesSketch.builder().setK(k1).build(skMem);
    for (int i = 0; i < n1; i++) { sketchIn1.update(i + 1); }

    WritableMemory uMem = WritableMemory.wrap(new byte[bytes]);
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(uMem); //virgin 256
    union.update(sketchIn1);
    assertEquals(union.getResult().getN(), n1);
    assertEquals(union.getMaxK(), 256);
    assertEquals(union.getEffectiveK(), k1);
    DoublesSketch result = union.getResult();
    assertEquals(result.getN(), 16);
    assertEquals(result.getMaxValue(), n1, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    assertEquals(result.getK(), k1);

    DoublesSketch sketchIn2 = buildAndLoadDQS(k2, n2, 17);
    union.reset();
    union.update(sketchIn2);
    result = union.getResult();
    assertEquals(result.getMaxValue(), n2 + 17, 0.0);
    assertEquals(result.getMinValue(), 1.0 + 17, 0.0);
    println("\nFinal" + union.getResult().toString(true, true));
  }

  @Test
  public void checkUnion4DirectCompact() {
    int k1 = 8;
    int n1 = 2 * k1; //16
    int k2 = 4;
    int n2 = 5 * k2; //8
    int bytes = DoublesSketch.getUpdatableStorageBytes(256, 50);//just for size
    WritableMemory skMem = WritableMemory.wrap(new byte[bytes]);
    UpdateDoublesSketch sketchIn0 = DoublesSketch.builder().setK(k1).build(skMem);
    for (int i = 0; i < n1; i++) { sketchIn0.update(i + 1); }
    CompactDoublesSketch sketchIn1 = sketchIn0.compact();

    WritableMemory uMem = WritableMemory.wrap(new byte[bytes]);
    DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(uMem); //virgin 256
    union.update(sketchIn1);
    assertEquals(union.getResult().getN(), n1);
    assertEquals(union.getMaxK(), 256);
    assertEquals(union.getEffectiveK(), k1);
    DoublesSketch result = union.getResult();
    assertEquals(result.getN(), 16);
    assertEquals(result.getMaxValue(), n1, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    assertEquals(result.getK(), k1);

    CompactDoublesSketch sketchIn2 = buildAndLoadDQS(k2, n2, 17).compact();
    union.reset();
    union.update(sketchIn2);
    result = union.getResult();
    assertEquals(result.getMaxValue(), n2 + 17, 0.0);
    assertEquals(result.getMinValue(), 1.0 + 17, 0.0);
    println("\nFinal" + union.getResult().toString(true, true));
  }

  @Test
  public void checkUnion5() { //Union is direct, valid and with larger K than valid input
    int k2 = 4;
    int n2 = 2 * k2; //8
    int bytes = DoublesSketch.getUpdatableStorageBytes(256, 50);//big enough
    WritableMemory skMem = WritableMemory.wrap(new byte[bytes]);
    DoublesSketch.builder().setK(256).build(skMem);

    DoublesUnion union = DoublesUnionImpl.heapifyInstance(skMem);
    assertEquals(union.getResult().getN(), 0);
    assertEquals(union.getMaxK(), 256);
    assertEquals(union.getEffectiveK(), 256);
    DoublesSketch result = union.getResult();
    assertEquals(result.getK(), 256);

    DoublesSketch sketchIn2 = buildAndLoadQS(k2, n2, 17);
    union.update(sketchIn2);
    println("\nFinal" + union.getResult().toString(true, true));
    assertEquals(union.getResult().getN(), n2);
  }

  @Test
  public void checkUnion5Direct() { //Union is direct, valid and with larger K than valid input
    int k2 = 4;
    int n2 = 2 * k2; //8
    int bytes = DoublesSketch.getUpdatableStorageBytes(256, 50);//big enough
    WritableMemory skMem = WritableMemory.wrap(new byte[bytes]);
    DoublesSketch.builder().setK(256).build(skMem);

    DoublesUnion union = DoublesUnionImpl.heapifyInstance(skMem);
    assertEquals(union.getResult().getN(), 0);
    assertEquals(union.getMaxK(), 256);
    assertEquals(union.getEffectiveK(), 256);
    DoublesSketch result = union.getResult();
    assertEquals(result.getK(), 256);

    DoublesSketch sketchIn2 = buildAndLoadDQS(k2, n2, 17);
    union.update(sketchIn2);
    println("\nFinal" + union.getResult().toString(true, true));
    assertEquals(union.getResult().getN(), n2);
  }

  @Test
  public void checkUnion6() {
    int k1 = 8;
    int n1 = 2 * k1; //16
    int k2 = 16;
    int n2 = 2 * k2; //32
    DoublesSketch sk1 = buildAndLoadQS(k1, n1, 0);
    DoublesSketch sk2 = buildAndLoadQS(k2, n2, n1);
    DoublesUnion union = DoublesUnionImpl.heapifyInstance(sk1);
    union.update(sk2);
    DoublesSketch result = union.getResult();
    assertEquals(result.getMaxValue(), n1 + n2, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    println("\nFinal" + union.getResult().toString(true, true));
  }

  @Test
  public void checkUnion6Direct() {
    int k1 = 8;
    int n1 = 2 * k1; //16
    int k2 = 16;
    int n2 = 2 * k2; //32
    DoublesSketch sk1 = buildAndLoadDQS(k1, n1, 0);
    DoublesSketch sk2 = buildAndLoadDQS(k2, n2, n1);
    DoublesUnion union = DoublesUnionImpl.heapifyInstance(sk1);
    union.update(sk2);
    DoublesSketch result = union.getResult();
    assertEquals(result.getMaxValue(), n1 + n2, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    println("\nFinal" + union.getResult().toString(true, true));
  }

  @Test
  public void checkUnion7() {
    DoublesUnion union = DoublesUnionImpl.heapInstance(16);
    DoublesSketch skEst = buildAndLoadQS(32, 64); //other is bigger, est
    union.update(skEst);
    DoublesSketch result = union.getResult();
    assertEquals(result.getMaxValue(), 64, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
//    println(skEst.toString(true, true));
//    println(union.toString(true, true));
  }

  @Test
  public void checkUnionQuantiles() {
    final int k = 128;
    final int n1 = k * 13;
    final int n2 = k * 8 + k / 2;
    final int n = n1 + n2;
    final double errorTolerance = 0.0175 * n; // assuming k = 128
    final UpdateDoublesSketch sketch1 = buildAndLoadQS(k, n1);
    final CompactDoublesSketch sketch2 = buildAndLoadQS(k, n2, n1).compact();
    final DoublesUnion union = DoublesUnion.builder().setMaxK(256).build(); //virgin 256
    union.update(sketch2);
    union.update(sketch1);
    final Memory mem = Memory.wrap(union.getResult().toByteArray(true));
    final DoublesSketch result = DoublesSketch.wrap(mem);
    assertEquals(result.getN(), n1 + n2);
    assertEquals(result.getK(), k);

    for (double fraction = 0.05; fraction < 1.0; fraction += 0.05) {
      assertEquals(result.getQuantile(fraction), fraction * n, errorTolerance);
    }
  }

  @Test
  public void checkUnion7Direct() {
    DoublesUnion union = DoublesUnionImpl.heapInstance(16);
    DoublesSketch skEst = buildAndLoadDQS(32, 64); //other is bigger, est
    union.update(skEst);
    DoublesSketch result = union.getResult();
    assertEquals(result.getMaxValue(), 64, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
//    println(skEst.toString(true, true));
//    println(union.toString(true, true));
  }

  @Test
  public void checkUpdateMemory() {
    DoublesSketch qs1 = buildAndLoadQS(256, 1000);
    int bytes = qs1.getCompactStorageBytes();
    WritableMemory dstMem = WritableMemory.wrap(new byte[bytes]);
    qs1.putMemory(dstMem);
    Memory srcMem = dstMem;

    DoublesUnion union = DoublesUnion.builder().build(); //virgin
    union.update(srcMem);
    for (int i=1000; i<2000; i++) union.update(i);
    DoublesSketch qs2 = union.getResult();
    assertEquals(qs2.getMaxValue(), 1999, 0.0);
    String s = union.toString();
    println(s); //enable printing to see
    union.reset(); //sets to null
  }

  @Test
  public void checkUpdateMemoryDirect() {
    DoublesSketch qs1 = buildAndLoadDQS(256, 1000);
    int bytes = qs1.getCompactStorageBytes();
    WritableMemory dstMem = WritableMemory.wrap(new byte[bytes]);
    qs1.putMemory(dstMem);
    Memory srcMem = dstMem;

    DoublesUnion union = DoublesUnion.builder().build(); //virgin
    union.update(srcMem);
    for (int i=1000; i<2000; i++) union.update(i);
    DoublesSketch qs2 = union.getResult();
    assertEquals(qs2.getMaxValue(), 1999, 0.0);
    String s = union.toString();
    println(s); //enable printing to see
    union.reset(); //sets to null
  }

  @Test
  public void checkUnionUpdateLogic() {
    HeapUpdateDoublesSketch qs1 = null;
    HeapUpdateDoublesSketch qs2 = (HeapUpdateDoublesSketch)buildAndLoadQS(256, 0);
    DoublesUnionImpl.updateLogic(256, qs1, qs2); //null, empty
    DoublesUnionImpl.updateLogic(256, qs2, qs1); //empty, null
    qs2.update(1); //no longer empty
    DoublesSketch result = DoublesUnionImpl.updateLogic(256, qs2, qs1); //valid, null
    assertEquals(result.getMaxValue(), result.getMinValue(), 0.0);
  }

  @Test
  public void checkUnionUpdateLogicDirect() {
    HeapUpdateDoublesSketch qs1 = null;
    DirectUpdateDoublesSketch qs2 = (DirectUpdateDoublesSketch)buildAndLoadDQS(256, 0);
    DoublesUnionImpl.updateLogic(256, qs1, qs2); //null, empty
    DoublesUnionImpl.updateLogic(256, qs2, qs1); //empty, null
    qs2.update(1); //no longer empty
    DoublesSketch result = DoublesUnionImpl.updateLogic(256, qs2, qs1); //valid, null
    assertEquals(result.getMaxValue(), result.getMinValue(), 0.0);
  }

  @Test
  public void checkUnionUpdateLogicDirectDownsampled() {
    final DirectUpdateDoublesSketch qs1 = (DirectUpdateDoublesSketch) buildAndLoadDQS(256, 1000);
    final DirectUpdateDoublesSketch qs2 = (DirectUpdateDoublesSketch) buildAndLoadDQS(128, 2000);
    final DoublesSketch result = DoublesUnionImpl.updateLogic(128, qs1, qs2);
    assertEquals(result.getMaxValue(), 2000.0, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
    assertEquals(result.getN(), 3000);
    assertEquals(result.getK(), 128);
  }

  @Test
  public void checkUnionUpdateLogic2() {
    DoublesSketch qs1 = DoublesSketch.builder().build();
    DoublesSketch qs2 = DoublesSketch.builder().build();
    DoublesUnion union = DoublesUnion.builder().build();
    union.update(qs1);
    union.update(qs2); //case 5
    qs1 = buildAndLoadQS(128, 1000);
    union.update(qs1);
    union.update(qs2); //case 9
    DoublesSketch result = union.getResult();
    //println(union.toString(true, true));
    assertEquals(result.getMaxValue(), 1000.0, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);

  }

  @Test
  public void checkUnionUpdateLogic2Direct() {
    DoublesSketch qs1 = DoublesSketch.builder().build();
    DoublesSketch qs2 = DoublesSketch.builder().build();
    DoublesUnion union = DoublesUnion.builder().build();
    union.update(qs1);
    union.update(qs2); //case 5
    qs1 = buildAndLoadDQS(128, 1000);
    union.update(qs1);
    union.update(qs2); //case 9
    DoublesSketch result = union.getResult();
    //println(union.toString(true, true));
    assertEquals(result.getMaxValue(), 1000.0, 0.0);
    assertEquals(result.getMinValue(), 1.0, 0.0);
  }

  @Test
  public void checkResultAndReset() {
    DoublesSketch qs1 = buildAndLoadQS(256, 0);
    DoublesUnion union = DoublesUnionBuilder.heapify(qs1);
    DoublesSketch qs2 = union.getResultAndReset();
    assertEquals(qs2.getK(), 256);
  }

  @Test
  public void checkResultAndResetDirect() {
    DoublesSketch qs1 = buildAndLoadDQS(256, 0);
    DoublesUnion union = DoublesUnionBuilder.heapify(qs1);
    DoublesSketch qs2 = union.getResultAndReset();
    assertEquals(qs2.getK(), 256);
  }

  @Test
  public void checkResultViaMemory() {
    // empty gadget
    final DoublesUnion union = DoublesUnion.builder().build();

    // memory too small
    WritableMemory mem = WritableMemory.allocate(1);
    try {
      union.getResult(mem);
      fail();
    } catch (final SketchesArgumentException e) {
      // expected
    }

    // sufficient memory
    mem = WritableMemory.allocate(8);
    DoublesSketch result = union.getResult(mem);
    assertTrue(result.isEmpty());

    final int k = 128;
    final int n = 1392;
    mem = WritableMemory.allocate(DoublesSketch.getUpdatableStorageBytes(k, n));
    final DoublesSketch qs = buildAndLoadQS(k, n);
    union.update(qs);
    result = union.getResult(mem);
    DoublesSketchTest.testSketchEquality(result, qs);
  }

  @Test
  public void updateWithDoubleValueOnly() {
    DoublesUnion union = DoublesUnion.builder().build();
    union.update(123.456);
    DoublesSketch qs = union.getResultAndReset();
    assertEquals(qs.getN(), 1);
  }

  @Test
  public void checkEmptyUnion() {
    DoublesUnionImpl union = DoublesUnionImpl.heapInstance(128);
    DoublesSketch sk = union.getResult();
    assertNotNull(sk);
    String s = union.toString();
    assertNotNull(s);
  }

  @Test
  public void checkUnionNulls() {
    DoublesUnion union = DoublesUnionImpl.heapInstance(128);
    DoublesSketch sk1 = union.getResultAndReset();
    DoublesSketch sk2 = union.getResultAndReset();
    assertNull(sk1);
    assertNull(sk2);
    union.update(sk2);
    DoublesSketch sk3 = union.getResultAndReset();
    assertNull(sk3);
  }

  @Test
  public void differentLargerK() {
    DoublesUnion union = DoublesUnion.builder().setMaxK(128).build();
    UpdateDoublesSketch sketch1 = buildAndLoadQS(256, 0);
    union.update(sketch1);
    Assert.assertEquals(union.getResult().getK(), 128);
    sketch1.update(1.0);
    union.update(sketch1);
    Assert.assertEquals(union.getResult().getK(), 128);
  }

  @Test
  public void differentLargerKDirect() {
    DoublesUnion union = DoublesUnion.builder().setMaxK(128).build();
    UpdateDoublesSketch sketch1 = buildAndLoadDQS(256, 0);
    union.update(sketch1);
    Assert.assertEquals(union.getResult().getK(), 128);
    sketch1.update(1.0);
    union.update(sketch1);
    Assert.assertEquals(union.getResult().getK(), 128);
  }

  @Test
  public void differentEmptySmallerK() {
    int k128 = 128;
    int k64 = 64;
    DoublesUnion union = DoublesUnion.builder().setMaxK(k128).build();
    assertTrue(union.isEmpty()); //gadget is null
    Assert.assertFalse(union.isDirect());

//    byte[] unionByteArr = union.toByteArray();
//    Assert.assertEquals(unionByteArr.length, 32 + 32); //empty

    UpdateDoublesSketch sketch1 = buildAndLoadQS(k64, 0); //build smaller empty sketch
    union.update(sketch1);
    assertTrue(union.isEmpty()); //gadget is valid
    Assert.assertFalse(union.isDirect());

//    unionByteArr = union.toByteArray();
//    int udBytes = DoublesSketch.getUpdatableStorageBytes(k64, 0);
//    Assert.assertEquals(unionByteArr.length, udBytes); //empty

    Assert.assertEquals(union.getResult().getK(), 128);
    sketch1.update(1.0);
    union.update(sketch1);
    Assert.assertEquals(union.getResult().getK(), 128);
  }

  @Test
  public void differentEmptySmallerKDirect() {
    int k128 = 128;
    int k64 = 64;
    DoublesUnion union = DoublesUnion.builder().setMaxK(k128).build();
    assertTrue(union.isEmpty()); //gadget is null
    Assert.assertFalse(union.isDirect());

//    byte[] unionByteArr = union.toByteArray();
//    Assert.assertEquals(unionByteArr.length, 32 + 32); //empty

    UpdateDoublesSketch sketch1 = buildAndLoadDQS(k64, 0); //build smaller empty sketch
    union.update(sketch1);
    assertTrue(union.isEmpty()); //gadget is valid
    Assert.assertFalse(union.isDirect());

//    unionByteArr = union.toByteArray();
//    int udBytes = DoublesSketch.getUpdatableStorageBytes(k64, 0);
//    Assert.assertEquals(unionByteArr.length, udBytes); //empty

    Assert.assertEquals(union.getResult().getK(), 128);
    sketch1.update(1.0);
    union.update(sketch1);
    Assert.assertEquals(union.getResult().getK(), 128);
  }

  @SuppressWarnings("deprecation")
  @Test
  public void checkDirectInstance() {
    int k = 128;
    int n = 1000;
    DoublesUnionBuilder bldr = DoublesUnion.builder();
    bldr.setK(k); // check the deprecated version
    Assert.assertEquals(bldr.getMaxK(), k);
    int bytes = DoublesSketch.getUpdatableStorageBytes(k, n);
    byte[] byteArr = new byte[bytes];
    WritableMemory mem = WritableMemory.wrap(byteArr);
    DoublesUnion union = bldr.build(mem);
    assertTrue(union.isEmpty());
    assertTrue(union.isDirect());
    for (int i = 1; i <= n; i++) {
      union.update(i);
    }
    Assert.assertFalse(union.isEmpty());
    DoublesSketch res = union.getResult();
    double median = res.getQuantile(.5);
    Assert.assertEquals(median, 500, 10);
    println(union.toString());
  }

  @Test
  public void checkWrapInstance() {
    int k = 128;
    int n = 1000;
    UpdateDoublesSketch sketch = DoublesSketch.builder().setK(k).build();
    for (int i = 1; i <= n; i++) {
      sketch.update(i);
    }
    double skMedian = sketch.getQuantile(.5);
    Assert.assertEquals(skMedian, 500, 10);

    byte[] byteArr = sketch.toByteArray(false);
    WritableMemory mem = WritableMemory.wrap(byteArr);
    DoublesUnion union = DoublesUnionBuilder.wrap(mem);
    Assert.assertFalse(union.isEmpty());
    assertTrue(union.isDirect());
    DoublesSketch sketch2 = union.getResult();
    double uMedian = sketch2.getQuantile(0.5);
    Assert.assertEquals(skMedian, uMedian, 0.0);
  }

  @Test
  public void printlnTest() {
    println("PRINTING: "+this.getClass().getName());
  }

  /**
   * @param s value to print
   */
  static void println(String s) {
    //System.out.println(s); //disable here
  }
}
