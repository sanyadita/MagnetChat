package com.magnet.imessage.model;

import android.location.Location;

import com.magnet.imessage.helpers.DateHelper;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.internal.channel.PubSubItemChannel;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.util.HashMap;
import java.util.Map;

public class Message {

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_PHOTO = "photo";
    public static final String TYPE_MAP = "location";
    public static final String TYPE_VIDEO = "video";

    private static final String TAG_TYPE = "type";
    private static final String TAG_TEXT = "message";
    private static final String TAG_LONGITUDE = "longitude";
    private static final String TAG_LATITUDE = "latitude";

    private Map<String, String> content;
    private long createTime;
    private UserInfo sender;
    private String messageId;
    private boolean isDelivered;

    public String getText() {
        if (content == null) {
            return null;
        }
        return content.get(TAG_TEXT);
    }

    public String getType() {
        if (content == null) {
            return null;
        }
        return content.get(TAG_TYPE);
    }

    public String getLatitudeLongitude() {
        if (content == null) {
            return null;
        }
        return content.get(TAG_LATITUDE) + "," + content.get(TAG_LONGITUDE);
    }

    public void setContent(Map<String, String> content) {
        this.content = content;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public UserInfo getSender() {
        return sender;
    }

    public void setSender(UserInfo sender) {
        this.sender = sender;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setIsDelivered(boolean isDelivered) {
        this.isDelivered = isDelivered;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;
        if (messageId == null && message.messageId == null) {
            return true;
        } else if (messageId == null || message.messageId == null) {
            return false;
        }
        return messageId.equals(message.messageId);
    }

    public static Message createMessageFrom(MMXMessage mmxMessage) {
        Message message = new Message();
        message.setContent(mmxMessage.getContent());
        message.setCreateTime(mmxMessage.getTimestamp().getTime());
        message.setMessageId(mmxMessage.getId());
        User sender = mmxMessage.getSender();
        if (sender == null) {
            sender = User.getCurrentUser();
        }
        UserInfo.UserInfoBuilder infoBuilder = new UserInfo.UserInfoBuilder();
        infoBuilder.userId(sender.getUserIdentifier());
        infoBuilder.displayName(sender.getFirstName() + " " + sender.getLastName());
        message.setSender(infoBuilder.build());
        return message;
    }

    public static Message createMessageFrom(PubSubItemChannel itemChannel, UserInfo sender) {
        Message message = new Message();
        message.setContent(itemChannel.getContent());
        message.setSender(sender);
        message.setCreateTime(0);
        message.setMessageId(itemChannel.getItemId());
        message.setCreateTime(DateHelper.getDateFromString(itemChannel.getMetaData().getCreationDate()));
        return message;
    }

    public static Map<String, String> makeContent(String text) {
        Map<String, String> content = new HashMap<>();
        content.put(TAG_TYPE, TYPE_TEXT);
        content.put(TAG_TEXT, text);
        return content;
    }

    public static Map<String, String> makeContent(Location location) {
        Map<String, String> content = new HashMap<>();
        content.put(TAG_TYPE, TYPE_MAP);
        content.put(TAG_LATITUDE, String.format("%.6f", location.getLatitude()));
        content.put(TAG_LONGITUDE, String.format("%.6f", location.getLongitude()));
        return content;
    }

}
