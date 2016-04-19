package org.apache.cassandra.hadoop.cql3;

import com.google.common.collect.Lists;
import org.apache.cassandra.db.marshal.AbstractType;
import org.apache.cassandra.db.marshal.CompositeType;
import org.apache.cassandra.db.marshal.UTF8Type;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BatchConfigHelper {
    private static final String INPUT_KEYRANGES_CONFIG = "cassandra.input.keyRanges";

    public static void setInputKeyRanges(Configuration conf, Collection<KeyRange> keyRanges)
    {
        assert keyRanges != null;

        String keyRangesString = "";
        for (KeyRange keyRange: keyRanges) {
            keyRangesString += (keyRange.start_token + "," + keyRange.end_token + " ");
        }
        conf.set(INPUT_KEYRANGES_CONFIG, Base64.encodeBase64String(keyRangesString.getBytes(StandardCharsets.UTF_8)));
    }


    public static ArrayList<KeyRange> getInputKeyRanges(Configuration conf)
    {
        String str = conf.get(INPUT_KEYRANGES_CONFIG);
        String[] decoded = new String(Base64.decodeBase64(str), StandardCharsets.UTF_8).split(" ");
        ArrayList<KeyRange> keyRanges = new ArrayList<KeyRange>();
        for(String tstring: decoded) {
            String[] tuple = tstring.split(",");
            KeyRange keyRange = new KeyRange();
            keyRange.setStart_token(tuple[0]);
            keyRange.setEnd_token(tuple[1]);
            keyRanges.add(keyRange);
        }

        return keyRanges;
    }

    public static ByteBuffer getCompositeColumnName(String... parts) {
        return getCompositeColumnName(Lists.newArrayList(parts));
    }

    public static ByteBuffer getCompositeColumnName(List<String> parts) {
        List<AbstractType<?>> keyTypes = Lists.newArrayListWithExpectedSize(parts.size());

        for (String ignored : parts) {
            keyTypes.add(UTF8Type.instance);
        }

        CompositeType compositeKey = CompositeType.getInstance(keyTypes);
        CompositeType.Builder builder = new CompositeType.Builder(compositeKey);

        for (String part : parts) {
            builder.add(ByteBufferUtil.bytes(part));
        }

        return builder.build();
    }
}
