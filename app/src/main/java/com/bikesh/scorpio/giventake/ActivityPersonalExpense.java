package com.bikesh.scorpio.giventake;

import android.content.Context;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Random;


public class ActivityPersonalExpense extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //loading templet xml
        setContentView(R.layout.main_template);


        //setting up toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);

        //setting up navigation drawer
        GiveNTakeApplication AC = (GiveNTakeApplication)getApplicationContext();
        View view = getWindow().getDecorView().findViewById(android.R.id.content);
        AC.setupDrawer(view, ActivityPersonalExpense.this, toolbar );

        //loading home activity templet in to template frame
        FrameLayout frame = (FrameLayout) findViewById(R.id.mainFrame);
        frame.removeAllViews();
        Context darkTheme = new ContextThemeWrapper(this, R.style.AppTheme);
        LayoutInflater inflater = (LayoutInflater) darkTheme.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View ActivityPersonalExpenseView=  inflater.inflate(R.layout.activity_personal_expense, null);



        frame.addView(ActivityPersonalExpenseView);


        ListView listView = (ListView) ActivityPersonalExpenseView.findViewById(R.id.listViewFromDB);

        String[] values = new String[] {
                "grocery shopping",
                "Outside food",
                "Mobile recharge",
                "Travelling ",
                "Petrol",
                "Other Expenses",
        };


       MySimpleArrayAdapter adapter = new MySimpleArrayAdapter(ActivityPersonalExpense.this, values);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new listItemClicked());



        ((ImageButton)ActivityPersonalExpenseView.findViewById(R.id.addEntry)).setOnClickListener(new openAddnewEntrry());
        ((ImageButton)ActivityPersonalExpenseView.findViewById(R.id.addExpenseGroup)).setOnClickListener(new openAddnewGroup());

    }

    private class listItemClicked implements android.widget.AdapterView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Intent i = new Intent(ActivityPersonalExpense.this, ActivityPersonalExpenseIndividual.class);
            startActivity(i);
        }
    }


    private class openAddnewEntrry implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            Intent i = new Intent(ActivityPersonalExpense.this, ActivityAddEntry.class);
            i.putExtra("fromActivity", "ActivityPersonalExpense");
            startActivity(i);

        }
    }
    private class openAddnewGroup implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            Intent i = new Intent(ActivityPersonalExpense.this, ActivityAddGroup.class);
            i.putExtra("fromActivity", "ActivityPersonalExpense");
            startActivity(i);

        }
    }

    class MySimpleArrayAdapter extends ArrayAdapter<String> {
        private final Context context;
        private final String[] values;

        public MySimpleArrayAdapter(Context context, String[] values) {
            super(context, R.layout.item_lend_and_borrow, values);
            this.context = context;
            this.values = values;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            // inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //View rowView = inflater.inflate(R.layout.item_lend_and_borrow, parent, false);

            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater.inflate(R.layout.item_lend_and_borrow, null);

            TextView textView = (TextView) rowView.findViewById(R.id.item_name);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.item_icon);
            textView.setText(values[position]);
            // change the icon for Windows and iPhone
            String s = values[position];
            //if (s.startsWith("iPhone")) {
            //    imageView.setImageResource(R.drawable.gear);
            //} else {
            imageView.setImageResource(R.drawable.marker);
            //}

            Random r = new Random();
            //rand.nextInt((max - min) + 1) + min;
            int amt = r.nextInt((500 - 80) + 1) + 80;
            TextView textPrice = (TextView) rowView.findViewById(R.id.item_studentnum);
            textPrice.setText(""+amt);

            Log.d("Log", "=============" + position);

            return rowView;
        }


    }




















    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_personal_expense, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




}
