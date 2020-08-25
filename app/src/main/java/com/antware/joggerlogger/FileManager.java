package com.antware.joggerlogger;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.antware.joggerlogger.LogViewModel.ExerciseStatus;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.json.JSONObject;

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

    public static class WaypointAdapter extends TypeAdapter<Waypoint> {
        public Waypoint read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            JsonParser parser = new JsonParser();
            JsonObject json = (JsonObject) parser.parse(reader.nextString());
            double altitude = json.get("Altitude").getAsDouble();
            double latitude = json.get("Latitude").getAsDouble();
            double longitude = json.get("Longitude").getAsDouble();
            long time = json.get("Time").getAsLong();
            ExerciseStatus status = getStatus(json.get("Status").getAsString());
            Location location = new Location("");
            location.setAltitude(altitude);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setTime(time);
            return new Waypoint(location, status);
        }

        private ExerciseStatus getStatus(String status) {
            switch (status) {
                case "Started": return ExerciseStatus.STARTED;
                case "Paused": return ExerciseStatus.PAUSED;
                case "Stopped": return ExerciseStatus.STOPPED;
                default: return ExerciseStatus.STOPPED_AFTER_PAUSED;
            }
        }

        public void write(JsonWriter writer, Waypoint waypoint) throws IOException {
            if (waypoint == null) {
                writer.nullValue();
                return;
            }
            //TODO: create JsonArray instead
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("Altitude", waypoint.getAltitude());
            jsonObject.addProperty("Latitude", waypoint.getLatitude());
            jsonObject.addProperty("Longitude", waypoint.getLongitude());
            jsonObject.addProperty("Time", waypoint.getTime());
            jsonObject.addProperty("Status", waypoint.getStatus().toString());
            String string = jsonObject.toString();
            writer.value(string);
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

        GsonBuilder gsonBuilder = new GsonBuilder().setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(Waypoint.class, new WaypointAdapter());
        Gson gson = gsonBuilder.create();
        String detailsString = gson.toJson(details);
        writeContent(fileName, detailsString, activity);
        Log.d("FileManager", detailsString);
    }

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
