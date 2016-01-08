package com.magnet.imessage.core;

import android.app.Application;

import com.magnet.imessage.R;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.model.Message;
import com.magnet.imessage.preferences.UserPreference;
import com.magnet.imessage.util.Logger;
import com.magnet.max.android.Max;
import com.magnet.max.android.config.MaxAndroidPropertiesConfig;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXMessage;

import java.util.List;

public class CurrentApplication extends Application {

    private static CurrentApplication instance;

    private List<Conversation> conversations;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Max.init(this.getApplicationContext(), new MaxAndroidPropertiesConfig(this, R.raw.magnetmax));
        UserPreference.getInstance(this);
        MMX.registerListener(eventListener);
    }

    public static CurrentApplication getInstance() {
        return instance;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
    }

    public Conversation getConversationByIdx(int idx) {
        if (conversations != null) {
            return conversations.get(idx);
        }
        return null;
    }

    private MMX.EventListener eventListener = new MMX.EventListener() {
        @Override
        public boolean onMessageReceived(MMXMessage mmxMessage) {
            if (mmxMessage.getChannel() != null) {
                for (Conversation conversation : conversations) {
                    if (conversation.getChannel().getName().equals(mmxMessage.getChannel().getName())) {
                        Message message = new Message();
                        message.setMmxMessage(mmxMessage);
                        conversation.addMessage(message);
                        break;
                    }
                }
            }
            Logger.debug("new message");
            return false;
        }
    };

}
