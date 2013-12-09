package com.azad.alarm;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;


import android.R.bool;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.format.Time;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class AlarmActivity extends Activity implements OnClickListener,OnItemSelectedListener
{
	
	ProgressBar progressBar;
	EditText etSequence;
	Button btnStart;
	Spinner sp_toneSelector;
	MyCountdownTimer myCountdownTimer;
	ArrayList<Integer> sequence;
	TextView tvTop;
	TextView tvRemaining;
	PopupWindow popupWindow;
	MediaPlayer mediaPlayer;
	PopupWindow exitPopupWindow;
	SharedPreferences prefs;
	SharedPreferences.Editor prefsEditor;
	
	boolean doubleBackToExitPressedOnce=false;
	boolean onTest = true;
	int currentIntervalNo=0;
	int alarmState;
	int OFF=0;
	int ON=1;
	int minuteFactor=60;
	int cycle;
	int progress;	
	
	int totalSeconds=0;
	int elapsed=0;

	String[] tones = {"Buzzer","Long Beep","Minions - Banana","Minions - Hello","Minions - Tadaa","Railroad Crossing Bell","Rooster","Temple Bell"};
	int[] map;
	ArrayAdapter<String> arrayAdapter;
	
	String startText = "START";
	String stopText =  "STOP";
	
	String sequenceKey = "seq_preference" ;
	String toneKey = "tone_key";
	
	TextView tvStatus;

    PowerManager mgr;
    WakeLock wakeLock;

	/** Called when the activity is first created. */
	
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
        
        etSequence = (EditText)findViewById(R.id.etSequence);
        btnStart = (Button)findViewById(R.id.btn_main);
        sp_toneSelector = (Spinner)findViewById(R.id.sp_tone_selector);
        tvStatus = (TextView)findViewById(R.id.tv_status);
        
        
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        tvTop = (TextView)findViewById(R.id.tv_header);
        tvRemaining = (TextView)findViewById(R.id.tv_remaining);
        alarmState=OFF;
        
        
        tvTop.setText("Interval sequence(Ex:1 2 3)");
        tvRemaining.setText("");
        tvRemaining.setGravity(Gravity.CENTER);
        
        
        btnStart.setOnClickListener(this);
        
        prefs = this.getSharedPreferences("azad.alarm.sharedPreferences", Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
        

        tvStatus.setText("Alarm is currently stopped");
        String initswqtext = prefs.getString(sequenceKey, null);
        
        if(initswqtext!=null)
        {
        	etSequence.setText(initswqtext);
        }
        
        int initialToneIndex = prefs.getInt(toneKey, 0);
        
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, tones);
        sp_toneSelector.setAdapter(arrayAdapter);
        sp_toneSelector.setOnItemSelectedListener(this);
        sp_toneSelector.setSelection(initialToneIndex);
        
        map = new int[tones.length];
        map[0] = R.raw.buzzer;
        map[1] = R.raw.long_beep;
        map[2] = R.raw.minions_banana;
        map[3] = R.raw.minions_hello;
        map[4] = R.raw.minions_taadaa;
        map[5] = R.raw.railroad_crossing_bell;
        map[6] = R.raw.rooster;
        map[7] = R.raw.temple_bell;
        
        
        progressBar.setProgress(0);
        
        btnStart.setBackgroundResource(R.drawable.button_shape);
        
        mgr = (PowerManager)getApplicationContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
       
    }
    
    @Override
    public void onBackPressed()
    {
    	// TODO Auto-generated method stub
    	//super.onBackPressed();
  /* 			
	    if (doubleBackToExitPressedOnce) 
	    {
	    	//Toast.makeText(this, "BACK pressed", Toast.LENGTH_SHORT).show();
            super.onBackPressed();
            return;
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        new Handler().postDelayed(new Runnable() {
            public void run() {
             doubleBackToExitPressedOnce=false;   
            }
        }, 5000);
  
*/    	
    	showExitPopUp(R.layout.exit_popup, R.id.btn_exit_continue, R.id.btn_exit_exit);
    }
    
    public void showExitPopUp(int popupLayout,int dismissButtonId,int exitButtonId) 
	{
    	if(exitPopupWindow!=null && exitPopupWindow.isShowing()) return;
		LayoutInflater layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);  
			    
		View popupView = layoutInflater.inflate(popupLayout, null);  
		exitPopupWindow = new PopupWindow(popupView,LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);  
			             
	    
	    /*INITIALISE*/
	    	    
	    Button btnDismiss = (Button)popupView.findViewById(dismissButtonId);
	    btnDismiss.setOnClickListener(new Button.OnClickListener(){

			     public void onClick(View v) {
			      // TODO Auto-generated method stub
			      exitPopupWindow.dismiss();
			     }});
			         
	    
	    Button btnExit = (Button)popupView.findViewById(exitButtonId);
	    btnExit.setOnClickListener(new Button.OnClickListener(){

			     public void onClick(View v) {
			      // TODO Auto-generated method stub
			    	 
			    	 
			    	 stopAlarm();
			    	 Intent intent = new Intent(Intent.ACTION_MAIN);
			    	 intent.addCategory(Intent.CATEGORY_HOME);
			    	 intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			    	 startActivity(intent);
			     }});

	    
	   // popupWindow.showAsDropDown(btnOpenPopup, 50, -30);
	    exitPopupWindow.showAtLocation(popupView.getRootView(),Gravity.CENTER, 0, 0);

	}

    
    
    @Override
    protected void onStop()
    {
    	// TODO Auto-generated method stub
    	super.onStop();
    	
    	if(alarmState==ON)
    		Toast.makeText(getApplicationContext(), "Stopping Alarm", Toast.LENGTH_SHORT).show();
    	stopAlarm();
    	
    		//Toast.makeText(getApplicationContext(), "They are killing me - on stop, state = "+alarmState, Toast.LENGTH_SHORT).show();
    }
    

    /**
     * Changes countdown
     */
    public void changeState()
	{
		//TextView tv = (TextView)findViewById(R.id.tv_header);
		//tv.setText( "Cycle "+cycle);
		elapsed++;
		
		tvRemaining.setGravity(Gravity.CENTER);
    	tvRemaining.setText(" "+(totalSeconds-elapsed) + " second(s) to next alarm");
    	
		progress = elapsed*100/totalSeconds;
		progressBar.setProgress(progress);
		
	}
    
    //schedules next alarm
    public void reschedule()
    {
    	
    	int c = cycle;
    	int in = currentIntervalNo;
    	if(in==0)in = sequence.size();
    	
    	int delay = nextDelay();
    	
    	totalSeconds = delay*minuteFactor;
    	elapsed=0;

    	tvStatus.setText("Alarm running. Cycle : "+cycle);
    	tvRemaining.setGravity(Gravity.CENTER);
    	tvRemaining.setText(" "+(totalSeconds) + " second(s) to next alarm");
    	Toast.makeText(getApplicationContext(), "Next Alarm after "+delay+" minute(s)", Toast.LENGTH_SHORT).show();
    	   	
		
		myCountdownTimer = new MyCountdownTimer(delay*1000*minuteFactor, 1000);
    	myCountdownTimer.start();
    	
    }
    
    

	public void onClick(View v) 
	{

		// TODO Auto-generated method stub
		if(v.getId()== R.id.btn_main)
		{
			if(alarmState==OFF)
			{				
				if(etSequence.getText().toString().trim().equals(""))
				{
					Toast.makeText(getApplicationContext(), "Set sequence first !!", Toast.LENGTH_LONG).show();
					return;
				}
		      
				currentIntervalNo=0;
		        sequence = new ArrayList<Integer>();
		        cycle=0;
		        

				boolean success = initialiseNewSequence();
				if(success)
				{
			        prefsEditor.putString(sequenceKey, etSequence.getText().toString().trim()); 
			        prefsEditor.commit();
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Invalid sequence given \nValid Sequences are like :\n5 5 5 or,\n5,5,5 or, \n5-5-5", Toast.LENGTH_LONG).show();
					return;
				}
				
				
				progressBar.setProgress(0);
				wakeLock.acquire();
				//myCountdownTimer = new MyCountdownTimer(nextDelay()*1000*minuteFactor, 1000);
				//myCountdownTimer.start();
				reschedule();
				
				tvStatus.setText("Alarm running. Cycle : "+cycle);
				
				alarmState=ON;
				btnStart.setText(stopText);
				btnStart.setBackgroundResource(R.drawable.button_shape_red);
				
				etSequence.setEnabled(false);
			}
		
			else 
			{
				stopAlarm();
				etSequence.setEnabled(true);
			}
		}	
//        timer.schedule(myTimerTask, nextDelay()*1000);
	}
	
	public void stopAlarm()
	{
		if(myCountdownTimer!=null)
			myCountdownTimer.cancel();
		
		if(wakeLock.isHeld())
			wakeLock.release();
		
		if(mediaPlayer!=null)
		{
			try
			{
				mediaPlayer.stop();
			}
			catch (Exception exception) {
				// TODO: handle exception
				Log.d("az", "already stopped - stop() failed");
			}
			try
			{
				mediaPlayer.reset();
				mediaPlayer.release();
			}
			catch (Exception exception) {
				// TODO: handle exception
				Log.d("az", "release failed");
			}

		}
		
		progressBar.setProgress(0);
		tvStatus.setText("Alarm is currently stopped");
		tvRemaining.setText("");
		alarmState = OFF;
		btnStart.setText(startText);
		btnStart.setBackgroundResource(R.drawable.button_shape);

	}
	
	private boolean initialiseNewSequence() 
	{
		String text = etSequence.getText().toString().trim();
		StringTokenizer st = new StringTokenizer(text,", -;");
		
		
		currentIntervalNo=0;
		
		try
		{
			sequence.clear();
			while(st.hasMoreTokens())
			{
				Integer interval = new Integer(st.nextElement().toString());
				if(interval.intValue()<=0) return false;
				sequence.add(interval);
			}
		}
		catch (Exception exception) {
			// TODO: handle exception
			return false;
		}
		return true;
	}


    public int nextDelay()
    {
    	int delay = sequence.get(currentIntervalNo);
    	if(currentIntervalNo==0)
    	{
    		cycle++;
    	}
    		
    	currentIntervalNo++;
    	currentIntervalNo = currentIntervalNo % sequence.size();
    			
    	
		return delay;
    }


	

    public class MyCountdownTimer extends CountDownTimer implements OnCompletionListener
    {

		public MyCountdownTimer(long millisInFuture, long countDownInterval) {
			super(millisInFuture, countDownInterval);
			// TODO Auto-generated constructor stub			
		}

		@Override
		public void onFinish() {
			
			if(popupWindow!=null)
				popupWindow.dismiss();
			// TODO Auto-generated method stub
			int item = sp_toneSelector.getSelectedItemPosition();
			progressBar.setProgress(100);
			
	//		if(mediaPlayer==null )
		//	{
					
				mediaPlayer = MediaPlayer.create(getApplicationContext(), map[item]);
	    		mediaPlayer.start();
		    	mediaPlayer.setOnCompletionListener(this);	
//			}
//			else if(mediaPlayer.isPl)

				
		    showPopUp(R.layout.mute_alarm, R.id.mute_alarm);
		    reschedule();
		}

		@Override
		public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			changeState();
		}

		public void onCompletion(MediaPlayer mp)
		{
			// TODO Auto-generated method stub
			if(mp!=null)mp.release();
			if(popupWindow!=null)
			{
				popupWindow.dismiss();
			}
			
		}
    	
    }



	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		// TODO Auto-generated method stub
		int item = sp_toneSelector.getSelectedItemPosition();
		prefsEditor.putInt(toneKey, item);
		prefsEditor.commit();	
	}

	public void onNothingSelected(AdapterView<?> arg0)
	{
		// TODO Auto-generated method stub
		
	}
    
	
	public void showPopUp(int popupLayout,int dismissButtonId) 
	{
		LayoutInflater layoutInflater = (LayoutInflater)getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);  
			    
		View popupView = layoutInflater.inflate(popupLayout, null);  
	    popupWindow = new PopupWindow(popupView,LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);  
			             
	    
	    /*INITIALISE*/
	    	    
	    Button btnDismiss = (Button)popupView.findViewById(dismissButtonId);
	    btnDismiss.setOnClickListener(new Button.OnClickListener(){

			     public void onClick(View v) {
			      // TODO Auto-generated method stub
			    	 
			    	 if(mediaPlayer.isPlaying())
			    		 mediaPlayer.stop();
			    	 
			      popupWindow.dismiss();
			     }});
			               
	   // popupWindow.showAsDropDown(btnOpenPopup, 50, -30);
	    popupWindow.showAtLocation(popupView.getRootView(),Gravity.CENTER, 0, 0);

	}
}