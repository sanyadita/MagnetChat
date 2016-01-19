package com.magnet.imessage.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<MessagesAdapter.ViewHolder> {

    private LayoutInflater inflater;
    private List<Message> messageList;
    private List<Integer> firstMsgIdxs;
    private List<String> dates;
    private Context context;

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        LinearLayout messageArea;
        TextView date;
        TextView sender;
        TextView text;
        TextView delivered;
        ImageView image;
        Message message;

        public ViewHolder(View itemView) {
            super(itemView);
            this.messageArea = (LinearLayout) itemView.findViewById(R.id.itemMessageArea);
            this.date = (TextView) itemView.findViewById(R.id.itemMessageDate);
            this.sender = (TextView) itemView.findViewById(R.id.itemMessageSender);
            this.image = (ImageView) itemView.findViewById(R.id.itemMessageImage);
            this.text = (TextView) itemView.findViewById(R.id.itemMessageText);
            this.delivered = (TextView) itemView.findViewById(R.id.itemMessageDelivered);
            this.image.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            Intent intent;
            if (message.getType() != null) {
                switch (message.getType()) {
                    case Message.TYPE_MAP:
                        String uri = String.format(Locale.ENGLISH, "geo:%s", message.getLatitudeLongitude());
                        intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        context.startActivity(intent);
                        break;
                    case Message.TYPE_VIDEO:
                        String newVideoPath = message.getUrl();
                        if (newVideoPath != null) {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newVideoPath));
                            intent.setDataAndType(Uri.parse(newVideoPath), "video/*");
                            context.startActivity(intent);
                        }
                        break;
                    case Message.TYPE_PHOTO:
                        String newImagePath = message.getUrl();
                        if (newImagePath != null) {
                            intent = new Intent(Intent.ACTION_VIEW, Uri.parse(newImagePath));
                            intent.setDataAndType(Uri.parse(newImagePath), "image/*");
                            context.startActivity(intent);
                        }
                        break;
                }
            }
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
        holder.message = message;
        configureDate(holder, message, position);
        if (message.getSender() == null || User.getCurrentUserId().equals(message.getSender().getUserId())) {
            makeMessageFromMe(holder, message);
        } else {
            makeMessageToMe(holder, message);
        }
        if (message.getType() != null) {
            switch (message.getType()) {
                case Message.TYPE_MAP:
                    configureMapMsg(holder, message);
                    break;
                case Message.TYPE_VIDEO:
                    configureVideoMsg(holder, message);
                    break;
                case Message.TYPE_PHOTO:
                    configureImageMsg(holder,message);
                    break;
                case Message.TYPE_TEXT:
                    configureTextMsg(holder, message);
                    break;
            }
        } else {
            configureTextMsg(holder, message);
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    private Message getItem(int position) {
        return messageList.get(position);
    }

    private void configureDate(ViewHolder viewHolder, Message message, int position) {
        String msgDay = DateHelper.getMessageDay(message.getCreateTime());
        if (!dates.contains(msgDay)) {
            firstMsgIdxs.add(position);
            dates.add(msgDay);
        }
        if (firstMsgIdxs.contains(position)) {
            viewHolder.date.setVisibility(View.VISIBLE);
            viewHolder.date.setText(String.format("%s %s", msgDay, DateHelper.getTime(message.getCreateTime())));
        } else {
            viewHolder.date.setVisibility(View.GONE);
        }
    }

    private void makeMessageToMe(ViewHolder viewHolder, Message message) {
        viewHolder.messageArea.setGravity(Gravity.LEFT | Gravity.START);
        viewHolder.image.setLayoutParams(viewHolder.messageArea.getLayoutParams());
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

    private void configureMediaMsg(ViewHolder holder) {
        holder.text.setVisibility(View.GONE);
        holder.image.setVisibility(View.VISIBLE);
    }

    private void configureMapMsg(ViewHolder holder, Message message) {
        configureMediaMsg(holder);
        String loc = "http://maps.google.com/maps/api/staticmap?center=" + message.getLatitudeLongitude() + "&zoom=18&size=700x300&sensor=false&markers=color:blue%7Clabel:S%7C" + message.getLatitudeLongitude();
        Picasso.with(context).load(loc).into(holder.image);
    }

    private void configureVideoMsg(ViewHolder holder, Message message) {
        configureMediaMsg(holder);
        holder.image.setImageResource(R.drawable.video_message);
    }

    private void configureImageMsg(ViewHolder holder, Message message) {
        configureMediaMsg(holder);
        if (message.getUrl() != null) {
            Picasso.with(context).load(message.getUrl()).into(holder.image);
        }
    }

    private void configureTextMsg(ViewHolder holder, Message message) {
        holder.text.setText(message.getText());
        holder.text.setVisibility(View.VISIBLE);
        holder.image.setVisibility(View.GONE);
    }

}
