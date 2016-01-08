package com.magnet.imessage.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.magnet.imessage.R;
import com.magnet.imessage.core.CurrentApplication;
import com.magnet.imessage.helpers.ChannelHelper;
import com.magnet.imessage.helpers.UserHelper;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.model.Message;
import com.magnet.imessage.ui.adapters.MessagesAdapter;
import com.magnet.imessage.util.Logger;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;

import java.util.List;

public class ChatActivity extends BaseActivity {

    public static final String TAG_IDX_FROM_CHANNELS_LIST = "idxFromChannelList";
    public static final String TAG_CREATE_WITH_USER_ID = "createWithUserId";
    public static final String TAG_CREATE_NEW = "createNew";

    private Conversation currentConversation;
    private MessagesAdapter adapter;
    private ListView messagesListView;

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        findViewById(R.id.chatSendBtn).setOnClickListener(this);
        messagesListView = (ListView) findViewById(R.id.chatMessageList);
        if (getIntent().getBooleanExtra(TAG_CREATE_NEW, false)) {
            String userId = getIntent().getStringExtra(TAG_CREATE_WITH_USER_ID);
            if (userId != null) {
                ChannelHelper.getInstance().createChannelForUsers(userId, createListener);
            }
        } else {
            int channelIdx = getIntent().getIntExtra(TAG_IDX_FROM_CHANNELS_LIST, -1);
            if (channelIdx >= 0) {
                currentConversation = CurrentApplication.getInstance().getConversationByIdx(channelIdx);
                if (currentConversation.getSuppliers().size() == 1) {
                    setTitle(UserHelper.getInstance().userNamesAsString(currentConversation.getSuppliers()));
                } else {
                    setTitle("Group");
                }
                String suppliers = UserHelper.getInstance().userNamesAsString(currentConversation.getSuppliers());
                setText(R.id.chatSuppliers, "To: " + suppliers);
                updateMessagesList(currentConversation.getMessages());
            }
        }
        MMX.registerListener(eventListener);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chatSendBtn:
                String text = getFieldText(R.id.chatMessageField);
                if (text != null) {
                    Message message = Message.createMessage(currentConversation.getChannel(), text);
                    currentConversation.sendMessage(message, new MMXChannel.OnFinishedListener<String>() {
                        @Override
                        public void onSuccess(String s) {
                            clearFieldText(R.id.chatMessageField);
                        }

                        @Override
                        public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                            Logger.error("send messages", throwable);
                            showMessage("Can't send message");
                        }
                    });
                }
                break;
        }
    }

    private void updateMessagesList(List<Message> messages) {
        adapter = new MessagesAdapter(this, messages);
        messagesListView.setAdapter(adapter);
    }

    private ChannelHelper.OnCreateChannelListener createListener = new ChannelHelper.OnCreateChannelListener() {
        @Override
        public void onSuccessCreated(MMXChannel channel) {
            currentConversation = new Conversation();
            currentConversation.setChannel(channel);
            CurrentApplication.getInstance().getConversations().add(currentConversation);
            ChannelHelper.getInstance().readSubscribersToConversation(channel, currentConversation, readChannelInfoListener);
            updateMessagesList(currentConversation.getMessages());
        }

        @Override
        public void onChannelExists(MMXChannel channel) {
            List<Conversation> conversations = CurrentApplication.getInstance().getConversations();
            for (Conversation conversation : conversations) {
                if (conversation.getChannel().getName().equals(channel.getName())) {
                    currentConversation = conversation;
                    break;
                }
            }
            if (currentConversation == null) {
                currentConversation = new Conversation();
                currentConversation.setChannel(channel);
                CurrentApplication.getInstance().getConversations().add(currentConversation);
                ChannelHelper.getInstance().readSubscribersToConversation(channel, currentConversation, readChannelInfoListener);
            }
            updateMessagesList(currentConversation.getMessages());
        }

        @Override
        public void onFailureCreated(Throwable throwable) {
            showMessage("Can't create conversation");
            finish();
        }
    };

    private ChannelHelper.OnReadChannelInfoListener readChannelInfoListener = new ChannelHelper.OnReadChannelInfoListener() {
        @Override
        public void onSuccessFinish() {
            String suppliers = UserHelper.getInstance().userNamesAsString(currentConversation.getSuppliers());
            setText(R.id.chatSuppliers, "To: " + suppliers);
        }

        @Override
        public void onFailure(Throwable throwable) {
            showMessage("Can't read conversation information");
            finish();
        }
    };

    private MMX.EventListener eventListener = new MMX.EventListener() {
        @Override
        public boolean onMessageReceived(MMXMessage mmxMessage) {
            if (adapter != null && currentConversation.getChannel().equals(mmxMessage.getChannel())) {
                adapter.notifyDataSetChanged();
            }
            return false;
        }
    };

    public static Intent getIntentWithChannel(Conversation conversation) {
        int channelIdx = CurrentApplication.getInstance().getConversations().indexOf(conversation);
        Intent intent = new Intent(CurrentApplication.getInstance(), ChatActivity.class);
        intent.putExtra(TAG_IDX_FROM_CHANNELS_LIST, channelIdx);
        return intent;
    }

    public static Intent getIntentForNewChannel(String userId) {
        Intent intent = new Intent(CurrentApplication.getInstance(), ChatActivity.class);
        intent.putExtra(TAG_CREATE_NEW, true);
        intent.putExtra(TAG_CREATE_WITH_USER_ID, userId);
        return intent;
    }

}
