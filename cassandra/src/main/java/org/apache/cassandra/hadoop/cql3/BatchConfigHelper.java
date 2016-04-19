package org.apache.cassandra.hadoop.cql3;

import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.utils.Hex;
import org.apache.commons.codec.binary.Base64;
import org.apache.hadoop.conf.Configuration;
import org.apache.thrift.TBase;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;

public class BatchConfigHelper {
    private static final String INPUT_KEYRANGES_CONFIG = "cassandra.input.keyRanges";

    public static void setInputKeyRanges(Configuration conf, Collection<KeyRange> keyRanges)
    {
        assert keyRanges != null;

        String keyRangesString = "";
        for (KeyRange keyRange: keyRanges) {
            keyRangesString += ((thriftToString(keyRange)) + " ");
        }

        conf.set(INPUT_KEYRANGES_CONFIG, Base64.encodeBase64String(keyRangesString.getBytes(StandardCharsets.UTF_8)));
    }


    public static ArrayList<KeyRange> getInputKeyRanges(Configuration conf)
    {
        String str = conf.get(INPUT_KEYRANGES_CONFIG);
        String[] decoded = new String(Base64.decodeBase64(str), StandardCharsets.UTF_8).split(" ");
        ArrayList<KeyRange> keyRanges = new ArrayList<>();
        for(String tstring: decoded) {
            keyRanges.add(keyRangeFromString(tstring));
        }

        return keyRanges;
    }

    private static String thriftToString(TBase object)
    {
        assert object != null;
        // this is so awful it's kind of cool!
        TSerializer serializer = new TSerializer(new TBinaryProtocol.Factory());
        try
        {
            return Hex.bytesToHex(serializer.serialize(object));
        }
        catch (TException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static KeyRange keyRangeFromString(String st)
    {
        assert st != null;
        TDeserializer deserializer = new TDeserializer(new TBinaryProtocol.Factory());
        KeyRange keyRange = new KeyRange();
        try
        {
            deserializer.deserialize(keyRange, Hex.hexToBytes(st));
        }
        catch (TException e)
        {
            throw new RuntimeException(e);
        }
        return keyRange;
    }
}
