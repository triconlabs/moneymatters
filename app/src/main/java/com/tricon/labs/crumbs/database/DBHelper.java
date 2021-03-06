package com.tricon.labs.crumbs.database;

/**
 * Author : bikesh on 5/8/2015.
 */


import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.tricon.labs.crumbs.common.Constants;
import com.tricon.labs.crumbs.models.Category;
import com.tricon.labs.crumbs.models.Contact;
import com.tricon.labs.crumbs.models.Group;
import com.tricon.labs.crumbs.models.GroupExpensesEntry;
import com.tricon.labs.crumbs.models.LendAndBorrowEntry;
import com.tricon.labs.crumbs.models.Member;
import com.tricon.labs.crumbs.models.Person;
import com.tricon.labs.crumbs.models.PersonalExpenseEntry;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

import static com.tricon.labs.crumbs.libraries.Utils.getContactByPhone;

public class DBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "GivnTake.db";

    public static final String USER_TABLE_NAME = "usertable";
    public static final String USER_TABLE_FIELDS[] = {"name", "email", "phone", "description", "photo"};

    public static final String LENDANDBORROW_TABLE_NAME = "lendandborrowtable";

    public static final String PERSONAL_TABLE_NAME = "personaltable";
    public static final String PERSONAL_TABLE_FIELDS[] = {"category_id", "created_date", "description", "amt"};

    public static final String COLLECTION_TABLE_NAME = "collectiontable";

    public static final String JOINTGROUP_TABLE_NAME = "joint_grouptable";
    public static final String JOINT_USER_GROUP_RELATION_TABLE_NAME = "joint_usergrouprelationtable";
    public static final String JOINTENTRY_TABLE_NAME = "joint_entrytable";

    public static final String JOINTSPLITE_TABLE_NAME = "joint_splittable"; //face 2


    public static final String ONLINEJOINTGROUP_TABLE_NAME = "onlinejoint_grouptable";

    //private HashMap hp;

    private static DBHelper mDBDbHelper = null;

    private DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    public static DBHelper getInstance(Context context) {
        if (mDBDbHelper == null) {
            mDBDbHelper = new DBHelper(context);
        }
        return mDBDbHelper;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(
                "create table " + USER_TABLE_NAME + "  (_id INTEGER primary key autoincrement, onlineid text DEFAULT '0', name text, email text, phone text,password text, description text, country_code text , photo BLOB )"
        );

        db.execSQL(
                "create table " + LENDANDBORROW_TABLE_NAME + "  (_id INTEGER primary key autoincrement, created_date DATE, description text, from_user INTEGER, to_user INTEGER, amt FLOAT )"
        );


        db.execSQL(
                "create table " + PERSONAL_TABLE_NAME + "  (_id INTEGER primary key autoincrement, collection_id INTEGER, created_date DATE, description text, amt FLOAT )"
        );
        db.execSQL(
                "create table " + COLLECTION_TABLE_NAME + "  (_id INTEGER primary key autoincrement, name text, description text, photo BLOB )"
        );


        db.execSQL(
                "create table " + JOINTGROUP_TABLE_NAME + "  (_id INTEGER primary key autoincrement, onlineid text DEFAULT '0', isonline INTEGER DEFAULT 0, owner text, name text,  members_count INTEGER,ismonthlytask INTEGER  DEFAULT 0 , description text, totalamt FLOAT DEFAULT 0, balanceamt FLOAT DEFAULT 0, photo BLOB )"  /* status=>new,updated (for knowing local changes)    */
        );
        db.execSQL(
                "create table " + JOINTENTRY_TABLE_NAME + "  (_id INTEGER primary key autoincrement, onlineid text DEFAULT '0', joint_group_id INTEGER, created_date DATE, description text, user_id INTEGER, amt FLOAT, is_split INTEGER DEFAULT 0, last_updated DATE, status text DEFAULT 'new' )"  /* status=>new,updated  (for knowing local changes)   */
        );
        db.execSQL(
                "create table " + JOINT_USER_GROUP_RELATION_TABLE_NAME + "  (_id INTEGER primary key autoincrement, user_id INTEGER, joint_group_id INTEGER  )"
        );

        db.execSQL(
                "create table " + JOINTSPLITE_TABLE_NAME + "  (_id INTEGER primary key autoincrement, jointtable_id INTEGER, user_id INTEGER, amt FLOAT )"
        );

        //table for storing online shard joint groups for offine use (backup database)
        //online_id:- this is the row id in parse, after inseting to parse this field will update
        //db.execSQL(
        //      "create table " + ONLINEJOINTGROUP_TABLE_NAME + "  (_id INTEGER primary key autoincrement, online_id text, name text, member_count INTEGER, owner text, description text, totalamt FLOAT DEFAULT 0, photo BLOB )"
        //);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS usertable");
        onCreate(db);
    }


    //-------------------------------------------------------------------------------------------------------
    //=======================================================================================================
    //common or global function

    // insert
    public long commonInsert(Map<String, String> data, String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            contentValues.put(entry.getKey(), entry.getValue());
        }
        long res = db.insert(table, null, contentValues);
        return res;
    }

    public int commonUpdate(Map<String, String> data, String table) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!entry.getKey().equals("_id")) {
                contentValues.put(entry.getKey(), entry.getValue());
            }
        }
        return db.update(table, contentValues, "_id = ? ", new String[]{data.get("_id")});
    }

    public int commonUpdateWhere(Map<String, String> data, String where, String table) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            if (!entry.getKey().equals("_id")) {
                contentValues.put(entry.getKey(), entry.getValue());
            }
        }

        db.update(table, contentValues, where + " = ? ", new String[]{data.get(where)});
        db.close();
        return 1;
    }

    public Integer commonDelete(String id, String table) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(table,
                "_id = ? ",
                new String[]{id});

        db.close();
        return result;

    }

    public Cursor commonGetWhere(String field, String value, String table) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + table + " where " + field + "=" + value + "", null);
        if (res != null) {
            res.moveToFirst();

        }
        return res;
    }

    public Cursor commonGet(String id, String table) {

        return commonGetWhere("_id", id, table);

    }

    public String commonGetField(String id, String field, String table) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select " + field + " from " + table + " where _id=" + id + "", null);
        if (res != null) {
            res.moveToFirst();
            if (!res.isAfterLast()) {
                return res.getString(res.getColumnIndex(field));
            }
            res.close();
        }
        return "";
    }

    //-------------------------------------------------------------------------------------------------------

    //Map<String, String> map = new HashMap<String, String>();
    //map.put("name", "demo");
    public long insertUser(Map<String, String> data) {

        return commonInsert(data, "usertable");
    }

    public int updateUser(Map<String, String> data) {
        return commonUpdate(data, "usertable");
    }


    public int updatUserOnlineIdByPhone(String phone, String onlineid) {

        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("onlineid", onlineid);
        db.update("usertable", contentValues, "phone = ? ", new String[]{phone});
        db.close();
        return 1;
    }

    public Map getUser(long id) {
        Map<String, String> data;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where _id=" + id + "", null);

        data = fetchUserData(res);

        res.close();
        return data;
    }

    public Map<String, String> fetchUserData(Cursor res) {
        Map<String, String> data = new HashMap<>();

        if (res != null) {
            res.moveToFirst();
            while (!res.isAfterLast()) {

                //Log.i("DB", res.getString(res.getColumnIndex("name")) );
                data.put("_id", res.getString(res.getColumnIndex("_id")));
                data.put("onlineid", res.getString(res.getColumnIndex("onlineid")));

                data.put("name", res.getString(res.getColumnIndex("name")));
                data.put("password", res.getString(res.getColumnIndex("password")));
                data.put("email", res.getString(res.getColumnIndex("email")));
                data.put("phone", res.getString(res.getColumnIndex("phone")));
                data.put("description", res.getString(res.getColumnIndex("description")));
                data.put("country_code", res.getString(res.getColumnIndex("country_code")));
                res.moveToNext();
            }
        }

        return data;
    }

    public Cursor getUserByEmail(String email) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where email='" + email.trim() + "'", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public String getUserField(String userId, String field) {

        return commonGetField(userId, field, "usertable");
    }


    public Map getUserbyOnlineId(String onlineId) {

        Map<String, String> data;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where onlineid=" + onlineId + "", null);

        data = fetchUserData(res);

        res.close();
        return data;
    }

    public String getUserPhone(String userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where _id='" + userId + "'", null);
        if (res != null) {
            res.moveToFirst();

            if (res.getCount() > 0) {
                String userPhone = res.getString(res.getColumnIndex("phone"));
                res.close();
                return userPhone;
            }
        }
        return null;
    }

    public String getdefaultContryCode() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select country_code from usertable where _id='1'", null);
        if (res != null) {
            res.moveToFirst();

            if (res.getCount() > 0) {
                String countryCode = res.getString(res.getColumnIndex("country_code"));
                res.close();
                return countryCode;
            }
        }
        return null;
    }

    public Cursor getUserByPhone(String phone) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where phone='" + phone.trim() + "'", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public int getNumRowsUsertable() {
        SQLiteDatabase db = this.getReadableDatabase();
        return (int) DatabaseUtils.queryNumEntries(db, USER_TABLE_NAME);
    }

    //get all users except me
    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where _id != 1", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    //get all users included me
    public Cursor getAllUsersIncludedMe() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable ", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }


    public ArrayList<Person> getLendAndBorrowList() {

        ArrayList<Person> list = new ArrayList<>();
        Person person;

        SQLiteDatabase db = this.getReadableDatabase();

        /*
        Cursor res = db.rawQuery("select * from usertable " +
                " where _id != 1 " +
                "and ( _id in ( select from_user from " + LENDANDBORROW_TABLE_NAME + " ) or _id in (select to_user from  " + LENDANDBORROW_TABLE_NAME + " ) )", null);

        */

        String sql = "select U._id, U.name, " +
                "( (select TOTAL(amt) from lendandborrowtable where from_user = U._id)-(select TOTAL(amt) from lendandborrowtable where to_user =  U._id) ) as balance" +
                " from usertable U   where U._id != 1  and ( U._id in ( select from_user from lendandborrowtable ) or U._id in (select to_user from  lendandborrowtable ) )";

        Cursor res = db.rawQuery(sql, null);

        if (res != null) {
            res.moveToFirst();

            while (res.isAfterLast() == false) {

                person = new Person();

                person.id = res.getInt(0);
                person.name = res.getString(1);

                if (res.getFloat(2) < 0) {
                    person.totalAmount = res.getFloat(2) * -1;
                    person.status = Person.STATUS_GET;
                } else {
                    person.totalAmount = res.getFloat(2);
                    person.status = Person.STATUS_GIVE;
                }


                list.add(person);

                res.moveToNext();
            }

            res.close();
        }
        //db.close();

        return list;
    }

    //------------------------------------------------------------------------------------------------------------


    public long insertEntry(Map<String, String> data) {
        return commonInsert(data, "lendandborrowtable");
    }

    public long insertLendAndBorrowEntry(Map<String, String> data) {
        return commonInsert(data, "lendandborrowtable");
    }


    public int updateEntry(Map<String, String> data) {
        return commonUpdate(data, "lendandborrowtable");
    }

    public int updateLendAndBorrowEntry(Map<String, String> data) {
        return commonUpdate(data, "lendandborrowtable");
    }


    public Integer deleteEntry(String id) {
        return commonDelete(id, "lendandborrowtable");
    }

    public Integer deleteLendAndBorrowEntry(int id) {
        return commonDelete(id + "", "lendandborrowtable");
    }

    public Cursor getEntryById(String entryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from lendandborrowtable where _id = " + entryId, null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }


    //createdDate = > "Month-Year" eg:- 5-2015
    public Cursor getUserEntrys(long userId, String createdDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from lendandborrowtable where (from_user = " + userId + " or to_user = " + userId + ") and STRFTIME('%m-%Y', created_date) = '" + createdDate + "' order by created_date ", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public ArrayList<LendAndBorrowEntry> getLendAndBorrowEntrysListByPerson(long userId) {

        ArrayList<LendAndBorrowEntry> list = new ArrayList<>();
        LendAndBorrowEntry lendAndBorrowEntry;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from lendandborrowtable where (from_user = " + userId + " or to_user = " + userId + ")  order by created_date ", null);

        if (res != null) {
            res.moveToFirst();

            while (res.isAfterLast() == false) {

                lendAndBorrowEntry = new LendAndBorrowEntry();

                lendAndBorrowEntry.entryId = res.getInt(res.getColumnIndex("_id"));
                lendAndBorrowEntry.fromUser = res.getInt(res.getColumnIndex("from_user"));
                lendAndBorrowEntry.toUser = res.getInt(res.getColumnIndex("to_user"));

                lendAndBorrowEntry.description = res.getString(res.getColumnIndex("description"));
                lendAndBorrowEntry.amount = res.getFloat(res.getColumnIndex("amt"));

                String date = res.getString(res.getColumnIndex("created_date"));
                //convert date into "dd MM yyyy" format
                SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat localDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
                try {
                    date = localDateFormat.format(dbDateFormat.parse(date));
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                lendAndBorrowEntry.date = date;

                if (lendAndBorrowEntry.fromUser == 1) {
                    lendAndBorrowEntry.status = LendAndBorrowEntry.STATUS_GET;
                } else {
                    lendAndBorrowEntry.status = LendAndBorrowEntry.STATUS_GIVE;
                }

                list.add(lendAndBorrowEntry);

                res.moveToNext();
            }

            res.close();
        }
        //db.close();

        return list;

    }


    public float getMonthTotalOfGive(long userId, String createdDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        float amt;
        Cursor res = db.rawQuery("select TOTAL(amt) from lendandborrowtable where from_user = " + userId + " and STRFTIME('%m-%Y', created_date) = '" + createdDate + "'", null);

        if (res != null) {
            res.moveToFirst();
            amt = res.getFloat(0);
            res.close();
        } else {
            amt = 0;
        }

        return amt;
    }

    public float getMonthTotalOfGet(long userId, String createdDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        float amt = 0;
        Cursor res = db.rawQuery("select TOTAL(amt) from lendandborrowtable where to_user = " + userId + " and STRFTIME('%m-%Y', created_date) = '" + createdDate + "'", null);

        if (res != null) {
            res.moveToFirst();
            amt = res.getFloat(0);
            res.close();
        } else {
            amt = 0;
        }

        return amt;
    }


    //<0 get or give
    public float getTotalBalance(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        float balance = 0;

        Cursor res = db.rawQuery("select ((select TOTAL(amt) from lendandborrowtable where from_user = " + userId + ")-(select TOTAL(amt) from lendandborrowtable where to_user = " + userId + "))", null);
        if (res != null) {
            res.moveToFirst();
            balance = res.getFloat(0);
            res.close();
        }
        db.close();

        return balance;
    }

    public Double getLendAndBorrowBalanceAmount(long userId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Double balance = Double.valueOf(0);

        Cursor res = db.rawQuery("select ((select TOTAL(amt) from lendandborrowtable where from_user = " + userId + ")-(select TOTAL(amt) from lendandborrowtable where to_user = " + userId + "))", null);
        if (res != null) {
            res.moveToFirst();
            balance = res.getDouble(0);
            res.close();
        }
        db.close();

        return balance;
    }


    public float getPrevBalance(long userId, String CurrentDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        float prevBalance = 0;

        Cursor res = db.rawQuery("select ((select TOTAL(amt) from lendandborrowtable where from_user = " + userId + " and  STRFTIME('%m-%Y', created_date) < '" + CurrentDate + "'  )-(select TOTAL(amt) from lendandborrowtable where to_user = " + userId + " and  STRFTIME('%m-%Y', created_date) < '" + CurrentDate + "' ))", null);

        if (res != null) {
            res.moveToFirst();
            prevBalance = res.getFloat(0);
            res.close();
        }

        return prevBalance;
    }


    public Map<String, String> getFinalResult() {
        Map<String, String> data = new HashMap<String, String>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor res = db.rawQuery(
                "select" +
                        " TOTAL ( ( select (select TOTAL(amt) from lendandborrowtable where from_user = 1 and to_user = u._id  )  -(select TOTAL(amt) from lendandborrowtable where from_user =u._id and to_user = 1   ) as amt where amt>0      ) )," +
                        " TOTAL ( ( select (select TOTAL(amt) from lendandborrowtable where from_user = 1 and to_user = u._id  )  -(select TOTAL(amt) from lendandborrowtable where from_user =u._id and to_user = 1   ) as amt where amt<0      ) )" +
                        "from usertable u where _id !=1", null);

        data.put("amt_toGet", "0.0");
        data.put("amt_toGive", "0.0");
        if (res != null) {
            res.moveToFirst();
            data.put("amt_toGet", "" + res.getFloat(0));
            data.put("amt_toGive", "" + ((res.getFloat(1) < 0) ? (res.getFloat(1) * -1) : 0));
            res.close();
        }

        return data;
    }

    //===========================================================================================================================

    public long insertCollection(Map<String, String> data) {
        return commonInsert(data, "collectiontable");
    }


    public Cursor getCollectionById(String entryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from collectiontable where _id = " + entryId, null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public int updateCollection(Map<String, String> data) {
        return commonUpdate(data, "collectiontable");
    }

    public Integer deleteCollection(String id) {
        return commonDelete(id, "collectiontable");
    }

    public Integer deleteCollectionEntrys(String id) {

        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete("personaltable",
                "collection_id = ? ",
                new String[]{id});

    }

    public Cursor getCategories() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from collectiontable", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public TreeSet<String> getAllCategories() {
        TreeSet<String> categories = new TreeSet<>();

        //add default categories to set
        categories.addAll(Arrays.asList(Constants.PERSONAL_EXPENSE_DEFAULT_CATEGORIES));

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor categoryCursor = db.rawQuery("select name from " + COLLECTION_TABLE_NAME, null);
        while (categoryCursor.moveToNext()) {
            categories.add(categoryCursor.getString(0).trim().toLowerCase());
        }
        categoryCursor.close();

        return categories;
    }

    public int getCategoryIdFromCategoryName(String categoryName) {
        int categoryId = -1;
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor categoryCursor = db.rawQuery("select _id from " + COLLECTION_TABLE_NAME + " WHERE upper(name) = upper(?)",
                new String[]{categoryName});
        if (categoryCursor.moveToNext()) {
            categoryId = categoryCursor.getInt(0);
        }
        categoryCursor.close();

        return categoryId;
    }


    public ArrayList<Category> getCategoriesListsByMonth(String selectedDate) {

        ArrayList<Category> list = new ArrayList<>();
        Category category;

        SQLiteDatabase db = this.getReadableDatabase();

        String sql = "select  C._id , C.name, total(P.amt)  as totalamount from personaltable P left join collectiontable C on C._id = P.collection_id where STRFTIME('%m-%Y', created_date) = '" + selectedDate + "' group by P.collection_id  ";
        Log.i("sql", sql);
        Cursor res = db.rawQuery(sql, null);
        if (res != null) {
            res.moveToFirst();

            while (!res.isAfterLast()) {

                category = new Category();

                category.id = res.getInt(0);
                category.name = res.getString(1);
                category.totalAmount = res.getFloat(2);

                list.add(category);

                res.moveToNext();
            }
            res.close();
        }
        db.close();
        return list;
    }

    public Cursor getAllCollectionByMonth(String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from collectiontable  where _id in ( select collection_id from personaltable where  STRFTIME('%m-%Y', created_date) = '" + month + "') ", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public float getCollectionTotalBalance(long ColId) {
        SQLiteDatabase db = this.getReadableDatabase();
        float balance = 0;
        Cursor res = db.rawQuery("select ((select TOTAL(amt) from personaltable where category_id = " + ColId + "  )", null);
        if (res != null) {
            res.moveToFirst();
            balance = res.getFloat(0);
            res.close();
        }

        db.close();
        return balance;
    }
    //===========================================================================================================================

    public long insertPersonalExpense(Map<String, String> data) {
        return commonInsert(data, "personaltable");
    }

    public Cursor getPersonalExpense(String entryId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from personaltable where _id = " + entryId, null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public int updatePersonalExpense(Map<String, String> data) {
        return commonUpdate(data, "personaltable");
    }

    public Integer deletePersonalExpense(String id) {
        return commonDelete(id, "personaltable");
    }

    public ArrayList<PersonalExpenseEntry> getPersonalExpense(int collectionId, String categoryName, String month) {
        ArrayList<PersonalExpenseEntry> entries = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        //Log.i("bm info", "select * from personaltable where collection_id = " + collectionId + "  and STRFTIME('%m-%Y', created_date) = '" + month + "'");
        Cursor personalExpenseCursor = db.rawQuery("select * from personaltable where collection_id = " + collectionId + "  and STRFTIME('%m-%Y', created_date) = '" + month + "'  order by created_date", null);
        while (personalExpenseCursor.moveToNext()) {
            int entryId = personalExpenseCursor.getInt(personalExpenseCursor.getColumnIndex("_id"));

            String date = personalExpenseCursor.getString(personalExpenseCursor.getColumnIndex("created_date"));
            //convert date into "dd MM yyyy" format
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat localDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            try {
                date = localDateFormat.format(dbDateFormat.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }


            String description = personalExpenseCursor.getString(personalExpenseCursor.getColumnIndex("description"));
            double amount = personalExpenseCursor.getDouble(personalExpenseCursor.getColumnIndex("amt"));
            entries.add(new PersonalExpenseEntry(entryId, collectionId, categoryName, date, description, amount));
        }
        personalExpenseCursor.close();

        return entries;
    }


    public double getMonthTotalOfPersonalExpenseIndividual(int collectionId, String createdDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        double amt = 0;
        Cursor cursor = db.rawQuery("select TOTAL(amt) from personaltable where collection_id = " + collectionId + " and STRFTIME('%m-%Y', created_date) = '" + createdDate + "'", null);
        if (cursor.moveToNext()) {
            amt = cursor.getDouble(0);
        }
        cursor.close();
        return amt;
    }

    public float getMonthTotalOfPersonalExpense(String createdDate) {
        SQLiteDatabase db = this.getReadableDatabase();
        float amt = 0;
        Cursor res = db.rawQuery("select TOTAL(amt) from personaltable where  STRFTIME('%m-%Y', created_date) = '" + createdDate + "'", null);

        if (res != null) {
            res.moveToFirst();
            amt = res.getFloat(0);
            res.close();
        } else {
            amt = 0;
        }

        db.close();
        return amt;
    }


    //===========================================================================================================================


    public long insertJointGroup(Map<String, String> data) {
        return commonInsert(data, JOINTGROUP_TABLE_NAME);
    }

    public int updateJointGroup(Map<String, String> data) {
        return commonUpdate(data, JOINTGROUP_TABLE_NAME);
    }

    public int deleteGroup(String GroupId) {

        //delet etable
        commonDelete(GroupId, JOINTGROUP_TABLE_NAME);

        //delete relation
        //

        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(JOINT_USER_GROUP_RELATION_TABLE_NAME,
                "joint_group_id = ? ",
                new String[]{GroupId});

        db.close();

        //delete entrys

        db = this.getWritableDatabase();
        result = db.delete(JOINTENTRY_TABLE_NAME,
                "joint_group_id = ? ",
                new String[]{GroupId});

        db.close();


        return 1;
    }

    public Cursor getJointGroupbyId(String groupId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + JOINTGROUP_TABLE_NAME + " where _id = " + groupId, null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public Cursor getJointGroupbyOnlineId(String onlineId) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + JOINTGROUP_TABLE_NAME + " where onlineid=" + onlineId + "", null);

        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public String getJointGroupIdByOnlineId(String onlineId) {

        Cursor c = getJointGroupbyOnlineId(onlineId);

        if (c.getCount() > 0) {
            return c.getString(c.getColumnIndex("_id"));
        }

        return null;
    }


    public String getJointGroupField(String id, String field) {

        return commonGetField(id, field, JOINTGROUP_TABLE_NAME);
    }

    public HashMap<String, String> fetchJointGroupbyId(String groupId) {

        Cursor res = getJointGroupbyId(groupId);

        HashMap<String, String> data = new HashMap<>();

        if (res != null) {
            res.moveToFirst();

            while (!res.isAfterLast()) {

                //Log.i("DB", res.getString(res.getColumnIndex("name")) );
                data.put("_id", res.getString(res.getColumnIndex("_id")));
                data.put("onlineid", res.getString(res.getColumnIndex("onlineid")));
                data.put("isonline", res.getString(res.getColumnIndex("isonline")));
                data.put("owner", res.getString(res.getColumnIndex("owner")));
                data.put("name", res.getString(res.getColumnIndex("name")));
                data.put("members_count", res.getString(res.getColumnIndex("members_count")));
                data.put("ismonthlytask", res.getString(res.getColumnIndex("ismonthlytask")));
                data.put("description", res.getString(res.getColumnIndex("description")));

                data.put("totalamt", res.getString(res.getColumnIndex("totalamt")));
                data.put("balanceamt", res.getString(res.getColumnIndex("balanceamt")));
                res.moveToNext();
            }
            res.close();
        }

        return data;
    }

    public Cursor getAllJointGroups() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + JOINTGROUP_TABLE_NAME, null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    //---

    public ArrayList<Group> getJointGroupsList() {

        ArrayList<Group> list = new ArrayList<>();
        Group group;

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor res;

        SimpleDateFormat dmy = new SimpleDateFormat("MM-yyyy", Locale.US);


        String sql = " select G._id,  G.name, G.totalamt,G.members_count,G.balanceamt," +
                " (G.totalamt / G.members_count  ) as perhead," +
                " (select Total(amt) from joint_entrytable where user_id = 1 and joint_group_id =  G._id  ) as i_spend, " +
                " (select Total(amt) from joint_entrytable where user_id = 1 and joint_group_id =  G._id and STRFTIME('%m-%Y', created_date) = '" + dmy.format(new Date()) + "'  ) as i_spendinmonth, " +
                "G.ismonthlytask" +
                " from joint_grouptable G";

        res = db.rawQuery(sql, null);


        if (res != null) {
            res.moveToFirst();

            while (res.isAfterLast() == false) {

                group = new Group();

                group.id = res.getInt(0);
                group.name = res.getString(1);
                group.totalAmount = res.getFloat(2);
                group.membersCount = res.getInt(3);
                group.balanceAmount = res.getFloat(4);
                group.amountPerHead = res.getFloat(5);
                group.amountSpentByMe = res.getFloat(6);
                group.amountSpentByMeCurrentMonth = res.getFloat(7);
                group.ismonthlytask = res.getInt(8);


                if (group.balanceAmount < 0) {
                    group.balanceAmount = group.balanceAmount * -1;
                    group.status = Person.STATUS_GET;
                } else {
                    group.status = Person.STATUS_GIVE;
                }


                list.add(group);

                res.moveToNext();
            }

            res.close();
        }
        //db.close();

        return list;
    }


    public Cursor getAllJointGroupsWithData() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = null;
        Map<String, String> data = new HashMap<String, String>();

        SimpleDateFormat dmy = new SimpleDateFormat("MM-yyyy", Locale.US);


        String sql = " select G._id,  G.name, G.totalamt,G.members_count,G.balanceamt," +
                " (G.totalamt / G.members_count  ) as perhead," +
                " (select Total(amt) from joint_entrytable where user_id = 1 and joint_group_id =  G._id  ) as i_spend, " +
                " (select Total(amt) from joint_entrytable where user_id = 1 and joint_group_id =  G._id and STRFTIME('%m-%Y', created_date) = '" + dmy.format(new Date()) + "'  ) as i_spendinmonth" +
                " from joint_grouptable G";

        res = db.rawQuery(sql, null);


        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }
    //---

    public Map<String, String> getJointGroup(Map<String, String> data) {
        SQLiteDatabase db = this.getReadableDatabase();

        Map<String, String> result = new HashMap<>();


        //data.put("name",  ((EditText) addGroupView.findViewById(R.id.name) ).getText().toString() );

        String where = "where";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String value = entry.getValue();

            if (value.contains("'")) {
                value = value.replace("'", "''").replace("\"", "\"\"");
            }


            where = where + " " + entry.getKey() + " = '" + value + "' and ";
        }

        if (where.equals("where")) {
            where = "";
        } else {
            where = where.substring(0, where.length() - 5); // removing last 'and'
        }

        Cursor res = db.rawQuery("select * from " + JOINTGROUP_TABLE_NAME + "  " + where + " ", null);

        data.put("_id", "0"); // just adding id fied for fetching

        if (res != null) {
            res.moveToFirst();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                result.put(entry.getKey(), res.getString(res.getColumnIndex(entry.getKey())));

            }
            res.close();
        }
        db.close();
        return result;
    }


    //===========================================================================================================================


//    "create table "+JOINT_USER_GROUP_RELATION_TABLE_NAME+"  (_id INTEGER primary key autoincrement, user_id INTEGER, joint_group_id INTEGER  )"

    public int insertUserGroupRelation(String groupId, ArrayList<String> members) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues;
        for (int i = 0; i < members.size(); i++) {

            if (isRelationExist(members.get(i), groupId) == 0) {

                contentValues = new ContentValues();
                contentValues.put("joint_group_id", groupId);
                contentValues.put("user_id", members.get(i));

                db.insert(JOINT_USER_GROUP_RELATION_TABLE_NAME, null, contentValues);
            }
        }
        db.close();
        return 1;
    }

    /*
    public int cleanupUserGroupRelation(String groupId, ArrayList<String> members)
    {

        String args="";
        for (int i = 0; i < members.size(); i++) {
            args= args+ members.get(i)+", ";
        }

        if(!args.equals("")) {
            args = args.substring(0, args.length() - 2);
        }

        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM "+JOINT_USER_GROUP_RELATION_TABLE_NAME+" WHERE joint_group_id = "+groupId+" and user_id NOT IN ("+args+");");
        db.close();

        db = this.getReadableDatabase();
        db.execSQL("DELETE FROM "+JOINTENTRY_TABLE_NAME+" WHERE joint_group_id = "+groupId+" and user_id NOT IN ("+args+");");
        db.close();
    }
    */


    public int insertRelation(String userId, String groupId) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues;


        contentValues = new ContentValues();
        contentValues.put("joint_group_id", groupId);
        contentValues.put("user_id", userId);

        db.insert(JOINT_USER_GROUP_RELATION_TABLE_NAME, null, contentValues);


        db.close();
        return 1;
    }

    public int isRelationExist(String userId, String groupId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + JOINT_USER_GROUP_RELATION_TABLE_NAME + " where user_id='" + userId + "' and joint_group_id = '" + groupId + "'", null);

        if (res != null) {
            res.moveToFirst();
            res.close();
            return res.getCount();
        }
        return 0;
    }


    public Cursor getAllUsersInGroup(String groupId) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from usertable where _id in ( select user_id from " + JOINT_USER_GROUP_RELATION_TABLE_NAME + " where joint_group_id = " + groupId + ")", null);
        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }


    public ArrayList<String> getAllUsersIdsInGroup(String groupId) {

        ArrayList<String> userIds = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select user_id from " + JOINT_USER_GROUP_RELATION_TABLE_NAME + " where joint_group_id = " + groupId, null);
        if (res != null) {
            res.moveToFirst();

            while (!res.isAfterLast()) {

                userIds.add(res.getString(res.getColumnIndex("user_id")));
                res.moveToNext();
            }
            res.close();
        }
        return userIds;
    }

    public HashSet<Contact> getGroupMembers(String groupId, boolean includeOwner) {

        HashSet<Contact> members = new HashSet<>();
        String query;
        if (includeOwner) {
            query = "select _id, name, phone from usertable where _id in ( select user_id from " + JOINT_USER_GROUP_RELATION_TABLE_NAME + " where joint_group_id = " + groupId + ")";
        } else {
            query = "select _id, name, phone from usertable where _id != 1 and _id in ( select user_id from " + JOINT_USER_GROUP_RELATION_TABLE_NAME + " where joint_group_id = " + groupId + ")";
        }

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery(query, null);
        int idColumnIndex = res.getColumnIndex("_id");
        int nameColumnIndex = res.getColumnIndex("name");
        int phoneColumnIndex = res.getColumnIndex("phone");
        while (res.moveToNext()) {
            members.add(new Contact(res.getInt(idColumnIndex), res.getString(nameColumnIndex), res.getString(phoneColumnIndex)));
        }
        res.close();
        return members;
    }

    //for ongoing single exp
    public Cursor getGroupUsersData(String groupId) {
        /*
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select U.name, " +
                "(select Total(amt) from joint_entrytable where user_id = U._id and joint_group_id = "+groupId+" ), " +
                "((select Total(amt)/(select count(user_id) from joint_usergrouprelationtable where  joint_group_id = "+groupId+"  ) from joint_entrytable where joint_group_id = "+groupId+" )- (select Total(amt) from joint_entrytable where user_id = U._id and joint_group_id = "+groupId+"  ))  " +
                "from usertable U where u._id in ( select user_id from  joint_usergrouprelationtable  where  joint_group_id = "+groupId+" ) ", null );

        if (res != null) {
            res.moveToFirst();
        }
        return res;
        */
        return getGroupUsersData(groupId, null);
    }

    //for monthly renewing
    public Cursor getGroupUsersData(String groupId, String month) {
        SQLiteDatabase db = this.getReadableDatabase();

        if (month != null) {
            month = "  and STRFTIME('%m-%Y', created_date) = '" + month + "'";
        } else {
            month = "";
        }

        Cursor res = db.rawQuery("select U.name, " +
                "(select Total(amt) from joint_entrytable where user_id = U._id and joint_group_id = " + groupId + month + " ), " +
                "((select Total(amt)/(select count(user_id) from joint_usergrouprelationtable where  joint_group_id = " + groupId + "  ) from joint_entrytable where joint_group_id = " + groupId + month + " )- (select Total(amt) from joint_entrytable where user_id = U._id and joint_group_id = " + groupId + month + "  ))  " +
                "from usertable U where u._id in ( select user_id from  joint_usergrouprelationtable  where  joint_group_id = " + groupId + " ) ", null);

        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public ArrayList<Member> getGroupMemberDetailsList(int groupId) {
        return getGroupMemberDetailsList(groupId, null);
    }

    public ArrayList<Member> getGroupMemberDetailsList(int groupId, String month) {

        ArrayList<Member> list = new ArrayList<>();
        Member member;

        SQLiteDatabase db = this.getReadableDatabase();

        if (month != null) {
            month = "  and STRFTIME('%m-%Y', created_date) = '" + month + "'";
        } else {
            month = "";
        }

        Cursor res = db.rawQuery("select U._id, U.name, " +
                "(select Total(amt) from joint_entrytable where user_id = U._id and joint_group_id = " + groupId + month + " ) as amt_spent, " +
                "((select Total(amt)/(select count(user_id) from joint_usergrouprelationtable where  joint_group_id = " + groupId + "  ) from joint_entrytable where joint_group_id = " + groupId + month + " )- (select Total(amt) from joint_entrytable where user_id = U._id and joint_group_id = " + groupId + month + "  )) as balance " +
                "from usertable U where u._id in ( select user_id from  joint_usergrouprelationtable  where  joint_group_id = " + groupId + " ) ", null);

        if (res != null) {
            res.moveToFirst();

            while (res.isAfterLast() == false) {

                member = new Member();

                member.id = res.getInt(0);
                member.name = res.getString(1);


                //String.format("%.2f",


                member.amountSpent = Float.parseFloat(String.format("%.2f", res.getFloat(2)));
                member.amountBalance = Float.parseFloat(String.format("%.2f", res.getFloat(3)));

                if (res.getFloat(3) < 0) {
                    member.amountBalance = member.amountBalance * -1;
                    member.status = Person.STATUS_GET;
                } else {
                    member.status = Person.STATUS_GIVE;
                }


                list.add(member);

                res.moveToNext();
            }

            res.close();
        }


        return list;
    }


    //=======================================================
    public int insertGroupEntry(Map<String, String> data) {
        commonInsert(data, JOINTENTRY_TABLE_NAME);
        //calculating total balance
        updateGroupTotalAndBalance(data.get("joint_group_id"));
        return 1;
    }

    public Cursor getGroupsingleEntry(String id) {

        return commonGetWhere("_id", id, JOINTENTRY_TABLE_NAME);

    }

    public String getGroupsingleEntryField(String rowId, String field) {

        return commonGetField(rowId, field, JOINTENTRY_TABLE_NAME);
    }


    public int updateGroupEntry(Map<String, String> data) {
        return commonUpdate(data, JOINTENTRY_TABLE_NAME);
    }


    public Map<String, String> getAllGroupTotalSpendGiveGet() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        Map<String, String> data = new HashMap<>();

        data.put("amt_spent", "0");
        data.put("amt_toGive", "0");
        data.put("amt_toGet", "0");


        res = db.rawQuery(" select" +
                " (select Total(amt) from joint_entrytable where user_id = 1 ) as total," +
                " (select Total(balanceamt) from joint_grouptable where balanceamt >0  ) as togive," +
                " (select Total(balanceamt) from joint_grouptable where balanceamt <0  ) as toget", null);


        if (res != null) {
            res.moveToFirst();
            data.put("amt_spent", "" + String.format("%.2f", res.getFloat(0)));
            data.put("amt_toGive", "" + String.format("%.2f", res.getFloat(1)));
            data.put("amt_toGet", "" + String.format("%.2f", ((res.getFloat(2) >= 0) ? res.getFloat(2) : (res.getFloat(2) * -1))));
            res.close();
        }

        db.close();
        return data;
    }

    //--


    public void updateGroupTotalAndBalance(String groupId) {

        SQLiteDatabase db = this.getWritableDatabase();

        SimpleDateFormat dmy = new SimpleDateFormat("MM-yyyy", Locale.US);

        Cursor c = getJointGroupbyId(groupId);

        String month = "";
        if (c.getInt(c.getColumnIndex("ismonthlytask")) == 1) {
            month = "  and STRFTIME('%m-%Y', created_date) = '" + dmy.format(new Date()) + "'";
        }


        //isted of this send the new amount to this function and use that
        Cursor res = db.rawQuery("UPDATE joint_grouptable " +
                " SET totalamt=(select Total(amt) from joint_entrytable where joint_group_id =" + groupId + month + "), " +
                " balanceamt = (((select Total(amt) from joint_entrytable where joint_group_id =" + groupId + month + ")/members_count )- (select Total(amt) from joint_entrytable where user_id = 1 and joint_group_id =  " + groupId + month + " )) " +
                " WHERE _id =" + groupId, null);

        res.moveToFirst();
        res.close();
    }


    public Map<String, String> getGroupEntry(Map<String, String> data) {
        SQLiteDatabase db = this.getReadableDatabase();

        Map<String, String> result = new HashMap<>();

        String where = "where";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String value = entry.getValue();

            if (value.contains("'")) {
                value = value.replace("'", "''").replace("\"", "\"\"");
            }
            where = where + " " + entry.getKey() + " = '" + value + "' and ";
        }

        if (where.equals("where")) {
            where = "";
        } else {
            where = where.substring(0, where.length() - 5); // removing last 'and'
        }

        Cursor res = db.rawQuery("select * from " + JOINTENTRY_TABLE_NAME + "  " + where + " ", null);

        data.put("_id", "0"); // just adding id fied for fetching

        if (res != null) {
            res.moveToFirst();

            for (Map.Entry<String, String> entry : data.entrySet()) {
                result.put(entry.getKey(), res.getString(res.getColumnIndex(entry.getKey())));

            }
            res.close();
        }

        db.close();
        return result;
    }


    public Cursor getGroupEntries(String groupId) {
        return getGroupEntries(groupId, null);
    }


    public Cursor getGroupEntries(String groupId, String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;

        if (month != null) {
            month = "  and STRFTIME('%m-%Y', created_date) = '" + month + "'";
        } else {
            month = "";
        }

        res = db.rawQuery("select E._id, E.onlineid, E.user_id, E.created_date,E.description,U.name,E.amt,E.is_split from " + JOINTENTRY_TABLE_NAME + " E, usertable U where E.user_id = U._id and  joint_group_id = " + groupId + month, null);

        if (res != null) {
            res.moveToFirst();
        }
        return res;
    }

    public ArrayList<GroupExpensesEntry> getGroupEntriesList(int groupId) {
        return getGroupEntriesList(groupId, null);
    }

    public ArrayList<GroupExpensesEntry> getGroupEntriesList(int groupId, String month) {

        ArrayList<GroupExpensesEntry> list = new ArrayList<>();
        GroupExpensesEntry groupExpensesEntry;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;

        if (month != null) {
            month = "  and STRFTIME('%m-%Y', created_date) = '" + month + "'";
        } else {
            month = "";
        }

        res = db.rawQuery("select  E._id, E.user_id, U.name, U.phone,  E.created_date,E.description,E.amt, E.onlineid,E.is_split from " + JOINTENTRY_TABLE_NAME + " E, usertable U where E.user_id = U._id and  joint_group_id = " + groupId + month, null);

        while (res.moveToNext()) {

            groupExpensesEntry = new GroupExpensesEntry();

            groupExpensesEntry.expenseId = res.getInt(0);
            groupExpensesEntry.spentBy = new Contact(res.getInt(1), res.getString(2), res.getString(3));
            groupExpensesEntry.groupId = groupId;

            //convert date into "dd MM yyyy" format
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            SimpleDateFormat localDateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.US);
            try {
                groupExpensesEntry.expenseDate = localDateFormat.format(dbDateFormat.parse(res.getString(4)));
            } catch (ParseException e) {
                e.printStackTrace();
            }

            groupExpensesEntry.description = res.getString(5);
            groupExpensesEntry.amount = Float.parseFloat(String.format("%.2f", res.getFloat(6)));


            if (groupExpensesEntry.amount < 0) {
                groupExpensesEntry.amount = groupExpensesEntry.amount * -1;
                groupExpensesEntry.status = Person.STATUS_GET;
            } else {
                groupExpensesEntry.status = Person.STATUS_GIVE;
            }

            list.add(groupExpensesEntry);
        }

        res.close();

        return list;
    }


    public Map<String, String> getGroupEntryTotalPerHead(int groupId) {
        return getGroupEntryTotalPerHead(groupId, null);
    }

    public Map<String, String> getGroupEntryTotalPerHead(int groupId, String month) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res;
        Map<String, String> data = new HashMap<>();

        data.put("total", "0");
        data.put("perhead", "0");

        if (month != null) {
            month = "  and STRFTIME('%m-%Y', created_date) = '" + month + "'";
        } else {
            month = "";
        }

        res = db.rawQuery("select Total(amt), " +
                " Total(amt)/(select count(user_id) from joint_usergrouprelationtable where  joint_group_id = " + groupId + "  ) " +
                " from " + JOINTENTRY_TABLE_NAME + " where  joint_group_id = " + groupId + month, null);

        if (res != null) {
            res.moveToFirst();
            data.put("total", "" + String.format("%.2f", res.getFloat(0)));
            data.put("perhead", "" + String.format("%.2f", res.getFloat(1)));
            res.close();
        }
        db.close();
        return data;
    }


    public Integer deleteGroupEntry(String id) {
        return commonDelete(id, JOINTENTRY_TABLE_NAME);
    }


    //===========================Online Group===============================

    public int insertOnlineGroup(Map<String, String> data) {

        Map existingGroup = getOnlineGroup(data.get("group_id"));

        if (existingGroup.size() > 0) {
            //update
            Log.i("api call db", "updating data");
            data.put("_id", existingGroup.get("_id").toString());
            data.remove("group_id");


            commonUpdate(data, JOINTGROUP_TABLE_NAME);
        } else {
            Log.i("api call db", "inserting data");
            data.remove("group_id");
            data.remove("_id");
            commonInsert(data, JOINTGROUP_TABLE_NAME);
        }

        isOnlineGroupExist(data.get("group_id"));

        return 1;
    }

    public Map getOnlineGroup(String online_id) {
        Map<String, String> data = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from " + JOINTGROUP_TABLE_NAME + " where onlineid='" + online_id + "'", null);

        if (res != null) {
            res.moveToFirst();

            if (!res.isAfterLast()) {
                //Log.i("DB", res.getString(res.getColumnIndex("name")) );
                data.put("_id", res.getString(res.getColumnIndex("_id")));
                data.put("onlineid", res.getString(res.getColumnIndex("onlineid")));
                data.put("owner", res.getString(res.getColumnIndex("owner")));
                data.put("name", res.getString(res.getColumnIndex("name")));
                data.put("members_count", res.getString(res.getColumnIndex("members_count")));
                data.put("ismonthlytask", res.getString(res.getColumnIndex("ismonthlytask")));
                data.put("description", res.getString(res.getColumnIndex("description")));
                data.put("totalamt", res.getString(res.getColumnIndex("totalamt")));
                data.put("balanceamt", res.getString(res.getColumnIndex("balanceamt")));
                res.moveToNext();
            }
            res.close();
        }

        return data;
    }

    public Boolean isOnlineGroupExist(String online_id) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select _id from " + JOINTGROUP_TABLE_NAME + " where onlineid=" + online_id + "", null);

        if (res != null) {
            res.moveToFirst();
            if (res.getCount() > 0) {
                return true;
            }
            res.close();
        }

        return false;
    }


    public void updateOnlineUserGroupRelation(Map<String, String> onlineUserdata, String group_id, ContentResolver ContentResolver) {

        //if(check phone already in user table){
        //get the user  _id
        //}
        //else{
        //  if(check user exsist in contact){
        //      get the data
        //      insert to user table
        //      get the user _id
        //  }
        //  else{
        //      inser data to user table
        //      get the user _id
        //  }
        //
        // }

        //  if(check same relation not exsist in relatoin)
        //      {
        //          update relation
        //      }


        String userId = "0";
        Cursor userdata = getUserByPhone(onlineUserdata.get("phone"));
        if (userdata.getCount() > 0) {
            //user already in user table
            userId = userdata.getString(userdata.getColumnIndex("_id"));
        } else {
            Map<String, String> newUser = new HashMap<>();

            //Log.i("api call", "checking contact "+ onlineUserdata.get("phone").toString());

            String name = getContactByPhone(onlineUserdata.get("phone"), ContentResolver);
            if (name != null) {
                newUser.put("name", name);
                //Log.i("api call", "exist in contact ");
            } else {
                newUser.put("name", onlineUserdata.get("name"));
                //Log.i("api call", "Not exist in contact ");
            }

            newUser.put("onlineid", onlineUserdata.get("onlineid"));
            newUser.put("phone", onlineUserdata.get("phone"));

            newUser.put("country_code", onlineUserdata.get("country_code"));


            //Log.i("api call", "inserting user in db ");
            insertUser(newUser);

            //Log.i("api call", "recursion ");
            updateOnlineUserGroupRelation(onlineUserdata, group_id, ContentResolver);
        }


        if (isRelationExist(userId, group_id) == 0) {
            insertRelation(userId, group_id);
        }

    }

    public void cleanupOnlineGroupRelation(ArrayList onlineGroupExistingUsers, String groupId) {
        /*
        * get all the users from relation who all are not existing in  onlineGroupExistingUsers
        * delete user from relation
        * delete user from entry
        // if(check anyone deleted from the grop from server)
        // update in local db
        * */

        //String[] exUserArray = new String[onlineGroupExistingUsers.size()];
        //String args = TextUtils.join(", ", exUserArray);

        //Log.i("api call","arg"+args);

        String args = "";
        for (int i = 0; i < onlineGroupExistingUsers.size(); i++) {
            args = args + onlineGroupExistingUsers.get(i) + ", ";
        }

        if (!args.equals("")) {
            args = args.substring(0, args.length() - 2);
        }
        Log.i("api call", "arg" + args);

        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM " + JOINT_USER_GROUP_RELATION_TABLE_NAME + " WHERE joint_group_id = " + groupId + " and user_id NOT IN (" + args + ");");
        db.close();

        db = this.getReadableDatabase();
        db.execSQL("DELETE FROM " + JOINTENTRY_TABLE_NAME + " WHERE joint_group_id = " + groupId + " and user_id NOT IN (" + args + ");");
        db.close();
    }


    public Boolean isOnlineEntryExist(String online_id) {

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select _id from " + JOINTENTRY_TABLE_NAME + " where onlineid=" + online_id + "", null);

        if (res != null) {
            res.moveToFirst();
            if (res.getCount() > 0) {
                return true;
            }
            res.close();
        }

        return false;
    }

    public void updateOnlineUserGroupEntry(Map<String, String> entryData, String groupId) {
        /*
        * if(check same entry exist using online id){
        *   update entry
        * }
        * else{
        * insert new
        * }
        * */

        Log.i("api call", entryData.toString());

        if (isOnlineEntryExist(entryData.get("onlineid"))) {
            Log.i("api call", "updating online entry");
            commonUpdateWhere(entryData, "onlineid", JOINTENTRY_TABLE_NAME);
        } else {

            Log.i("api call", "adding new online entry");
            insertGroupEntry(entryData);
        }

    }

    public void cleanupOnlineGroupEntry(ArrayList existingEntrys, String groupId) {

        String args = "";
        for (int i = 0; i < existingEntrys.size(); i++) {
            args = args + existingEntrys.get(i) + ", ";
        }

        if (!args.equals("")) {
            args = args.substring(0, args.length() - 2);
        }

        Log.i("api call", "arg" + args);

        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM " + JOINTENTRY_TABLE_NAME + " WHERE joint_group_id = " + groupId + " and onlineid NOT IN (" + args + ");");
        db.close();

    }


    public void cleanupOnlineGroup(ArrayList existingGroups) {


        String args = "";
        for (int i = 0; i < existingGroups.size(); i++) {
            args = args + existingGroups.get(i) + ", ";
        }

        if (!args.equals("")) {
            args = args.substring(0, args.length() - 2);
        }

        Log.i("api call", "arg" + args);

        SQLiteDatabase db = this.getReadableDatabase();
        db.execSQL("DELETE FROM " + JOINTGROUP_TABLE_NAME + " WHERE isonline=1 and onlineid NOT IN (" + args + ");");
        db.close();

        db = this.getReadableDatabase();
        db.execSQL("DELETE FROM " + JOINTENTRY_TABLE_NAME + " WHERE joint_group_id in ( select _id from " + JOINTGROUP_TABLE_NAME + " where onlineid IN (" + args + ")  );");
        db.close();

    }


    //---------------------------------------------------------------------------------/
    public long registerUserFromContact(String phone, String name) {
        /*
        //Context context,
        String APP_SETTINGS_PREFERENCES = "APPSWTTINGSPREFERENCES" ;
        SharedPreferences sharedpreferences;
        sharedpreferences = context.getSharedPreferences(APP_SETTINGS_PREFERENCES, Context.MODE_PRIVATE);

        String countryCode = sharedpreferences.getString("CountryCode", "IN");

        phone=parsePhone(phone,countryCode);
        */

        //Locale.getDefault().getCountry()
        long userId = 0;

        Log.i("Phone n", phone);

        Cursor cursorUser = getUserByPhone(phone);

        Log.i("Phone is exsist", cursorUser.getCount() + "");

        if (cursorUser.getCount() == 0) {

            Map<String, String> data = new HashMap<String, String>();
            data.put("name", name);
            data.put("phone", phone);

            userId = insertUser(data);
        } else {
            if (cursorUser.moveToFirst()) {
                userId = cursorUser.getLong(cursorUser.getColumnIndex("_id"));
            }
        }
        return userId;
    }
}