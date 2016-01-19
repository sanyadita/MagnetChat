package com.magnet.imessage.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
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

import java.util.HashMap;
import java.util.List;

import nl.changer.polypicker.Config;
import nl.changer.polypicker.ImagePickerActivity;

public class ChatActivity extends BaseActivity {

    public static final String TAG_CHANNEL_NAME = "channelName";
    public static final String TAG_CREATE_WITH_USER_ID = "createWithUserId";
    public static final String TAG_CREATE_NEW = "createNew";

    private static final String[] ATTACHMENT_VARIANTS = {"Send photo", "Send location", "Send video", "Cancel"};

    public static final int INTENT_REQUEST_GET_IMAGES = 14;
    public static final int INTENT_SELECT_VIDEO = 13;

    private Conversation currentConversation;
    private MessagesAdapter adapter;
    private RecyclerView messagesListView;
    private String channelName;
    private AlertDialog attachmentDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        findViewById(R.id.chatSendBtn).setOnClickListener(this);
        findViewById(R.id.chatAddAttachment).setOnClickListener(this);

        messagesListView = (RecyclerView) findViewById(R.id.chatMessageList);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(false);
        messagesListView.setLayoutManager(layoutManager);
        gpsTracker = new GPSTracker(this);

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
            case R.id.chatAddAttachment:
                showAttachmentDialog();
                break;
        }
    }

    @Override
    protected void onPause() {
        MMX.unregisterListener(eventListener);
        if (attachmentDialog != null && attachmentDialog.isShowing()){
            attachmentDialog.dismiss();
        }
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
                if (currentConversation != null) {
                    String name = currentConversation.getChannel().getName();
                    startActivity(DetailsActivity.createIntentForChannel(name));
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == INTENT_REQUEST_GET_IMAGES) {
                Parcelable[] parcelableUris = intent.getParcelableArrayExtra(ImagePickerActivity.EXTRA_IMAGE_URIS);

                if (parcelableUris == null) {
                    return;
                }

                // Java doesn't allow array casting, this is a little hack
                Uri[] uris = new Uri[parcelableUris.length];
                System.arraycopy(parcelableUris, 0, uris, 0, parcelableUris.length);

                if (uris != null && uris.length > 0) {
                    for (Uri uri : uris) {
//                        sendMedia(KEY_MESSAGE_IMAGE, uri.toString());
                    }
                }
            } else if (requestCode == INTENT_SELECT_VIDEO) {
                Uri videoUri = intent.getData();
//                String videoPath = FileHelper.getPath(this, videoUri);
//                sendMedia(KEY_MESSAGE_VIDEO, videoPath);
            }
        }
    }

    private void showAttachmentDialog() {
        if (attachmentDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setItems(ATTACHMENT_VARIANTS, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            break;
                        case 1:
                            break;
                        case 2:
                            break;
                        case 3:
                            attachmentDialog.dismiss();
                            break;
                    }
                }
            });
            builder.setCancelable(false);
            attachmentDialog = builder.create();
        }
        attachmentDialog.show();
    }

    private void selectImage() {
        Intent intent = new Intent(this, ImagePickerActivity.class);
        Config config = new Config.Builder()
                .setTabBackgroundColor(R.color.white)
                .setSelectionLimit(1)
                .build();
        ImagePickerActivity.setConfig(config);
        startActivityForResult(intent, INTENT_REQUEST_GET_IMAGES);
    }

    private void selectVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a Video "), INTENT_SELECT_VIDEO);
    }

    private void sendLocation() {
        Location
        if (gpsTracker.canGetLocation() && gpsTracker.getLatitude() != 0.00 && gpsTracker.getLongitude() != 0.00) {
            double myLat = gpsTracker.getLatitude();
            double myLong = gpsTracker.getLongitude();
            String latlng = (Double.toString(myLat) + "," + Double.toString(myLong));

            String username = MMX.getCurrentUser().getDisplayName();
            updateList(username, KEY_MESSAGE_MAP, latlng, false);

            HashMap<String, String> content = new HashMap<>();
            content.put("type", KEY_MESSAGE_MAP);
            content.put("latitude", Double.toString(myLat));
            content.put("longitude", Double.toString(myLong));
            send(content);
        }else{
            mGPS.showSettingsAlert(this);
        }
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
        if (CurrentApplication.getInstance().getConversations().get(conversation.getChannel().getName()) == null) {
            finish();
        }
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
            ChannelHelper.getInstance().readChannelInfo(channel, readChannelInfoListener);
        }

        @Override
        public void onChannelExists(MMXChannel channel) {
            currentConversation = CurrentApplication.getInstance().getConversationByName(channel.getName());
            if (currentConversation == null) {
                ChannelHelper.getInstance().readChannelInfo(channel, readChannelInfoListener);
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
            if (lastConversation == null) {
                showMessage("Can't start conversation");
                finish();
            } else {
                prepareConversation(lastConversation);
            }
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
