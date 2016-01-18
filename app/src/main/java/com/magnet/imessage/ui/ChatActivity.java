package com.magnet.imessage.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.magnet.imessage.R;
import com.magnet.imessage.core.CurrentApplication;
import com.magnet.imessage.helpers.ChannelHelper;
import com.magnet.imessage.helpers.UserHelper;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.model.Message;
import com.magnet.imessage.ui.adapters.MessagesAdapter;
import com.magnet.imessage.util.Logger;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.util.List;

public class ChatActivity extends BaseActivity {

    public static final String TAG_CHANNEL_NAME = "channelName";
    public static final String TAG_CREATE_WITH_USER_ID = "createWithUserId";
    public static final String TAG_CREATE_NEW = "createNew";

    private Conversation currentConversation;
    private MessagesAdapter adapter;
    private RecyclerView messagesListView;
    private String channelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        findViewById(R.id.chatSendBtn).setOnClickListener(this);

        messagesListView = (RecyclerView) findViewById(R.id.chatMessageList);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(false);
        messagesListView.setLayoutManager(layoutManager);

        if (getIntent().getBooleanExtra(TAG_CREATE_NEW, false)) {
            String userId = getIntent().getStringExtra(TAG_CREATE_WITH_USER_ID);
            if (userId != null) {
                ChannelHelper.getInstance().createChannelForUsers(userId, createListener);
            }
        } else {
            channelName = getIntent().getStringExtra(TAG_CHANNEL_NAME);
            if (channelName != null) {
                prepareConversation(CurrentApplication.getInstance().getConversationByName(channelName));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chatSendBtn:
                String text = getFieldText(R.id.chatMessageField);
                if (text != null && !text.isEmpty()) {
                    currentConversation.sendMessage(text, new Conversation.OnSendMessageListener() {
                        @Override
                        public void onSuccessSend(Message message) {
                            CurrentApplication.getInstance().getMessagesToApproveDeliver().put(message.getMessageId(), message);
                            clearFieldText(R.id.chatMessageField);
                            updateList();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            Logger.error("send messages", throwable);
                            showMessage("Can't send message");
                        }
                    });
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        MMX.unregisterListener(eventListener);
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentConversation != null) {
            prepareConversation(currentConversation);
        }
        MMX.registerListener(eventListener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuChatOpenDetails:
                String name = currentConversation.getChannel().getName();
                startActivity(DetailsActivity.createIntentForChannel(name));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setMessagesList(List<Message> messages) {
        adapter = new MessagesAdapter(this, messages);
        messagesListView.setAdapter(adapter);
    }

    private void updateList() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            messagesListView.smoothScrollToPosition(adapter.getItemCount());
        }
    }

    private void prepareConversation(Conversation conversation) {
        currentConversation = conversation;
        List<UserInfo> suppliersList = conversation.getSuppliersList();
        if (conversation.getSuppliers().size() == 1) {
            setTitle(UserHelper.getInstance().userNamesAsString(suppliersList));
        } else {
            setTitle("Group");
        }
        conversation.setHasUnreadMessage(false);
        String suppliers = UserHelper.getInstance().userNamesAsString(suppliersList);
        setText(R.id.chatSuppliers, "To: " + suppliers);
        setMessagesList(conversation.getMessages());
    }

    private ChannelHelper.OnCreateChannelListener createListener = new ChannelHelper.OnCreateChannelListener() {
        @Override
        public void onSuccessCreated(MMXChannel channel) {
            if (CurrentApplication.CURR_APP_MODE == CurrentApplication.APP_MODE.NORMAL) {
                ChannelHelper.getInstance().readChannelInfo(channel, readChannelInfoListener);
            } else {
                ChannelHelper.getInstance().readChannelInfoOld(channel, readChannelInfoListener);
            }
        }

        @Override
        public void onChannelExists(MMXChannel channel) {
            currentConversation = CurrentApplication.getInstance().getConversationByName(channel.getName());
            if (currentConversation == null) {
                if (CurrentApplication.CURR_APP_MODE == CurrentApplication.APP_MODE.NORMAL) {
                    ChannelHelper.getInstance().readChannelInfo(channel, readChannelInfoListener);
                } else {
                    ChannelHelper.getInstance().readChannelInfoOld(channel, readChannelInfoListener);
                }
            } else {
                prepareConversation(currentConversation);
                MMX.registerListener(eventListener);
            }
        }

        @Override
        public void onFailureCreated(Throwable throwable) {
            showMessage("Can't create conversation");
            finish();
        }
    };

    private ChannelHelper.OnReadChannelInfoListener readChannelInfoListener = new ChannelHelper.OnReadChannelInfoListener() {
        @Override
        public void onSuccessFinish(Conversation lastConversation) {
            prepareConversation(lastConversation);
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
            if (adapter != null && mmxMessage.getChannel() != null && channelName.equals(mmxMessage.getChannel().getName())) {
                updateList();
                currentConversation.setHasUnreadMessage(false);
            }
            return false;
        }

        @Override
        public boolean onMessageAcknowledgementReceived(User from, String messageId) {
            if (adapter != null) {
                updateList();
            }
            return false;
        }
    };

    public static Intent getIntentWithChannel(Conversation conversation) {
        String name = conversation.getChannel().getName();
        Intent intent = new Intent(CurrentApplication.getInstance(), ChatActivity.class);
        intent.putExtra(TAG_CHANNEL_NAME, name);
        return intent;
    }

    public static Intent getIntentForNewChannel(String userId) {
        Intent intent = new Intent(CurrentApplication.getInstance(), ChatActivity.class);
        intent.putExtra(TAG_CREATE_NEW, true);
        intent.putExtra(TAG_CREATE_WITH_USER_ID, userId);
        return intent;
    }

}
