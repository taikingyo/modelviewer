package com.gmail.taikingyo.modelviewer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

public class IndexActivity extends AppCompatActivity {
    private String fileName;
    private String dataName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dataName = getString(R.string.iData_name);
        final Intent intent = new Intent(this, MainActivity.class);

        setContentView(R.layout.index);

        ArrayList<String> models = new ArrayList<String>();
        try {
            String[] paths = getAssets().list("");
            for(String s : paths) {
                if(isFolder(s)) findModel(s, models);
                else if(s.endsWith(".mqo") || s.endsWith(".pmx")) models.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, models);
        ListView list = (ListView)findViewById(R.id.list_ModelFile);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                fileName = (String) ((TextView)view).getText();
                start(intent);
            }
        });
    }

    private void findModel(String path, ArrayList<String> models) {
        try {
            String[] paths = getAssets().list(path);

            for(String s : paths) {
                s = path + "/" + s;
                if(isFolder(s)) {
                    findModel(s, models);
                }else if(s.endsWith(".mqo") || s.endsWith(".pmx")) models.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isFolder(String path) {
        try {
            return getAssets().list(path).length > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void start(Intent intent) {
        if(!fileName.equals("")) {
            intent.putExtra(dataName, fileName);
            startActivity(intent);
        }
    }
}
