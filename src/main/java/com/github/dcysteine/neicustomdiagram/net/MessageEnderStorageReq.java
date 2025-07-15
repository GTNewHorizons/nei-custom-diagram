package com.github.dcysteine.neicustomdiagram.net;

import static com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageUtil.MAX_FREQUENCY;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import com.github.dcysteine.neicustomdiagram.lib.io.DataIn;
import com.github.dcysteine.neicustomdiagram.lib.io.DataOut;
import com.github.dcysteine.neicustomdiagram.lib.net.MessageToServer;
import com.github.dcysteine.neicustomdiagram.lib.net.NetworkWrapper;
import com.github.dcysteine.neicustomdiagram.main.config.ConfigOptions;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageFrequency;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import codechicken.enderstorage.api.EnderStorageManager;
import codechicken.enderstorage.storage.liquid.EnderLiquidStorage;

public class MessageEnderStorageReq extends MessageToServer {

    private EnderStorageUtil.Owner owner;
    private EnderStorageUtil.Type type;

    public MessageEnderStorageReq() {}

    public MessageEnderStorageReq(EnderStorageUtil.Owner owner, EnderStorageUtil.Type type) {
        this.owner = owner;
        this.type = type;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return NcdNetHandler.ENDER_STORAGE;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(owner.ordinal());
        data.writeVarInt(type.ordinal());
    }

    @Override
    public void readData(DataIn data) {
        owner = EnderStorageUtil.Owner.values()[data.readVarInt()];
        type = EnderStorageUtil.Type.values()[data.readVarInt()];
    }

    @Override
    public void onMessage(EntityPlayerMP player) {
        EnderStorageManager storageManager = EnderStorageManager.instance(false);
        JsonArray data = new JsonArray();

        if (owner == EnderStorageUtil.Owner.GLOBAL) {
            if (ConfigOptions.ENDER_STORAGE_SERVER_VIEW_LEVEL.get() == 2
                    || (ConfigOptions.ENDER_STORAGE_SERVER_VIEW_LEVEL.get() == 1
                            && MinecraftServer.getServer().getConfigurationManager().func_152603_m()
                                    .func_152700_a(player.getDisplayName()) == null)) {
                new MessageEnderStorageRes(owner, type, data).sendTo(player);
                return;
            }
        }

        switch (type) {
            case TANK:
                Map<EnderStorageFrequency, EnderLiquidStorage> map = new LinkedHashMap<>();
                IntStream.rangeClosed(0, MAX_FREQUENCY).map(EnderStorageUtil::reverseInt).forEach(
                        freq -> map.put(
                                EnderStorageFrequency.createReverse(freq),
                                (EnderLiquidStorage) storageManager
                                        .getStorage(owner.stringParam(player), freq, type.stringParam)));
                List<Map.Entry<EnderStorageFrequency, EnderLiquidStorage>> datas = map.entrySet().stream()
                        .filter(entry -> !EnderStorageUtil.isEmpty(entry.getValue())).collect(Collectors.toList());

                datas.forEach(tank -> {
                    JsonObject value = new JsonObject();
                    value.addProperty("frequency", tank.getKey().frequency());
                    value.addProperty("name", tank.getValue().getFluid().getFluid().getName());
                    value.addProperty("amount", tank.getValue().getFluid().amount);
                    data.add(value);
                });
                break;
        }

        new MessageEnderStorageRes(owner, type, data).sendTo(player);
    }
}
