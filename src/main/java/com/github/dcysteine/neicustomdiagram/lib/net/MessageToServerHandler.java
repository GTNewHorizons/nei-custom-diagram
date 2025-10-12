package com.github.dcysteine.neicustomdiagram.lib.net;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

enum MessageToServerHandler implements IMessageHandler<MessageToServer, IMessage> {

    INSTANCE;

    @Override
    public IMessage onMessage(MessageToServer message, MessageContext context) {

        message.onMessage(context.getServerHandler().playerEntity);

        return null;
    }
}
