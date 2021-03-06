package com.magnet.magnetchat.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;

import com.magnet.magnetchat.R;
import com.magnet.magnetchat.core.CurrentApplication;
import com.magnet.magnetchat.helpers.ChannelHelper;
import com.magnet.magnetchat.helpers.UserHelper;
import com.magnet.magnetchat.model.Conversation;
import com.magnet.magnetchat.model.Message;
import com.magnet.magnetchat.ui.adapters.ConversationsAdapter;
import com.magnet.magnetchat.util.Logger;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.MMX;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener, AdapterView.OnItemClickListener {

    private DrawerLayout drawer;
    private String username;
    private ConversationsAdapter adapter;
    private ListView conversationsList;
    private List<Conversation> conversations;
    private AlertDialog leaveDialog;
    private Thread searchThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        if (User.getCurrentUser() != null) {
            username = UserHelper.getInstance().userNameAsString(User.getCurrentUser());
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(username);
        setSupportActionBar(toolbar);

        SearchView searchView = (SearchView) findViewById(R.id.homeSearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (searchThread == null) {
                    searchMessage(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) {
                    hideKeyboard();
                    showList(CurrentApplication.getInstance().getConversations());
                }
                return false;
            }
        });
        searchView.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                showAllConversations();
                return true;
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        TextView navigationName = (TextView) navigationView.getHeaderView(0).findViewById(R.id.homeUserName);
        navigationName.setText(username);

        conversationsList = (ListView) findViewById(R.id.homeConversationsList);
        conversationsList.setOnItemClickListener(this);
        conversationsList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                showLeaveDialog(adapter.getItem(position));
                return false;
            }
        });
        ChannelHelper.getInstance().readConversations(readChannelInfoListener);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (adapter != null) {
            Conversation conversation = adapter.getItem(position);
            if (conversation != null && conversation.getSuppliers() != null)
                startActivity(ChatActivity.getIntentWithChannel(conversation));
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuHomeCreateConversation:
                startActivity(ChooseUserActivity.getIntentToCreateChannel());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuChangePassword:
                startActivity(new Intent(this, ChangePasswordActivity.class));
                break;
            case R.id.menuSignOut:
                UserHelper.getInstance().logout(logoutListener);
                break;
        }
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        showAllConversations();
        ChannelHelper.getInstance().rereadConversations(null);
        MMX.registerListener(eventListener);
        registerReceiver(onAddedConversation, new IntentFilter("com.magnet.imessage.ADDED_CONVERSATION"));
    }

    @Override
    protected void onPause() {
        MMX.unregisterListener(eventListener);
        unregisterReceiver(onAddedConversation);
        if (leaveDialog != null && leaveDialog.isShowing()) {
            leaveDialog.dismiss();
        }
        if (searchThread != null) {
            searchThread.interrupt();
            searchThread = null;
        }
        super.onPause();
    }

    @Override
    public void onClick(View v) {

    }

    private void showAllConversations() {
        showList(CurrentApplication.getInstance().getConversations());
    }

    private void showList(Map<String, Conversation> conversationMap) {
        if(null == adapter) {
            conversations = new ArrayList<>(conversationMap.values());
            adapter = new ConversationsAdapter(this, conversations);
            conversationsList.setAdapter(adapter);
        } else {
            findViewById(R.id.homeProgress).setVisibility(View.GONE);
            conversations.clear();
            conversations.addAll(conversationMap.values());
            adapter.notifyDataSetChanged();
        }
    }

    private void updateList() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void showLeaveDialog(final Conversation conversation) {
        if (leaveDialog == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    leaveDialog.dismiss();
                }
            });
            leaveDialog = builder.create();
        }
        if (conversation.getChannel().getOwnerId().equals(User.getCurrentUserId())) {
            leaveDialog.setMessage("Are you sure that you want to delete conversation");
            leaveDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    findViewById(R.id.homeProgress).setVisibility(View.VISIBLE);
                    ChannelHelper.getInstance().deleteChannel(conversation, new ChannelHelper.OnLeaveChannelListener() {
                        @Override
                        public void onSuccess() {
                            findViewById(R.id.homeProgress).setVisibility(View.GONE);
                            showAllConversations();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            findViewById(R.id.homeProgress).setVisibility(View.GONE);
                            showMessage("Can't delete the conversation");
                        }
                    });
                    leaveDialog.dismiss();
                }
            });
        } else {
            leaveDialog.setMessage("Are you sure that you want to leave conversation");
            leaveDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Leave", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    findViewById(R.id.homeProgress).setVisibility(View.VISIBLE);
                    ChannelHelper.getInstance().unsubscribeFromChannel(conversation, new ChannelHelper.OnLeaveChannelListener() {
                        @Override
                        public void onSuccess() {
                            findViewById(R.id.homeProgress).setVisibility(View.GONE);
                            showAllConversations();
                        }

                        @Override
                        public void onFailure(Throwable throwable) {
                            findViewById(R.id.homeProgress).setVisibility(View.GONE);
                            showMessage("Can't leave the conversation");
                        }
                    });
                    leaveDialog.dismiss();
                }
            });
        }
        leaveDialog.show();
    }

    private void searchMessage(final String query) {
        final Map<String, Conversation> searchResult = new LinkedHashMap<>();
        for (Conversation conversation : CurrentApplication.getInstance().getConversations().values()) {
            for (Message message : conversation.getMessages()) {
                if (message.getText() != null && message.getText().toLowerCase().contains(query.toLowerCase())) {
                    searchResult.put(conversation.getChannel().getName(), conversation);
                }
            }
        }
        showList(searchResult);
    }

    private ChannelHelper.OnReadChannelInfoListener readChannelInfoListener = new ChannelHelper.OnReadChannelInfoListener() {
        @Override
        public void onSuccessFinish(Conversation lastConversation) {
            findViewById(R.id.homeProgress).setVisibility(View.GONE);
            showList(CurrentApplication.getInstance().getConversations());
        }

        @Override
        public void onFailure(Throwable throwable) {
            findViewById(R.id.homeProgress).setVisibility(View.GONE);
        }
    };

    private UserHelper.OnLogoutListener logoutListener = new UserHelper.OnLogoutListener() {
        @Override
        public void onSuccess() {
            startActivity(new Intent(HomeActivity.this, LoginActivity.class));
            finish();
        }

        @Override
        public void onFailedLogin(ApiError apiError) {
            showMessage("Can't sign out");
        }
    };

    private MMX.EventListener eventListener = new MMX.EventListener() {
        @Override
        public boolean onMessageReceived(MMXMessage mmxMessage) {
            Logger.debug("onMessageReceived");
            showAllConversations();
            return false;
        }

        @Override
        public boolean onMessageAcknowledgementReceived(User from, String messageId) {
            Logger.debug("onMessageAcknowledgementReceived");
            updateList();
            return false;
        }

        @Override
        public boolean onInviteReceived(MMXChannel.MMXInvite invite) {
            Logger.debug("onInviteReceived");
            updateList();
            return false;
        }

        @Override
        public boolean onInviteResponseReceived(MMXChannel.MMXInviteResponse inviteResponse) {
            Logger.debug("onInviteResponseReceived");
            updateList();
            return false;
        }

        @Override
        public boolean onMessageSendError(String messageId, MMXMessage.FailureCode code, String text) {
            Logger.debug("onMessageSendError");
            updateList();
            return false;
        }
    };

    private BroadcastReceiver onAddedConversation = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            showAllConversations();
        }
    };

}
