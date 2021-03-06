package com.tricon.labs.crumbs.activities.lendandborrow;

import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.tricon.labs.crumbs.R;
import com.tricon.labs.crumbs.adapters.AdapterContactList;
import com.tricon.labs.crumbs.database.DBHelper;
import com.tricon.labs.crumbs.models.Contact;
import com.tricon.labs.crumbs.models.LendAndBorrowEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import static com.tricon.labs.crumbs.libraries.Utils.getContactList;

public class ActivityAddOrEditEntry extends AppCompatActivity {

    private DBHelper mDBHelper;

    private LendAndBorrowEntry mLendAndBorrowEntry;

    private Contact mSelectedContact = null;

    private Button mBtnDate;
    private TextInputLayout mTILUserName;
    private TextInputLayout mTILAmount;
    private TextInputLayout mTILDescription;
    private AutoCompleteTextView mACTVUserName;
    private EditText mETAmount;
    private EditText mETDescription;
    private RadioGroup mRadioGroup;
    private RadioButton mRBLend;

    private ProgressDialog mPDSaveData;

    private static final int CREATE_ENTRY = 1;
    private static final int EDIT_ENTRY = 2;
    private static int ENTRY_TYPE = CREATE_ENTRY;

    int mSelectedUserID = 0;
    String mSelectedUserName = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lend_and_borrow_add_entry);

        overridePendingTransition(R.anim.slide_in_from_bottom, R.anim.slide_out_to_top);

        //setup toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.widget_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(android.support.v7.appcompat.R.drawable.abc_ic_clear_mtrl_alpha);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
        }

        //setup views
        mBtnDate = (Button) findViewById(R.id.btn_date);
        mRadioGroup = (RadioGroup) findViewById(R.id.rg_lend_and_borrow);
        mRBLend = (RadioButton) findViewById(R.id.rb_lend);
        RadioButton mRBBorrow = (RadioButton) findViewById(R.id.rb_borrow);

        mTILUserName = (TextInputLayout) findViewById(R.id.til_user_name);
        mTILAmount = (TextInputLayout) findViewById(R.id.til_amount);
        mTILDescription = (TextInputLayout) findViewById(R.id.til_description);
        mACTVUserName = (AutoCompleteTextView) findViewById(R.id.actv_user_name);
        mETAmount = (EditText) findViewById(R.id.et_amount);
        mETDescription = (EditText) findViewById(R.id.et_description);

        //set progress dialog
        mPDSaveData = new ProgressDialog(this);
        mPDSaveData.setCancelable(false);
        mPDSaveData.setIndeterminate(true);
        mPDSaveData.setMessage("Saving Data...");

        //set autocomplete threshold
        mACTVUserName.setThreshold(1);

        mDBHelper = DBHelper.getInstance(this);
        new FetchUserFromContactTask().execute();

        //get data from intent
        Bundle extras = getIntent().getExtras();
        ENTRY_TYPE = CREATE_ENTRY;

        //if extras is not null, that means user is either creating entry under specific category or editing previous entry.
        if (extras != null) {
            mLendAndBorrowEntry = extras.getParcelable("ENTRY");
            boolean isEditEntry = extras.getBoolean("EDITENTRY", false);

            mSelectedUserID = extras.getInt("SELECTEDUSERID", 0);
            mSelectedUserName = extras.getString("SELECTEDUSERNAME", "");


            //if creating Entry For a Specific Category then disable autocomplete text view
            mACTVUserName.setEnabled(isEditEntry);

            if (isEditEntry) {
                ENTRY_TYPE = EDIT_ENTRY;
                getSupportActionBar().setTitle("Edit Lend and Borrow");
            }
        } else {
            mLendAndBorrowEntry = new LendAndBorrowEntry();
        }

        //set data in views
        mBtnDate.setText(mLendAndBorrowEntry.date);
        mACTVUserName.setText(mSelectedUserName);
        mACTVUserName.setSelection(mSelectedUserName.length());
        if (mLendAndBorrowEntry.amount == 0) {
            mETAmount.setText("");
        } else {
            mETAmount.setText(mLendAndBorrowEntry.amount + "");
        }
        mETDescription.setText(mLendAndBorrowEntry.description);
        if (mLendAndBorrowEntry.toUser == 1) {
            mRBBorrow.setChecked(true);
        } else {
            mRBLend.setChecked(true);

        }

        //bind listeners
        mBtnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDatePicker();
            }
        });

        mACTVUserName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                mSelectedContact = (Contact) parent.getAdapter().getItem(position);
                mACTVUserName.setText(mSelectedContact.name);
                mACTVUserName.setSelection(mSelectedContact.name.length());
            }
        });

        mACTVUserName.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (mSelectedContact != null && !mSelectedContact.name.equals(mACTVUserName.getText().toString())) {
                    mSelectedContact = null;

                }
                mSelectedUserID = 0;
                return false;
            }
        });

        mACTVUserName.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTILUserName.setError(null);
                return false;
            }
        });

        mETAmount.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTILAmount.setError(null);
                return false;
            }
        });

        mETDescription.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mTILDescription.setError(null);
                return false;
            }
        });

    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.slide_in_from_top, R.anim.slide_out_to_bottom);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_activity_lend_and_borrow_add_entry, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                saveData();
                break;
            default:
                break;
        }
        return true;
    }

    private void openDatePicker() {
        //To show current date in the datepicker
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            public void onDateSet(DatePicker datepicker, int selectedyear, int selectedmonth, int selectedday) {
                selectedmonth++;
                String actualMonth = "" + selectedmonth;
                if (selectedmonth < 10) {
                    actualMonth = "0" + actualMonth;
                }

                String actualDay = "" + selectedday;
                if (selectedday < 10) {
                    actualDay = "0" + actualDay;
                }

                mBtnDate.setText(actualDay + "-" + actualMonth + "-" + selectedyear);
            }
        }, year, month, day).show();
    }


    private void saveData() {
        String userName = mACTVUserName.getText().toString().trim();

        // setting toUser from ActivityLendAndBorrowIndividual add new entry otherwise it will be -1
        if (TextUtils.isEmpty(userName)) {
            mTILUserName.setError("Name Required");
            return;
        }

        if (TextUtils.isEmpty(mETAmount.getText().toString())) {
            mTILAmount.setError("Amount Required");
            return;
        }

        if (mSelectedContact == null && mSelectedUserID == 0) {
            mTILUserName.setError("Invalid contact");
            return;
        }

        //if user from home screen it will be -1
        if (mSelectedUserID == 0) {
            mSelectedUserID = (int) mDBHelper.registerUserFromContact(mSelectedContact.phone, mSelectedContact.name);
        }

        if (mSelectedUserID == 0) {
            mTILUserName.setError("invalid name");
            return;
        }

        saveEntry(mSelectedUserID);
    }


    private void saveEntry(int userId) {
        String newDate = mBtnDate.getText().toString().trim();

        int newFromUser, newToUser;

        if (mRadioGroup.getCheckedRadioButtonId() == mRBLend.getId()) {
            newFromUser = 1;
            newToUser = userId;
        } else {
            newFromUser = userId;
            newToUser = 1;
        }

        double newAmount = Double.parseDouble(mETAmount.getText().toString().trim());
        String newDescription = mETDescription.getText().toString().trim();

        //if there is any change in any field then save the entry otherwise finish the activity
        if ((mLendAndBorrowEntry.fromUser != newFromUser)
                || (mLendAndBorrowEntry.toUser != newToUser)
                || (!mLendAndBorrowEntry.date.equals(newDate))
                || (mLendAndBorrowEntry.amount != newAmount)
                || (!mLendAndBorrowEntry.description.equals(newDescription))) {

            new SaveEntryTask(newDate, newFromUser, newToUser, newAmount, newDescription).execute();

        } else {
            finish();
        }
    }


    private class SaveEntryTask extends AsyncTask<Void, Void, Boolean> {

        private String mDate;
        int mFromUser;
        int mToUser;
        private double mAmount;
        private String mDescription;


        SaveEntryTask(String date, int fromUser, int toUser, double amount, String description) {

            this.mDate = date;
            this.mFromUser = fromUser;
            this.mToUser = toUser;
            this.mAmount = amount;
            this.mDescription = description;
        }

        @Override
        protected void onPreExecute() {
            mPDSaveData.setMessage("Saving Entry");
            mPDSaveData.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Map<String, String> entryData = new HashMap<>();

            //convert date into "yyyy MM dd" format
            SimpleDateFormat localDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            try {
                mDate = dbDateFormat.format(localDateFormat.parse(mDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            entryData.put("created_date", mDate);
            entryData.put("description", mDescription);
            entryData.put("amt", mAmount + "");
            entryData.put("from_user", mFromUser + "");
            entryData.put("to_user", mToUser + "");

            if (ENTRY_TYPE == CREATE_ENTRY) {

                return mDBHelper.insertLendAndBorrowEntry(entryData) > 0;

            } else {

                entryData.put("_id", mLendAndBorrowEntry.entryId + "");

                return mDBHelper.updateLendAndBorrowEntry(entryData) > 0;
            }

        }

        @Override
        protected void onPostExecute(Boolean success) {
            mPDSaveData.dismiss();
            if (success) {
                Toast.makeText(ActivityAddOrEditEntry.this,
                        "Data saved successfully",
                        Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(ActivityAddOrEditEntry.this,
                        "Something went wrong while saving entry.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class FetchUserFromContactTask extends AsyncTask<Void, Void, HashSet<Contact>> {

        @Override
        protected HashSet<Contact> doInBackground(Void... params) {

            //getting user from contact
            return getContactList(getApplicationContext());
        }

        @Override
        protected void onPostExecute(HashSet<Contact> members) {
            mACTVUserName.setAdapter(new AdapterContactList(ActivityAddOrEditEntry.this, new ArrayList<>(members)));
        }
    }

}