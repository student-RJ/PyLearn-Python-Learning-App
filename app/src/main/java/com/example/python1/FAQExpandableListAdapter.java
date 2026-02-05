package com.example.python1;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

class FAQExpandableListAdapter extends BaseExpandableListAdapter {

    private final List<String> questions;
    private final HashMap<String, List<String>> answers;

    public FAQExpandableListAdapter(List<String> questions, HashMap<String, List<String>> answers) {
        this.questions = questions;
        this.answers = answers;
    }

    @Override
    public int getGroupCount() {
        return questions.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return answers.get(questions.get(groupPosition)).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return questions.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return answers.get(questions.get(groupPosition)).get(childPosition);

    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        String question = (String) getGroup(groupPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_expandable_list_item_1, parent, false);
        }

        TextView textView = (TextView) convertView.findViewById(android.R.id.text1);
        textView.setText(question);
        textView.setTextSize(16f);
        textView.setPadding(60, 24, 24, 24);
        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild,
                             View convertView, ViewGroup parent) {
        String answer = (String) getChild(groupPosition, childPosition);

        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.child_faq, parent, false);
        }

        TextView textView = convertView.findViewById(R.id.faq_child);
        textView.setText(answer);

        return convertView;
    }


    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }
}

