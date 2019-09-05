package fung.lyricspotter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

public class DialogActivity extends AppCompatActivity {

    private Button yes;
    private Button no;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        yes = findViewById(R.id.yesButton);
        no = findViewById(R.id.noButton);

        yes.setOnClickListener(func -> {




        });
    }
}
