package com.github.dcysteine.neicustomdiagram.lib.io;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.annotation.Nullable;

import com.github.dcysteine.neicustomdiagram.util.CommonUtils;
import com.github.dcysteine.neicustomdiagram.util.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;

public class DataIn {

    @FunctionalInterface
    public interface Deserializer<T> {

        T read(DataIn data);
    }

    public static final Deserializer<String> STRING = DataIn::readString;
    public static final Deserializer<Integer> INT = DataIn::readInt;
    public static final Deserializer<Boolean> BOOLEAN = DataIn::readBoolean;

    public static final Deserializer<UUID> UUID = DataIn::readUUID;
    public static final Deserializer<JsonElement> JSON = DataIn::readJson;

    private final ByteBuf byteBuf;

    public DataIn(ByteBuf io) {
        byteBuf = io;
    }

    public int getPosition() {
        return byteBuf.readerIndex();
    }

    public boolean isReadable() {
        return byteBuf.isReadable();
    }

    public boolean readBoolean() {
        return byteBuf.readBoolean();
    }

    public byte readByte() {
        return byteBuf.readByte();
    }

    public void readBytes(byte[] bytes, int off, int len) {
        byteBuf.readBytes(bytes, off, len);
    }

    public void readBytes(byte[] bytes) {
        readBytes(bytes, 0, bytes.length);
    }

    public short readUnsignedByte() {
        return byteBuf.readUnsignedByte();
    }

    public short readShort() {
        return byteBuf.readShort();
    }

    public int readUnsignedShort() {
        return byteBuf.readUnsignedShort();
    }

    public int readInt() {
        return byteBuf.readInt();
    }

    public long readLong() {
        return byteBuf.readLong();
    }

    public float readFloat() {
        return byteBuf.readFloat();
    }

    public double readDouble() {
        return byteBuf.readDouble();
    }

    public String readString() {
        int s = readVarInt();

        if (s == 0) {
            return "";
        }

        byte[] bytes = new byte[s];
        readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public <T> Collection<T> readCollection(@Nullable Collection<T> collection, Deserializer<T> deserializer) {
        if (collection != null) {
            collection.clear();
        }

        int num = readVarInt();

        if (num == 0) {
            return collection == null ? Collections.emptyList() : collection;
        }

        int size = Math.abs(num);

        if (collection == null) {
            boolean list = num > 0;

            if (size == 1) {
                return list ? Collections.singletonList(deserializer.read(this))
                        : Collections.singleton(deserializer.read(this));
            }

            collection = list ? new ArrayList<>(size) : new HashSet<>(size);
        }

        while (--size >= 0) {
            collection.add(deserializer.read(this));
        }

        return collection;
    }

    public <T> Collection<T> readCollection(Deserializer<T> deserializer) {
        return readCollection(null, deserializer);
    }

    public <K, V> Map<K, V> readMap(@Nullable Map<K, V> map, Deserializer<K> keyDeserializer,
            Deserializer<V> valueDeserializer) {
        if (map != null) {
            map.clear();
        }

        int num = readVarInt();

        if (num == 0) {
            return map == null ? Collections.emptyMap() : map;
        }

        int size = Math.abs(num);

        if (map == null) {
            boolean linked = num < 0;

            if (keyDeserializer == INT) {
                map = CommonUtils.cast(linked ? new HashMap<>(size) : new Int2ObjectOpenHashMap<V>(size));
            } else {
                map = linked ? new LinkedHashMap<>(size) : new HashMap<>(size);
            }
        }

        while (--size >= 0) {
            K key = keyDeserializer.read(this);
            V value = valueDeserializer.read(this);
            map.put(key, value);
        }

        return map;
    }

    public <K, V> Map<K, V> readMap(Deserializer<K> keyDeserializer, Deserializer<V> valueDeserializer) {
        return readMap(null, keyDeserializer, valueDeserializer);
    }

    public UUID readUUID() {
        long msb = readLong();
        long lsb = readLong();
        return new UUID(msb, lsb);
    }

    public JsonElement readJson() {
        switch (readUnsignedByte()) {
            case 0:
                return JsonNull.INSTANCE;
            case 1: {
                JsonObject json = new JsonObject();

                for (Map.Entry<String, JsonElement> entry : readMap(STRING, JSON).entrySet()) {
                    json.add(entry.getKey(), entry.getValue());
                }

                return json;
            }
            case 2: {
                JsonArray json = new JsonArray();

                for (JsonElement json1 : readCollection(JSON)) {
                    json.add(json1);
                }

                return json;
            }
            case 3: {
                String s = readString();
                return s.isEmpty() ? JsonUtils.JSON_EMPTY_STRING : new JsonPrimitive(s);
            }
            case 4:
                return JsonUtils.JSON_ZERO;
            case 5:
                return JsonUtils.JSON_TRUE;
            case 6:
                return JsonUtils.JSON_FALSE;
            case 7:
                return new JsonPrimitive(readVarLong());
            case 8:
                return new JsonPrimitive(readFloat());
            case 9:
                return new JsonPrimitive(readDouble());
            case 10:
                return JsonUtils.JSON_EMPTY_STRING;
        }

        return JsonNull.INSTANCE;
    }

    public IntArrayList readIntList() {
        int size = readVarInt();

        if (size == 0) {
            return new IntArrayList();
        }

        IntArrayList list = new IntArrayList();

        for (int i = 0; i < size; i++) {
            list.add(readInt());
        }

        return list;
    }

    public int readVarInt() {
        int b = readByte();

        switch (b) {
            case 121:
                return readByte();
            case 122:
                return readShort();
            case 123:
                return readInt();
            default:
                return b;
        }
    }

    public long readVarLong() {
        int b = readByte();

        switch (b) {
            case 121:
                return readByte();
            case 122:
                return readShort();
            case 123:
                return readInt();
            case 124:
                return readLong();
            default:
                return b;
        }
    }
}
