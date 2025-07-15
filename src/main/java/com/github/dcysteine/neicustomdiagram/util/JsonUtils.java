package com.github.dcysteine.neicustomdiagram.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.event.ClickEvent;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.world.storage.ThreadedFileIOBase;
import net.minecraftforge.common.util.Constants;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class JsonUtils {

    public static final Gson gson = new Gson();

    public static final JsonPrimitive JSON_TRUE = new JsonPrimitive(true);
    public static final JsonPrimitive JSON_FALSE = new JsonPrimitive(false);
    public static final JsonPrimitive JSON_EMPTY_STRING = new JsonPrimitive("");
    public static final JsonPrimitive JSON_ZERO = new JsonPrimitive(0);

    public static boolean isNull(@Nullable JsonElement element) {
        return element == null || element == JsonNull.INSTANCE || element.isJsonNull();
    }

    public static JsonElement nonnull(@Nullable JsonElement json) {
        return isNull(json) ? JsonNull.INSTANCE : json;
    }

    public static JsonElement parse(@Nullable Reader reader) throws Exception {
        if (reader == null) {
            return JsonNull.INSTANCE;
        }

        JsonReader jsonReader = new JsonReader(reader);
        JsonElement element;
        boolean lenient = jsonReader.isLenient();
        jsonReader.setLenient(true);
        element = Streams.parse(jsonReader);

        if (!element.isJsonNull() && jsonReader.peek() != JsonToken.END_DOCUMENT) {
            throw new JsonSyntaxException("Did not consume the entire document.");
        }

        return element;
    }

    public static void toJson(Writer writer, @Nullable JsonElement element, boolean prettyPrinting) {
        if (isNull(element)) {
            try {
                writer.write("null");
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return;
        }

        JsonWriter jsonWriter = new JsonWriter(writer);
        jsonWriter.setLenient(true);
        jsonWriter.setHtmlSafe(false);
        jsonWriter.setSerializeNulls(true);

        if (prettyPrinting) {
            jsonWriter.setIndent("\t");
        }

        try {
            Streams.write(element, jsonWriter);
        } catch (Exception ex) {
            throw new JsonIOException(ex);
        }
    }

    public static String toJson(@Nullable JsonElement element, boolean prettyPrinting) {
        StringWriter writer = new StringWriter();
        toJson(writer, element, prettyPrinting);
        return writer.toString();
    }

    public static void toJson(File file, @Nullable JsonElement element, boolean prettyPrinting) {
        try (OutputStreamWriter output = new OutputStreamWriter(
                new FileOutputStream(FileUtils.newFile(file)),
                StandardCharsets.UTF_8); BufferedWriter writer = new BufferedWriter(output)) {
            toJson(writer, element, prettyPrinting);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String toJson(@Nullable JsonElement element) {
        return toJson(element, false);
    }

    public static void toJson(File file, @Nullable JsonElement element) {
        toJson(file, element, true);
    }

    public static void toJsonSafe(final File file, final @Nullable JsonElement element) {
        ThreadedFileIOBase.threadedIOInstance.queueIO(() -> {
            toJson(file, element);
            return false;
        });
    }

    public static JsonArray toArray(JsonElement element) {
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }

        JsonArray array = new JsonArray();

        if (!element.isJsonNull()) {
            array.add(element);
        }

        return array;
    }

    public static JsonElement serializeClickEvent(@Nullable ClickEvent event) {
        if (event == null) {
            return JsonNull.INSTANCE;
        }

        JsonObject o = new JsonObject();
        o.addProperty("action", event.getAction().getCanonicalName());
        o.addProperty("value", event.getValue());
        return o;
    }

    @Nullable
    public static ClickEvent deserializeClickEvent(JsonElement element) {
        if (isNull(element)) {
            return null;
        } else if (element.isJsonPrimitive()) {
            return new ClickEvent(ClickEvent.Action.OPEN_URL, element.getAsString());
        }

        JsonObject o = element.getAsJsonObject();

        if (o != null) {
            JsonPrimitive a = o.getAsJsonPrimitive("action");
            ClickEvent.Action action = a == null ? null : ClickEvent.Action.getValueByCanonicalName(a.getAsString());
            JsonPrimitive v = o.getAsJsonPrimitive("value");
            String s = v == null ? null : v.getAsString();

            if (action != null && s != null && action.shouldAllowInChat()) {
                return new ClickEvent(action, s);
            }
        }

        return null;
    }

    public static JsonObject fromJsonTree(JsonObject o) {
        JsonObject map = new JsonObject();
        fromJsonTree0(map, null, o);
        return map;
    }

    private static void fromJsonTree0(JsonObject map, @Nullable String id0, JsonObject o) {
        for (Map.Entry<String, JsonElement> entry : o.entrySet()) {
            if (entry.getValue() instanceof JsonObject) {
                fromJsonTree0(
                        map,
                        (id0 == null) ? entry.getKey() : (id0 + '.' + entry.getKey()),
                        entry.getValue().getAsJsonObject());
            } else {
                map.add((id0 == null) ? entry.getKey() : (id0 + '.' + entry.getKey()), entry.getValue());
            }
        }
    }

    public static JsonObject toJsonTree(Collection<Map.Entry<String, JsonElement>> tree) {
        JsonObject o1 = new JsonObject();

        for (Map.Entry<String, JsonElement> entry : tree) {
            findGroup(o1, entry.getKey()).add(lastKeyPart(entry.getKey()), entry.getValue());
        }

        return o1;
    }

    private static String lastKeyPart(String s) {
        int idx = s.lastIndexOf('.');

        if (idx != -1) {
            return s.substring(idx + 1);
        }

        return s;
    }

    private static JsonObject findGroup(JsonObject parent, String s) {
        int idx = s.indexOf('.');

        if (idx != -1) {
            String s0 = s.substring(0, idx);

            JsonElement o = parent.get(s0);

            if (o == null) {
                o = new JsonObject();
                parent.add(s0, o);
            }

            return findGroup(o.getAsJsonObject(), s.substring(idx + 1, s.length() - 1));
        }

        return parent;
    }

    public static String fixJsonString(String json) {
        if (json.isEmpty()) {
            return "\"\"";
        }

        if (json.indexOf(' ') != -1
                && !((json.startsWith("\"") && json.endsWith("\"")) || (json.startsWith("{") && json.endsWith("}"))
                        || (json.startsWith("[") && json.endsWith("]")))) {
            json = "\"" + json + "\"";
        }

        return json;
    }

    public static JsonElement toJson(@Nullable NBTBase nbt) {
        if (nbt == null) {
            return JsonNull.INSTANCE;
        }

        switch (nbt.getId()) {
            case Constants.NBT.TAG_COMPOUND: {
                NBTTagCompound tagCompound = (NBTTagCompound) nbt;
                JsonObject json = new JsonObject();

                if (!tagCompound.hasNoTags()) {
                    for (Object s : tagCompound.func_150296_c()) {
                        json.add(s.toString(), toJson(tagCompound.getTag(s.toString())));
                    }
                }

                return json;
            }
            case Constants.NBT.TAG_LIST: {
                JsonArray json = new JsonArray();
                NBTTagList list = (NBTTagList) nbt;

                if (list.tagCount() != 0) {
                    for (int i = 0; i < list.tagCount(); i++) {
                        json.add(toJson(list.getCompoundTagAt(i)));

                    }
                }

                return json;
            }
            case Constants.NBT.TAG_STRING: {
                String s = ((NBTTagString) nbt).func_150285_a_();
                return s.isEmpty() ? JSON_EMPTY_STRING : new JsonPrimitive(s);
            }
            case Constants.NBT.TAG_BYTE:
                return new JsonPrimitive(((NBTTagByte) nbt).func_150290_f());
            case Constants.NBT.TAG_SHORT:
                return new JsonPrimitive(((NBTTagShort) nbt).func_150289_e());
            case Constants.NBT.TAG_INT:
                return new JsonPrimitive(((NBTTagInt) nbt).func_150287_d());
            case Constants.NBT.TAG_LONG:
                return new JsonPrimitive(((NBTTagLong) nbt).func_150291_c());
            case Constants.NBT.TAG_FLOAT:
                return new JsonPrimitive(((NBTTagFloat) nbt).func_150288_h());
            case Constants.NBT.TAG_DOUBLE:
                return new JsonPrimitive(((NBTTagDouble) nbt).func_150286_g());
            case Constants.NBT.TAG_BYTE_ARRAY: {
                JsonArray json = new JsonArray();
                NBTTagByteArray ba = (NBTTagByteArray) nbt;

                if (ba.func_150292_c().length != 0) {
                    for (byte v : ba.func_150292_c()) {
                        json.add(new JsonPrimitive(v));
                    }
                }

                return json;
            }
            case Constants.NBT.TAG_INT_ARRAY: {
                JsonArray json = new JsonArray();
                NBTTagIntArray ia = (NBTTagIntArray) nbt;

                if (ia.func_150302_c().length != 0) {
                    for (int v : ia.func_150302_c()) {
                        json.add(new JsonPrimitive(v));
                    }
                }

                return json;
            }
            default:
                return JsonNull.INSTANCE;
        }
    }

    @Nullable
    public static NBTBase toNBT(@Nullable JsonElement element) {
        if (isNull(element)) {
            return null;
        }

        try {
            return JsonToNBT.func_150315_a(toJson(element));
        } catch (Exception ex) {
            return null;
        }
    }

    public static JsonElement copy(JsonElement json) {
        if (isNull(json)) {
            return JsonNull.INSTANCE;
        } else if (json.isJsonObject()) {
            JsonObject json1 = new JsonObject();

            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                json1.add(entry.getKey(), copy(entry.getValue()));
            }

            return json1;
        } else if (json.isJsonArray()) {
            JsonArray json1 = new JsonArray();

            for (JsonElement element : json.getAsJsonArray()) {
                json1.add(copy(element));
            }

            return json1;
        }

        return json;
    }

    public static JsonElement fromJson(Reader json) {
        return (json == null) ? JsonNull.INSTANCE : new JsonParser().parse(json);
    }

    public static JsonElement fromJson(File json) {
        try {
            if (json == null || !json.exists()) return JsonNull.INSTANCE;
            BufferedReader reader = new BufferedReader(new FileReader(json));
            JsonElement e = fromJson(reader);
            reader.close();
            return e;
        } catch (Exception ex) {}
        return JsonNull.INSTANCE;
    }

    public static void copy(JsonObject from, JsonObject to) {
        for (Map.Entry<String, JsonElement> entry : from.entrySet()) {
            to.add(entry.getKey(), copy(entry.getValue()));
        }
    }
}
