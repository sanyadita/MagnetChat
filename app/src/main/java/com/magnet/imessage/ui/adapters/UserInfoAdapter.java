package com.magnet.imessage.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.magnet.imessage.R;
import com.magnet.mmx.client.internal.channel.UserInfo;

import java.util.List;

public class UserInfoAdapter extends ArrayAdapter<UserInfo> {

    private LayoutInflater inflater;
    private AddUserListener addUser;
    private boolean canAddUser;

    private class ViewHolder {
        ImageView icon;
        TextView firstName;
        TextView lastName;
    }

    public interface AddUserListener {
        void addUser();
    }

    public UserInfoAdapter(Context context, List<UserInfo> users, boolean canAddUser, AddUserListener addUser) {
        super(context, R.layout.item_user, users);
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.addUser = addUser;
        this.canAddUser = canAddUser;
    }

    @Override
    public int getCount() {
        if (canAddUser) {
            return super.getCount() + 1;
        }
        return super.getCount();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (canAddUser && position == getCount() - 1) {
            View addUserView = inflater.inflate(R.layout.item_add_user, parent, false);
            addUserView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (addUser != null)
                        addUser.addUser();
                }
            });
            return addUserView;
        } else {
            UserInfo userInfo = getItem(position);
            final ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.item_user, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.itemUserIcon);
                viewHolder.firstName = (TextView) convertView.findViewById(R.id.itemUserFirstName);
                viewHolder.lastName = (TextView) convertView.findViewById(R.id.itemUserLastName);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }
            if (userInfo.getDisplayName() != null) {
                viewHolder.firstName.setText(userInfo.getDisplayName());
            }
        }
        return convertView;
    }

}
