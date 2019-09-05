package fung.lyricspotter.Connectors;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Debug;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import fung.lyricspotter.Model.Song;
import fung.lyricspotter.VolleyCallBack;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SongService {
    private ArrayList<Song> songs = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private RequestQueue queue;

    public SongService(Context context) {
        sharedPreferences = context.getSharedPreferences("SPOTIFY", 0);
        queue = Volley.newRequestQueue(context);
    }

    public ArrayList<Song> getSongs() {
        return songs;
    }

    public ArrayList<Song> getRecentlyPlayedTracks(final VolleyCallBack callBack) {
        String endpoint = "https://api.spotify.com/v1/me/player/recently-played";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, endpoint, null, response -> {
                    Gson gson = new Gson();

                    JSONArray jsonArray = response.optJSONArray("items");
                    for (int n = 0; n < jsonArray.length(); n++) {
                        try {
                            JSONObject object = jsonArray.getJSONObject(n);
                            object = object.optJSONObject("track");

                            JSONArray temp = object.getJSONArray("artists");
                            Song song = gson.fromJson(object.toString(), Song.class);
                            for (int i = 0; i < temp.length(); i++) {
                                JSONObject temp2 = temp.getJSONObject(i);
                                String name = temp2.optString("name", "NO ARTIST");
                                song.setArtist(name);
                            }
                            songs.add(song);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    callBack.onSuccess();
                }, error -> {
                    // If the user has for some reason not connected a Spotify account,
                    // this function will always have return code 400 (Bad Request)
                    // That is to be expected
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", "");
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        queue.add(jsonObjectRequest);
        return songs;
    }

    public ArrayList<Song> getCurrentSong(final VolleyCallBack callBack) {
        // There should only be one current song play so ensure
        // that we return an array with just one element
        Log.e("getCurrentSong", "Called");
        String endpoint = "https://api.spotify.com/v1/me/player/currently-playing";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, endpoint, null, response -> {
            Gson gson = new Gson();
            songs.clear();
            JSONObject jsonObject = response.optJSONObject("item");
            try {
                JSONArray temp = jsonObject.getJSONArray("artists");
                Song song = gson.fromJson(jsonObject.toString(), Song.class);

                // For the purposes of this app, I generally only need the first
                // (main) artist. I'll assume the first entry in the array is the
                // main artist.
                String artist = "";
                if (temp != null) {
                    artist = temp.getJSONObject(0).optString("name", "NO ARTIST");
                }

                song.setArtist(artist);
                song.setName(jsonObject.optString("name", "Cannot find name"));
                songs.add(0, song);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            callBack.onSuccess();

        }, error -> {
            // If the user has for some reason not connected a Spotify account,
            // this function will always have return code 400 (Bad Request)
            // That is to be expected
                    Log.i("Information:", "Spotify is currently not playing a song");
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", "");
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                return headers;
            }
        };
        queue.add(jsonObjectRequest);
        // This should only have one object in it
        return songs;
    }

    public void addSongToLibrary(Song song) {
        JSONObject payload = preparePutPayload(song);
        JsonObjectRequest jsonObjectRequest = prepareSongLibraryRequest(payload);
        queue.add(jsonObjectRequest);
    }

    private JsonObjectRequest prepareSongLibraryRequest(JSONObject payload) {
        return new JsonObjectRequest(Request.Method.PUT, "https://api.spotify.com/v1/me/tracks", payload, response -> {
        }, error -> {
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                String token = sharedPreferences.getString("token", "");
                String auth = "Bearer " + token;
                headers.put("Authorization", auth);
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
    }

    private JSONObject preparePutPayload(Song song) {
        JSONArray idarray = new JSONArray();
        idarray.put(song.getId());
        JSONObject ids = new JSONObject();
        try {
            ids.put("ids", idarray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return ids;
    }
}
