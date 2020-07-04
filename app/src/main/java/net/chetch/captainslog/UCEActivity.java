package net.chetch.captainslog;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.chetch.utilities.UncaughtExceptionHandler;

public class UCEActivity extends GenericActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);

        /*setContentView(R.layout.activity_uce);


        String report = getIntent().getStringExtra(UncaughtExceptionHandler.REPORT);

        Button closeButton = findViewById(R.id.uceCloseButton);
        closeButton.setEnabled(false);
        closeButton.setOnClickListener(this);

        Digest digest = new Digest("Uncaught Exception Report", report);
        viewModel.postDigest(digest).observe(this, t->{
            closeButton.setEnabled(true);
        });

        TextView tv = findViewById(R.id.uceErrorReport);
        tv.setText(report);

        startTimer(10);*/
    }


    @Override
    protected void onTimer(){
        //Button closeButton = findViewById(R.id.uceCloseButton);
        //closeButton.setEnabled(true);
    }

    @Override
    public void onClick(View v) {
        finish();
        System.exit(0);
    }
}
