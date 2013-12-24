package com.azad.alarm;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

public class AlarmService extends Service
{
	public static MediaPlayer mediaPlayer;
	static MyCountdownTimer myCountdownTimer;
	static ArrayList<Integer> sequence;
	static int currentIntervalNo=0;
	static int minuteFactor=1;
	static int cycle;
	static int progress;		
	static int totalSeconds=0;
	static int elapsed=0;

	static int selectedTune=0;
	static boolean isDisplayed=false;
	static boolean alarmRunning=false;
	public static AlarmActivity alarmActivity;
	public static AlarmService alarmService;
	public static void initialise(AlarmActivity ac)
	{
		alarmActivity = ac;
	}
	
	@Override
	public void onCreate()
	{
		// TODO Auto-generated method stub
		super.onCreate();
		alarmService = this;
		
		//Toast.makeText(this,"Service created ...", Toast.LENGTH_SHORT).show();
	}
	
	//schedules next alarm //ok
    public void reschedule()
    {   	
    	int c = cycle;
    	int in = currentIntervalNo;
    	if(in==0)in = sequence.size();
    	
    	int delay = nextDelay();
    	
    	totalSeconds = delay*minuteFactor;
    	elapsed=0;

    	if(isDisplayed)
    	{
	    	
	    	alarmActivity.updateUIItemState();
    	}
    	//Toast.makeText(getApplicationContext(), "Next Alarm after "+delay+" minute(s)", Toast.LENGTH_SHORT).show();
    	   	
		
		myCountdownTimer = new MyCountdownTimer(delay*1000*minuteFactor, 1000);
    	myCountdownTimer.start();
    	
    }
    
    //OK
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
		public void onFinish() 
		{
			
			if(isDisplayed && alarmActivity!=null)
			{	
				if(alarmActivity.popupWindow!=null)
					alarmActivity.popupWindow.dismiss();
				alarmActivity.progressBar.setProgress(100);
			}
			// TODO Auto-generated method stub
								
			Context context = getApplicationContext();
			
			if(!isDisplayed)
			{
				Intent intent = new Intent(context,AlarmActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}
	        //SharedPreferences prefs = getSharedPreferences("azad.alarm.sharedPreferences", Context.MODE_PRIVATE);
	        //int toneIndex = prefs.getInt(key, defValue)

			
			mediaPlayer = MediaPlayer.create(getApplicationContext(), selectedTune);
    		mediaPlayer.start();
	    	mediaPlayer.setOnCompletionListener(this);	


			//Toast.makeText(getApplicationContext(), "ITS ALARM TIME ", Toast.LENGTH_LONG).show();	
		    
	    	try
	    	{
	    		alarmActivity.showPopUp(R.layout.mute_alarm, R.id.mute_alarm);
	    	}
	    	catch (Exception exception) {
				// TODO: handle exception
			}
		    
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
			if(isDisplayed && alarmActivity!=null)
				if(alarmActivity.popupWindow!=null)
				{
					alarmActivity.popupWindow.dismiss();
				}
			
		}
    
    }

    public static void changeState()
   	{
   		//TextView tv = (TextView)findViewById(R.id.tv_header);
   		//tv.setText( "Cycle "+cycle);
   		if(alarmRunning)
   		{
	    	elapsed++;
	   
	   		if(isDisplayed)
	   		{  			
		   		alarmActivity.tvRemaining.setGravity(Gravity.CENTER);
		   		alarmActivity.tvRemaining.setText(" "+(totalSeconds-elapsed) + " second(s) to next alarm");
		       	
		   		progress = elapsed*100/totalSeconds;
		   		alarmActivity.progressBar.setProgress(progress);
	   		}
   		}
   	}
    
    
       
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// TODO Auto-generated method stub
		alarmRunning=true;
		
		//if(isDisplayed)alarmActivity.updateUIItemState();
		
		currentIntervalNo=0;
		cycle=0;
		reschedule();
		
		return super.onStartCommand(intent, flags, startId);

	}

	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();
		alarmService = null;
		stopAlarm();
		//Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_LONG).show();
	}
	
	public static boolean initialiseNewSequence(String text) 
	{
		
		//Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		StringTokenizer st = new StringTokenizer(text," ");
		
		
		currentIntervalNo=0;
		
		try
		{
			if(sequence!=null)
				sequence.clear();
			else
				sequence = new ArrayList<Integer>();
			
			while(st.hasMoreTokens())
			{
				Integer interval = new Integer(st.nextElement().toString());
				if(interval.intValue()<=0) return false;
				sequence.add(interval);
			}
		}
		catch (Exception exception) {
			// TODO: handle exception
			exception.printStackTrace();
			return false;
		}
		return true;
	}

	public void stopAlarm()
	{
		alarmRunning=false;
		if(isDisplayed)
			alarmActivity.updateUIItemState();
		if(myCountdownTimer!=null)
			myCountdownTimer.cancel();
		
		
		
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
		

	}
	@Override
	public IBinder onBind(Intent arg0)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
