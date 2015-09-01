package com.example.prasanga.recordsoundsecret;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MobileActivity extends AppCompatActivity implements MessageReceiverListener, HitListener {

    private int maxLives = 5;

    private CheckBox serverCheck;
    private TextView ipText;
    private LinearLayout serverOptions;
    private TextView player1Text ,player2Text;
    private int player1Lives, player2Lives;

    private long player1repeatrequested, player2repeatrequested;

    private HandeldToHandeldCommunicator handeldToHandeldCommunicator;
    private HandeldToWatchCommunicator handeldToWatchCommunicator;

    private HitDetector hitDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mobile);

        serverOptions = (LinearLayout) findViewById(R.id.serveroptions);
        serverCheck = (CheckBox) findViewById(R.id.checkBox);
        ipText = (TextView) findViewById(R.id.iptext);
        player1Text = (TextView) findViewById(R.id.player1lives);
        player2Text = (TextView) findViewById(R.id.player2lives);

        handeldToWatchCommunicator = new HandeldToWatchCommunicator(this,this);

        serverOptions.setVisibility(View.INVISIBLE);

        UpdateScores();
    }

    protected void onStart(){
        super.onStart();
        handeldToWatchCommunicator.Connect(true);
    }

    protected void onStop(){
        handeldToWatchCommunicator.Connect(false);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_mobile, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void SyncHandelds(View v){
        if(handeldToHandeldCommunicator != null){
            handeldToHandeldCommunicator.Stop();
            handeldToHandeldCommunicator = null;
        }
        handeldToHandeldCommunicator = new HandeldToHandeldCommunicator(ipText.getText().toString(), serverCheck.isChecked(),this,this);

        serverOptions.setVisibility(serverCheck.isChecked()? View.VISIBLE: View.INVISIBLE);

        if(serverCheck.isChecked()){
            hitDetector = new HitDetector(this);
        }
    }

    private void UpdateScores(){
        this.runOnUiThread(new Runnable() {
            public void run() {

                player1Text.setText("" + player1Lives);
                player2Text.setText("" + player2Lives);

                if (handeldToWatchCommunicator == null || handeldToHandeldCommunicator == null) {
                    return;
                }

                handeldToHandeldCommunicator.SendMessage("lives:" + player2Lives);
                handeldToWatchCommunicator.SendMessage("lives:" + player1Lives);

                if (player1Lives <= 0) {
                    handeldToWatchCommunicator.SendMessage("lose");
                    handeldToHandeldCommunicator.SendMessage("win");
                } else if (player2Lives <= 0) {
                    handeldToWatchCommunicator.SendMessage("win");
                    handeldToHandeldCommunicator.SendMessage("lose");
                }
            }
        });
    }
    private void TryRepeat(){
        if(Math.abs(player2repeatrequested - player1repeatrequested) < 1000
                && player2repeatrequested != 0
                && player1repeatrequested != 0)
        {
            player2repeatrequested = player1repeatrequested = 0;
            Restart(null);
        }
    }

    public void Restart(View v){
        if(serverCheck.isChecked()){
            player1Lives = player2Lives = maxLives;

            UpdateScores();

            TextView clashDelayText = (TextView) findViewById(R.id.clashdelay);
            TextView clientDelayText = (TextView) findViewById(R.id.clietdelay);

            if(clashDelayText.getText().toString().length() > 0
                    && clientDelayText.getText().toString().length() > 0){
                int clashDelay = Integer.parseInt(clashDelayText.getText().toString());
                int clientDelay = Integer.parseInt(clientDelayText.getText().toString());

                hitDetector.maxClashDelay = clashDelay;
            }

            handeldToWatchCommunicator.SendMessage("start");
            handeldToHandeldCommunicator.SendMessage("start");
        }
    }

    public void onHandheldWatchMessageReceived(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        if(serverCheck.isChecked())
        {
            if(msg.contains("slash")){
                hitDetector.Player1Slashes(msg.split(":")[1]);
            }
            else if(msg.contains("repeat")){
                player1repeatrequested = System.currentTimeMillis();
                TryRepeat();
            }
        }
        else if(handeldToHandeldCommunicator != null){
            handeldToHandeldCommunicator.SendMessage(msg);
        }
    }

    public void onHandHeldHandheldMessageReceived(String msg){
        if(msg.contains("slash"))
        {
            if(serverCheck.isChecked()){
                hitDetector.Player2Slashes(msg.split(":")[1]);
            }
        }else if(msg.contains("repeat")){
            player2repeatrequested = System.currentTimeMillis();
            TryRepeat();
        }else{
            handeldToWatchCommunicator.SendMessage(msg);
        }
    }

    public void onHitDetected(int player){
        if(player2Lives <= 0 || player1Lives <= 0){
            return;
        }
        switch (player){
            case 1:
                player2Lives--;
                break;
            case 2:
                player1Lives--;
                break;
            default:
                break;
        }

        UpdateScores();

    }

}
