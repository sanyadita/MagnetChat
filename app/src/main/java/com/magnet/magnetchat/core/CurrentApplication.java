package com.magnet.magnetchat.core;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.magnet.magnetchat.R;
import com.magnet.magnetchat.helpers.ChannelHelper;
import com.magnet.magnetchat.helpers.InternetConnection;
import com.magnet.magnetchat.helpers.UserHelper;
import com.magnet.magnetchat.model.Conversation;
import com.magnet.magnetchat.model.Message;
import com.magnet.magnetchat.preferences.UserPreference;
import com.magnet.magnetchat.util.Logger;
import com.magnet.max.android.Max;
import com.magnet.max.android.User;
import com.magnet.max.android.config.MaxAndroidPropertiesConfig;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class CurrentApplication extends MultiDexApplication {

    private static CurrentApplication instance;

    private Map<String, Conversation> conversations;
    private Map<String, Message> messagesToApproveDeliver;
    private Notification notification;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        Max.init(this.getApplicationContext(), new MaxAndroidPropertiesConfig(this, R.raw.magnetmax));
        UserPreference.getInstance(this);
        InternetConnection.getInstance(this);
        MMX.registerListener(eventListener);
        MMX.registerWakeupBroadcast(this, new Intent("MMX_WAKEUP_ACTION"));
    }

    //Enable MultiDex
    public void attachBaseContext(Context base) {
        MultiDex.install(base);
        super.attachBaseContext(base);
    }

    public static CurrentApplication getInstance() {
        return instance;
    }

    public Map<String, Conversation> getConversations() {
        if (conversations == null) {
            conversations = new TreeMap<>();
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

    public void messageNotification() {
        if (notification == null) {
            PendingIntent intent = PendingIntent.getActivity(this, 0, new Intent(Intent.ACTION_MAIN)
                            .addCategory(Intent.CATEGORY_DEFAULT)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .setPackage(this.getPackageName()),
                    PendingIntent.FLAG_UPDATE_CURRENT);
            notification = new Notification.Builder(this).setAutoCancel(true).setSmallIcon(getApplicationInfo().icon)
                    .setContentTitle("New message is available").setContentIntent(intent).build();
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(12345, notification);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);
    }

    public Conversation getConversationByName(String name) {
        return getConversations().get(name);
    }

    public void removeConversations() {
        conversations = null;
    }

    private MMX.EventListener eventListener = new MMX.EventListener() {
        @Override
        public boolean onLoginRequired(MMX.LoginReason reason) {
            Logger.debug("login required", reason.name());
            UserHelper.getInstance().checkAuthentication(null);
            return false;
        }

        @Override
        public boolean onInviteReceived(MMXChannel.MMXInvite invite) {
            Logger.debug("invite to", invite.getInviteInfo().getChannel().getName());
            return super.onInviteReceived(invite);
        }

        @Override
        public boolean onMessageReceived(MMXMessage mmxMessage) {
            if (mmxMessage.getSender() != null && !mmxMessage.getSender().getUserIdentifier().equals(User.getCurrentUserId())) {
                messageNotification();
            }
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
