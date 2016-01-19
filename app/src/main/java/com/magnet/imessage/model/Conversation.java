package com.magnet.imessage.model;

import android.location.Location;

import com.magnet.imessage.helpers.UserHelper;
import com.magnet.imessage.util.Logger;
import com.magnet.max.android.Attachment;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.internal.channel.PubSubItemChannel;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.io.File;
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
    public void addSupplier(User user) {
        UserInfo.UserInfoBuilder infoBuilder = new UserInfo.UserInfoBuilder();
        infoBuilder.userId(user.getUserIdentifier());
        infoBuilder.displayName(user.getFirstName() + " " + user.getLastName());
        getSuppliers().put(user.getUserIdentifier(), infoBuilder.build());
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
                    if (sender.getUserId() != null) {
                        suppliers.put(sender.getUserId(), sender);
                    } else {
                        sender = null;
                    }
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

    public void sendTextMessage(final String text, final OnSendMessageListener listener) {
        if (channel != null) {
            Map<String, String> content = Message.makeContent(text);
            sendMessage(content, listener);
        } else {
            throw new Error();
        }
    }

    public void sendLocation(Location location, final OnSendMessageListener listener) {
        if (channel != null) {
            Map<String, String> content = Message.makeContent(location);
            sendMessage(content, listener);
        } else {
            throw new Error();
        }
    }

    public void sendMedia(final String filePath, final String type, final OnSendMessageListener listener) {
        if (channel != null) {
            File file = new File(filePath);
            String fileType = Message.FILE_TYPE_PHOTO;
            if (type.equals(Message.TYPE_VIDEO)) {
                fileType = Message.FILE_TYPE_VIDEO;
            }
            Attachment attachment = new Attachment(file, fileType, file.getName(), "From " + UserHelper.getInstance().userNameAsString(User.getCurrentUser()));
            attachment.upload(new Attachment.UploadListener() {
                @Override
                public void onStart(Attachment attachment) {
                    Logger.debug("start upload", filePath);
                }

                @Override
                public void onComplete(Attachment attachment) {
                    Logger.debug("complete upload", filePath);
                    Map<String, String> content = Message.makeMediaContent(attachment.getDownloadUrl(), type);
                    sendMessage(content, listener);
                }

                @Override
                public void onError(Attachment attachment, Throwable throwable) {
                    Logger.error("error upload", throwable);
                    listener.onFailure(throwable);
                }
            });
        } else {
            throw new Error();
        }
    }

    private void sendMessage(Map<String, String> content, final OnSendMessageListener listener) {
        final Message message = new Message();
        message.setContent(content);
        message.setCreateTime(System.currentTimeMillis());
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
    }

    public String ownerId() {
        if (channel == null) {
            return null;
        }
        return channel.getOwnerId();
    }

}
