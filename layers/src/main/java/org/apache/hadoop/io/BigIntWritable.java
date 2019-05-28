package org.apache.hadoop.io;

import java.math.BigInteger;
import java.util.Arrays;
import org.apache.hadoop.io.*;

/**
  * @author James McClain
  */
public class BigIntWritable
    extends BytesWritable
    implements WritableComparable<BinaryComparable> {

    public BigIntWritable() {
	super();
    }

    public BigIntWritable(byte[] bytes) {
	super(bytes);
    }

    @Override
    public int compareTo(BinaryComparable that) {
	BigInteger  left = new BigInteger(Arrays.copyOf(this.getBytes(), this.getLength()));
	BigInteger right = new BigInteger(Arrays.copyOf(that.getBytes(), that.getLength()));
	return left.compareTo(right);
    }

  public static class Comparator extends WritableComparator {
    public Comparator() {
      super(BigIntWritable.class);
    }

    @Override
    public int compare(WritableComparable a, WritableComparable b) {
	return a.compareTo(b);
    }
  }

  static {
    WritableComparator.define(BigIntWritable.class, new Comparator());
  }

}
