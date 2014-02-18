package com.azad.reminder;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import com.azad.reminder.R;
import com.azad.reminder.ReminderService.MyCountdownTimer;


import android.R.bool;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.opengl.Visibility;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.Time;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.TextView.BufferType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.MediaController;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ReminderActivity extends Activity implements OnClickListener,OnItemSelectedListener
{
	
	ProgressBar progressBar;
	EditText etSequence;
	Button btnStart,btnPause,btnDelete;

	Spinner sp_toneSelector;
	Spinner sp_sequence_selector;
	TextView tvTop;
	TextView tvRemaining;
	PopupWindow popupWindow;
	PopupWindow exitPopupWindow;
	SharedPreferences prefs;
	SharedPreferences.Editor prefsEditor;
	
	boolean doubleBackToExitPressedOnce=false;
	boolean onTest = true;


	String[] tones = {"Mute","Long Beep","Short Beep"};
	int[] map;
	ArrayAdapter<String> arrayAdapter;
	
	String startText = "START";
	String stopText =  "STOP";
	
	String sequenceKey = "seq_preference" ;
	String toneKey = "tone_key";
	
	TextView tvStatus;


    /** Called when the activity is first created. */
	
	
	/*
	void changeColorEditTextStart()
	{
		String text = 	prefs.getString(sequenceKey, null);;

		
		//if(true) return;
		SpannableString seq=null;
		String part1 = "";
		String part2 = "";
		String part3 = "";
		
		StringTokenizer st = new StringTokenizer(text,ReminderService.delimeterList);
		
		boolean first=true;
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();
			Log.d("tokens",token);
			if(first)
			{
				part1 += token;
				first=false;
			}
			
			else
			{
				
				 part3 += ( ", "+token);

			}
			
		}

		
		try
		{
		 seq = new SpannableString( part1+part3);
		 Log.d("et text", seq.toString());
		  
		 seq.setSpan(new ForegroundColorSpan(Color.RED), 0,part1.length(), 0);
		
		 etSequence.setText(seq,BufferType.SPANNABLE);
		}
		catch (Exception exception) {
			// TODO: handle exception
			Log.d("et text length", " "+ seq.length());
		}
	} 
	
	*/
	void changeColorEditText()
	{

		String text = 	prefs.getString(sequenceKey, null);


		if(ReminderService.alarmRunning==false)
		{
			etSequence.setText(text);
			return;
		}
		
		//if(true) return;
		SpannableString seq=null;
		String part1 = "";
		String part2 = "";
		String part3 = "";
		
		StringTokenizer st = new StringTokenizer(text,ReminderService.delimeterList);
		int count=0;
		int countLimit = ReminderService.currentIntervalNo;
		if(countLimit == 0) countLimit = ReminderService.sequence.size();
		
		
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();
			//Log.d("tokens",token);
			if(count< countLimit - 1)
			{
				if(count==0) part1 += token;
				else part1 += ( ", "+token);
			}
			else if(count== countLimit - 1)
			{
				part2 += token;
							
			}
			else
			{
				if(count==0) part3 += token;
				else part3 += ( ", "+token);

			}
			
			count++;
		}
		if(!part1.equals(""))part1 += ", ";

		
		try
		{
			seq = new SpannableString( part1+part2+part3);
			//Log.d("et text", seq.toString());
		  
			seq.setSpan(new ForegroundColorSpan(Color.RED), part1.length(), part1.length()+part2.length(), 0);
		
		
			//etSequence.setText("");
			
			etSequence.setText(seq,BufferType.SPANNABLE);
		}
		catch (Exception exception) {
			// TODO: handle exception
			Log.d("ERROR ! et text length", " "+ seq.length());
		}
	} 
    
	 @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
       // ReminderService.initialiseStateVariables();

       
        
        etSequence = (EditText)findViewById(R.id.etSequence);
        btnStart = (Button)findViewById(R.id.btn_main);
        btnPause = (Button)findViewById(R.id.btn_pause);
        sp_toneSelector = (Spinner)findViewById(R.id.sp_tone_selector);
        sp_sequence_selector = (Spinner)findViewById(R.id.spinner_sequence_selector_id);
        tvStatus = (TextView)findViewById(R.id.tv_status);
        btnDelete = (Button) findViewById(R.id.btn_delete);
        
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        tvTop = (TextView)findViewById(R.id.tv_header);
        tvRemaining = (TextView)findViewById(R.id.tv_remaining);


        btnStart.setOnClickListener(this);
        btnPause.setOnClickListener(this);
        btnDelete.setOnClickListener(this);
        
    
        prefs = this.getSharedPreferences("azad.alarm.sharedPreferences", Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
        

//	        tvStatus.setText("Alarm is currently stopped");
        String initswqtext = prefs.getString(sequenceKey, null);
        
        if(initswqtext!=null)
        {
        	etSequence.setText(initswqtext);
        }
        
        
        int initialToneIndex = prefs.getInt(toneKey, 0);
        if(initialToneIndex>=tones.length)
        {
        	prefsEditor.putInt(toneKey, 0);
        	prefsEditor.commit();
        	initialToneIndex=0;
        	
        }
        
        arrayAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, tones);
        sp_toneSelector.setAdapter(arrayAdapter);
        sp_toneSelector.setOnItemSelectedListener(this);
        sp_toneSelector.setSelection(initialToneIndex);
        
        map = new int[tones.length];
        map[0] = R.raw.mute;
        map[1] = R.raw.longbeep;
        map[2] = R.raw.shortbeep;
        
        ReminderService.selectedTune=map[initialToneIndex];
        
         Log.d("ON CREATE","On CREATE");      
     //  updateUIItemState();
       updateSequenceSpinner();
    }
    

	
    @Override
    protected void onResume()
    {
    	// TODO Auto-generated method stub
    	super.onResume();
    	ReminderService.isDisplayed=true;
    	ReminderService.alarmActivity = this;
    	
    	
    	//changeColorEditText();
    	
    	//initUI();
    	
    /*	if(ReminderService.alarmRunning)
    		Log.d("ON RESUME ", "Alarlm running " );
    	else
    		Log.d("ON RESUME ", "Alarlm not running " );
    */	
    	initialiseUIState();
    	//changeColorEditText();
    }
    
    
    @Override
    protected void onPause()
    {
    	// TODO Auto-generated method stub
    	super.onPause();
    	ReminderService.isDisplayed=false;
    	ReminderService.alarmActivity=null;
    }
       
    public void initialiseUIState()
    {

        tvTop.setText("Interval sequence(Ex:1 2 3)");
        tvRemaining.setText("");
        tvRemaining.setGravity(Gravity.CENTER);
        
        
		if(ReminderService.alarmRunning == false) //not running
		{
			
			
			etSequence.setEnabled(true);
			
			String text = 	prefs.getString(sequenceKey, null);
			etSequence.setText(text);
			
			sp_sequence_selector.setEnabled(true);
			tvStatus.setText("Alarm is currently stopped");			
			tvRemaining.setText("");
			btnStart.setText(startText);
			btnStart.setBackgroundResource(R.drawable.button_shape);
			
			btnDelete.setEnabled(true);
			btnPause.setVisibility(View.GONE);
			progressBar.setProgress(0);
			
		}
		else
		{
			btnDelete.setEnabled(false);
			etSequence.setEnabled(false);
			sp_sequence_selector.setEnabled(false);
			
			
			int progress = ReminderService.elapsed*100/ReminderService.totalSeconds;
	   		progressBar.setProgress(progress);
	   		
	   		btnStart.setBackgroundResource(R.drawable.button_shape_red);
			btnStart.setText(stopText);
			
			tvRemaining.setText(" "+(ReminderService.totalSeconds-ReminderService.elapsed) + " second(s) to next alarm");
			
			if(ReminderService.alarmPaused == false) //not paused - running
			{	
				tvStatus.setText("Alarm running. Cycle : "+ ReminderService.cycle);
				btnPause.setText("Pause");
		   		
			}
			else
			{
				tvStatus.setText("Alarm paused. Cycle : "+ ReminderService.cycle);
		   		btnPause.setText("RESUME");
			}
			
			changeColorEditText();
		}
		

    }
    
    String[] sequences;
    ArrayAdapter<String> arrayAdapterForSequences;
    public void updateSequenceSpinner()
	{
    	int numberOfSequences = prefs.getInt("NumberOfSequences", 0);
		sequences = new String[numberOfSequences];
		for(int i=0;i<numberOfSequences;i++)
		{
			sequences[i] = prefs.getString("sequence"+i, "1 2 3");
		}
		arrayAdapterForSequences = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_spinner_item, sequences);
		sp_sequence_selector.setAdapter(arrayAdapterForSequences);
		sp_sequence_selector.setOnItemSelectedListener(new Sp_SequenceSelectorListener());
		
		int index = prefs.getInt("SelectedSequenceIndex", 0);
		if(numberOfSequences>0)
			sp_sequence_selector.setSelection(index);
	}
    
    class Sp_SequenceSelectorListener implements OnItemSelectedListener
    {

		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3)
		{
			// TODO Auto-generated method stub
			//Toast.makeText(getApplicationContext(), "HERE", Toast.LENGTH_LONG).show();
			int index = sp_sequence_selector.getSelectedItemPosition();
			
			prefsEditor.putInt("SelectedSequenceIndex", index);
			prefsEditor.commit();
			
			//String text = prefs.getString("sequence"+index, "1 2 3");
			//etSequence.setText(text);
			changeColorEditText();
			
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{
			// TODO Auto-generated method stub
			
		}
    	
    }
  
    public void addSequence(String text)
    {
    	if(existsAlready(text)) return;
    	
    	int numberOfSequences = prefs.getInt("NumberOfSequences", 0);
    	prefsEditor.putInt("NumberOfSequences", numberOfSequences+1);
    	prefsEditor.putInt("SelectedSequenceIndex", numberOfSequences);
    	prefsEditor.putString("sequence"+numberOfSequences, text);
    	prefsEditor.commit();
    	updateSequenceSpinner();
    }
    
    public void removeSequence()
    {
    	int numberOfSequences = prefs.getInt("NumberOfSequences", 0);
    	
    	int index = sp_sequence_selector.getSelectedItemPosition();
    	
    	if(index<0) return;
    	//select index
    	
    	for(int i=index+1;i<numberOfSequences;i++)
    	{
    		String seq = prefs.getString("sequence"+(i),"" );
    		prefsEditor.putString("sequence"+(i-1), seq);
    	}
    	
    	if(numberOfSequences>1)
    		prefsEditor.putInt("SelectedSequenceIndex", 0);
    	else
    	{
    		prefsEditor.putInt("SelectedSequenceIndex", 0);
    		etSequence.setText("");
    	}
    	
    	prefsEditor.putInt("NumberOfSequences", numberOfSequences-1);
    	prefsEditor.commit();
    	updateSequenceSpinner();
    	
    }
    public boolean existsAlready(String text)
	{
    	int numberOfSequences = prefs.getInt("NumberOfSequences", 0);
    	
    	for(int i=0;i<numberOfSequences;i++)
    	{
    		if(prefs.getString("sequence"+i, "").equalsIgnoreCase(text))
    				return true;
    	}
    	return false;
	}
	public void onClick(View v) 
	{

		// TODO Auto-generated method stub
		if(v.getId()== R.id.btn_main)
		{
			if(ReminderService.alarmRunning==false)
			{	

				if(etSequence.getText().toString().trim().equals(""))
				{
					Toast.makeText(getApplicationContext(), "Set sequence first !!", Toast.LENGTH_LONG).show();
					return;
				}
				String text = etSequence.getText().toString().trim();

				text = ReminderService.initialiseNewSequence(text);
				if(text != null)
				{
					addSequence(text);
			        prefsEditor.putString(sequenceKey, etSequence.getText().toString().trim()); 
			        prefsEditor.commit();
				}
				else
				{
					Toast.makeText(getApplicationContext(), "Invalid sequence given \nValid Sequences are like :\n5 5 5 or,\n5,5,5 or, \n5-5-5", Toast.LENGTH_LONG).show();
					return;
				}
				
				startAlarm();
				
//				changeColorEditText();
			}
		
			else 
			{
				stopAlarm();
				etSequence.setEnabled(true);
			}
		}
		else if(v.getId()==R.id.btn_pause)
		{
//			Toast.makeText(getApplicationContext(), "sadf", Toast.LENGTH_LONG).show();
			if(ReminderService.alarmRunning==false)return;
			
			if(ReminderService.alarmPaused == true)
			{
				resume();
			}
			else 
			{
				pause();
			}
			
		}
		
		else if(v.getId()==R.id.btn_delete)
		{
			removeSequence();	
		}
//        timer.schedule(myTimerTask, nextDelay()*1000);
	}
	
	
	public void pause()
	{
		ReminderService.alarmService.pause();	
	}
	
	public void resume()
	{
		
		ReminderService.alarmService.resume();
		
	}
	
	/*
	public void updateUIItemState()
	{
	

		if(ReminderService.alarmRunning == false) //not running
		{
			etSequence.setEnabled(true);
			sp_sequence_selector.setEnabled(true);
			tvStatus.setText("Alarm is currently stopped");			
			tvRemaining.setText("");
			btnStart.setText(startText);
			btnStart.setBackgroundResource(R.drawable.button_shape);
			
			btnDelete.setEnabled(true);
			btnPause.setVisibility(View.GONE);
			progressBar.setProgress(0);
		}
		else
		{
		
			btnDelete.setEnabled(false);
			etSequence.setEnabled(false);
			sp_sequence_selector.setEnabled(false);
			
			tvStatus.setText("Alarm running. Cycle : "+ ReminderService.cycle);
			tvRemaining.setText(" "+(ReminderService.totalSeconds-ReminderService.elapsed) + " second(s) to next alarm");
			btnStart.setBackgroundResource(R.drawable.button_shape_red);
			btnStart.setText(stopText);
	   		int progress = ReminderService.elapsed*100/ReminderService.totalSeconds;
	   		progressBar.setProgress(progress);
	   		
	   		if(ReminderService.alarmPaused)
	   		{
	   			btnPause.setText("RESUME");
	   		}
	   		else
	   		{
	   			btnPause.setText("Pause");
	   		}
		}
		changeColorEditText();
	}
	*/

	public void startAlarm()
	{
		startService(new Intent(ReminderActivity.this,ReminderService.class));
	}
	
	public void stopAlarm()
	{

		stopService(new Intent(ReminderActivity.this,ReminderService.class));
	}
	
	
   
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		// TODO Auto-generated method stub
		int item = sp_toneSelector.getSelectedItemPosition();
		
		prefsEditor.putInt(toneKey, item);
		prefsEditor.commit();	
		
        ReminderService.selectedTune=map[item];

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
			    	 
			    	 if(ReminderService.mediaPlayer.isPlaying())
			    		 ReminderService.mediaPlayer.stop();
			    	 
			      popupWindow.dismiss();
			     }});
			               
	   // popupWindow.showAsDropDown(btnOpenPopup, 50, -30);
	    popupWindow.showAtLocation(popupView.getRootView(),Gravity.CENTER, 0, 0);

	}


	public void updateUI_RemainingSecondsInfo()
	{
   		tvRemaining.setText(" "+(ReminderService.totalSeconds-ReminderService.elapsed) + " second(s) to next alarm");
       	
//   		progress = elapsed*100/totalSeconds;
   		progressBar.setProgress(ReminderService.progress);

	}
	
	public void updateUI_Running_toPause()
	{
   		if(ReminderService.alarmRunning)
   		{
   			btnPause.setText("RESUME");
			tvStatus.setText("Alarm paused. Cycle : "+ ReminderService.cycle);
	
   		}
		
	}

	public void updateUI_pause_to_resume()
	{
		btnPause.setText("Pause");
		tvStatus.setText("Alarm running. Cycle : "+ ReminderService.cycle);
	}
	
	public void updateUI_STOP_TO_START()
	{
		btnDelete.setEnabled(false);
		etSequence.setEnabled(false);
		sp_sequence_selector.setEnabled(false);
		
		tvStatus.setText("Alarm running. Cycle : "+ ReminderService.cycle);		
		tvRemaining.setText(" "+(ReminderService.totalSeconds-ReminderService.elapsed) + " second(s) to next alarm");
		
		btnStart.setBackgroundResource(R.drawable.button_shape_red);
		btnStart.setText(stopText);
		
   		//int progress = ReminderService.elapsed*100/ReminderService.totalSeconds;
   		progressBar.setProgress(0);
   		
   		btnPause.setEnabled(true);
   		btnPause.setVisibility(View.VISIBLE);
   		if(ReminderService.alarmPaused)
   		{
   			btnPause.setText("RESUME");
   		}
   		else
   		{
   			btnPause.setText("Pause");
   		}
		
		changeColorEditText();
	}

	
	public void updateUI_running_to_stop()
	{
		
//
		initialiseUIState();
		/*btnDelete.setEnabled(true);
		etSequence.setEnabled(true);
		etSequence.setText(prefs.getString(sequenceKey, null));
		sp_sequence_selector.setEnabled(true);
		
		tvStatus.setText("Alarm is currently stopped");			
		tvRemaining.setText("");
		btnStart.setText(startText);
		btnStart.setBackgroundResource(R.drawable.button_shape);
		
		btnPause.setVisibility(View.GONE);
		progressBar.setProgress(0);
		
		changeColorEditText();*/
	}
}