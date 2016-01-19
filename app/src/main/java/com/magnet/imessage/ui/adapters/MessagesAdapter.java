package com.magnet.imessage.ui.adapters;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.magnet.imessage.R;
import com.magnet.imessage.helpers.DateHelper;
import com.magnet.imessage.model.Message;
import com.magnet.max.android.User;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<Message> messageList;
    private List<Integer> firstMsgIdxs;
    private List<String> dates;
    private Context context;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        LinearLayout messageArea;
        TextView date;
        TextView sender;
        TextView text;
        TextView delivered;
        ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            this.messageArea = (LinearLayout) itemView.findViewById(R.id.itemMessageArea);
            this.date = (TextView) itemView.findViewById(R.id.itemMessageDate);
            this.sender = (TextView) itemView.findViewById(R.id.itemMessageSender);
            this.image = (ImageView) itemView.findViewById(R.id.itemMessageImage);
            this.text = (TextView) itemView.findViewById(R.id.itemMessageText);
            this.delivered = (TextView) itemView.findViewById(R.id.itemMessageDelivered);
        }
    }

    public MessagesAdapter(Context context, List<Message> messages) {
        inflater = LayoutInflater.from(context);
        this.messageList = messages;
        this.context = context;
        firstMsgIdxs = new ArrayList<>();
        dates = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            String msgDay = DateHelper.getMessageDay(messages.get(i).getCreateTime());
            if (!dates.contains(msgDay)) {
                firstMsgIdxs.add(i);
                dates.add(msgDay);
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (position >= getItemCount()) {
            return;
        }
        Message message = getItem(position);
        String msgDay = DateHelper.getMessageDay(message.getCreateTime());
        if (!dates.contains(msgDay)) {
            firstMsgIdxs.add(position);
            dates.add(msgDay);
        }
        if (firstMsgIdxs.contains(position)) {
            holder.date.setVisibility(View.VISIBLE);
            holder.date.setText(String.format("%s %s", msgDay, DateHelper.getTime(message.getCreateTime())));
        } else {
            holder.date.setVisibility(View.GONE);
        }
        if (message.getSender() == null || User.getCurrentUserId().equals(message.getSender().getUserId())) {
            makeMessageFromMe(holder, message);
        } else {
            makeMessageToMe(holder, message);
        }
        if (message.getType() != null) {
            switch (message.getType()) {
                case Message.TYPE_MAP:
                    holder.text.setVisibility(View.GONE);
                    holder.image.setVisibility(View.VISIBLE);
                    String loc = "http://maps.google.com/maps/api/staticmap?center=" + message.getLatitudeLongitude() + "&zoom=18&size=700x300&sensor=false&markers=color:blue%7Clabel:S%7C" + message.getLatitudeLongitude();
//                holder.image.setImageURI(Uri.parse(loc));
                    Picasso.with(context).load(loc).into(holder.image);
                    break;
                case Message.TYPE_VIDEO:
                    holder.text.setVisibility(View.GONE);
                    holder.image.setVisibility(View.VISIBLE);
                    break;
                case Message.TYPE_PHOTO:
                    holder.text.setVisibility(View.GONE);
                    holder.image.setVisibility(View.VISIBLE);
                    break;
                case Message.TYPE_TEXT:
                    holder.text.setText(message.getText());
                    holder.text.setVisibility(View.VISIBLE);
                    holder.image.setVisibility(View.GONE);
                    break;
            }
        } else {
            holder.text.setText(message.getText());
            holder.text.setVisibility(View.VISIBLE);
            holder.image.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private Message getItem(int position) {
        return messageList.get(position);
    }

    private void makeMessageToMe(ViewHolder viewHolder, Message message) {
        viewHolder.messageArea.setGravity(Gravity.LEFT | Gravity.START);
        viewHolder.text.setBackgroundResource(R.drawable.msg_received);
        viewHolder.text.setTextColor(Color.BLACK);
        viewHolder.delivered.setVisibility(View.GONE);
        if (message.getSender() != null) {
            viewHolder.sender.setText(message.getSender().getDisplayName());
        }
        viewHolder.sender.setVisibility(View.VISIBLE);
    }

    private void makeMessageFromMe(ViewHolder viewHolder, Message message) {
        viewHolder.messageArea.setGravity(Gravity.RIGHT | Gravity.END);
        viewHolder.text.setBackgroundResource(R.drawable.msg_sent);
        viewHolder.text.setTextColor(Color.WHITE);
        viewHolder.sender.setVisibility(View.GONE);
        if (message.isDelivered()) {
            viewHolder.delivered.setVisibility(View.VISIBLE);
        } else {
            viewHolder.delivered.setVisibility(View.GONE);
        }
    }

}
