package com.prach.mashup.barcodereview;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

public class BookDatabaseService extends Service {
	private static final String TAG = "com.prach.mashup.BookDatabaseService";
	private static final int BDB_SERVICE_CODE = 0x62646201; //GPS1
	//62 64 62 BDB
	
	@Override
	public IBinder onBind(Intent arg0) {
		return new BDBServiceBinder();
	}
	
	public void debug(String msg){
		Log.d(TAG,msg);
	}
	
	private class BDBServiceBinder extends Binder {
		@Override
		protected synchronized boolean onTransact(int code, Parcel data, Parcel reply,int flags) {
			if(code==BDB_SERVICE_CODE){
				Bundle bundle = data.readBundle();
				String command = bundle.getString("COMMAND");
				Log.i(TAG,"COMMAND="+command);
				if(command.equals("Add")){
					//INPUT: COMMAND(Add) ISBN Title Price
					//OUTOUT: RESULT(TRUE/FALSE)
				}else if(command.equals("Summary")){
					//INPUT: COMMAND(Summary)
					//OUTPUT: ISBN[] Title[] Price[] 
				}
				return true;
			}else 
				return false;
		}
	}
}
