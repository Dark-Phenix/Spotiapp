package com.example.spotiapp;


import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class FileMethods {

    /** Writes a string into a file, und overrites everything
     *
     * @param filename filename
     * @param text text
     */
    public static void inFile(Context context, String filename, String text, boolean append) {
        FileOutputStream fos = null;
        int mode = Context.MODE_PRIVATE;
        if (append)
        {
            mode = Context.MODE_APPEND;
        }

        try {
            fos = context.openFileOutput(filename, mode);
            fos.write(text.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void inFile(Context context, String filename, List<?> list)
    {
        String text = new GsonBuilder().setPrettyPrinting().create().toJson(list);
        inFile(context, filename, text, false);
    }

    public static void addToFile(Context context, String filename, String text)
    {
        inFile(context, filename, text, true);
    }


    /** Reads a list from a file
     *
     * @param filename FILENAME
     * @return SavedTrack list from file
     */
    public static <T> List<T> listFromJson(Context context, String filename, Class<T> tClass) {
        try {
            // create Gson instance
            Gson gson = new Gson();
            // create a reader
            FileInputStream fis = context.openFileInput(filename);
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader reader = new BufferedReader(isr);

            // create TypeToken with the actual class type
            Type listType = TypeToken.getParameterized(List.class, tClass).getType();

            // convert JSON string to a list of objects
            List<T> myList = gson.fromJson(reader, listType);
            fis.close();
            isr.close();
            reader.close();

            return myList;

        } catch (Exception ex) {
            ex.printStackTrace();
            Log.e("FILE", "Error to read file!");
            return null;
        }
    }

}
