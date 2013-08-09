package com.Test.IntegrateTsunami;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.MotionEvent;
import android.view.View;

public class OverlayView extends View{
	
	public static String TAG = "";
	public IntegrateTsunamiActivity mainActivity ;

	public OverlayView(Context context) {
		super(context);
		mainActivity = (IntegrateTsunamiActivity)this.getContext();
		TAG = mainActivity.TAG;
		// TODO Auto-generated constructor stub
		
	}
	
	// タッチイベント
	@Override
	public boolean onTouchEvent(MotionEvent event){
		mainActivity.onTouchEventHandler(event);
		return true;
	}
	
	
	@Override
	public void onDraw (Canvas c){
		Paint paint = new Paint();
		paint.setARGB(255,255,255,255);
		String [] a = new String[15];
		a[0] = "magX:" + mainActivity.magX;
		a[1] = "magY:" + mainActivity.magY;
		a[2] = "magZ:" + mainActivity.magZ;
		a[3] = "waterHeight:" + mainActivity.waterHeight;
		a[4] = "floodHeight:" + mainActivity.floodHeight;
		a[5] = "userHeight:" + mainActivity.userHeight;
		a[6] = "RPCSX:" + mainActivity.RPCSX;
		a[7] = "RPCSY:" + mainActivity.RPCSY;
		a[8] = "latitude:" + mainActivity.latitude;
		a[9] = "longitude:" + mainActivity.longitude;
		a[10] = "offsetRPCSX:" + mainActivity.offsetRPCSX;
		a[11] = "offsetRPCSY:" + mainActivity.offsetRPCSY;
		a[12] = "currentMode:" + mainActivity.currentMode;
		a[13] = "FLAG_SYNC_SENSOR:" + mainActivity.FLAG_SYNC_SENSOR;
		a[14] = "FLAG_SYNC_GPS:" + mainActivity.FLAG_SYNC_GPS;
		int r = 315;
		for(int i=0; i<a.length; i++){
			c.drawText(a[i], 10, r, paint);
			r+=15;
		}
		//c.drawText("unko", 10, 50, paint);
		invalidate();
	}
}
