package com.magnet.imessage.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;

import com.magnet.imessage.R;
import com.magnet.imessage.core.CurrentApplication;
import com.magnet.imessage.helpers.ChannelHelper;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.ui.adapters.UsersAdapter;
import com.magnet.imessage.util.Logger;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.util.ArrayList;
import java.util.List;

public class ChooseUserActivity extends BaseActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener, AdapterView.OnItemClickListener {

    public static final String TAG_ADD_USER_TO_CHANNEL = "addUserToChannel";

    private enum ActivityMode {MODE_TO_CREATE, MODE_TO_ADD_USER};

    private UsersAdapter adapter;
    private ListView userList;
    private ActivityMode currentMode;
    private Conversation conversation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_user);
        userList = (ListView) findViewById(R.id.chooseUserList);
        userList.setOnItemClickListener(this);
        SearchView search = (SearchView) findViewById(R.id.chooseUserSearch);
        search.setOnQueryTextListener(this);
        search.setOnCloseListener(this);
        searchUsers("");
        currentMode = ActivityMode.MODE_TO_CREATE;
        String channelName = getIntent().getStringExtra(TAG_ADD_USER_TO_CHANNEL);
        if (channelName != null) {
            conversation = CurrentApplication.getInstance().getConversationByName(channelName);
            currentMode = ActivityMode.MODE_TO_ADD_USER;
        }
        setTitle("New Message");
    }

    @Override
    public void onClick(View v) {
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        User selectedUser = adapter.getItem(position);
        switch (currentMode) {
            case MODE_TO_ADD_USER:
                addUserToChannel(selectedUser);
                break;
            case MODE_TO_CREATE:
                startActivity(ChatActivity.getIntentForNewChannel(selectedUser.getUserIdentifier()));
                finish();
                break;
        }
    }

    @Override
    public boolean onClose() {
        hideKeyboard();
        searchUsers("");
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        hideKeyboard();
        searchUsers(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        if (newText.isEmpty()) {
            hideKeyboard();
            searchUsers("");
        }
        return true;
    }

    private void addUserToChannel(final User user) {
        findViewById(R.id.chooseUserProgress).setVisibility(View.VISIBLE);
        ChannelHelper.getInstance().addUserToConversation(conversation, user, new ChannelHelper.OnAddUserListener() {
            @Override
            public void onSuccessAdded() {
                findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                finish();
            }

            @Override
            public void onUserSetExists(String channelSetName) {
                findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                Conversation anotherConversation = CurrentApplication.getInstance().getConversationByName(channelSetName);
                startActivity(ChatActivity.getIntentWithChannel(anotherConversation));
                finish();
            }

            @Override
            public void onWasAlreadyAdded() {
                findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                showMessage("User was already added");
                finish();

            }

            @Override
            public void onFailure(Throwable throwable) {
                findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                showMessage("Can't add user to channel");
            }
        });
    }

    private void searchUsers(@NonNull String query) {
        findViewById(R.id.chooseUserProgress).setVisibility(View.VISIBLE);
        User.search("lastName:" + query + "*", 100, 0, "lastName:asc", new ApiCallback<List<User>>() {
            @Override
            public void success(List<User> users) {
                users.remove(User.getCurrentUser());
                if (conversation != null) {
                    List<Integer> idxToRemove = new ArrayList<Integer>();
                    for (UserInfo userInfo : conversation.getSuppliers().values()) {
                        for (int i = 0; i < users.size(); i++) {
                            if (userInfo.getUserId().equals(users.get(i).getUserIdentifier())) {
                                idxToRemove.add(i);
                            }
                        }
                    }
                    for (Integer idx : idxToRemove) {
                        users.remove(idx.intValue());
                    }
                }
                findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                Logger.debug("find users", "success");
                updateList(users);
            }

            @Override
            public void failure(ApiError apiError) {
                findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                showMessage("Can't find users");
                Logger.error("find users", apiError);
            }
        });
    }

    private void updateList(List<User> users) {
        adapter = new UsersAdapter(this, users);
        userList.setAdapter(adapter);
    }

    public static Intent getIntentToCreateChannel() {
        return new Intent(CurrentApplication.getInstance(), ChooseUserActivity.class);
    }

    public static Intent getIntentToAddUserToChannel(String channelName) {
        Intent intent = new Intent(CurrentApplication.getInstance(), ChooseUserActivity.class);
        intent.putExtra(TAG_ADD_USER_TO_CHANNEL, channelName);
        return intent;
    }

}
