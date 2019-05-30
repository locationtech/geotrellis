package geotrellis.layers.cassandra;

import com.datastax.driver.core.DataType;
import com.datastax.driver.core.exceptions.InvalidTypeException;
import com.datastax.driver.core.ProtocolVersion;
import com.datastax.driver.core.TypeCodec;
import com.datastax.driver.core.TypeCodec.PrimitiveLongCodec;

import java.nio.ByteBuffer;

import java.math.BigInteger;
import java.util.Arrays;


/*
 * This coded is used for backward compatibilty only.
 *
 * @author James McClain
 */
public class BigIntegerIffBigint extends TypeCodec<BigInteger> {

    public static final BigIntegerIffBigint instance = new BigIntegerIffBigint();
    private static final PrimitiveLongCodec _instance = TypeCodec.bigint();

    private BigIntegerIffBigint() {
        super(DataType.bigint(), BigInteger.class);
    }

    @Override
    public ByteBuffer serialize(BigInteger value, ProtocolVersion protocolVersion) {
	return _instance.serialize(value.longValue(), protocolVersion);
    }

    @Override
    public BigInteger deserialize(ByteBuffer bytes, ProtocolVersion protocolVersion) {
	return BigInteger.valueOf(_instance.deserialize(bytes, protocolVersion));
    }

    @Override
    public String format(BigInteger value) {
	return _instance.format(value.longValue());
    }

    @Override
    public BigInteger parse(String value) {
	return BigInteger.valueOf(_instance.parse(value));
    }

}
