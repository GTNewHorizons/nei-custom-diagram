package com.github.dcysteine.neicustomdiagram.net;

import com.github.dcysteine.neicustomdiagram.lib.net.NetworkWrapper;

public class NcdNetHandler {

    static final NetworkWrapper ENDER_STORAGE = NetworkWrapper.newWrapper("ncd_enderstorage");

    public static void init() {
        ENDER_STORAGE.register(new MessageEnderStorageReq());
        ENDER_STORAGE.register(new MessageEnderStorageRes());
    }
}
