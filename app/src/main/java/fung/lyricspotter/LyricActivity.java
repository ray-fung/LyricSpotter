package fung.lyricspotter;

import android.content.Intent;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Whitelist;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

import fung.lyricspotter.Connectors.SongService;
import fung.lyricspotter.Model.Song;

public class LyricActivity extends AppCompatActivity {

    private Song song;
    private SongService songService;
    private ArrayList<Song> recentlyPlayedTracks;
    private Button search;
    private EditText artistText;
    private EditText songText;
    private TextView lyricText;
    private boolean quitApp;
    private SharedPreferences mSharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lyric);

        if (!Settings.canDrawOverlays(this)) {
            //If the draw over permission is not available open the settings screen
            //to grant the permission.
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            //startActivityForResult(intent, CODE_DRAW_OVER_OTHER_APP_PERMISSION);
            startActivity(intent);
        }

        songService = new SongService(getApplicationContext());
        SharedPreferences mSharedPreferences = this.getSharedPreferences("SPOTIFY", 0);
        getTracks();

        // Setup items in the layout
        artistText = findViewById(R.id.artistText);
        songText = findViewById(R.id.songText);
        search = findViewById(R.id.searchBtn);
        //Button testSpotify = findViewById(R.id.testSpotify);

        // Find the lyric textView and make it scrollable
        lyricText = findViewById(R.id.lyricText);
        lyricText.setMovementMethod(new ScrollingMovementMethod());

        // This boolean is used to test whether we are quitting the app or not
        // Used to determine when to start lyric head service
        quitApp = true;

        // Setup buttons
        search.setOnClickListener(func -> {
            parseHTML(artistText.getText().toString(), songText.getText().toString());
        });
    }

    // Get the most recently played tracks of the user
    private void getTracks() {
        songService.getRecentlyPlayedTracks(() -> {
            recentlyPlayedTracks = songService.getSongs();
            updateSong();
        });
    }

    // Update the current song
    private void updateSong() {
        if (recentlyPlayedTracks.size() > 0) {
            song = recentlyPlayedTracks.get(0);
        }
    }

    // Function to search the Spotify information of a user
    // to determine what lyrics to search
    private void spotifySearch() {
        songService.getCurrentSong(() -> {
            Log.e("Parse HTML2", "Called");
            recentlyPlayedTracks = songService.getSongs();
            updateSong();
            songText.setText(song.getName());
            artistText.setText(song.getArtist());
            Log.e("Parse HTML3", "Called");
            parseHTML(song.getArtist(), song.getName());
        });
    }

    // Use this to create our own ActionBar with a button
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mymenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // This function is used to control what happens
    // when the buttons in the action bar are pressed
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.settings) {
            quitApp = false;
            Intent newIntent = new Intent(LyricActivity.this, SettingsActivity.class);
            startActivity(newIntent);
        }
        return super.onOptionsItemSelected(item);
    }

    // Parse the HTML of a page of lyrics using JSoup
    private void parseHTML(String artist, String song) {
        try {
            // Network calls cannot be made on the main thread or else
            // an exception is thrown
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            // Setup and clean URL
            // Remove all text between parentheses and brackets as well
            String baseUrl = getResources().getString(R.string.genius_url);
            baseUrl += artist.replaceAll("\\([^()]*\\)", "").replaceAll("\\[[^()]*\\]", "").trim();
            baseUrl += " ";
            baseUrl += song.replaceAll("\\([^()]*\\)", "").replaceAll("\\[[^()]*\\]", "").trim();
            baseUrl += " lyrics";

            // Edge case found in artist name k?d
            baseUrl = baseUrl.replaceAll("\\?", "-");
            baseUrl = baseUrl.replaceAll(" ", "-");

            // Edge case I found with a Taylor Swift song
            baseUrl = baseUrl.replaceAll("&", "and");

            Log.e("URL2", baseUrl);
            URL temp = new URL(baseUrl);

            try {
                Document document = Jsoup.parse(temp, 6000);

                // All this is used to clean up the HTML code to properly print out
                // breaks and paragraphs
                document.outputSettings(new Document.OutputSettings().prettyPrint(false)); //makes html() preserve linebreaks and spacing
                document.select("br").append("\\n");
                document.select("p").prepend("\\n\\n");

                // Isolate the "lyrics" element of the HTML document
                Element test1 = document.select("div.lyrics").first();
                String t = test1.text().replaceAll("\\\\n", "\n");
                String test = Jsoup.clean(t, "", Whitelist.none(), new Document.OutputSettings().prettyPrint(false));
                // Print out substring to basically delete the extra line at the beginning
                lyricText.setText(test.substring(2));
            } catch (HttpStatusException e) {
                // Error retrieving the URL. Most likely a wrong URL
                // Print error message in the lyrics box
                lyricText.setText(R.string.html_404);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        Log.e("onResume", "Being called!");
        quitApp = true;
        Long currTime = System.currentTimeMillis();
        if (mSharedPreferences == null) {
            mSharedPreferences = this.getSharedPreferences("SPOTIFY", 0);
        }
        Long sinceLastLogin = mSharedPreferences.getLong("time_since_last", currTime);

        if (sinceLastLogin != currTime && currTime - 3600000L > sinceLastLogin) {
            Log.e("Lyric", "Refreshing Token");
            // It has been over an hour since the last login, which means that the
            // API token needs to be refreshed (since Spotify doesn't support
            // the Android SDK with refresh tokens)
            Intent newIntent = new Intent(LyricActivity.this, SplashActivity.class);
            startActivity(newIntent);
        }
        Log.e("Lyric", "Spotify Search");
        spotifySearch();
        super.onResume();
    }

    @Override
    public void onStop() {
        Log.e("onStop: LyricActivity", "Being called!");
        SharedPreferences temp = this.getSharedPreferences("chatHead", 0);
        boolean allowChatHead = temp.getBoolean("allow_lyric_head", false);
        if (allowChatHead && quitApp) {
            startService(new Intent(LyricActivity.this, LyricHeadService.class));
            finish();
        }
        super.onStop();
    }
}
