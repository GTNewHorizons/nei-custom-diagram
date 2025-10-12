package com.github.dcysteine.neicustomdiagram.lib.net;

import com.github.dcysteine.neicustomdiagram.main.NeiCustomDiagram;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

enum MessageToClientHandler implements IMessageHandler<MessageToClient, IMessage> {

    INSTANCE;

    @Override
    public IMessage onMessage(MessageToClient message, MessageContext context) {
        NeiCustomDiagram.instance.handleClientMessage(message);
        return null;
    }
}
