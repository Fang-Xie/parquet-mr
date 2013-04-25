package parquet.column.values.bitpacking;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import parquet.column.values.ValuesReader;
import parquet.column.values.bitpacking.BitPacking.BitPackingWriter;

/**
 * Improvable micro benchmark for bitpacking
 * run with: -verbose:gc -Xmx2g -Xms2g
 * @author Julien Le Dem
 *
 */
public class BitPackingPerfTest {

  public static void main(String[] args) throws IOException {
    int COUNT = 800000;
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    BitPackingWriter w = BitPacking.getBitPackingWriter(1, baos);
    long t0 = System.currentTimeMillis();
    for (int i = 0 ; i < COUNT; ++i) {
      w.write(i % 2);
    }
    w.finish();
    long t1 = System.currentTimeMillis();
    System.out.println("written in " + (t1 - t0) + "ms");
    System.out.println();
    byte[] bytes = baos.toByteArray();
    System.out.println(bytes.length);
    int[] result = new int[COUNT];
    for (int l = 0; l < 5; l++) {
      long s = manual(bytes, result);
      long b = generated(bytes, result);
      float ratio = (float)b/s;
      System.out.println("                                             " + ratio + (ratio < 1 ? " < 1 => GOOD" : " >= 1 => BAD"));
    }
  }

  private static void verify(int[] result) {
    int error = 0;
    for (int i = 0 ; i < result.length; ++i) {
      if (result[i] != i % 2) {
        error ++;
      }
    }
    if (error != 0) {
      throw new RuntimeException("errors: " + error + " / " + result.length);
    }
  }

  private static long manual(byte[] bytes, int[] result)
      throws IOException {
    return readNTimes(bytes, result, new BitPackingValuesReader(1));
  }

  private static long generated(byte[] bytes, int[] result)
      throws IOException {
    return readNTimes(bytes, result, new ByteBitPackingValuesReader(1));
  }

  private static long readNTimes(byte[] bytes, int[] result, ValuesReader r)
      throws IOException {
    System.out.println();
    long t = 0;
    int N = 10;
    System.gc();
    System.out.print("                                             " + r.getClass().getSimpleName());
    System.out.print(" no gc <");
    for (int k = 0; k < N; k++) {
      long t2 = System.nanoTime();
      r.initFromPage(result.length, bytes, 0);
      for (int i = 0; i < result.length; i++) {
        result[i] = r.readInteger();
      }
      long t3 = System.nanoTime();
      t += t3 - t2;
    }
    System.out.println("> read in " + t/1000 + "µs " + (N * result.length / (t / 1000)) + " values per µs");
    verify(result);
    return t;
  }

}

