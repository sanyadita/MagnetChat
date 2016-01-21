package com.magnet.imessage.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.magnet.imessage.R;
import com.magnet.imessage.core.CurrentApplication;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.ui.adapters.UsersAdapter;
import com.magnet.max.android.User;

public class DetailsActivity extends BaseActivity {

    public static final String TAG_CHANNEL_NAME = "channelName";

    private String channelName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        channelName = getIntent().getStringExtra(TAG_CHANNEL_NAME);
        if (channelName != null) {
            Conversation currentConversation = CurrentApplication.getInstance().getConversationByName(channelName);
            boolean canAddUser = false;
            if (currentConversation.getChannel().getOwnerId().equals(User.getCurrentUserId())) {
                canAddUser = true;
            }
            UsersAdapter adapter = new UsersAdapter(this, currentConversation.getSuppliersList(), addUserListener);
            ListView listView = (ListView) findViewById(R.id.detailsSubscribersList);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public void onClick(View v) {

    }

    private UsersAdapter.AddUserListener addUserListener = new UsersAdapter.AddUserListener() {
        @Override
        public void addUser() {
            startActivity(ChooseUserActivity.getIntentToAddUserToChannel(channelName));
            finish();
        }
    };

    public static Intent createIntentForChannel(String channelName) {
        Intent intent = new Intent(CurrentApplication.getInstance(), DetailsActivity.class);
        intent.putExtra(TAG_CHANNEL_NAME, channelName);
        return intent;
    }

}
