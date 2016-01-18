package com.magnet.imessage.core;

import android.app.Application;

import com.magnet.imessage.R;
import com.magnet.imessage.helpers.ChannelHelper;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.model.Message;
import com.magnet.imessage.preferences.UserPreference;
import com.magnet.max.android.Max;
import com.magnet.max.android.User;
import com.magnet.max.android.config.MaxAndroidPropertiesConfig;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXMessage;

import java.util.HashMap;
import java.util.Map;

public class CurrentApplication extends Application {

    public enum APP_MODE {DEV, NORMAL}

    public static APP_MODE CURR_APP_MODE = APP_MODE.DEV;

    private static CurrentApplication instance;

    private Map<String, Conversation> conversations;
    private Map<String, Message> messagesToApproveDeliver;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        if (CURR_APP_MODE == APP_MODE.DEV) {
            Max.init(this.getApplicationContext(), new MaxAndroidPropertiesConfig(this, R.raw.magnetmax_dev));
        } else {
            Max.init(this.getApplicationContext(), new MaxAndroidPropertiesConfig(this, R.raw.magnetmax));
        }
        UserPreference.getInstance(this);
        MMX.registerListener(eventListener);
    }

    public static CurrentApplication getInstance() {
        return instance;
    }

    public Map<String, Conversation> getConversations() {
        if (conversations == null) {
            conversations = new HashMap<>();
        }
        return conversations;
    }

    public Map<String, Message> getMessagesToApproveDeliver() {
        if (messagesToApproveDeliver == null) {
            messagesToApproveDeliver = new HashMap<>();
        }
        return messagesToApproveDeliver;
    }

    public void addConversation(String channelName, Conversation conversation) {
        getConversations().put(channelName, conversation);
    }

    public void approveMessage(String messageId) {
        Message message = getMessagesToApproveDeliver().get(messageId);
        if (message != null) {
            message.setIsDelivered(true);
            messagesToApproveDeliver.remove(messageId);
        }
    }

    public Conversation getConversationByName(String name) {
        return getConversations().get(name);
    }

    public void removeConversations() {
        conversations = null;
    }

    private MMX.EventListener eventListener = new MMX.EventListener() {

        @Override
        public boolean onMessageReceived(MMXMessage mmxMessage) {
            ChannelHelper.getInstance().receiveMessage(mmxMessage);
            return false;
        }

        @Override
        public boolean onMessageAcknowledgementReceived(User from, String messageId) {
            approveMessage(messageId);
            return false;
        }
    };

}
