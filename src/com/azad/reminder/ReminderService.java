package com.azad.reminder;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;
import com.azad.reminder.R;

public class ReminderService extends Service
{
	
	//state variables
	static int currentIntervalNo=0;
	static int cycle;
	static int progress;		
	static int totalSeconds=0;
	static int elapsed=0;

	static boolean isDisplayed=false;
	static boolean alarmRunning=false;
	static boolean alarmPaused =false;

	static int delay;
	static int selectedTune=0;

	
	static long vibrationPattern[]={100,500,300,500,300,500,300};
	public static MediaPlayer mediaPlayer;
	static MyCountdownTimer myCountdownTimer;
	static ArrayList<Integer> sequence;
	static int minuteFactor= 60;
		public static ReminderActivity alarmActivity;
	public static ReminderService alarmService;
	static Notification note;
	static PendingIntent pi;
	static WakeLock fullWakeLock, partialWakeLock;
	static String delimeterList=", ;";
	
	public static void initialise(ReminderActivity ac)
	{
		alarmActivity = ac;
	}
	
	public static void updateStateVariables_Stopped_to_Start()
	{
		alarmRunning = true;
		alarmPaused = false;
		currentIntervalNo=0;
		cycle=0;
	}
	
	public static void updateStateVariables_Running_toPause()
	{
		alarmRunning = true; //no need
    	alarmPaused = true;
	}
	
	public static void updateStateVariables_Pause_to_Resume()
	{
		alarmRunning = true; //no need
    	alarmPaused = false;
	}
	
	public static void updateStateVariables_Running_to_Stop()
	{
		alarmRunning = false;
		alarmPaused = false;
	}
	 //OK
    
	public static void updateStateVariables_on_tick()
	{
   		if(alarmRunning)
   		{
   			
   			if(alarmPaused==false)
   			{
		    	elapsed++;
		   		progress = elapsed*100/totalSeconds;

		   
		   		if(isDisplayed)
		   		{  			
		   			alarmActivity.updateUI_RemainingSecondsInfo();
		   		}
   			}
   			
   		}

	}
	
	public int updateStateVariables_next_alarm()
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
     
	@Override
	public void onCreate()
	{
		// TODO Auto-generated method stub
		super.onCreate();
		alarmService = this;
		
		//updateStateVariables_Stopped_to_Start();
		
	    note=new Notification(R.drawable.icon_gray, "Alarm started", System.currentTimeMillis());
		Intent i=new Intent(this, ReminderActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pi=PendingIntent.getActivity(this, 0,i, 0);
		note.setLatestEventInfo(this, "Alarm","Alarm is running", pi);
		note.flags|=Notification.FLAG_NO_CLEAR;
		startForeground(200805030, note);

		PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    fullWakeLock = powerManager.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "Loneworker - FULL WAKE LOCK");
	    partialWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Loneworker - PARTIAL WAKE LOCK");

	    partialWakeLock.acquire();
	    
		//Toast.makeText(this,"Service created ...", Toast.LENGTH_SHORT).show();
	}
	
	void changeNotificationText(String notificationText,String latestEventText)
	{
		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getApplicationContext().getSystemService(ns);
		long when = System.currentTimeMillis();

		Notification notification = new Notification(R.drawable.icon_gray, notificationText, System.currentTimeMillis());
		/*<set your intents here>*/
		Intent i=new Intent(this, ReminderActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		pi=PendingIntent.getActivity(this, 0,i, 0);
		notification.setLatestEventInfo(this, "Alarm",latestEventText, pi);
		notification.flags|=Notification.FLAG_NO_CLEAR;
		
		mNotificationManager.notify(200805030, notification);
	}
	
	//schedules next alarm //ok
    public void reschedule()
    {   	
    	//int in = currentIntervalNo;
    	//if(in==0)in = sequence.size();
    	
    	delay = updateStateVariables_next_alarm();
    	
    	totalSeconds = delay*minuteFactor;
    	elapsed=0;

    	if(isDisplayed)
    	{
	    	alarmActivity.initialiseUIState();
    	}
    	//Toast.makeText(getApplicationContext(), "Next Alarm after "+delay+" minute(s)", Toast.LENGTH_SHORT).show();
    	   	
		
		myCountdownTimer = new MyCountdownTimer(delay*1000*minuteFactor, 1000);
    	myCountdownTimer.start();
    	
    }
    
    
	public void pause()
	{
		updateStateVariables_Running_toPause();
		if(isDisplayed)alarmActivity.updateUI_Running_toPause();
    	myCountdownTimer.cancel();
    	changeNotificationText("Alarm Paused","Alarm is paused");
 
	}
	
	public void resume()
	{
		
		updateStateVariables_Pause_to_Resume();
		if(elapsed>0) elapsed--;
		myCountdownTimer = new MyCountdownTimer((totalSeconds-elapsed)*1000, 1000);
		myCountdownTimer.start();
		
    	changeNotificationText("Alarm Resumed","Alarm is running");

		//note.setLatestEventInfo(this, "Alarm","Alarm is running", pi);
		//changeState();
		if(isDisplayed)alarmActivity.updateUI_pause_to_resume();
		
	}
    

    class MyCountdownTimer extends CountDownTimer implements OnCompletionListener
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
			
			/*if(!isDisplayed)
			{
				Intent intent = new Intent(context,ReminderActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(intent);
			}*/
	        //SharedPreferences prefs = getSharedPreferences("azad.alarm.sharedPreferences", Context.MODE_PRIVATE);
	        //int toneIndex = prefs.getInt(key, defValue)

			
			mediaPlayer = MediaPlayer.create(getApplicationContext(), selectedTune);
    		mediaPlayer.start();
	    	mediaPlayer.setOnCompletionListener(this);	


			//Toast.makeText(getApplicationContext(), "ITS ALARM TIME ", Toast.LENGTH_LONG).show();	
		    
	    	if(selectedTune != R.raw.mute)
	    	{
		    	try
		    	{
		    		alarmActivity.showPopUp(R.layout.mute_alarm, R.id.mute_alarm);
		    	}
		    	catch (Exception exception) {
					// TODO: handle exception
				}
	    	}
	    	else
	    	{
	    		//vibrate
	    		Vibrator vibrator;
	    		vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
	    		vibrator.vibrate(vibrationPattern,-1);
	    	}
		    reschedule();
		}

		@Override
		public void onTick(long millisUntilFinished) {
			// TODO Auto-generated method stub
			updateStateVariables_on_tick();

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

    
	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		// TODO Auto-generated method stub
		updateStateVariables_Stopped_to_Start();
		//if(isDisplayed)alarmActivity.updateUIItemState();	
		reschedule();
		
		if(isDisplayed)alarmActivity.updateUI_STOP_TO_START();
		
		
		
		return super.onStartCommand(intent, flags, startId);

	}

	@Override
	public void onDestroy()
	{
		// TODO Auto-generated method stub
		super.onDestroy();
		
		
		alarmService = null;
		stopAlarm();
		
		if(fullWakeLock.isHeld()){
	        fullWakeLock.release();
	    }
	    if(partialWakeLock.isHeld()){
	        partialWakeLock.release();
	    }
	    
		stopForeground(true);
		
		//Toast.makeText(this, "Service destroyed ...", Toast.LENGTH_LONG).show();
	}
	
	public static String initialiseNewSequence(String text) 
	{
		
		//Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
		StringTokenizer st = new StringTokenizer(text, delimeterList);
		
		
		//for(int i=0;i<text.length();i++)
		//	text.replace(text.charAt(i), ' ');
		
		String newS="";
		//currentIntervalNo=0;
		
		int num=0;
		try
		{
			if(sequence!=null)
				sequence.clear();
			else
				sequence = new ArrayList<Integer>();
			
			while(st.hasMoreTokens())
			{
				Integer interval = new Integer(st.nextToken().toString());
				if(interval.intValue()<=0) return null;
				
				sequence.add(interval);
				if(num==0)	newS += (interval);
				else
					newS += (", "+interval);
				num++;
			}
		}
		catch (Exception exception) {
			// TODO: handle exception
			exception.printStackTrace();
			return null;
		}
		
		return newS;
	}

	public void stopAlarm()
	{

		updateStateVariables_Running_to_Stop();
		if(isDisplayed)
			alarmActivity.updateUI_running_to_stop();
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
