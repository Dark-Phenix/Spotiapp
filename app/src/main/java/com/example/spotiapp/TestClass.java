package com.example.spotiapp;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * Behandelt alle Methoden aus deer py Datei, die auf den Favourite Songs basieren
 */
public class TestClass {

    private final Map<String, String> config1;
    private final Map<String, String> config2;

    public TestClass(Map<String, String> config1, Map<String, String> config2) {
        this.config1 = config1;
        this.config2 = config2;
    }

}
