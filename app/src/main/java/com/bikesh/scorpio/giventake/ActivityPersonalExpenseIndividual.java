package com.bikesh.scorpio.giventake;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bikesh.scorpio.giventake.model.DBHelper;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class ActivityPersonalExpenseIndividual extends ActionBarActivity {


    View personalExpenseIndividualView;
    DBHelper myDb;
    String fromActivity=null;
    int  colId=0;
    String colName="";

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
        AC.setupDrawer(view, ActivityPersonalExpenseIndividual.this, toolbar);

        //loading home activity templet in to template frame
        FrameLayout frame = (FrameLayout) findViewById(R.id.mainFrame);
        frame.removeAllViews();
        Context darkTheme = new ContextThemeWrapper(this, R.style.AppTheme);
        LayoutInflater inflater = (LayoutInflater) darkTheme.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        personalExpenseIndividualView=  inflater.inflate(R.layout.activity_personal_expense_individual, null);

        frame.addView(personalExpenseIndividualView);


        myDb = new DBHelper(this);

        Bundle extras = getIntent().getExtras();

        if(extras == null) {
            fromActivity= null;
        } else {
            fromActivity= extras.getString("fromActivity");
            colId = Integer.parseInt(extras.getString("colId"));
            colName = extras.getString("colName");
        }


        SimpleDateFormat dmy = new SimpleDateFormat("MM-yyyy");
        String cDate = dmy.format(new Date());

        SimpleDateFormat dbmy = new SimpleDateFormat("yyyy-MM");
        String cdbDate = dbmy.format(new Date());

        TextView dateChanger= (TextView)personalExpenseIndividualView.findViewById(R.id.dateChanger);
        TextView dateChangerForDb= (TextView)personalExpenseIndividualView.findViewById(R.id.dateChangerForDb);

        dateChanger.setOnClickListener(new CustomDatePicker(ActivityPersonalExpenseIndividual.this, dateChanger, dateChangerForDb, true));

        dateChanger.setText(cDate);
        dateChangerForDb.setText(cdbDate);

        dateChangerForDb.addTextChangedListener(new dateChange());


        Cursor entrys =  myDb.getPersonalExpense(colId, cDate);
        generateTable(entrys);

        ((ImageButton)personalExpenseIndividualView.findViewById(R.id.addEntry)).setOnClickListener(new openAddnewEntrry());
    }


    @Override
    public void onBackPressed() {
        startActivity(new Intent(ActivityPersonalExpenseIndividual.this, ActivityPersonalExpense.class));
        finish();
    }

    //Todo :- handle back click , and goto prev activity page
    @Override
    public void onResume() {
        super.onResume();

        //Cursor entrys =  myDb.getPersonalExpense(colId, ((TextView) personalExpenseIndividualView.findViewById(R.id.dateChanger)).getText().toString());
        //generateTable(entrys);
    }

    private void generateTable(Cursor cursor) {

        TableLayout tableLayout = (TableLayout) personalExpenseIndividualView.findViewById(R.id.tableLayout);
        TableRow tr, th;
        boolean colorFlag=false;
        TextView tv;
        String fields[]={"created_date", "description",  "amt"};

        //setting headder
        String tablehead[]={"Date", "Description", "Amt"};
        tableLayout.removeAllViews();
        th = new TableRow(this);
        th.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));
        tr = new TableRow(this);
        tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));


        Log.i("bm info", "" + colId+ " cc "+ cursor.getCount());
        Log.i("bm info", "" + tablehead.length);


        for (int i=0 ;i< tablehead.length; i++) {
            tv = generateTextview();
            tv.setText(tablehead[i]);
            tv.setTypeface(null, Typeface.BOLD);
            tr.addView(tv);
        }
        tableLayout.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        //----

        while(cursor.isAfterLast() == false){

            tr = new TableRow(this);
            tr.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT));

            tr.setClickable(true);
            tr.setOnClickListener(new tableRowClicked(Integer.parseInt(cursor.getString(cursor.getColumnIndex("_id")))));

            Log.i("bm info", "" + fields.length);

            for (int i=0 ;i<fields.length; i++) {

                tv = generateTextview();

                //changing date format
                if(fields[i].equals("created_date")){

                    try {

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//set format of date you receiving from db
                        Date date = (Date) sdf.parse(  cursor.getString(cursor.getColumnIndex(fields[i]))  );
                        SimpleDateFormat newDate = new SimpleDateFormat("dd-MM-yyyy");//set format of new date
                        tv.setText(""+ newDate.format(date) );
                    }
                    catch(ParseException pe) {
                        tv.setText(cursor.getString(cursor.getColumnIndex(fields[i])));
                    }
                }
                else {
                    tv.setText(cursor.getString(cursor.getColumnIndex(fields[i])));
                }

                tr.addView(tv);
            }

            cursor.moveToNext();

            if(colorFlag){
                tr.setBackgroundColor(Color.rgb(240, 242, 242));
                colorFlag=false;
            }
            else {
                tr.setBackgroundColor(Color.rgb(234, 237, 237));
                colorFlag=true;
            }
            // Add row to TableLayout.
            tableLayout.addView(tr, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
        }





        float amtHolder;
        amtHolder = myDb.getMonthTotalOfPersonalExpenseIndividual(colId, ((TextView) personalExpenseIndividualView.findViewById(R.id.dateChanger)).getText().toString());
        ((TextView)personalExpenseIndividualView.findViewById(R.id.monthlyTotal)).setText(": "+amtHolder);

        /*
        amtHolder = myDb.getMonthTotalOfGet(userId, ((TextView) lendAndBorrowPersonalView.findViewById(R.id.dateChanger)).getText().toString() );
        ((TextView)lendAndBorrowPersonalView.findViewById(R.id.monthTotalToGet)).setText(": " + amtHolder);

        float balanceAmt=  myDb.getTotalBalance(userId);

        ((TextView)lendAndBorrowPersonalView.findViewById(R.id.balanceAmt)).setText(": "+balanceAmt);

        if(balanceAmt<0){
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.balanceAmtLabel)).setText("Balance amount get from him/her");
            balanceAmt=balanceAmt*-1;
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.balanceAmt)).setText(": "+balanceAmt);
        }
        else if (balanceAmt>0){
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.balanceAmtLabel)).setText("Balance amount give to him/her");
        }
        else{
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.balanceAmtLabel)).setText("Balance amount");
        }


        amtHolder = myDb.getPrevBalance(userId, ((TextView)lendAndBorrowPersonalView.findViewById(R.id.dateChanger)).getText().toString() );

        if(amtHolder<0){
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.prevBalanceAmtLabel)).setText("Previous balance amount get from him/her");
            amtHolder=amtHolder*-1;
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.prevBalanceAmt)).setText(": "+amtHolder);
        }
        else if (amtHolder>0){
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.prevBalanceAmtLabel)).setText("Previous balance amount give to him/her");
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.prevBalanceAmt)).setText(": "+amtHolder);
        }
        else{
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.prevBalanceAmtLabel)).setText("Previous balance amount");
            ((TextView)lendAndBorrowPersonalView.findViewById(R.id.prevBalanceAmt)).setText(": "+amtHolder);
        }
        */


    }

    private TextView generateTextview() {
        TextView tv = new TextView(this);
        tv.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1f));
        tv.setPadding(5, 5, 5, 5);
        tv.setClickable(false);
        return tv;
    }


    private class tableRowClicked implements View.OnClickListener {
        int rowId=0;
        public tableRowClicked(int id) {
            rowId=id;
        }

        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(), "clicked" + rowId, Toast.LENGTH_LONG).show();
        }
    }



    private class dateChange implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s) {
            Toast.makeText(getApplicationContext(),""+((TextView)personalExpenseIndividualView.findViewById(R.id.dateChangerForDb)).getText(),Toast.LENGTH_LONG).show();

            Cursor entrys =  myDb.getPersonalExpense(colId, ((TextView) personalExpenseIndividualView.findViewById(R.id.dateChanger)).getText().toString() );

            generateTable(entrys);

        }
    }


    private class openAddnewEntrry implements View.OnClickListener {
        @Override
        public void onClick(View v) {

            Intent i = new Intent(ActivityPersonalExpenseIndividual.this, ActivityAddEntry.class);
            i.putExtra("fromActivity", "ActivityPersonalExpenseIndividual");
            i.putExtra("ID", ""+colId );
            i.putExtra("Name",  colName );
            startActivity(i);

        }
    }






















    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_personal_expense_individual, menu);
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
