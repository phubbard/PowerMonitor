package net.phfactor.powermonitor;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActionBar;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends FragmentActivity implements
		ActionBar.OnNavigationListener
{
	static final String TAG = "PowerMonitor";
	
	private Handler mHandler = new Handler();
	private Runnable rUpdater;
	public static TextView curPower, maxReading, difference;
	public static ProgressBar cpBar;
	public static double tareValue;
	
	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String STATE_SELECTED_NAVIGATION_ITEM = "selected_navigation_item";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set up the action bar to show a dropdown list.
		final ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);

		// Set up the dropdown list navigation in the action bar.
		actionBar.setListNavigationCallbacks(
		// Specify a SpinnerAdapter to populate the dropdown list.
				new ArrayAdapter<String>(actionBar.getThemedContext(),
						android.R.layout.simple_list_item_1,
						android.R.id.text1, new String[] {
								getString(R.string.title_section1),
								getString(R.string.title_section2),
								getString(R.string.title_section3), }), this);
		
		// See http://android-developers.blogspot.com/2007/11/stitch-in-time.html
		rUpdater = new Runnable()
		{
			public void run()
			{
				GetReading reader = new GetReading();
				reader.execute();
				
				mHandler.postDelayed(this, Constants.UPDATE_DELAY_MSEC);
			}
		};
		
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState)
	{
		// Restore the previously serialized current dropdown position.
		if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM))
		{
			getActionBar().setSelectedNavigationItem(
					savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState)
	{
		// Serialize the current dropdown position.
		outState.putInt(STATE_SELECTED_NAVIGATION_ITEM, getActionBar()
				.getSelectedNavigationIndex());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onNavigationItemSelected(int position, long id)
	{
		// When the given dropdown item is selected, show its contents in the
		// container view.
		Fragment fragment = new DummySectionFragment();
		Bundle args = new Bundle();
		args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
		fragment.setArguments(args);
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).commit();
		return true;
	}

	 /*!
	    * @brief To convert the InputStream to String we use the BufferedReader.readLine()
	    * method. We iterate until the BufferedReader return null which means
	    * there's no more data to read. Each line will appended to a StringBuilder
	    * and returned as String.
	    */
	private static String convertStreamToString(InputStream is) 
	{
       BufferedReader reader = new BufferedReader(new InputStreamReader(is));
       StringBuilder sb = new StringBuilder();

       String line = null;
       try {
           while ((line = reader.readLine()) != null) {
               sb.append(line + "\n");
           }
       } catch (IOException e) {
           e.printStackTrace();
       } finally {
           try {
               is.close();
           } catch (IOException e) {
               e.printStackTrace();
           }
       }
       return sb.toString();
   }
		
	public static String syncGetUrl(String url)
	{
		HttpURLConnection connection = null;
		try
		{
			URL mURL = new URL(url);
			connection = (HttpURLConnection) mURL.openConnection();
			
			// OK, should be connected at this point, try and read the response.
			InputStream in = new BufferedInputStream(connection.getInputStream());
			String res_str = convertStreamToString(in);
			return res_str;			
		}
		catch (MalformedURLException me)
		{
			Log.e(TAG, "Bad server URL '" + url + "'");
			return null;
		}
		catch (IOException ie)
		{
			Log.e(TAG, "IO error on transfer: " + ie.getMessage());
			Log.e(TAG, "error was", ie);
			return null;
		}
		finally
		{
			if (connection != null)
				connection.disconnect();
		}		
	}
	
	public static void displayReadings(Double cur, Double rmax)
	{
		curPower.setText(String.format("%4.2f", cur));
		maxReading.setText(String.format("%4.2f", rmax));
		
		int progress = (int) ((cur / rmax) * 100.0);
		cpBar.setProgress(progress);
		
		difference.setText(String.format("%4.2f", cur- tareValue));
	}
	
	public static class GetReading extends AsyncTask<Void, Void, String>
	{
		double cp, mp;
		
		private void parseDatum(String datum)
		{
			if (datum == null)
				return;
			
			JSONObject jobj;
			try
			{
				jobj = new JSONObject(datum);
				
				JSONObject jdata = jobj.getJSONObject("strip");
				cp = jdata.getDouble("watts");
				mp = jdata.getDouble("peak");
			}
			catch (JSONException je)
			{
				Log.e(TAG, "Error parsing data");
				cp = mp = 0.0;
			}
		}
		
		@Override
		protected void onPostExecute(String result)
		{
			parseDatum(result);
			displayReadings(cp, mp);
			
			super.onPostExecute(result);
		}

		@Override
		protected String doInBackground(Void... params)
		{
			return syncGetUrl(net.phfactor.powermonitor.Constants.URL);
		}
	}
	
	@Override
	protected void onPause()
	{
		mHandler.removeCallbacks(rUpdater);
		super.onPause();
	}

	@Override
	protected void onResume()
	{
		mHandler.postDelayed(rUpdater, Constants.UPDATE_DELAY_MSEC);		
		super.onResume();
	}
	
	/**
	 * A fragment representing a section of the app, but that just calls fetch/display
	 */
	public static class DummySectionFragment extends Fragment implements OnClickListener
	{
		Button tare;
		/**
		 * The fragment argument representing the section number for this
		 * fragment.
		 */
		public static final String ARG_SECTION_NUMBER = "section_number";

		public DummySectionFragment()
		{
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState)
		{
			View rootView = inflater.inflate(R.layout.fragment_main_dummy,	container, false);
			
			curPower = (TextView) rootView.findViewById(R.id.curPowerTxt);
			maxReading = (TextView) rootView.findViewById(R.id.maxPowerTxt);
			difference = (TextView) rootView.findViewById(R.id.curDifference);
			cpBar = (ProgressBar) rootView.findViewById(R.id.curPowerBar);
						
			tare = (Button) rootView.findViewById(R.id.button1);
			tare.setOnClickListener(this);
			
			tareValue = 0.0;
			return rootView;
		}

		@Override
		public void onClick(View v)
		{
			if (tare != null)
				tareValue = Double.parseDouble(curPower.getText().toString());
			else
				Log.e(TAG, "null button ref");
		}
	}

}
