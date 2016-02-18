package com.abysmel.dashspinner;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements DashSpinner.OnDownloadIntimationListener {

	float       mnProgress   = 0.0f;
	DashSpinner mDashSpinner = null;

	Handler mHandler = new Handler();

	//Run to 100% and then show Success
	Runnable runnableSuccess = new Runnable() {
		@Override
		public void run() {
			setProgress();

			//SUCCESS
			if (mnProgress <= 1.0) mHandler.postDelayed(this, 30);
			else mDashSpinner.showSuccess();
		}
	};

	//Run to 50% and show failure
	Runnable runnableFailure = new Runnable() {
		@Override
		public void run() {
			setProgress();
			//FAILURE
			if (mnProgress <= 0.5) mHandler.postDelayed(this, 30);
			else mDashSpinner.showFailure();
		}
	};

	//Show Unknown Error
	Runnable runnableUnknown = new Runnable() {
		@Override
		public void run() {
			//UNKNOWN
			mDashSpinner.showUnknown();
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mDashSpinner = (DashSpinner) findViewById(R.id.progress_spinner);
		mDashSpinner.setOnDownloadIntimationListener(this);

		//Success Button
		findViewById(R.id.success_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDashSpinner.resetValues();
				mnProgress = 0.0f;
				mHandler.post(runnableSuccess);
			}
		});

		//Failure Button
		findViewById(R.id.failure_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDashSpinner.resetValues();
				mnProgress = 0.0f;
				mHandler.post(runnableFailure);
			}
		});

		//Unknown Error Button
		findViewById(R.id.unknown_btn).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mDashSpinner.resetValues();
				mnProgress = 0.0f;
				mHandler.post(runnableUnknown);
			}
		});
	}


	private void setProgress() {
		mnProgress += 0.01;
		mDashSpinner.setProgress(mnProgress);
	}

	@Override
	public void onDownloadIntimationDone(DashSpinner.DASH_MODE dashMode) {
		switch (dashMode) {
			case SUCCESS:
				Toast.makeText(this, "Download Successful!", Toast.LENGTH_SHORT).show();
				break;
			case FAILURE:
				Toast.makeText(this, "Download Failed!", Toast.LENGTH_SHORT).show();
				break;
			case UNKNOWN:
				Toast.makeText(this, "Unknown Download Error!", Toast.LENGTH_SHORT).show();
				break;
		}
	}
}
