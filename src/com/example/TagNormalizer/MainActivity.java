package com.example.TagNormalizer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends Activity {

    private File currentDirectory;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (state.equals(Environment.MEDIA_MOUNTED)) {
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        }
        else if (state.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        }
        else {
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        if (mExternalStorageAvailable && mExternalStorageWriteable) {
            currentDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
            if (currentDirectory == null)
                currentDirectory = new File("/");
            updateListView();

            ((ListView)findViewById(R.id.main_listView_dirs)).setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    currentDirectory = (File) adapterView.getItemAtPosition(i);
                    updateListView();
                }
            });
            ((Button)findViewById(R.id.main_button_scan)).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(view.getContext(), DiscrepancyList.class);
                    intent.putExtra("directory", currentDirectory);
                    startActivity(intent);
                }
            });
        }
    }

    private void updateListView() {
        ((EditText)findViewById(R.id.main_editText_cwd)).setText(currentDirectory.getAbsolutePath());
        ArrayList<File> contents = getListOfDirectories(currentDirectory);

        CustomAdapter adapter = new CustomAdapter(this, contents);
        ListView lv = (ListView)findViewById(R.id.main_listView_dirs);
        lv.setAdapter(adapter);
    }

    private ArrayList<File> getListOfDirectories(File directory) {
        File[] dirs = directory.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        ArrayList<File> ret = new ArrayList<File>();
        for (File f : dirs) {
            ret.add(f);
        }
        Collections.sort(ret, new Comparator<File>() {
            @Override
            public int compare(File file, File file2) {
                return file.getName().compareTo(file2.getName());
            }
        });
        ret.add(0, directory.getParentFile());
        return ret;
    }

    public class CustomAdapter extends ArrayAdapter {

        CustomAdapter(Context context, ArrayList<File> list) {
            super(context, R.layout.directory_list_item, list);
        }

        @Override
        public View getView(int position, View contentView, ViewGroup parent) {
            if (contentView == null) {
                LayoutInflater vi = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                contentView = vi.inflate(R.layout.directory_list_item, null);
            }
            TextView tv = (TextView)contentView.findViewById(R.id.list_item_text);

            if (((File)getItem(position)).equals(currentDirectory.getParentFile())) {
                tv.setText("..");
            }
            else {
                tv.setText(((File) getItem(position)).getName());
            }
            return contentView;
        }

    }
}
