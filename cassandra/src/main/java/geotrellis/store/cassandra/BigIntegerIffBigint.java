package geotrellis.store.cassandra;

import com.datastax.oss.driver.api.core.ProtocolVersion;
import com.datastax.oss.driver.api.core.type.DataType;
import com.datastax.oss.driver.api.core.type.DataTypes;
import com.datastax.oss.driver.api.core.type.codec.PrimitiveLongCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodec;
import com.datastax.oss.driver.api.core.type.codec.TypeCodecs;
import com.datastax.oss.driver.api.core.type.reflect.GenericType;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.Nullable;

import java.nio.ByteBuffer;
import java.math.BigInteger;

/*
 * This coded is used for backward compatibilty only.
 *
 * @author James McClain
 */
public class BigIntegerIffBigint implements TypeCodec<BigInteger> {

    public static final BigIntegerIffBigint instance = new BigIntegerIffBigint();
    private static final PrimitiveLongCodec _instance = TypeCodecs.BIGINT;

    @NonNull
    @Override
    public GenericType<BigInteger> getJavaType() {
        return GenericType.BIG_INTEGER;
    }

    @NonNull
    @Override
    public DataType getCqlType() {
        return DataTypes.BIGINT;
    }

    @Nullable
    @Override
    public ByteBuffer encode(@Nullable BigInteger value, @NonNull ProtocolVersion protocolVersion) {
        return _instance.encode(value.longValue(), protocolVersion);
    }

    @Nullable
    @Override
    public BigInteger decode(@Nullable ByteBuffer bytes, @NonNull ProtocolVersion protocolVersion) {
        return BigInteger.valueOf(_instance.decode(bytes, protocolVersion));
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
