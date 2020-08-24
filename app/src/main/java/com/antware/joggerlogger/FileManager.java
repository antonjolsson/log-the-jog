package com.antware.joggerlogger;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Calendar;

public class FileManager {

    private static FileManager fileManager;

    private FileManager() {}

    public static FileManager getInstance() {
        if (fileManager == null) fileManager = new FileManager();
        return fileManager;
    }

    private static class WaypointSerializer implements JsonSerializer<Waypoint> {

        @Override
        public JsonElement serialize(Waypoint waypoint, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("Altitude", waypoint.getAltitude());
            jsonObject.addProperty("Latitude", waypoint.getLatitude());
            jsonObject.addProperty("Longitude", waypoint.getLongitude());
            jsonObject.addProperty("Time", waypoint.getTime());
            return jsonObject;
        }
    }

    private static class WaypointDeserializer implements JsonDeserializer<Location> {
        public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new Location(json.getAsJsonPrimitive().getAsString());
        }
    }

    public void save(LogViewModel model, Activity activity) {
        ExerciseDetails details = ExerciseDetails.getInstance();
        String fileName = getDate(model.getWaypoints().get(0).getTime());
        details.setFileName(fileName);
        details.setDuration(model.getDuration().getValue());
        details.setDistance(model.getDistance().getValue());
        details.setPace(model.getPace().getValue());
        details.setAvgSpeed(model.getAvgSpeed().getValue());
        details.setWaypoints(model.getWaypoints());
        details.setWaypoint(model.getWaypoints().getLast());
        //details.setLocation(model.getWaypoints().getLast().getLocation());

        GsonBuilder gson = new GsonBuilder().serializeNulls().setPrettyPrinting();
        gson.registerTypeAdapter(Waypoint.class, new WaypointSerializer());
        String jsonString = new Gson().toJson(details);
        writeContent(fileName, jsonString, activity);
        //String statistics = getStatistics(model, gson);
        //writeContent(fileName, statistics, activity);
        Log.d("FileManager", jsonString);
        /*WaypointList waypoints = (WaypointList) model.getWaypoints();
        for (Waypoint waypoint : waypoints) {
            String data = String.format(Locale.ENGLISH, "Status: %1s CurrentSpeed: %2f\n",
                    waypoint.getStatus().toString(), waypoint.getCurrentSpeed());
            data += gson.toJson(waypoint.getLocation());
            writeContent(fileName, data, activity);
            Log.d("FileManager", data);
        }*/
    }

   /* private String getStatistics(LogViewModel model, Gson gson) {
        String duration = gson.toJson(model.getDuration().toString());

        String stats = String.format(Locale.ENGLISH, "Duration: ")
    }*/

    private void writeContent(String fileName, String content, Activity activity) {
        try (FileOutputStream fos = activity.openFileOutput(fileName, Context.MODE_PRIVATE)) {
            fos.write(content.getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getDate(long timeStamp) {
        DateFormat df = DateFormat.getDateTimeInstance();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);
        return df.format(calendar.getTime());
    }

}
