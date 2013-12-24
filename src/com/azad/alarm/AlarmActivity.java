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
	Spinner sp_sequence_selector;
	TextView tvTop;
	TextView tvRemaining;
	PopupWindow popupWindow;
	PopupWindow exitPopupWindow;
	SharedPreferences prefs;
	SharedPreferences.Editor prefsEditor;
	
	boolean doubleBackToExitPressedOnce=false;
	boolean onTest = true;


	String[] tones = {"Long Beep","Short Beep"};
	int[] map;
	ArrayAdapter<String> arrayAdapter;
	
	String startText = "START";
	String stopText =  "STOP";
	
	String sequenceKey = "seq_preference" ;
	String toneKey = "tone_key";
	
	TextView tvStatus;

    PowerManager mgr;

    /** Called when the activity is first created. */
	
    
    @Override
    protected void onResume()
    {
    	// TODO Auto-generated method stub
    	super.onResume();
    	AlarmService.isDisplayed=true;
    	AlarmService.alarmActivity = this;
    }
    
    @Override
    protected void onPause()
    {
    	// TODO Auto-generated method stub
    	super.onPause();
    	AlarmService.isDisplayed=false;
    	AlarmService.alarmActivity=null;
    }
    private void initUI()
    {
        etSequence = (EditText)findViewById(R.id.etSequence);
        btnStart = (Button)findViewById(R.id.btn_main);
        sp_toneSelector = (Spinner)findViewById(R.id.sp_tone_selector);
        sp_sequence_selector = (Spinner)findViewById(R.id.spinner_sequence_selector_id);
        tvStatus = (TextView)findViewById(R.id.tv_status);
        
        
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        tvTop = (TextView)findViewById(R.id.tv_header);
        tvRemaining = (TextView)findViewById(R.id.tv_remaining);

    }
    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
       
        
        initUI();
        

        
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
        map[0] = R.raw.longbeep;
        map[1] = R.raw.shortbeep;
        
        AlarmService.selectedTune=map[initialToneIndex];
        
               
       updateUIItemState();
       updateSequenceSpinner();
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
			
			String text = prefs.getString("sequence"+index, "1 2 3");
			etSequence.setText(text);
			
		}

		public void onNothingSelected(AdapterView<?> arg0)
		{
			// TODO Auto-generated method stub
			
		}
    	
    }
  
    public void addSequence(String text)
    {
    	int numberOfSequences = prefs.getInt("NumberOfSequences", 0);
    	prefsEditor.putInt("NumberOfSequences", numberOfSequences+1);
    	prefsEditor.putInt("SelectedSequenceIndex", numberOfSequences);
    	prefsEditor.putString("sequence"+numberOfSequences, text);
    	prefsEditor.commit();
    	updateSequenceSpinner();
    }
	public void onClick(View v) 
	{

		// TODO Auto-generated method stub
		if(v.getId()== R.id.btn_main)
		{
			if(AlarmService.alarmRunning==false)
			{	

				if(etSequence.getText().toString().trim().equals(""))
				{
					Toast.makeText(getApplicationContext(), "Set sequence first !!", Toast.LENGTH_LONG).show();
					return;
				}
				String text = etSequence.getText().toString().trim();

				boolean success = AlarmService.initialiseNewSequence(text);
				if(success)
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
			}
		
			else 
			{
				stopAlarm();
				etSequence.setEnabled(true);
			}
		}	
//        timer.schedule(myTimerTask, nextDelay()*1000);
	}
	
	
	public void updateUIItemState()
	{
		if(AlarmService.alarmRunning == false) //not running
		{
			tvStatus.setText("Alarm is currently stopped");			
			tvRemaining.setText("");
			btnStart.setText(startText);
			btnStart.setBackgroundResource(R.drawable.button_shape);
			progressBar.setProgress(0);
		}
		else
		{
			tvStatus.setText("Alarm running. Cycle : "+ AlarmService.cycle);
			tvRemaining.setText(" "+(AlarmService.totalSeconds) + " second(s) to next alarm");
			btnStart.setBackgroundResource(R.drawable.button_shape_red);
			btnStart.setText(stopText);
	   		int progress = AlarmService.elapsed*100/AlarmService.totalSeconds;
	   		progressBar.setProgress(progress);

		}
	}
	public void startAlarm()
	{
		progressBar.setProgress(0);
		
		etSequence.setEnabled(false);
		updateUIItemState();
		startService(new Intent(AlarmActivity.this,AlarmService.class));
		
	}
	
	public void stopAlarm()
	{
	
		progressBar.setProgress(0);
		
		stopService(new Intent(AlarmActivity.this,AlarmService.class));
	}
	
	
   
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
	{
		// TODO Auto-generated method stub
		int item = sp_toneSelector.getSelectedItemPosition();
		
		prefsEditor.putInt(toneKey, item);
		prefsEditor.commit();	
		
        AlarmService.selectedTune=map[item];

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
			    	 
			    	 if(AlarmService.mediaPlayer.isPlaying())
			    		 AlarmService.mediaPlayer.stop();
			    	 
			      popupWindow.dismiss();
			     }});
			               
	   // popupWindow.showAsDropDown(btnOpenPopup, 50, -30);
	    popupWindow.showAtLocation(popupView.getRootView(),Gravity.CENTER, 0, 0);

	}
}