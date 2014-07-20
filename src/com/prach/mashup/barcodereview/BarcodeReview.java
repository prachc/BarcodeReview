package com.prach.mashup.barcodereview;

import java.util.List;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;

public class BarcodeReview extends Activity {
	private int[] ProcessNumber = null;
    private String[] ProcessName = null;
    private int current_pnum;
    private Parcel reply1,reply2,reply3; 
    
    private String BarcodeSearch;
    private String ProductTitle;
    private String ProductPrice;
    private String BookImage;
    private String BookDescription;
    private String BookReview;
    private String YenPrice; 
    private String TranslatedDescription;
    private String TranslatedReview;
    
	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ProcessNumber = getResources().getIntArray(R.array.process_number);
        ProcessName = getResources().getStringArray(R.array.process_name);
        current_pnum = -1;	
    }
    
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			current_pnum = -1;
			onResume();
		}
		return super.onKeyDown(keyCode, event);
	}
    
    private static final int SERVICE_PARCEL_READY_1 = 0x101;
    private static final int SERVICE_PARCEL_READY_2 = 0x102;
    private static final int SERVICE_PARCEL_READY_3 = 0x103;
    private static final int SERVICE_PARCEL_ALL_READY = 0x10F;
    private static final int SERVICE_PARCEL_FAILED = 0xF01;
    
    private boolean bService1 = false,bService2 = false,bService3=false;
    
    private Handler mHandler = new Handler() {
        @Override public void handleMessage(Message msg) {
            switch (msg.what) {
                case SERVICE_PARCEL_READY_1:
                	Bundle replybundle1 = reply1.readBundle();
                	if(replybundle1==null)
                		Log.i("SERVICE_PARCEL_READY_1","null");
                	String[] outputs1 = replybundle1.getStringArray("OUTPUTS");
                	YenPrice = outputs1[0];
					bService1 = true;

					if(bService2&&bService3)
						mHandler.sendEmptyMessage(SERVICE_PARCEL_ALL_READY);
					else
						debug("bService1 finished, other=not yet");
					break;
                case SERVICE_PARCEL_READY_2:
                	Bundle replybundle2 = reply2.readBundle();
                	if(replybundle2==null)
                		Log.i("SERVICE_PARCEL_READY_2","null"); 
                	String[] outputs2 = replybundle2.getStringArray("OUTPUTS");
                	TranslatedDescription = outputs2[0];
					bService2 = true;

					if(bService1&&bService3)
						mHandler.sendEmptyMessage(SERVICE_PARCEL_ALL_READY);
					else
						debug("bService2 finished, other=not yet");
                	break;
                case SERVICE_PARCEL_READY_3:
                	Bundle replybundle3 = reply3.readBundle();
                	if(replybundle3==null)
                		Log.i("SERVICE_PARCEL_READY_3","null"); 
                	String[] outputs3 = replybundle3.getStringArray("OUTPUTS");
                	TranslatedReview = filterTranslatedReview(outputs3[0]);
					bService3 = true;

					if(bService1&&bService2)
						mHandler.sendEmptyMessage(SERVICE_PARCEL_ALL_READY);
					else
						debug("bService3 finished, other=not yet");
                	break;
                case SERVICE_PARCEL_ALL_READY:
                	current_pnum+=3;
                	debug("all bService finished");
					onResume();
                	break;
                case SERVICE_PARCEL_FAILED:
                	showDialog("WSC Parcel Data", "Service Parcel Failed");
                	break;
                default:
                    super.handleMessage(msg);
            }
        }
    };
    
    public void debug(String msg){
		Log.d("com.prach.mashup.BarcodeReview",msg);
	}
    
	@SuppressWarnings("unused")
	private void showInterruptDialog(String process, String message) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Mashup Execution Interrupted");
		builder.setMessage("Relaunch current process ("+process+") or Quit this program ("+R.string.app_name+")");
		builder.setPositiveButton("Relaunch",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						/* User clicked Yes so do some stuff */
					}
				});
		builder.setNegativeButton("Quit",
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						finish();
					}
				});
		builder.show();
	}
	
	 private void showDialog(String title, String message) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	//debug("onResume():current_pnum="+current_pnum);
    	Intent intent = null;
    	String URL = null;
    	String[] scripts = null;
    	
    	switch (current_pnum) {
		case -1: //start : barcode reader
			intent = new Intent("com.google.zxing.client.android.SCAN");
			intent.putExtra("SCAN_MODE", "BAR_CODE_MODE");
            startActivityForResult(intent, ProcessNumber[++current_pnum]);	
			//bcode = "9780470344712";
			break;
		case 0: //process1 : Amazon
			//BarcodeSearch = "9780470344712";
			intent = new Intent("com.prach.mashup.WAExtractor"); 
			URL = "http://www.amazon.com/?ie=UTF8&force-full-site=1";
			//ocount = 1;
			scripts = new String[2];
			scripts[0] = 
				"prach = new Object;\n"+
				"prach.input = '"+BarcodeSearch+"';\n"+
				"var tagArray1 = document.getElementsByTagName('table');\n"+
				"var parentElement;\n"+
				"for(var i=0;i<tagArray1.length;i++){\n"+
				"    if(i>4&&i<12&&tagArray1[i].id=='subDropdownTable'){\n"+
				"        parentElement = tagArray1[i];\n"+
				"        break;\n"+
				"    }\n"+
				"}\n"+
				"var tagArray2 = parentElement.getElementsByTagName('input');\n"+
				"var childElement;\n"+
				"for(var i=0;i<tagArray2.length;i++)\n"+
				"    if(i==0&&tagArray2[i].id=='twotabsearchtextbox'&&tagArray2[i].name=='field-keywords'&&tagArray2[i].className=='searchSelect')\n"+
				"        childElement = tagArray2[i];\n"+
				"childElement.focus();\n"+
				"childElement.value=prach.input;\n"+
				"childElement.form.submit();";
			scripts[1] = 
				"var tagArray1 = document.getElementsByTagName('div');\n"+
				"var parentElement;\n"+
				"for(var i=0;i<tagArray1.length;i++){\n"+
				"    if(i>38&&i<46&&tagArray1[i].className=='title'){\n"+
				"        parentElement = tagArray1[i];\n"+
				"        break;\n"+
				"    }\n"+
				"}\n"+
				"var tagArray2 = parentElement.getElementsByTagName('a');\n"+
				"var childElement;\n"+
				"for(var i=0;i<tagArray2.length;i++)\n"+
				"    if(i==0&&tagArray2[i].className=='title')\n"+
				"        childElement = tagArray2[i];\n"+
				"var ProductTitle = childElement.textContent;"+
				"window.prach.addOutput(ProductTitle,'ProductTitle');" +
				
			    "var ProductPrice = new Array();"+
			    "var tagArray1 = document.getElementsByTagName('span');"+
			    "var parentElement;"+
			    "for(var i=0;i<tagArray1.length;i++){"+
			    "    if(i>=23&&i<31&&tagArray1[i].className=='subPrice'){"+
			    "        parentElement = tagArray1[i];"+
			    "        break;"+
			    "    }"+
			    "}"+
			    "if(parentElement==undefined)"+
			    "    window.prach.setfinishstate('false');"+
			    "/*case 5: single parent&child, single child-tag*/"+
			    "var tagArray2 = parentElement.getElementsByTagName('span');"+
			    "var childElement;"+
			    "for(var i=0;i<tagArray2.length;i++){"+
			    "    if(i==0&&tagArray2[i].className=='price'){"+
			    "        childElement = tagArray2[i];"+
			    "        ProductPrice.push(childElement.textContent);"+
			    "    }"+
			    "}"+
			    "window.prach.addOutputArray(ProductPrice,'ProductPrice');"+
			    "window.prach.setfinishstate('true');";
			intent.putExtra("MODE", "EXTRACTION");
			intent.putExtra("URL", URL);
			intent.putExtra("SCRIPTS", scripts);
			//intent.putExtra("OCOUNT", ocount);
			//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        startActivityForResult(intent, ProcessNumber[++current_pnum]);
			break;
		case 1: //process2 : GoodReads
			//ProductTitle = "Professional Android Application Development";
			intent = new Intent("com.prach.mashup.WAExtractor"); 
			URL = "http://www.goodreads.com/";
			//ocount = 3;
			scripts = new String[3];
			scripts[0] =
			"prach = new Object;\n"+
			"prach.input = '"+ProductTitle+"';\n"+
			"var tagArray1 = document.getElementsByTagName('form');"+
			"var parentElement;"+
			"for(var i=0;i<tagArray1.length;i++){"+
			"    if(i>=0&&i<6&&tagArray1[i].name=='headerSearchForm'){"+
			"        parentElement = tagArray1[i];"+
			"        break;"+
			"    }"+
			"}"+
			"var tagArray2 = parentElement.getElementsByTagName('input');"+
			"var childElement;"+
			"for(var i=0;i<tagArray2.length;i++)"+
			"    if(i==1&&tagArray2[i].id=='search_query'&&tagArray2[i].name=='search[query]')"+
			"        childElement = tagArray2[i];"+
			"childElement.focus();"+
			"childElement.value=prach.input;"+
			"childElement.form.submit();";
			scripts[1]=
			"var tagArray1 = document.getElementsByTagName('table');"+
			"var parentElement;"+
			"for(var i=0;i<tagArray1.length;i++){"+
			"    if(i>=0&&i<6&&tagArray1[i].className=='tableList'){"+
			"        parentElement = tagArray1[i];"+
			"        break;"+
			"    }"+
			"}"+
			"var tagArray2 = parentElement.getElementsByTagName('a');"+
			"var childElement;"+
			"for(var i=0;i<tagArray2.length;i++)"+
			"    if(i==2&&tagArray2[i].className=='bookTitle')"+
			"        childElement = tagArray2[i];"+
			"childElement.focus();"+
			"window.location = childElement.href;";
			scripts[2]=
			"var tagArray1 = document.getElementsByTagName('div');"+
			"var parentElement;"+
			"for(var i=0;i<tagArray1.length;i++){"+
			"    if(i>=11&&i<19&&tagArray1[i].id=='imagecol'&&tagArray1[i].className=='col'){"+
			"        parentElement = tagArray1[i];"+
			"        break;"+
			"    }"+
			"}"+
			"var tagArray2 = parentElement.getElementsByTagName('img');"+
			"var childElement;"+
			"for(var i=0;i<tagArray2.length;i++)"+
			"    if(i==0&&tagArray2[i].id=='coverImage')"+
			"        childElement = tagArray2[i];"+
			"childElement.focus();"+
			"var BookImage = childElement.src;"+
			
			"var tagArray1 = document.getElementsByTagName('div');"+
			"var parentElement;"+
			"for(var i=0;i<tagArray1.length;i++){"+
			"    if(i>=21&&i<29&&tagArray1[i].id=='description'&&(tagArray1[i].className.indexOf('readable')!=-1&&tagArray1[i].className.indexOf('stacked')!=-1)){"+
			"        parentElement = tagArray1[i];"+
			"        break;"+
			"    }"+
			"}"+
			"var tagArray2 = parentElement.getElementsByTagName('span');"+
			"var childElement;"+
			"for(var i=0;i<tagArray2.length;i++)"+
			"    if(i==1&&tagArray2[i].className=='reviewText')"+
			"        childElement = tagArray2[i];"+
			"var BookDescription = childElement.textContent;"+
			
			"var tagArray1 = document.getElementsByTagName('div');"+
			"var parentElement;"+
			"for(var i=0;i<tagArray1.length;i++){"+
			"    if(i>=62&&i<70&&tagArray1[i].className=='bigBoxContent'){"+
			"        parentElement = tagArray1[i];"+
			"        break;"+
			"    }"+
			"}"+
			"var tagArray2 = parentElement.getElementsByTagName('span');"+
			"var childElement;"+
			"var BookReview = '';"+
			"for(var i=0;i<tagArray2.length;i++){"+
			"    if(tagArray2[i].className=='reviewText'){"+
			"        childElement = tagArray2[i];"+
			"        BookReview += (childElement.textContent+'\\n');"+
			"    }"+
			"}"+
			"window.prach.addOutput(BookImage,'BookImage');" +
			"window.prach.addOutput(BookDescription,'BookDescription');" +
			"window.prach.addOutput(BookReview,'BookReview');" +
			"window.prach.setfinishstate('true');";
		
			intent.putExtra("MODE", "EXTRACTION");
			intent.putExtra("URL", URL);
			intent.putExtra("SCRIPTS", scripts);
			//intent.putExtra("OCOUNT", ocount);
			//intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        startActivityForResult(intent, ProcessNumber[++current_pnum]);
			break;
		case 2:
			try {
				Thread serviceThread1 = new Thread(new Runnable() {
					public void run() {
						Intent i = new Intent("com.prach.mashup.WSCService");
						//i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
						boolean isConnected = bindService(i,new ServiceConnection(){
							final int serviceCode = 0x101;
							public void onServiceConnected(ComponentName name,IBinder service) {
								Log.d("onServiceConnected","Service connected: "+ name.flattenToShortString());
								//Toast.makeText(ILContext,"Service connected",Toast.LENGTH_SHORT);
								Parcel data = Parcel.obtain();
								Bundle bundle = new Bundle();

								
								String base = "http://www.exchangerate-api.com/";
								String[] paths = { "usd", "jpy", ProductPrice };
								String[] keys = { "k" };
								String[] values = { "PQkn3-quzTZ-PNDav" };
								String mode = "SELF";
								String query = "";
								
								/*String base = "http://ajax.googleapis.com/";
								String[] paths = { "ajax", "services", "language","translate" };
								String[] keys = { "q" , "v", "langpair"};
								String[] values = { "where's the bathroom","1.0","en|ja" };
								String mode = "JSON";
								String query = "responseData.translatedText";*/

								bundle.putString("BASE", base);
								bundle.putStringArray("PATHS",paths);
								bundle.putStringArray("KEYS", keys);
								bundle.putStringArray("VALUES",values);
								bundle.putString("QUERY", query);
								bundle.putString("MODE", mode);
								//bundle.putString("OUTPUT_NAME", "OUTPUTS");

								data.writeBundle(bundle);
								boolean res = false;
								try {
									reply1 = Parcel.obtain();
									res = service.transact(serviceCode, data,reply1, 0);
								} catch (RemoteException ex) {
									Log.e("onServiceConnected","Remote exception when calling service",ex);
									res = false;
								}
								if (res)
									mHandler.sendEmptyMessage(SERVICE_PARCEL_READY_1);
								else
									mHandler.sendEmptyMessage(SERVICE_PARCEL_FAILED);
							}

							public void onServiceDisconnected(ComponentName name) {
								Log.d("onServiceConnected","Service disconnected: "+ name.flattenToShortString());
								//Toast.makeText(ILContext,"Service disconnected",Toast.LENGTH_SHORT);								
							}
						}, Context.BIND_AUTO_CREATE);
						
						if (!isConnected) {
							Log.d("bkgd runnable 1","Service could not be connected ");
							mHandler.sendEmptyMessage(SERVICE_PARCEL_FAILED);
						}
					}
				});
				Thread serviceThread2 = new Thread(new Runnable() {
					public void run() {
						Intent i = new Intent("com.prach.mashup.WSCService");
						//i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
						boolean isConnected = bindService(i,new ServiceConnection(){
							final int serviceCode = 0x101;
							public void onServiceConnected(ComponentName name,IBinder service) {
								Log.d("onServiceConnected","Service connected: "+ name.flattenToShortString());
								//Toast.makeText(ILContext,"Service connected",Toast.LENGTH_SHORT);
								Parcel data = Parcel.obtain();
								Bundle bundle = new Bundle();

								
								String base = "http://ajax.googleapis.com/";
								String[] paths = new String[]{ "ajax", "services", "language","translate" };
								String[] keys = new String[]{ "q" , "v", "langpair"};
								String[] values = new String[]{ BookDescription,"1.0","en|ja" };
								String mode = "JSON";
								String query = "responseData.translatedText";
								
								/*String base = "http://ajax.googleapis.com/";
								String[] paths = { "ajax", "services", "language","translate" };
								String[] keys = { "q" , "v", "langpair"};
								String[] values = { "where's the bathroom","1.0","en|ja" };
								String mode = "JSON";
								String query = "responseData.translatedText";*/

								bundle.putString("BASE", base);
								bundle.putStringArray("PATHS",paths);
								bundle.putStringArray("KEYS", keys);
								bundle.putStringArray("VALUES",values);
								bundle.putString("QUERY", query);
								bundle.putString("MODE", mode);
								//bundle.putString("OUTPUT_NAME", "OUTPUTS");

								data.writeBundle(bundle);
								boolean res = false;
								try {
									reply2 = Parcel.obtain();
									res = service.transact(serviceCode, data,reply2, 0);
								} catch (RemoteException ex) {
									Log.e("onServiceConnected","Remote exception when calling service",ex);
									res = false;
								}
								if (res)
									mHandler.sendEmptyMessage(SERVICE_PARCEL_READY_2);
								else
									mHandler.sendEmptyMessage(SERVICE_PARCEL_FAILED);
							}

							public void onServiceDisconnected(ComponentName name) {
								Log.d("onServiceConnected","Service disconnected: "+ name.flattenToShortString());
								//Toast.makeText(ILContext,"Service disconnected",Toast.LENGTH_SHORT);								
							}
						}, Context.BIND_AUTO_CREATE);
						
						if (!isConnected) {
							Log.d("bkgd runnable 1","Service could not be connected ");
							mHandler.sendEmptyMessage(SERVICE_PARCEL_FAILED);
						}
					}
				});
				Thread serviceThread3 = new Thread(new Runnable() {
					public void run() {
						Intent i = new Intent("com.prach.mashup.WSCService");
						//i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
						boolean isConnected = bindService(i,new ServiceConnection(){
							final int serviceCode = 0x101;
							public void onServiceConnected(ComponentName name,IBinder service) {
								Log.d("onServiceConnected","Service connected: "+ name.flattenToShortString());
								//Toast.makeText(ILContext,"Service connected",Toast.LENGTH_SHORT);
								Parcel data = Parcel.obtain();
								Bundle bundle = new Bundle();

								
								String base = "http://ajax.googleapis.com/";
								String[] paths = new String[]{ "ajax", "services", "language","translate" };
								String[] keys = new String[]{ "q" , "v", "langpair"};
								String[] values = new String[]{ BookReview,"1.0","en|ja" };
								String mode = "JSON";
								String query = "responseData.translatedText";
								
								/*String base = "http://ajax.googleapis.com/";
								String[] paths = { "ajax", "services", "language","translate" };
								String[] keys = { "q" , "v", "langpair"};
								String[] values = { "where's the bathroom","1.0","en|ja" };
								String mode = "JSON";
								String query = "responseData.translatedText";*/

								bundle.putString("BASE", base);
								bundle.putStringArray("PATHS",paths);
								bundle.putStringArray("KEYS", keys);
								bundle.putStringArray("VALUES",values);
								bundle.putString("QUERY", query);
								bundle.putString("MODE", mode);
								//bundle.putString("OUTPUT_NAME", "OUTPUTS");

								data.writeBundle(bundle);
								boolean res = false;
								try {
									reply3 = Parcel.obtain();
									res = service.transact(serviceCode, data,reply3, 0);
								} catch (RemoteException ex) {
									Log.e("onServiceConnected","Remote exception when calling service",ex);
									res = false;
								}
								if (res)
									mHandler.sendEmptyMessage(SERVICE_PARCEL_READY_3);
								else
									mHandler.sendEmptyMessage(SERVICE_PARCEL_FAILED);
							}

							public void onServiceDisconnected(ComponentName name) {
								Log.d("onServiceConnected","Service disconnected: "+ name.flattenToShortString());
								//Toast.makeText(ILContext,"Service disconnected",Toast.LENGTH_SHORT);								
							}
						}, Context.BIND_AUTO_CREATE);
						
						if (!isConnected) {
							Log.d("bkgd runnable 1","Service could not be connected ");
							mHandler.sendEmptyMessage(SERVICE_PARCEL_FAILED);
						}
					}
				});
				serviceThread1.start();
				serviceThread2.start();
				serviceThread3.start();
				
			} catch (Exception ex) {			
				Log.e(getClass().getSimpleName(),"Could not complete service call: "+ ex.getLocalizedMessage(), ex);
			}

			break;
		case 5:
			intent = new Intent("com.prach.mashup.WAExtractor");
			StringBuffer extra_HTML_SOURCE = new StringBuffer();
			extra_HTML_SOURCE.append("<html><head><meta http-equiv='content-type' content='text/html;charset=UTF-8'></head>" +
					"<body><div align='center'><big><font style='color:blue'>GoodReads Result</font></big><br>");
			extra_HTML_SOURCE.append("<img src='");
			extra_HTML_SOURCE.append(BookImage); 
			extra_HTML_SOURCE.append("'/>");
			extra_HTML_SOURCE.append("<br><big><font style='color:blue'>Price:</font></big>"+YenPrice+" yen<br>");
			extra_HTML_SOURCE.append("<big><font style='color:blue'>Descriptions:</font></big><br></div>");
			extra_HTML_SOURCE.append(TranslatedDescription);
			extra_HTML_SOURCE.append("<div align='center'><br><big><font style='color:blue'>Reviews:</font></big><br></div>");
			extra_HTML_SOURCE.append(TranslatedReview);
			extra_HTML_SOURCE.append("</body></html>");
			intent.putExtra("MODE", "DISPLAY");
			intent.putExtra("HTML_SOURCE", extra_HTML_SOURCE.toString());
			startActivityForResult(intent, ProcessNumber[++current_pnum]);
			//intent.putExtra("SCRIPTS", scripts);
			break;
		default:
			break;
		}
    }
    
	public void killProcess(String pname){
		ActivityManager actManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
		List<ActivityManager.RunningAppProcessInfo> processList = actManager.getRunningAppProcesses();
		for (int i = 0; i < processList.size(); i++) {
			//debug("processName["+i+"]:"+processList.get(i).processName);
			if(processList.get(i).processName.contains(pname)){
				//debug("killProces:("+processList.get(i).pid+")"+processList.get(i).processName);
				android.os.Process.killProcess(processList.get(i).pid);
			}
		}
	}
    
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == ProcessNumber[0]) {
			if (resultCode == RESULT_OK) {
				String ScanResult = intent.getStringExtra("SCAN_RESULT");
				BarcodeSearch = ScanResult;
			} else if (resultCode == RESULT_CANCELED) {
				showDialog(ProcessName[0], "canceled");
			}
			//killProcess("com.google.zxing.client.android");
		} else if (requestCode == ProcessNumber[1]) {
			if (resultCode == RESULT_OK) {
				String[] outputs = intent.getStringArrayExtra("OUTPUTS");
				String[] names = intent.getStringArrayExtra("NAMES");
				ProductTitle = filterProductTitle(getStringByName(outputs, names, "ProductTitle"));
				ProductPrice = filterPrice(getStringByName(outputs, names, "ProductPrice"));
			} else if (resultCode == RESULT_CANCELED) {
				showDialog(ProcessName[1], "canceled");
			}
		} else if (requestCode == ProcessNumber[2]) {
			if (resultCode == RESULT_OK) {
				String[] outputs = intent.getStringArrayExtra("OUTPUTS");
				String[] names = intent.getStringArrayExtra("NAMES");
				BookImage = getStringByName(outputs, names, "BookImage");
				BookDescription = filterBookDescription(getStringByName(outputs, names, "BookDescription"));
				BookReview = filterBookReview(getStringByName(outputs, names, "BookReview"));
			} else if (resultCode == RESULT_CANCELED) {
				showDialog(ProcessName[2], "canceled");
			}
		}
	}

	protected String getStringByName(String[] outputs, String[] names, String key) {
		String result = "";
		for (int i = 0; i < names.length; i++) {
			if(names[i].equals(key)){
				result = outputs[i];
			}
		}
		return result;
	}

	protected String[] getArrayByName(String[] outputs, String[] names, String key) {
		String[] results = new String[0];
		for (int i = 0; i < names.length; i++) {
			if(names[i].equals(key)){
				concat(results,outputs[i]);
			}
		}
		return results;
	}
	
	public String[] concat(String[] A, String B){
		if(A==null) 
			return new String[] {B};
		
		String[] C = new String[A.length+1];
		System.arraycopy(A, 0, C, 0, A.length);
		C[A.length] = B;
		return C;
	}
	
	public String filterProductTitle(String ptitle){
		String[] temp = ptitle.split("\\(",2);
		return temp[0];
	}
	
	public String filterPrice(String price){
		return price.replace("$", "");
	}
	
	public String filterBookDescription(String text){
		return text.replaceAll("\\s+", " ");
	}
	
	public String filterBookReview(String text){
		return text.replaceAll("\\s+", " ");
	}
	
	public String filterTranslatedReview(String text){
		return text.replaceAll("\\\n", "<br>");
	}
}