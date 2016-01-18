package com.magnet.imessage.model;

import com.magnet.imessage.util.Logger;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.internal.channel.PubSubItemChannel;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Conversation {

    private Map<String, UserInfo> suppliers;
    private List<Message> messages;
    private boolean hasUnreadMessage;
    private MMXChannel channel;

    public interface OnSendMessageListener {
        void onSuccessSend(Message message);
        void onFailure(Throwable throwable);
    }

    public Conversation() {
    }

    public Map<String, UserInfo> getSuppliers() {
        if (suppliers == null) {
            suppliers = new HashMap<>();
        }
        return suppliers;
    }

    public List<UserInfo> getSuppliersList() {
        return new ArrayList<>(getSuppliers().values());
    }

    public void addSupplier(UserInfo userInfo) {
        getSuppliers().put(userInfo.getUserId(), userInfo);
    }

    public void setSuppliers(List<UserInfo> suppliersList) {
        this.suppliers = new HashMap<>();
        for (UserInfo userInfo : suppliersList) {
            suppliers.put(userInfo.getUserId(), userInfo);
        }
    }

    public boolean hasUnreadMessage() {
        return hasUnreadMessage;
    }

    public void setHasUnreadMessage(boolean hasUnreadMessage) {
        this.hasUnreadMessage = hasUnreadMessage;
    }

    public MMXChannel getChannel() {
        return channel;
    }

    public void setChannel(MMXChannel channel) {
        this.channel = channel;
    }

    public List<Message> getMessages() {
        if (messages == null) {
            messages = new ArrayList<>();
        }
        return messages;
    }

    public void setMessages(List<PubSubItemChannel> pubSubItems) {
        if (pubSubItems != null) {
            messages = new ArrayList<>(pubSubItems.size());
            for (PubSubItemChannel item : pubSubItems) {
                UserInfo sender = suppliers.get(item.getPublisher().getUserId());
                if (sender == null) {
                    sender = item.getPublisherInfo();
                    suppliers.put(sender.getUserId(), sender);
                }
                messages.add(Message.createMessageFrom(item, sender));
            }
        } else {
            messages = new ArrayList<>();
        }
    }

    public void addMessage(Message message) {
        if (!getMessages().contains(message)) {
            messages.add(message);
        }
    }

    public void sendMessage(final String text, final OnSendMessageListener listener) {
        if (channel != null) {
            Map<String, String> content = Message.makeContent(text);
            final Message message = new Message();
            message.setContent(content);
            channel.publish(content, new MMXChannel.OnFinishedListener<String>() {
                @Override
                public void onSuccess(String s) {
                    Logger.debug("send message", "success");
                    message.setMessageId(s);
                    addMessage(message);
                    listener.onSuccessSend(message);
                }

                @Override
                public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                    listener.onFailure(throwable);
                }
            });
        } else {
            throw new Error();
        }
    }

    public String ownerId() {
        if (channel == null) {
            return null;
        }
        return channel.getOwnerId();
    }

}
