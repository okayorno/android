package com.example.android.notepad;

import android.app.ListActivity;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;
import android.widget.EditText;
import android.content.DialogInterface;
import android.app.AlertDialog;
import android.widget.TextView;
import android.graphics.Color;


import android.view.ContextMenu.ContextMenuInfo;



public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";

    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    };

    private static final int COLUMN_INDEX_TITLE = 1;

    private String searchQuery = ""; // 用于保存搜索查询的字符串
    private Cursor currentCursor; // 当前的数据光标

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);

        // 初始加载所有数据
        currentCursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                null,
                null,
                NotePad.Notes.DEFAULT_SORT_ORDER
        );

        String[] dataColumns = { NotePad.Notes.COLUMN_NAME_TITLE ,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE };
        int[] viewIDs = { android.R.id.text1, R.id.time };

        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                currentCursor,
                dataColumns,
                viewIDs
        );

        setListAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu); // 这里加载菜单

        // 为反向排序菜单项添加


        // 为搜索菜单项添加一个替代菜单项
        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);
        if (clipboard.hasPrimaryClip()) {
            mPasteItem.setEnabled(true);
        } else {
            mPasteItem.setEnabled(false);
        }

        final boolean haveItems = getListAdapter().getCount() > 0;
        if (haveItems) {
            Uri uri = ContentUris.withAppendedId(getIntent().getData(), getSelectedItemId());
            Intent[] specifics = new Intent[1];
            specifics[0] = new Intent(Intent.ACTION_EDIT, uri);

            MenuItem[] items = new MenuItem[1];
            Intent intent = new Intent(null, uri);
            intent.addCategory(Intent.CATEGORY_ALTERNATIVE);

            menu.addIntentOptions(
                    Menu.CATEGORY_ALTERNATIVE,
                    Menu.NONE,
                    Menu.NONE,
                    null,
                    specifics,
                    intent,
                    Menu.NONE,
                    items
            );

            if (items[0] != null) {
                items[0].setShortcut('1', 'e');
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_add:
                startActivity(new Intent(Intent.ACTION_INSERT, getIntent().getData()));
                return true;
            case R.id.menu_paste:
                startActivity(new Intent(Intent.ACTION_PASTE, getIntent().getData()));
                return true;
            case R.id.menu_search:  // 处理搜索按钮
                startSearchActivity();
                return true;
            case R.id.menu_toggle_theme:  // 处理切换昼夜模式
                toggleNightMode();
                return true;
            case R.id.menu_reverse_sort:  // 处理反向排序
                toggleSortOrder();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private boolean isSortedDescending = false; // 用于标记当前是否为降序

    private void toggleSortOrder() {
        String sortOrder = isSortedDescending ?
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC" :
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";

        currentCursor = getContentResolver().query(
                getIntent().getData(),
                PROJECTION,
                null,  // 不需要过滤条件
                null,
                sortOrder  // 根据是否反向排序来决定排序顺序
        );

        // 更新数据源
        SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
        adapter.changeCursor(currentCursor);  // 更新数据源

        // 切换排序状态
        isSortedDescending = !isSortedDescending;
    }

    private void toggleNightMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("Settings", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        boolean isNightMode = sharedPreferences.getBoolean("NightMode", false);

        // 切换模式
        if (isNightMode) {
            editor.putBoolean("NightMode", false);
            editor.apply();
            setDayMode();
        } else {
            editor.putBoolean("NightMode", true);
            editor.apply();
            setNightMode();
        }
    }
    private void setDayMode() {
        // 设置白天模式，背景白色
        findViewById(android.R.id.content).setBackgroundColor(Color.WHITE);

        // 遍历所有列表项并设置颜色
        ListView listView = getListView();  // 获取 ListView
        for (int i = 0; i < listView.getChildCount(); i++) {
            View listItem = listView.getChildAt(i);  // 获取列表中的每一项
            TextView titleTextView = (TextView) listItem.findViewById(android.R.id.text1);  // 获取标题 TextView
            TextView timeTextView = (TextView) listItem.findViewById(R.id.time);  // 获取时间 TextView

            // 设置每一项的文字颜色
            titleTextView.setTextColor(Color.BLACK);
            timeTextView.setTextColor(Color.GRAY);
        }
    }

    private void setNightMode() {
        // 设置夜间模式，背景黑色
        findViewById(android.R.id.content).setBackgroundColor(Color.BLACK);

        // 遍历所有列表项并设置颜色
        ListView listView = getListView();  // 获取 ListView
        for (int i = 0; i < listView.getChildCount(); i++) {
            View listItem = listView.getChildAt(i);  // 获取列表中的每一项
            TextView titleTextView = (TextView) listItem.findViewById(android.R.id.text1);  // 获取标题 TextView
            TextView timeTextView = (TextView) listItem.findViewById(R.id.time);  // 获取时间 TextView

            // 设置每一项的文字颜色
            titleTextView.setTextColor(Color.WHITE);
            timeTextView.setTextColor(Color.LTGRAY);
        }
    }


    private void startSearchActivity() {
        // 弹出输入框，用户输入查询内容
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("查找:");

        // 设置输入框
        final EditText input = new EditText(this);
        builder.setView(input);

        // 设置确定按钮
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // 获取用户输入的查询内容
                searchQuery = input.getText().toString();
                performSearch();
            }
        });

        // 设置取消按钮
        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    private void performSearch() {
        // 判断是否有搜索词
        if (TextUtils.isEmpty(searchQuery)) {
            // 如果搜索词为空，则显示所有笔记
            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    null, // 不需要过滤条件
                    null,
                    NotePad.Notes.DEFAULT_SORT_ORDER
            );
        } else {
            // 执行搜索，查找标题中包含搜索词的笔记
            String selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            String[] selectionArgs = new String[]{"%" + searchQuery + "%"}; // 模糊匹配搜索词

            currentCursor = getContentResolver().query(
                    getIntent().getData(),
                    PROJECTION,
                    selection, // 查询条件
                    selectionArgs, // 查询参数
                    NotePad.Notes.DEFAULT_SORT_ORDER // 排序方式
            );
        }

        // 如果查询结果不为空，刷新列表
        if (currentCursor != null && currentCursor.getCount() > 0) {
            // 查询结果不为空，更新适配器的数据源
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.changeCursor(currentCursor); // 更新数据源
        } else {
            // 如果没有搜索结果，设置一个空的 Cursor，使 ListView 显示空白
            currentCursor = null;  // 或者 new Cursor(null) 也行
            SimpleCursorAdapter adapter = (SimpleCursorAdapter) getListAdapter();
            adapter.changeCursor(currentCursor);  // 更新适配器的数据源为 null
        }
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = (Cursor) getListAdapter().getItem(info.position);
        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.setHeaderTitle(cursor.getString(COLUMN_INDEX_TITLE));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);

        switch (item.getItemId()) {
            case R.id.context_open:
                startActivity(new Intent(Intent.ACTION_EDIT, noteUri));
                return true;
            case R.id.context_copy:
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newUri(
                        getContentResolver(),
                        "Note",
                        noteUri)
                );
                return true;
            case R.id.context_delete:
                getContentResolver().delete(
                        noteUri,
                        null,
                        null
                );
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            startActivity(new Intent(Intent.ACTION_EDIT, uri));
        }
    }
}
