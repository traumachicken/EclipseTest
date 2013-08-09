package com.Test.IntegrateTsunami;

import jp.jasminesoft.gcat.scalc.DMSconv;

import jp.jasminesoft.gcat.scalc.LatLong2XY;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Config;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.ViewFlipper;

import com.unity3d.player.UnityPlayer;
import com.unity3d.player.UnityPlayerActivity;

public class IntegrateTsunamiActivity extends UnityPlayerActivity implements SensorEventListener, LocationListener, OnClickListener {
	
	public static final String TAG = "Tsunami";
	
	//東京モデルの左下の平面直角座標
	//public static final double TKYOffsetX = -35998.4575713; 
	//public static final double TKYOffsetY = -7500.4590171;
	
	//宮城大学モデルの平面直角座標
	public static final double TKYOffsetX = -183453.02320; 
	public static final double TKYOffsetY = 413.2737542;
		
	//自宅
	public static final double MYOffsetX = -36970.2458322;
	public static final double MYOffsetY = -33882.7526542;
	
	//エスペランサ
	public static final double BubaiOffsetX = -36818.7690171448;
	public static final double BubaiOffsetY = -33670.37175097716;
	
	//大学
	//public static final double MYOffsetX = 
	
	//CAMERA ROTATION
	private SensorManager mSensorManager;
	private Sensor mMagneticField;
	private Sensor mAccelerometer;
	public static float[] mMagneticFieldValues;
	public static float[] mAccelerometerValues;
	public static final int DIMENSION = 3;
	public static final int MATRIX_SIZE = 16;
	private boolean mMagneticFieldRegistered;
	private boolean mAccelerometerRegistered;
	public float magX;
	public float magY;
	public float magZ;
	
	//Touch Event
	private PointF mLastDown = new PointF(0,0);
	private float touchSensitivity = 15; // 低いほど敏感
	private float touchThreshold = 6; // この値以上の大きさの入力は無視する

	//GPS
	private boolean GPSInit = false ;
	private LocationManager mLocationManager = null;
	private static Location mNowLocation = null;
	public double latitude = 0;
	public double longitude = 0;
	public double RPCSX = 0;
	public double RPCSY = 0;
	public double offsetRPCSX = 0;
	public double offsetRPCSY = 0;
	public double lastRPCSX = 0;
	public double lastRPCSY = 0;
	
	public int kei = 0;

	private float lastMagX = 0;
	private float lastMagY = 0;
	private float lastMagZ = 0;
	
	//Height
	public double userHeight = 1.6; // camera height from ground
	public double overlookHeight = 400 ;
	
	//MODE
	public int currentMode = 0;
	public static final int MODE_PERSPECTIVE = 0;
	public static final int MODE_OVERLOOK = 1;
	
	//FLAG
	public boolean FLAG_SYNC_SENSOR = true ;
	public boolean FLAG_SYNC_GPS = true ;
	public boolean FLAG_SYNC_GROUND = true ;
	public boolean FLAG_GALAXY_FIX = false ;
	private boolean mTouch;
	
	//MENU
	public static final int MENU_SELECT_PERSPECTIVE = 0;
	public static final int MENU_SELECT_OVERLOOK = 1;
	public static final int MENU_SELECT_SYNC_SENSOR = 2;
	public static final int MENU_SELECT_SYNC_GPS = 3;
	public static final int MENU_SELECT_SYNC_GROUND = 4;
	public static final int MENU_SELECT_RENDERING_OPTION = 5;
	public static final int MENU_SELECT_PREFERENCE = 6;
	
	// Materialの設定
	public int currentBuildingMaterial = 0 ;
	public int currentWaterMaterial = 0 ;
	public int currentGroundMaterial = 0 ;
	public static final int MATERIAL_TEXTURE = 0;
	public static final int MATERIAL_TRANSPARENT = 1;
	public static final int MATERIAL_CULLING = 2;
	public static final int MATERIAL_GRID = 3;
	
	//
	public float waterHeight = 10; // 水面高（0地点からの）
	
	// Unityからアクセス
	// 無いとエラー
	// 浸水高は地形モデルから算出するのでUnityじゃないと計算できない
	public double floodHeight = 0; // 浸水高（地面からの）
	
	//
	public boolean FLAG_WAVE_ANIMATION = false ;
	
	//
	public boolean UNITY_READY = false ;
	
	
	// Layout
	public ViewFlipper flipper;
	public static final int VIEW_MAP_ID = 0;
	public static final int VIEW_AR_ID = 1;
	public static final int VIEW_WATER_ID = 2;
	public static final int VIEW_FLOOD_ID = 3;
	
	// mode
	public static final int MODE_MAP = 0;
	public static final int MODE_AR = 1;
	public static final int MODE_WATER = 2;
	public static final int MODE_FLOOD = 3;
	
	// NEXUS
	public static final boolean MODE_NEXUS = true;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        
        // Sensorの初期化
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
		if (mSensorManager != null) {
			mMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
			mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		}
		// GPSの初期化
		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		
        
        // Viewの初期化
        viewSetup();
        
        // GPSの値を取得するまで初期位置に設定
        RPCSX = offsetRPCSX ;
        RPCSY = offsetRPCSY ;
        lastRPCSX = RPCSX ;
        lastRPCSY = RPCSY ;
        
        // test
        setUserHeight(1.7);
        UpdateWaveAnimationState();
    }
    
    public void viewSetup(){
    	
    	// TouchEventを取得するのに必要　+その他
    	addContentView(new OverlayView(this), new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
    	
    	
    	// add main view	
    	View view = this.getLayoutInflater().inflate(R.layout.main, null);
    	addContentView(view,new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
    	
    	// main view UI
    	//
    	//
    	// store button
    	Button flipNextButton = (Button)findViewById(R.id.flip_next_btn);
    	flipNextButton.setOnClickListener((OnClickListener) this);
    	Button flipPreviousButton = (Button)findViewById(R.id.flip_previous_btn);
    	flipPreviousButton.setOnClickListener((OnClickListener) this);
    	// find flipper
    	flipper = (ViewFlipper)findViewById(R.id.flipper);
    	
    	
    	
    }
    
    
    @Override
	protected void onResume() {
				
		if (Config.DEBUG)
			Log.d(TAG, "onResume");
		
		//MagneticField
		if (mMagneticField != null) {
			mSensorManager.registerListener(this, mMagneticField,
					SensorManager.SENSOR_DELAY_GAME);
			mMagneticFieldRegistered = true;
		}
		//Accelerometer
		if (mAccelerometer != null) {
			mSensorManager.registerListener(this, mAccelerometer,
					SensorManager.SENSOR_DELAY_GAME);
			mAccelerometerRegistered = true;
		}
		//GPS
		if (mLocationManager != null)
			mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
		super.onResume();
	}
    
    
    @Override
	protected void onPause() {
		
		//Sensor
		if (mMagneticFieldRegistered) {
			mSensorManager.unregisterListener(this, mMagneticField);
			mMagneticFieldRegistered = false;
		}
		if (mAccelerometerRegistered) {
			mSensorManager.unregisterListener(this, mAccelerometer);
			mAccelerometerRegistered = false;
		}
		//GPS
		if(mLocationManager!=null)
			mLocationManager.removeUpdates(this);
		
		super.onPause();
	}
    
    @Override
	protected void onStop() {
		super.onStop();
		if (Config.DEBUG)
			Log.d(TAG, "onStop");
		mSensorManager.unregisterListener(this);
	}
    
    
    //センサーの更新
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Skip if unreliable.
		/*
		if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE){
			Log.d(TAG, "Sensors unreliable. Update skipped.");
			  return;
		}
		*/
		
		// TODO Auto-generated method stub
		switch (event.sensor.getType()) {
		case Sensor.TYPE_MAGNETIC_FIELD:
			mMagneticFieldValues = event.values.clone();
			break;
		case Sensor.TYPE_ACCELEROMETER:
			mAccelerometerValues = event.values.clone();
			break;
		}

		if (mMagneticFieldValues != null && mAccelerometerValues != null) {
			float[] rotationMatrix = new float[MATRIX_SIZE];
			float[] inclinationMatrix = new float[MATRIX_SIZE];
			float[] remapedMatrix = new float[MATRIX_SIZE];
			float[] orientationValues = new float[MATRIX_SIZE];

			SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix,
					mAccelerometerValues, mMagneticFieldValues);
			SensorManager.remapCoordinateSystem(rotationMatrix,
					SensorManager.AXIS_X, SensorManager.AXIS_Z, remapedMatrix);
			SensorManager.getOrientation(remapedMatrix, orientationValues);

			magX = (float) Math.toDegrees(orientationValues[1]);
			magY = (float) Math.toDegrees(orientationValues[2]);
			magZ = (float) Math.toDegrees(orientationValues[0]);
			
			if ( MODE_NEXUS )
			{
				magX *= -1;
				magY *= -1;
				magZ *= -1;
			}
		}
		
		if(FLAG_SYNC_SENSOR)	UpdateCameraRotationBySensor();
	}
	
	// GPSの更新
	@Override
	public void onLocationChanged(Location arg0) {
		
		// 緯度経度の取得
		mNowLocation = arg0 ;
		latitude = (double) mNowLocation.getLatitude(); //digit
		longitude = (double) mNowLocation.getLongitude(); //digit
		
		// 平面直角座標系へ変換
		LatLong2XY ll2xy = new LatLong2XY(-1);
		
		//　10進数からDMSへ変換
		ll2xy.setLatitude(DMSconv.deg2dms((float)latitude));
		ll2xy.setLongitude(DMSconv.deg2dms((float)longitude));
		
		// 変数へ代入
		kei = ll2xy.getKei();
		RPCSX = ll2xy.getX();
		RPCSY = ll2xy.getY();
		
		// Debug
		Log.d(TAG, ""+kei+":"+DMSconv.deg2dms((float)latitude)+","+DMSconv.deg2dms((float)longitude)+","+RPCSX+","+RPCSY);
		
		// 初期動作
		// とりあえず現在地でマップが見えるオフセット値に設定
		if(GPSInit == false){
			setOffsetRPCS(RPCSX, RPCSY);
			GPSInit = true ;
		}
		
		// GPS同期がオンならアップデート
		if(FLAG_SYNC_GPS)	UpdateCameraPositionByGPS();
	}
	
	//　GPSでカメラを移動
	public void UpdateCameraPositionByGPS() {
		UnityPlayer.UnitySendMessage("Main Camera", "setRPCSX", String.valueOf(RPCSX - offsetRPCSX));
		UnityPlayer.UnitySendMessage("Main Camera", "setRPCSY", String.valueOf(RPCSY - offsetRPCSY));
		lastRPCSX = RPCSX;
		lastRPCSY = RPCSY;
	}

	// タッチでカメラを移動
	public void UpdateCameraPositionByTouch() {
		UnityPlayer.UnitySendMessage("Main Camera", "setRPCSX", String.valueOf(lastRPCSX - offsetRPCSX));
		UnityPlayer.UnitySendMessage("Main Camera", "setRPCSY", String.valueOf(lastRPCSY - offsetRPCSY));
	}

	// モデルの原点の直角平面座標で設定
	public void setOffsetRPCS( double ox, double oy ){
		offsetRPCSX = ox ;
		offsetRPCSY = oy ;
	}
	
	// モデルの原点を緯度経度で設定
	// 使ってない？
	public void setOffsetLLCS( double ox, double oy ){
		LatLong2XY ll2xy = new LatLong2XY(-1);
		ll2xy.setLatitude(DMSconv.deg2dms((float)latitude));
		ll2xy.setLongitude(DMSconv.deg2dms((float)longitude));
		setOffsetRPCS(ll2xy.getX(), ll2xy.getY());
	}
	
	// ユーザーの地面からの高さ
	public void setUserHeight( double height ){
		userHeight = height ;
		// Unity側更新
		UpdateUserHeight(userHeight);
	}

	@Override
	public void onProviderDisabled(String provider) {}
	@Override
	public void onProviderEnabled(String provider) {}
	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {}
	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {	}
	
	// タッチイベントハンドラー
	// OverlayViewからコールバック
	//　デフォルトのはUnityが横取りするので使えない
	public boolean onTouchEventHandler(MotionEvent event){
		
		// debug log
		Log.d(TAG, ""+event.getAction());
		
		// 
		switch(event.getAction()){
			case MotionEvent.ACTION_DOWN: // 押されたとき
				//　タッチフラグ（使ってない）
				mTouch = true ;

				// センサーとGPSの同期をオフ
				if(currentMode == MODE_AR)		FLAG_SYNC_SENSOR = false ;
				if(currentMode == MODE_MAP)		FLAG_SYNC_GPS = false ;
				
				// 最期にタッチされた位置を記憶
				updateLastTouch(event);
				
				Test();
				break;
			case MotionEvent.ACTION_UP: // 離されたとき
				
				// 同期が切れるのでボタンを表示
				if(currentMode == MODE_PERSPECTIVE && !FLAG_SYNC_SENSOR){
					// 同期ボタンの追加
				}
				
				// 同期が切れるのでボタンを表示
				if(currentMode == MODE_OVERLOOK && !FLAG_SYNC_GPS){
					// 同期ボタンの追加
				}
				
				// タッチオフ（使ってない）
				mTouch = false ;
				
				break;
			case MotionEvent.ACTION_MOVE: //　ドラッグ時
				
				// ドラッグ量を取得
				PointF diff = touchMove(event);
				
				// 現在のモードによって動作を変更
				if		(currentMode == MODE_AR)		scrollCameraAngle(diff);
				else if	(currentMode == MODE_MAP)		scrollCameraPosition(diff);	
				else if (currentMode == MODE_WATER)		scrollUserHeight(diff);
				else if (currentMode == MODE_FLOOD) 	scrollWaterHeight(diff);
				break;

			default:
				break;
		}
		return false;
	}
	
	public void updateLastTouch(MotionEvent event){
		
		// 最期にタッチされた位置
		mLastDown.x = event.getX();
		mLastDown.y = event.getY();
		
	}
	
	// ドラッグ量を返す
	public PointF touchMove(MotionEvent event){
		PointF diff = new PointF();
		
		// 最期にタッチした位置からドラッグ量を計算
		diff.x = mLastDown.x - event.getX();
		diff.y = mLastDown.y - event.getY();
		
		// 最期にタッチした位置を更新 
		updateLastTouch(event);
		
		// ドラッグ量を返す
		return diff;
	}

	public void scrollCameraAngle(PointF diff){
		lastMagZ += diff.x / touchSensitivity;
		lastMagX += diff.y / touchSensitivity;
		UpdateCameraRotationByTouch();
	}
	
	private void scrollCameraPosition(PointF diff) {
		lastRPCSX += diff.x / touchSensitivity;
		lastRPCSY += diff.y / touchSensitivity;
		UpdateCameraPositionByTouch();
	}
	
	private void scrollUserHeight(PointF diff) {
		userHeight += diff.y / ( touchSensitivity * 4 );
		UpdateUserHeight(userHeight);
	}
	
	private void scrollWaterHeight(PointF diff) {
		waterHeight += diff.y / ( touchSensitivity * 4 );
		UpdateWaterHeight();
	}
	
	//　オプションメニューの初期化
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.game_menu, menu);
	    return true;
	}
	
	//　オプションメニューボタンの設定
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.SET_ORIGIN) {
			setOffsetRPCS(RPCSX-600, RPCSY-600);
			ForceUpdatePosition();
			return true ;
		} else if (item.getItemId() == R.id.SET_LOCAL){
			setOffsetRPCS(TKYOffsetX, TKYOffsetY);
			ForceUpdatePosition();
			return true;
		} else if (item.getItemId() == R.id.MENU_SELECT_SYNC_SENSOR){
			FLAG_SYNC_SENSOR = !FLAG_SYNC_SENSOR;
			return true;
		} else if (item.getItemId() == R.id.MENU_SELECT_SYNC_GPS){
			FLAG_SYNC_GPS = !FLAG_SYNC_GPS;
			return true;
		} else if (item.getItemId() == R.id.MENU_SELECT_SYNC_GROUND){
			FLAG_SYNC_GROUND = !FLAG_SYNC_GROUND;
			setGrounding();
			return true;
		} else if (item.getItemId() == R.id.MENU_SELECT_PREFERENCE){
			return true;
		} else if (item.getItemId() == R.id.NEXT_BUILDING_MATERIAL){
			UpdateBuildingMaterial();
			return true;
		} else if (item.getItemId() == R.id.NEXT_WATER_MATERIAL){
			UpdateWaterMaterial();
			return true;
		} else if (item.getItemId() == R.id.NEXT_GROUND_MATERIAL){
			UpdateGroundMaterial();
			return true;
		} else if (item.getItemId() == R.id.GALAXY_FIX){
			ToggleGalaxyFix();
			return true;
		} else if (item.getItemId() == R.id.CAPTURE_SCREEN){
			CaptureScreen();
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}
	
	// UnityPlayerActivityがTrueを返すとメニューが表示されないのでオーバーライドする必要がある
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if( keyCode == KeyEvent.KEYCODE_MENU )	return false;
		if( keyCode == KeyEvent.KEYCODE_HOME )	{
			finish();
			return true ;
		}
        return super.onKeyDown(keyCode, event); 
	}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if( keyCode == KeyEvent.KEYCODE_MENU )	return false;
        return super.onKeyDown(keyCode, event); 
	}
	
	
	/*** Unityに送信 ***/
	
	// カメラのアングルの更新
	public void UpdateCameraRotationBySensor() {
		UnityPlayer.UnitySendMessage("Main Camera", "setAngleX", String.valueOf(magX));
		UnityPlayer.UnitySendMessage("Main Camera", "setAngleY", String.valueOf(magY));
		UnityPlayer.UnitySendMessage("Main Camera", "setAngleZ", String.valueOf(magZ));
		lastMagX = magX;
		lastMagY = magY;
		lastMagZ = magZ;
	}
	
	// カメラのアングルの手動更新
	public void UpdateCameraRotationByTouch() {
		// Log.d(TAG, ""+lastMagX+", "+lastMagY+", "+lastMagZ);
		UnityPlayer.UnitySendMessage("Main Camera", "setAngleX", String.valueOf(lastMagX));
		UnityPlayer.UnitySendMessage("Main Camera", "setAngleY", String.valueOf(lastMagY));
		UnityPlayer.UnitySendMessage("Main Camera", "setAngleZ", String.valueOf(lastMagZ));
	}
	
	// 位置を即時反映
	// Unity送信
	public void ForceUpdatePosition() {
		UpdateCameraPositionByGPS();
		UnityPlayer.UnitySendMessage("Main Camera", "forceUpdateLocation", "");
	}
	
	// 地面からのカメラの高さ
	// Unity送信
	public void UpdateUserHeight(double height) {
		UnityPlayer.UnitySendMessage("Main Camera", "setUserHeight", String.valueOf(height));
	}
	
	// 地面に立つかどうか
	// Unity送信
	public void setGrounding() {
		UnityPlayer.UnitySendMessage("Main Camera", "setGrounding", String.valueOf(FLAG_SYNC_GROUND));
	}
	
	// 建物のマテリアルのサイクルを次へ
	// Unity送信
	public void UpdateBuildingMaterial(){
		currentBuildingMaterial += 1;
		if(currentBuildingMaterial > 3)	currentBuildingMaterial = 0;
		UnityPlayer.UnitySendMessage("Buildings", "setMaterialMode", String.valueOf(currentBuildingMaterial));
	}
	
	//　水面のマテリアルのサイクルを次へ
	// Unity送信
	public void UpdateWaterMaterial(){
		currentWaterMaterial += 1;
		if(currentWaterMaterial > 3)	currentWaterMaterial = 0;
		UnityPlayer.UnitySendMessage("Water Surface", "setMaterialMode", String.valueOf(currentWaterMaterial));
	}
	
	// 地形のマテリアルのサイクルを次へ
	// Unity送信
	public void UpdateGroundMaterial(){
		currentGroundMaterial += 1;
		if(currentGroundMaterial > 3)	currentGroundMaterial = 0;
		UnityPlayer.UnitySendMessage("Ground Surface", "setMaterialMode", String.valueOf(currentGroundMaterial));
	}
	
	// 水面の高さを更新
	// Unity送信
	public void UpdateWaterHeight(){
		UnityPlayer.UnitySendMessage("Water Surface", "setHeight", String.valueOf(waterHeight));
	}
	
	//　未実装
	public void UpdateWaveAnimationState(){
		//UnityPlayer.UnitySendMessage("", "setWavingAnimation", String.valueOf(FLAG_WAVE_ANIMATION));
	}
	
	// Galaxyバグ対応
	// Unity送信
	public void ToggleGalaxyFix(){
		FLAG_GALAXY_FIX = !FLAG_GALAXY_FIX ;
		UnityPlayer.UnitySendMessage("Main Camera", "setGalaxyFix", String.valueOf(FLAG_GALAXY_FIX));
	}
	
	// スクリーンショットの撮影
	// Unity送信
	public void CaptureScreen(){
		UnityPlayer.UnitySendMessage("Main Camera", "captureScreen", "");
	}
	
	/*
	 * Unity起動後に呼びに来る。
	 * 正確にはGame ControllerがStart()したとき。
	 */
	public void UnityReadyCallback(){
		UNITY_READY = true;
	}
	
	// touch に配置
	// ？
	public void Test(){
		UpdateWaterHeight();
		Log.d(TAG, "floodHeight: "+floodHeight);
	}
	
	 @Override
     public void onUserLeaveHint(){
             // ホームボタンが押された時や、他のアプリが起動した時に呼ばれる
             // 戻るボタンが押された場合には呼ばれない
            finish();
     }

	 // ボタンハンドラ
	@Override
	public void onClick(View v) {
		// 押されたボタンのIDを取得
		switch(v.getId()){
		
			// [次へ]ボタン
			case R.id.flip_next_btn:
				
				// flipperを次へ
				flipper.showNext();
				
				// 現在のモードを更新
				currentMode = flipper.indexOfChild(flipper.getCurrentView());
				
				// DEBUG
				Log.d(TAG, ""+flipper.getCurrentView().getId());
				Log.d(TAG, "index"+flipper.indexOfChild(flipper.getCurrentView()));
				break;

			// [前へ]ボタン
			case R.id.flip_previous_btn:
				
				// flipperを前へ
				flipper.showPrevious();
				
				// 現在のモードを更新
				currentMode = flipper.indexOfChild(flipper.getCurrentView());
				
				// DEBUG
				Log.d(TAG, ""+flipper.getCurrentView().getId());
				Log.d(TAG, "index"+flipper.indexOfChild(flipper.getCurrentView()));
				break;
		}
	}
}