package com.github.dcysteine.neicustomdiagram.net;

import com.github.dcysteine.neicustomdiagram.generators.enderstorage.chestoverview.EnderStorageChestOverview;
import com.github.dcysteine.neicustomdiagram.generators.enderstorage.tankoverview.EnderStorageTankOverview;
import com.github.dcysteine.neicustomdiagram.lib.io.DataIn;
import com.github.dcysteine.neicustomdiagram.lib.io.DataOut;
import com.github.dcysteine.neicustomdiagram.lib.net.MessageToClient;
import com.github.dcysteine.neicustomdiagram.lib.net.NetworkWrapper;
import com.github.dcysteine.neicustomdiagram.util.enderstorage.EnderStorageUtil;
import com.google.gson.JsonArray;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MessageEnderStorageRes extends MessageToClient {

    private EnderStorageUtil.Owner owner;
    private EnderStorageUtil.Type type;
    private JsonArray jsonData;

    public MessageEnderStorageRes() {}

    public MessageEnderStorageRes(EnderStorageUtil.Owner owner, EnderStorageUtil.Type type, JsonArray jsonData) {
        this.owner = owner;
        this.type = type;
        this.jsonData = jsonData;
    }

    @Override
    public NetworkWrapper getWrapper() {
        return NcdNetHandler.ENDER_STORAGE;
    }

    @Override
    public void writeData(DataOut data) {
        data.writeVarInt(owner.ordinal());
        data.writeVarInt(type.ordinal());
        data.writeJson(jsonData);
    }

    @Override
    public void readData(DataIn data) {
        owner = EnderStorageUtil.Owner.values()[data.readVarInt()];
        type = EnderStorageUtil.Type.values()[data.readVarInt()];
        jsonData = data.readJson().getAsJsonArray();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void onMessage() {
        switch (type) {
            case CHEST:
                EnderStorageChestOverview.INSTANCE.updateJsonData(owner, jsonData);
                break;
            case TANK:
                EnderStorageTankOverview.INSTANCE.updateJsonData(owner, jsonData);
                break;
        }
    }
}
