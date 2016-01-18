package com.magnet.imessage.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Toast;

import com.magnet.imessage.R;
import com.magnet.imessage.core.CurrentApplication;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.ui.adapters.UsersAdapter;
import com.magnet.imessage.util.Logger;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMXChannel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        return false;
    }

    private void addUserToChannel(User user) {
        findViewById(R.id.chooseUserProgress).setVisibility(View.VISIBLE);
        if (conversation.getSuppliers().get(user.getUserIdentifier()) == null) {
            Set<User> userSet = new HashSet<>();
            userSet.add(user);
            conversation.getChannel().addSubscribers(userSet, new MMXChannel.OnFinishedListener<List<String>>() {
                @Override
                public void onSuccess(List<String> strings) {
                    findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                    finish();
                }

                @Override
                public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                    findViewById(R.id.chooseUserProgress).setVisibility(View.GONE);
                    showMessage("Can't add user to channel");
                    Logger.error("add user", throwable);
                }
            });
        } else {
            Toast.makeText(this, "User was already added", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void searchUsers(@NonNull String query) {
        findViewById(R.id.chooseUserProgress).setVisibility(View.VISIBLE);
        User.search("lastName:" + query + "*", 100, 0, "lastName:asc", new ApiCallback<List<User>>() {
            @Override
            public void success(List<User> users) {
                users.remove(User.getCurrentUser());
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
