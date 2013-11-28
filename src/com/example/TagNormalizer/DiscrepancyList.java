package com.example.TagNormalizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.media.MediaMetadataRetriever;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA.
 * User: stvc
 * Date: 11/27/13
 * Time: 11:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class DiscrepancyList extends Activity {

    private ListView discrepancyList;
    private TextView noneFound;
    private ProgressBar progessBar;

    private ArrayList<Discrepancy> discrepancies;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.discrepancy_list);
        Bundle extras = getIntent().getExtras();
        File dir = (File)extras.get("directory");

        discrepancyList = (ListView)findViewById(R.id.discrepancy_list_listView);
        noneFound = (TextView)findViewById(R.id.discrepancy_list_textView_none);
        progessBar = (ProgressBar)findViewById(R.id.discrepancy_list_progressBar);

        new FindDiscrepanciesTask().execute(dir);
    }

    private void updateDiscrepancyList() {
        discrepancyList.clearChoices();
        if (discrepancies.size() > 0) {
            ArrayAdapter<Discrepancy> adapter = new ArrayAdapter<Discrepancy>(discrepancyList.getContext(), android.R.layout.simple_list_item_1, discrepancies);
            discrepancyList.setAdapter(adapter);
            discrepancyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int index, long l) {
                    AlertDialog.Builder b = new AlertDialog.Builder(view.getContext());
                    ArrayAdapter<Discrepancy> adapter = (ArrayAdapter<Discrepancy>)adapterView.getAdapter();
                    HashMap<String, ArrayList<File>> map = adapter.getItem(index).getUnmatchedFiles();
                    String[] keys = map.keySet().toArray(new String[map.size()]);

                    String[] itemList = new String[keys.length];
                    for (int i=0; i<keys.length; i++) {
                        if (keys[i] == null)
                            itemList[i] = "[None]";
                        else
                            itemList[i] = keys[i];
                        itemList[i] += " (" + map.get(keys[i]).size() + ")";
                    }

                    b.setTitle("Select the correct Album Artist");
                    b.setItems(itemList, new CustomDialogInterfaceClickListener(adapter.getItem(index), keys));
                    b.show();
                }
            });
        }
        else {
            discrepancyList.setVisibility(View.GONE);
            noneFound.setVisibility(View.VISIBLE);
        }
    }

    private void setAlbumArtistTagData(File file, String data) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            if (data == null) {
                tag.deleteField(FieldKey.ALBUM_ARTIST);
            }
            else {
                tag.setField(FieldKey.ALBUM_ARTIST, data);
            }
            audioFile.commit();
        }
        catch (Exception ex) {
            System.out.println("MyMESSAGE----Exception caught----" + ex.getMessage());
        }
    }

    private void handleDiscrepancy(Discrepancy discrepancy, String correctKey) {
        HashMap<String, ArrayList<File>> map = discrepancy.getUnmatchedFiles();
        for (Map.Entry<String, ArrayList<File>> entry : map.entrySet()) {
            if (entry.getKey() != correctKey) {
                for (File f : entry.getValue()) {
                    setAlbumArtistTagData(f, correctKey);
                }
            }
        }
        discrepancies.remove(discrepancy);
        updateDiscrepancyList();
    }

    private class CustomDialogInterfaceClickListener implements DialogInterface.OnClickListener {

        private Discrepancy discrep;
        private String[] keys;

        public CustomDialogInterfaceClickListener(Discrepancy d, String[] keys) {
            discrep = d;
            this.keys = keys;
        }
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            handleDiscrepancy(discrep, keys[i]);
        }
    }

    private class FindDiscrepanciesTask extends AsyncTask<File, Void, Void> {

        protected Void doInBackground(File... files) {
            discrepancies = new ArrayList<Discrepancy>();

            for (File f : files) {
                discrepancies.addAll(findDiscrepancies(f));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void v) {
            progessBar.setVisibility(View.GONE);
            updateDiscrepancyList();
        }

        private ArrayList<Discrepancy> findDiscrepancies(File searchDir) {
            ArrayList<Discrepancy> foundDiscrepancies = new ArrayList<Discrepancy>();

            // filter of subdirectories and music files
            File[] files = searchDir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    String extension = "";
                    if (file.isFile()) {
                        int i = file.getName().lastIndexOf('.');
                        if (i > 0) {
                            extension = file.getName().substring(i+1);
                        }
                    }
                    return file.isDirectory() || extension.equals("mp3");  //To change body of implemented methods use File | Settings | File Templates.
                }
            });

            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            HashMap<String, ArrayList<File>> albumTagsInDir = new HashMap<String, ArrayList<File>>();
            for (File f : files) {
                if (f.isDirectory()) {
                    foundDiscrepancies.addAll(findDiscrepancies(f));
                }
                else {
                    mmr.setDataSource(f.getAbsolutePath());
                    String albumName = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST);
                    if (!albumTagsInDir.containsKey(albumName)) {
                        albumTagsInDir.put(albumName, new ArrayList<File>());
                    }
                    albumTagsInDir.get(albumName).add(f);
                }
            }
            if (albumTagsInDir.size() > 1) {
                foundDiscrepancies.add(new Discrepancy(searchDir, albumTagsInDir));
            }

            return foundDiscrepancies;
        }
    }

    public class Discrepancy {
        private File directory;
        private HashMap<String, ArrayList<File>> unmatchedFiles;

        Discrepancy(File dir, HashMap<String, ArrayList<File>> unmatched) {
            directory = dir;
            unmatchedFiles = unmatched;
        }

        public File getDirectory() {
            return directory;
        }

        public HashMap<String, ArrayList<File>> getUnmatchedFiles() {
            return unmatchedFiles;
        }

        public String toString() {
            return directory.getName();
        }
    }
}