package com.tricon.labs.crumbs.Fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.tricon.labs.crumbs.R;
import com.tricon.labs.crumbs.activities.lendandborrow.ActivityLendAndBorrowIndividual;
import com.tricon.labs.crumbs.adapters.AdapterLendAndBorrow;
import com.tricon.labs.crumbs.database.DBHelper;
import com.tricon.labs.crumbs.models.Person;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class FragmentLendAndBorrow extends Fragment {

    private TextView mTVGive;
    private TextView mTVGet;

    private DBHelper mDBHelper;
    private List<Person> mPersonList = new ArrayList<>();
    private AdapterLendAndBorrow mAdapter;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        //setup views
        View mRootView = inflater.inflate(R.layout.fragment_lend_and_borrow, container, false);
        mTVGive = (TextView) mRootView.findViewById(R.id.tv_give_amt);
        mTVGet = (TextView) mRootView.findViewById(R.id.tv_get_amt);
        ListView lvPersons = (ListView) mRootView.findViewById(R.id.lv_persons);

        //get db instance
        mDBHelper = DBHelper.getInstance(getActivity());

        //set list view adapter
        mAdapter = new AdapterLendAndBorrow(mPersonList);
        lvPersons.setAdapter(mAdapter);

        //set empty view
        lvPersons.setEmptyView(mRootView.findViewById(android.R.id.empty));

        //set listeners
        lvPersons.setOnItemClickListener(new ListItemClicked());

        return mRootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        populateListViewFromDB();
    }

    private class ListItemClicked implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Person person = mPersonList.get(position);
            Intent i = new Intent(getActivity(), ActivityLendAndBorrowIndividual.class);
            i.putExtra("USERID", person.id);
            i.putExtra("USERNAME", person.name);
            startActivity(i);
        }
    }

    private void populateListViewFromDB() {
        //Todo :- need to implement pagination
        mPersonList.clear();
        mPersonList.addAll(mDBHelper.getLendAndBorrowList());
        mAdapter.notifyDataSetChanged();

        Map<String, String> finalResult = mDBHelper.getFinalResult();
        mTVGive.setText(finalResult.get("amt_toGive"));
        mTVGet.setText(finalResult.get("amt_toGet"));
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        //Saving data while orientation changes
        super.onSaveInstanceState(outState);
    }
}
