package com.komodo.doodlefight;

import android.os.Bundle;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication {
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	 super.onCreate(savedInstanceState);
          getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
          AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
          cfg.useGL20 = true;
          //  cfg.useCompass = true;
           initialize(new DoodleFight(), cfg);
          //initialize(new LineDrawing(), false);
    }
}