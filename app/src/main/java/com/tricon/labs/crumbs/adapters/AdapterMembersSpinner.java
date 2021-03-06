package com.tricon.labs.crumbs.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.tricon.labs.crumbs.R;
import com.tricon.labs.crumbs.models.Contact;

import java.util.ArrayList;
import java.util.List;

public class AdapterMembersSpinner extends BaseAdapter {

    private List<Contact> mMembers;
    private LayoutInflater mInflater;

    public AdapterMembersSpinner(Context context, ArrayList<Contact> members) {
        mMembers = new ArrayList<>(members);
        this.mInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return mMembers.size();
    }

    @Override
    public Contact getItem(int position) {
        return mMembers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        Contact contact = getItem(position);
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.adapter_members_spinner, parent, false);
            holder = new ViewHolder();
            holder.tvName = (TextView) convertView.findViewById(R.id.tv_name);
            holder.tvPhone = (TextView) convertView.findViewById(R.id.tv_phone);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.tvName.setText(contact.name);
        holder.tvPhone.setText(contact.phone);

        return convertView;
    }

    private static class ViewHolder {
        TextView tvName;
        TextView tvPhone;
    }
}
