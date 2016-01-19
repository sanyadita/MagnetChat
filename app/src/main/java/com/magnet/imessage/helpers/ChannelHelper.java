package com.magnet.imessage.helpers;

import android.content.Intent;

import com.magnet.imessage.core.CurrentApplication;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.model.Message;
import com.magnet.imessage.util.Logger;
import com.magnet.max.android.ApiCallback;
import com.magnet.max.android.ApiError;
import com.magnet.max.android.User;
import com.magnet.mmx.client.api.ChannelMatchType;
import com.magnet.mmx.client.api.ListResult;
import com.magnet.mmx.client.api.MMXChannel;
import com.magnet.mmx.client.api.MMXMessage;
import com.magnet.mmx.client.internal.channel.ChannelSummaryResponse;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChannelHelper {

    public static final String ACTION_ADDED_CONVERSATION = "com.magnet.imessage.ADDED_CONVERSATION";

    private static ChannelHelper instance;

    public interface OnReadChannelInfoListener {
        void onSuccessFinish(Conversation conversation);

        void onFailure(Throwable throwable);
    }

    public interface OnCreateChannelListener {
        void onSuccessCreated(MMXChannel channel);

        void onChannelExists(MMXChannel channel);

        void onFailureCreated(Throwable throwable);
    }

    public interface OnLeaveChannelListener {
        void onSuccess();

        void onFailure(Throwable throwable);
    }

    public interface OnFindChannelByUsersListener {
        void onSuccessFound(List<MMXChannel> mmxChannels);

        void onFailure(Throwable throwable);
    }

    public interface OnAddUserListener {
        void onSuccessAdded();

        void onUserSetExists(String channelSetName);

        void onWasAlreadyAdded();

        void onFailure(Throwable throwable);
    }

    private ChannelHelper() {

    }

    public static ChannelHelper getInstance() {
        if (instance == null) {
            instance = new ChannelHelper();
        }
        return instance;
    }

    public void readConversations(final OnReadChannelInfoListener listener) {
        MMXChannel.getAllSubscriptions(new MMXChannel.OnFinishedListener<List<MMXChannel>>() {
            @Override
            public void onSuccess(List<MMXChannel> channels) {
                Logger.debug("read conversations", "success");
                readChannelsInfo(channels, listener);
            }

            @Override
            public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                Logger.error("read conversations", throwable);
                listener.onFailure(throwable);
            }
        });
    }

    public void readChannelInfo(MMXChannel channel, final OnReadChannelInfoListener listener) {
        List<MMXChannel> channelList = new ArrayList<>(1);
        channelList.add(channel);
        readChannelsInfo(channelList, listener);
    }

    public void readChannelsInfo(List<MMXChannel> channels, final OnReadChannelInfoListener listener) {
        final Map<String, MMXChannel> channelMap = new HashMap<>(channels.size());
        for (MMXChannel channel : channels) {
            channelMap.put(channel.getName(), channel);
        }
        MMXChannel.getChannelSummary(new HashSet<>(channels), 1000, 100, new MMXChannel.OnFinishedListener<List<ChannelSummaryResponse>>() {
            @Override
            public void onSuccess(List<ChannelSummaryResponse> channelSummaryResponses) {
                Conversation lastConversation = null;
                for (ChannelSummaryResponse channelResponse : channelSummaryResponses) {
                    Conversation conversation = new Conversation();
                    conversation.setChannel(channelMap.get(channelResponse.getChannelName()));
                    List<UserInfo> infoList = new ArrayList<>(channelResponse.getPublishedItemCount());
                    for (UserInfo info : channelResponse.getSubscribers()) {
                        if (!info.getUserId().equals(User.getCurrentUserId())) {
                            infoList.add(info);
                        }
                    }
                    conversation.setSuppliers(infoList);
                    conversation.setMessages(channelResponse.getMessages());
                    CurrentApplication.getInstance().addConversation(channelResponse.getChannelName(), conversation);
                    CurrentApplication.getInstance().sendBroadcast(new Intent(ACTION_ADDED_CONVERSATION));
                    lastConversation = conversation;
                }
                listener.onSuccessFinish(lastConversation);
            }

            @Override
            public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                Logger.error("read conversations", throwable);
                listener.onFailure(throwable);
            }
        });
    }

    public void addUserToConversation(final Conversation conversation, final User user, final OnAddUserListener listener) {
        if (conversation.getSuppliers().get(user.getUserIdentifier()) == null) {
            List<String> userInConversation = new ArrayList<>();
            for (UserInfo userInfo : conversation.getSuppliers().values()) {
                userInConversation.add(userInfo.getUserId());
            }
            userInConversation.add(User.getCurrentUserId());
            userInConversation.add(user.getUserIdentifier());
            findChannelByUsers(userInConversation, new OnFindChannelByUsersListener() {
                @Override
                public void onSuccessFound(final List<MMXChannel> mmxChannels) {
                    if (mmxChannels.size() == 0) {
                        Set<User> userSet = new HashSet<>();
                        userSet.add(user);
                        conversation.getChannel().addSubscribers(userSet, new MMXChannel.OnFinishedListener<List<String>>() {
                            @Override
                            public void onSuccess(List<String> strings) {
                                Logger.debug("add user", "success");
                                conversation.addSupplier(user);
                                listener.onSuccessAdded();
                            }

                            @Override
                            public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                                Logger.error("add user", throwable);
                                listener.onFailure(throwable);
                            }
                        });
                    } else {
                        conversation.getChannel().delete(new MMXChannel.OnFinishedListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Logger.debug("delete old", "success");
                                String channelName = conversation.getChannel().getName();
                                CurrentApplication.getInstance().getConversations().remove(channelName);
                                listener.onUserSetExists(mmxChannels.get(0).getName());
                            }

                            @Override
                            public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                                Logger.error("add user", throwable);
                                listener.onFailure(throwable);
                            }
                        });
                    }
                }

                @Override
                public void onFailure(Throwable throwable) {
                    listener.onFailure(throwable);
                }
            });
        } else {
            listener.onWasAlreadyAdded();
        }
    }

    public String getNameForChannel() {
        return DateHelper.getDateWithoutSpaces();
    }

    public void createChannelForUsers(final String userId, final OnCreateChannelListener listener) {
        findChannelByUsers(Arrays.asList(userId, User.getCurrentUserId()), new OnFindChannelByUsersListener() {
            @Override
            public void onSuccessFound(List<MMXChannel> mmxChannels) {
                if (mmxChannels.size() > 0) {
                    Logger.debug("channel exists");
                    listener.onChannelExists(mmxChannels.get(0));
                } else {
                    Set<String> users = new HashSet<>();
                    users.add(userId);
                    String summary = User.getCurrentUser().getUserName();
                    MMXChannel.create(getNameForChannel(), summary, false, MMXChannel.PublishPermission.SUBSCRIBER, users, new MMXChannel.OnFinishedListener<MMXChannel>() {
                        @Override
                        public void onSuccess(MMXChannel channel) {
                            Logger.debug("create conversation", "success");
                            listener.onSuccessCreated(channel);
                        }

                        @Override
                        public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                            Logger.error("create conversation", throwable);
                            listener.onFailureCreated(throwable);
                        }
                    });
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                listener.onFailureCreated(throwable);
            }
        });
    }

    public void findChannelByUsers(List<String> userIds, final OnFindChannelByUsersListener listener) {
        User.getUsersByUserIds(userIds, new ApiCallback<List<User>>() {
            @Override
            public void success(List<User> userList) {
                MMXChannel.findChannelsBySubscribers(new HashSet<>(userList), ChannelMatchType.EXACT_MATCH, new MMXChannel.OnFinishedListener<ListResult<MMXChannel>>() {
                    @Override
                    public void onSuccess(ListResult<MMXChannel> mmxChannelListResult) {
                        listener.onSuccessFound(mmxChannelListResult.items);
                    }

                    @Override
                    public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                        Logger.error("find chan by users", throwable);
                        listener.onFailure(throwable);
                    }
                });
            }

            @Override
            public void failure(ApiError apiError) {
                Logger.error("find users by ids", apiError);
                listener.onFailure(apiError);
            }
        });
    }

    public void receiveMessage(MMXMessage mmxMessage) {
        if (mmxMessage.getChannel() != null) {
            Conversation conversation = CurrentApplication.getInstance().getConversationByName(mmxMessage.getChannel().getName());
            if (conversation != null) {
                Message message = Message.createMessageFrom(mmxMessage);
                conversation.addMessage(message);
                UserInfo sender = message.getSender();
                if (sender != null && sender.getUserId() != null) {
                    if (!sender.getUserId().equals(User.getCurrentUserId())) {
                        if (conversation.getSuppliers().get(sender.getUserId()) == null) {
                            conversation.addSupplier(sender);
                        }
                        conversation.setHasUnreadMessage(true);
                    }
                }
            } else {
                List<MMXChannel> mmxChannelList = new ArrayList<>();
                mmxChannelList.add(mmxMessage.getChannel());
                readChannelsInfo(mmxChannelList, new ChannelHelper.OnReadChannelInfoListener() {
                    @Override
                    public void onSuccessFinish(Conversation conversation) {
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                    }
                });
            }
        }
        Logger.debug("new message");
        mmxMessage.acknowledge(new MMXMessage.OnFinishedListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Logger.debug("acknowledge", "success");
            }

            @Override
            public void onFailure(MMXMessage.FailureCode failureCode, Throwable throwable) {
                Logger.error("acknowledge", throwable, "error");
            }
        });
    }

    public void unsubscribeFromChannel(final Conversation conversation, final OnLeaveChannelListener listener) {
        final MMXChannel channel = conversation.getChannel();
        if (channel != null) {
            channel.unsubscribe(new MMXChannel.OnFinishedListener<Boolean>() {
                @Override
                public void onSuccess(Boolean aBoolean) {
                    CurrentApplication.getInstance().getConversations().remove(channel.getName());
                    Logger.debug("unsubscribe", "success");
                    listener.onSuccess();
                }

                @Override
                public void onFailure(MMXChannel.FailureCode failureCode, Throwable throwable) {
                    Logger.error("unsubscribe", throwable);
                    listener.onFailure(throwable);
                }
            });
        }
    }

}
