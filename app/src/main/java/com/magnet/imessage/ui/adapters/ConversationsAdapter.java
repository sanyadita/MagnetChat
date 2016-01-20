package com.magnet.imessage.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.magnet.imessage.R;
import com.magnet.imessage.helpers.DateHelper;
import com.magnet.imessage.helpers.UserHelper;
import com.magnet.imessage.model.Conversation;
import com.magnet.imessage.model.Message;
import com.magnet.max.android.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConversationsAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private Map<String, Conversation> conversationMap;
    private ArrayList<String> keys;

    private class ConversationViewHolder {
        ImageView newMessage;
        ImageView icon;
        TextView users;
        TextView date;
        TextView lastMessage;
    }

    public ConversationsAdapter(Context context, Map<String, Conversation> conversations) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.conversationMap = conversations;
        keys = new ArrayList<>(conversations.keySet());
    }

    @Override
    public int getCount() {
        return conversationMap.size();
    }

    @Override
    public Conversation getItem(int position) {
        return conversationMap.get(keys.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void notifyDataSetChanged() {
        keys = new ArrayList<>(conversationMap.keySet());
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final ConversationViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_conversation, parent, false);
            viewHolder = new ConversationViewHolder();
            viewHolder.newMessage = (ImageView) convertView.findViewById(R.id.itemConversationNewMsg);
            viewHolder.icon = (ImageView) convertView.findViewById(R.id.itemConversationIcon);
            viewHolder.users = (TextView) convertView.findViewById(R.id.itemConversationUsers);
            viewHolder.date = (TextView) convertView.findViewById(R.id.itemConversationDate);
            viewHolder.lastMessage = (TextView) convertView.findViewById(R.id.itemConversationLastMsg);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ConversationViewHolder) convertView.getTag();
        }
        if (position >= getCount()) {
            return convertView;
        }
        Conversation conversation = getItem(position);
        if (conversation.getSuppliers() != null) {
            if (conversation.getSuppliers().size() == 0) {
                User currentUser = User.getCurrentUser();
                viewHolder.users.setText(String.format("%s %s", currentUser.getFirstName(), currentUser.getLastName()));
            } else {
                String suppliers = UserHelper.getInstance().userNamesAsString(conversation.getSuppliersList());
                viewHolder.users.setText(suppliers);
                if (conversation.getSuppliers().size() > 1) {
                    viewHolder.icon.setImageResource(R.mipmap.ic_many);
                } else {
                    viewHolder.icon.setImageResource(R.mipmap.ic_one);
                }
            }
        }
        if (conversation.hasUnreadMessage()) {
            viewHolder.newMessage.setVisibility(View.VISIBLE);
        } else {
            viewHolder.newMessage.setVisibility(View.INVISIBLE);
        }
        viewHolder.date.setText(DateHelper.getConversationLastDate(conversation.getChannel().getLastTimeActive()));
        List<Message> messages = conversation.getMessages();
        if (messages != null && messages.size() > 0) {
            Message message = messages.get(messages.size() - 1);
            if (message.getType() != null) {
                switch (message.getType()) {
                    case Message.TYPE_MAP:
                        viewHolder.lastMessage.setText("User's location");
                        break;
                    case Message.TYPE_VIDEO:
                        viewHolder.lastMessage.setText("User's video");
                        break;
                    case Message.TYPE_PHOTO:
                        viewHolder.lastMessage.setText("User's photo");
                        break;
                    case Message.TYPE_TEXT:
                        viewHolder.lastMessage.setText(message.getText());
                        break;
                }
            } else {
                viewHolder.lastMessage.setText(message.getText());
            }
        }
        return convertView;
    }

}