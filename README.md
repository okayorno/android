# NotePad

## 简介

NotePad 是一款简单的 Android 便签应用，用户可以轻松地添加、删除、搜索和管理便签内容。它支持便签标题和内容的编辑，以及字体大小和颜色的调整。以下是我的配置代码：

```gradle
classpath 'com.android.tools.build:gradle:8.6.0'  // 在 build.gradle (项目级) 中配置
plugins {                                       // 在 build.gradle (应用级) 中配置
    id 'com.android.application'
}
```

### android 配置

```gradle
android {
    compileSdk 33 // 更新到更新的编译 SDK 版本

    defaultConfig {
        applicationId "com.example.android.notepad"
        minSdkVersion 21 // 推荐将 minSdkVersion 设置为至少 21，以支持更新的 API
        targetSdkVersion 33 // 更新目标 SDK 版本
    
        testApplicationId "com.example.android.notepad.tests"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner" // 使用 androidx 的测试运行器
    }
    
    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro' // 更新 ProGuard 配置文件名
        }
    }
    
    namespace 'com.example.android.notepad'
}
```

### 说明

打开源代码后，项目是一个简单的记事本应用，具有以下功能：新建笔记、删除笔记、修改笔记内容、编辑标题，以及复制和粘贴功能。界面设计如下图所示。

![界面设计](file:///C:/%5CUsers%5C%E6%9F%92%5CAppData%5CRoaming%5CTypora%5Ctypora-user-images%5Cimage-20241201213417591.png

### 界面主题设置

```xml
android:theme="@android:style/Theme.Holo.Light"
```

------

## 基础功能

### 1. 时间戳实现

原始项目只显示笔记标题，在此基础上，我添加了一张图片作为笔记图标，并在标题下方增加了时间戳。首先，在 `notelist_item.xml` 文件中增加以下代码：

```xml
<ImageView
    android:id="@+id/image1"                            // 图标，替换成自己选择的图标
    android:layout_width="53dp"
    android:layout_height="64dp"
    android:layout_marginRight="8dp"
    android:src="@drawable/notebook" />

<TextView
    android:id="@android:id/text2"                       // 时间戳
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:textAppearance="?android:attr/textAppearanceLarge"
    android:gravity="center_vertical"
    android:paddingLeft="5dp"
    android:singleLine="true" />
```
![图片描述](https://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEIXrwMl2oS2AZPiGtLtaYKKLPGwRN2rqA188XVrutqLnnjVD0n8UDydbwtQIFda*TMNP1PlOQT4dZIJXn0EoO1Q!/b&bo=IgKGAyIChgMDByI!&rf=viewer_4)

### 2. 搜索功能实现

为了实现搜索功能，首先需要在 `list_options_menu.xml` 文件中添加一个搜索图标的跳转项，并在 `strings.xml` 中定义 `menu_search`，类似的，其他的 `@String` 字符串也需要定义。

```xml
<item
    android:id="@+id/menu_search"
    android:icon="@android:drawable/ic_menu_search"
    android:title="@string/menu_search"
    android:showAsAction="always" />
else if (item.getItemId() == R.id.menu_search) {
    Intent intent = new Intent(this, NoteSearch.class);
    startActivity(intent);
    return true;
}
package com.example.android.notepad;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

public class NoteSearch extends Activity implements SearchView.OnQueryTextListener {

    ListView listView;
    SQLiteDatabase sqLiteDatabase;
    
    /**
     * The columns needed by the cursor adapter
     */
    private static final String[] PROJECTION = new String[]{
            NotePad.Notes._ID, // 0
            NotePad.Notes.COLUMN_NAME_TITLE, // 1
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE // 时间
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.note_search);
    
        SearchView searchView = findViewById(R.id.search_view);
        Intent intent = getIntent();
        
        // 设置默认 URI 如果没有传递
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }
    
        listView = findViewById(R.id.list_view);
        sqLiteDatabase = new NotePadProvider.DatabaseHelper(this).getReadableDatabase();
    
        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryHint("查找");
        searchView.setOnQueryTextListener(this);  // 实现 setOnQueryTextListener 接口
    }
    
    @Override
    public boolean onQueryTextSubmit(String query) {
        Toast.makeText(this, "您选择的是：" + query, Toast.LENGTH_SHORT).show();
        return false;
    }
    
    @Override
    public boolean onQueryTextChange(String string) {
        // 动态搜索
        String selection1 = NotePad.Notes.COLUMN_NAME_TITLE + " like ? or " + NotePad.Notes.COLUMN_NAME_NOTE + " like ?";
        String[] selection2 = {"%" + string + "%", "%" + string + "%"};
        
        Cursor cursor = sqLiteDatabase.query(
                NotePad.Notes.TABLE_NAME,
                PROJECTION,  // The columns to return from the query
                selection1,  // The columns for the where clause
                selection2,  // The values for the where clause
                null,         // don't group the rows
                null,         // don't filter by row groups
                NotePad.Notes.DEFAULT_SORT_ORDER  // The sort order
        );
    
        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };
    
        int[] viewIDs = {
                android.R.id.text1,
                android.R.id.text2
        };
    
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(
                this,                             // The Context for the ListView
                R.layout.noteslist_item,         // Points to the XML for a list item
                cursor,                           // The cursor to get items from
                dataColumns,
                viewIDs
        );
    
        listView.setAdapter(adapter);
        return true;
    }

}
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <SearchView
        android:id="@+id/search_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:iconifiedByDefault="false" />
    
    <ListView
        android:id="@+id/list_view"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
<activity android:name="NoteSearch"
    android:exported="true"
    android:label="@string/menu_search"
    android:theme="@android:style/Theme.Holo.Light">
    <intent-filter>
        <action android:name="android.intent.action.SEARCH" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```
![图片描述](http://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEO1IPopx4nQWJWNLBNL*TTHG0VnX73Gmaju1VyQDaM.SGy7PdwI5Znz4bp0.gRSFLKkI1XGikL6W.MhCddEEBD0!/b&bo=LAKxAywCsQMDFzI!&rf=viewer_4)

------

## 附加功能

### 1. 笔记排序

第一步是在菜单文件 `list_options_menu.xml` 中添加排序选项。

```xml
<item
    android:id="@+id/menu_sort"
    android:title="@string/menu_sort"
    android:icon="@android:drawable/ic_menu_sort_by_size"
    android:showAsAction="always" >
    <menu>
        <item
            android:id="@+id/menu_sort1"
            android:title="@string/menu_sort1"/>
        <item
            android:id="@+id/menu_sort2"
            android:title="@string/menu_sort2"/>
        <item
            android:id="@+id/menu_sort3"
            android:title="@string/menu_sort3"/>
    </menu>
</item>
else if (item.getItemId() == R.id.menu_sort1) {
    cursor = managedQuery(
            getIntent().getData(),
            PROJECTION,
            null,
            null,
            NotePad.Notes._ID
    );
    adapter = new MyCursorAdapter(
            this,
            R.layout.noteslist_item,
            cursor,
            dataColumns,
            viewIDs
    );
    setListAdapter(adapter);
    return true;
} else if (item.getItemId() == R.id.menu_sort2) {
    cursor = managedQuery(
            getIntent().getData(),
            PROJECTION,
            null,
            null,
            NotePad.Notes.DEFAULT_SORT_ORDER
    );
    adapter = new MyCursorAdapter(
            this,
            R.layout.noteslist_item,
            cursor,
            dataColumns,
            viewIDs
    );
    setListAdapter(adapter);
    return true;
} else if (item.getItemId() == R.id.menu_sort3) {
    cursor = managedQuery(
            getIntent().getData(),
            PROJECTION,
            null,
           
```
![图片描述](http://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEO1IPopx4nQWJWNLBNL*TTGxWN.3yVoBb4eUzDlkmaX2rh66zlR.PVGCcT7pcYMy5g0.8XfB4syYUDUguVXimTI!/b&bo=JgKzAyYCswMDFzI!&rf=viewer_4)
![图片描述](http://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEO1IPopx4nQWJWNLBNL*TTHdPH*e6Mj9ln9iwKVUFEoxOjUjF9BIxG9HEOQV2.zrPz2KaErsQ7H0I6xCFv6QIBM!/b&bo=JQKoAyUCqAMDFzI!&rf=viewer_4)
![图片描述](http://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEIXrwMl2oS2AZPiGtLtaYKL7PviCWfkDrPwvgcUe1LZE76sCIP1lRgiakSolbOq3vw*QlO83YrDooTQZzzo3wZ4!/b&bo=JQLDAyUCwwMDFzI!&rf=viewer_4)

2.**功能文档：更改背景（黑白）**

**功能概述**
 本功能允许用户在 NotePad 应用中切换背景颜色，支持黑白两种主题。用户可以通过点击按钮来切换背景颜色和文字颜色，实现更好的视觉体验，适应不同的使用环境（如昼夜模式）。

**功能实现**

1. **按钮设置**：在应用界面上设置一个按钮，用户点击该按钮后触发背景颜色切换功能。

   ```xml
   <Button
       android:id="@+id/themeToggleButton"
       android:layout_width="wrap_content"
       android:layout_height="wrap_content"
       android:text="切换背景" />
   ```

2. **颜色切换**：按钮点击后，界面背景颜色会在黑色和白色之间切换，文字颜色也随之变化，以确保良好的可读性。

   ```java
   Button themeToggleButton = findViewById(R.id.themeToggleButton);
   themeToggleButton.setOnClickListener(new View.OnClickListener() {
       @Override
       public void onClick(View v) {
           if (isNightMode) {
               setDayMode();
           } else {
               setNightMode();
           }
       }
   });
   ```

3. **昼夜模式**：白色背景为日间模式，黑色背景为夜间模式，按钮每次点击都会在这两种模式之间切换。

   ```java
   private void setDayMode() {
       getWindow().getDecorView().setBackgroundColor(Color.WHITE);
       TextView textView = findViewById(R.id.textView);
       textView.setTextColor(Color.BLACK);
       isNightMode = false;
   }
   
   private void setNightMode() {
       getWindow().getDecorView().setBackgroundColor(Color.BLACK);
       TextView textView = findViewById(R.id.textView);
       textView.setTextColor(Color.WHITE);
       isNightMode = true;
   }
   ```

4. **按钮状态管理**：`isNightMode` 变量用来判断当前是夜间模式还是日间模式，以便进行正确的切换。

   ```java
   private boolean isNightMode = false;  // 默认为日间模式
   
   ```

   ![图片描述](http://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEO1IPopx4nQWJWNLBNL*TTGxWN.3yVoBb4eUzDlkmaX2rh66zlR.PVGCcT7pcYMy5g0.8XfB4syYUDUguVXimTI!/b&bo=JgKzAyYCswMDFzI!&rf=viewer_4)
![图片描述](http://m.qpic.cn/psc?/0451802a-3cf5-40df-84cb-fa749e3f65d7/TmEUgtj9EK6.7V8ajmQrEIXrwMl2oS2AZPiGtLtaYKL7PviCWfkDrPwvgcUe1LZE76sCIP1lRgiakSolbOq3vw*QlO83YrDooTQZzzo3wZ4!/b&bo=JQLDAyUCwwMDFzI!&rf=viewer_4)

