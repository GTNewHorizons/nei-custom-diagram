package com.github.dcysteine.neicustomdiagram.lib.io;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.UUID;

import javax.annotation.Nullable;

import com.github.dcysteine.neicustomdiagram.util.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.netty.buffer.ByteBuf;

public class DataOut {

    @FunctionalInterface
    public interface Serializer<T> {

        void write(DataOut data, T object);
    }

    public static final Serializer<String> STRING = DataOut::writeString;
    public static final Serializer<Integer> INT = DataOut::writeInt;
    public static final Serializer<Boolean> BOOLEAN = DataOut::writeBoolean;

    public static final Serializer<UUID> UUID = DataOut::writeUUID;
    public static final Serializer<JsonElement> JSON = DataOut::writeJson;

    private final ByteBuf byteBuf;

    public DataOut(ByteBuf io) {
        byteBuf = io;
    }

    public void writeBoolean(boolean value) {
        byteBuf.writeBoolean(value);
    }

    public void writeByte(int value) {
        byteBuf.writeByte(value);
    }

    public void writeBytes(byte[] bytes, int off, int len) {
        byteBuf.writeBytes(bytes, off, len);
    }

    public void writeBytes(byte[] bytes) {
        writeBytes(bytes, 0, bytes.length);
    }

    public void writeShort(int value) {
        byteBuf.writeShort(value);
    }

    public void writeInt(int value) {
        byteBuf.writeInt(value);
    }

    public void writeLong(long value) {
        byteBuf.writeLong(value);
    }

    public void writeFloat(float value) {
        byteBuf.writeFloat(value);
    }

    public void writeDouble(double value) {
        byteBuf.writeDouble(value);
    }

    public void writeUUID(UUID id) {
        writeLong(id.getMostSignificantBits());
        writeLong(id.getLeastSignificantBits());
    }

    public void writeString(String string) {
        if (string.isEmpty()) {
            writeVarInt(0);
            return;
        }

        byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        writeBytes(bytes);
    }

    public <T> void writeCollection(Collection<T> collection, Serializer<T> serializer) {
        int size = collection.size();

        if (size == 0) {
            writeVarInt(0);
            return;
        }

        if (collection instanceof Set) {
            writeVarInt(-size);
        } else {
            writeVarInt(size);
        }

        for (T object : collection) {
            serializer.write(this, object);
        }
    }

    public <K, V> void writeMap(Map<K, V> map, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        int size = map.size();

        if (size == 0) {
            writeVarInt(0);
            return;
        }

        if (map instanceof LinkedHashMap || map instanceof SortedMap) {
            writeVarInt(-size);
        } else {
            writeVarInt(size);
        }

        for (Map.Entry<K, V> entry : map.entrySet()) {
            keySerializer.write(this, entry.getKey());
            valueSerializer.write(this, entry.getValue());
        }
    }

    public int writeJson(@Nullable JsonElement element) {
        if (JsonUtils.isNull(element)) {
            writeByte(0);
            return 0;
        } else if (element.isJsonObject()) {
            writeByte(1);

            Set<Map.Entry<String, JsonElement>> set = element.getAsJsonObject().entrySet();
            Map<String, JsonElement> map = new LinkedHashMap<>(set.size());

            for (Map.Entry<String, JsonElement> entry : set) {
                map.put(entry.getKey(), entry.getValue());
            }

            writeMap(map, STRING, JSON);
            return 1;
        } else if (element.isJsonArray()) {
            writeByte(2);

            JsonArray json = element.getAsJsonArray();
            Collection<JsonElement> collection = new ArrayList<>(json.size());

            for (JsonElement json1 : json) {
                collection.add(json1);
            }

            writeCollection(collection, JSON);
            return 2;
        }

        JsonPrimitive primitive = element.getAsJsonPrimitive();

        if (primitive.isBoolean()) {
            if (primitive.getAsBoolean()) {
                writeByte(5);
                return 5;
            } else {
                writeByte(6);
                return 6;
            }
        } else if (primitive.isNumber()) {
            if (primitive == JsonUtils.JSON_ZERO) {
                writeByte(4);
                return 4;
            }

            Number number = primitive.getAsNumber();

            if (number.doubleValue() == 0D) {
                writeByte(4);
                return 4;
            }

            Class<? extends Number> n = number.getClass();

            if (n == Float.class) {
                writeByte(8);
                writeFloat(primitive.getAsFloat());
                return 8;
            } else if (n == Double.class) {
                writeByte(9);
                writeDouble(primitive.getAsDouble());
                return 9;
            } else {
                writeByte(7);
                writeVarLong(primitive.getAsLong());
                return 7;
            }
        }

        String string = primitive.getAsString();

        if (string.isEmpty()) {
            writeByte(10);
            return 10;
        }

        writeByte(3);
        writeString(string);
        return 3;
    }

    public void writeVarInt(int value) {
        if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            writeByte(123);
            writeInt(value);
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            writeByte(122);
            writeShort(value);
        } else if (value >= 121 && value <= 123) {
            writeByte(121);
            writeByte(value);
        } else {
            writeByte(value);
        }
    }

    public void writeVarLong(long value) {
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            writeByte(124);
            writeLong(value);
        } else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            writeByte(123);
            writeInt((int) value);
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            writeByte(122);
            writeShort((int) value);
        } else if (value >= 121 && value <= 124) {
            writeByte(121);
            writeByte((int) value);
        } else {
            writeByte((int) value);
        }
    }
}
