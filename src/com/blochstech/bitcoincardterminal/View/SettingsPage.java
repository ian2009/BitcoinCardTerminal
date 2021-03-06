package com.blochstech.bitcoincardterminal.View;

import java.text.DecimalFormat;

import com.blochstech.bitcoincardterminal.MainActivity;
import com.blochstech.bitcoincardterminal.R;
import com.blochstech.bitcoincardterminal.Interfaces.Currency;
import com.blochstech.bitcoincardterminal.Model.Communication.CurrencyApiConnector;
import com.blochstech.bitcoincardterminal.Model.Communication.TypeConverter;
import com.blochstech.bitcoincardterminal.Utils.EventListener;
import com.blochstech.bitcoincardterminal.Utils.RegexUtil;
import com.blochstech.bitcoincardterminal.Utils.SyntacticSugar;
import com.blochstech.bitcoincardterminal.ViewModel.SettingsPageVM;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.MessageManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.NavigationManager;
import com.blochstech.bitcoincardterminal.ViewModel.ViewStateManagers.PageManager;
import com.blochstech.bitcoincardterminal.scanner.zbar.Result;
import com.blochstech.bitcoincardterminal.scanner.zbar.ZBarScannerView;
import com.blochstech.bitcoincardterminal.scanner.zbar.ZBarScannerView.ResultHandler;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

//Everything in view classes will be destroyed at whim by Android. VMs and model/database should hold logic and information.
//Views defined in layout xml files.
public class SettingsPage extends Fragment {
	private View myView;
	private SettingsPageVM myVM;
	private UpdateListener updateListener = new UpdateListener();
	
	private EasyText addressBox;
	private EasyText feeBox;
	private CheckBox courtesyOK;
	private Button btcButton;
	private Button appleButton;
	private Button dollarButton;
	private Button yuanButton;
	private Button euroButton;
	private Button okButton;
	private TextView currencyText;
	private TextView feeCurrencyText;
	
	private OnClickListener btnListener = new ButtonListener();
	private TextListener textListener = new TextListener();
	private TextClickListener textClickListener = new TextClickListener();
	
	private boolean initialized = false;
	

	
	/**
     * Used when user selects QRCode
     */
	private AlertDialog qrDialog = null;
	private ZBarScannerView zBarScannerView = null;
    private void setupQRCodeScanner() {
    	try{
	    	if(qrDialog == null){
		    	AlertDialog.Builder scanQRdialog = new AlertDialog.Builder(MainActivity.instance);
		    	View dialoglayout = LayoutInflater.from(MainActivity.instance).inflate(R.layout.zbar_capture, null);
		    	zBarScannerView = (ZBarScannerView)dialoglayout.findViewById(R.id.scanner_view);
		    	scanQRdialog.setView(dialoglayout);
		    	
		    	//final AlertDialog dialog = scanQRdialog.create();
		    	qrDialog = scanQRdialog.create();
		    	
		    	if(zBarScannerView != null) {
			    	 zBarScannerView.setResultHandler(new ResultHandler() {
						
						@Override
						public void handleResult(Result rawResult) {
							// TODO Auto-generated method stub
							String scan = rawResult.getContents();
							if(!RegexUtil.isMatch(scan, RegexUtil.CommonPatterns.BASE58_CHARS) || !TypeConverter.verifyBase58CheckSum(scan)) {
								Toast.makeText(getActivity(), "Invalid Receiving Bitcoin address: "+scan, Toast.LENGTH_LONG).show();
							}else{
								myVM.Address(scan);
							}
							hideQrDialog();
						}
					});
			       
			    }
		    	
		    	qrDialog.setOnDismissListener(new OnDismissListener() {
					
					@Override
					public void onDismiss(DialogInterface dialog) {
						   if(zBarScannerView != null) {
							   zBarScannerView.stopCamera();
						   }
					}
				});
		    	
		    	//zBarScannerView.startCamera();
	    	}
    	}catch (Exception ex){
    		MessageManager.Instance().AddMessage(ex.toString(), true);
    	}
    }
    
    private void showQrDialog(){
    	setupQRCodeScanner();
    	if(qrDialog != null){
    		qrDialog.show();
    		stopOrStartCamera(true);
    	}
    }
    private void hideQrDialog(){
    	if(qrDialog != null){
    		qrDialog.hide();
    		stopOrStartCamera(false);
    	}
    }
    private void stopOrStartCamera(boolean value){
	    try{
	    	if(zBarScannerView == null)
	    		return;
	    	
	    	if(value){
	    		zBarScannerView.startCamera();
	    	}else{
	    		zBarScannerView.stopCamera();
	    	}
    	}catch (Exception ex){
			MessageManager.Instance().AddMessage(ex.toString(), true);
		}
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	myView = inflater.inflate(R.layout.settingspage, container, false);
    	
    	//Setup:
    	if(PageManager.Instance().isInitialized(PageManager.ViewModelTags.SETTINGS_PAGE_VM)){
    		myVM = SyntacticSugar.<SettingsPageVM>castAs(PageManager.Instance().getVM(PageManager.ViewModelTags.SETTINGS_PAGE_VM));
    	}
    	if(myVM == null){
    		myVM = new SettingsPageVM();
    		PageManager.Instance().setVM(PageManager.ViewModelTags.SETTINGS_PAGE_VM, myVM);
    	}
    	
    	//Save views for faster access:
    	addressBox = new EasyText(SyntacticSugar.<EditText>castAs(myView.findViewById(R.id.addressText)));
    	SetUseType(false);
    	feeBox = new EasyText(SyntacticSugar.<EditText>castAs(myView.findViewById(R.id.feeText)));
    	courtesyOK = SyntacticSugar.<CheckBox>castAs(myView.findViewById(R.id.courtesyCheckBox));
    	btcButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.btcButton));
    	appleButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.appleButton));
    	dollarButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.dollarButton));
    	yuanButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.yuanButton));
    	euroButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.euroButton));
    	okButton = SyntacticSugar.<Button>castAs(myView.findViewById(R.id.okSettingsButton));
    	currencyText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.currencyText));
    	feeCurrencyText = SyntacticSugar.<TextView>castAs(myView.findViewById(R.id.feeCurrency));
    	
    	//Begin:
        myVM.UpdateEvent.register(updateListener);

    	btcButton.setOnClickListener(btnListener);
    	appleButton.setOnClickListener(btnListener);
    	dollarButton.setOnClickListener(btnListener);
    	yuanButton.setOnClickListener(btnListener);
    	euroButton.setOnClickListener(btnListener);
    	okButton.setOnClickListener(btnListener);
    	courtesyOK.setOnClickListener(btnListener);
    	addressBox.updateEvent.register(textListener);
    	addressBox.clickEvent.register(textClickListener);
    	feeBox.updateEvent.register(textListener);
    	
    	setupQRCodeScanner();
    	
    	initialized = true;
    	update();
        
        return myView;
    }
    
    @Override
    public void onPause(){ //Fragment may be destroyed after this.
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.unregister(updateListener);
    	if(feeBox != null)
    		feeBox.ignoreTextChanges(true);
    	if(addressBox != null)
    		addressBox.ignoreTextChanges(true);
    	//stopOrStartCamera(false);
    	super.onPause();
    }
    
    @Override
    public void onResume(){
    	super.onResume();
    	if(myVM != null && myVM.UpdateEvent != null)
    		myVM.UpdateEvent.register(updateListener);
    	if(feeBox != null)
    		feeBox.ignoreTextChanges(false);
    	if(addressBox != null)
    		addressBox.ignoreTextChanges(false);
    	//stopOrStartCamera(true);
    	update();
    }
    
    private void update(){
    	try{
    		if(initialized){
	    		addressBox.setText(myVM.Address());
	        	addressBox.setValid(myVM.IsAddressValid());
	        	
	        	feeBox.setText(String.format("%.2f",myVM.Fee())); //Means precision = 7 and floating point to decimal conversion.
	        	
				selectButton(myVM.ChosenCurrency());
				
				DecimalFormat df = new DecimalFormat("0.#######");
				
				currencyText.setText("('"
						+myVM.ChosenCurrency().Description()
						+"' --> 1"
						+myVM.ChosenCurrency().Symbol()+" ~ "
						+df.format(CurrencyApiConnector.DollarValue(myVM.ChosenCurrency()))+"$)");
	        	feeCurrencyText.setText(myVM.ChosenCurrency().Symbol());
				
	        	courtesyOK.setChecked(myVM.CourtesyOK());
    		}
    	}catch (Exception ex){
    		MessageManager.Instance().AddMessage(ex.toString(), true);
    	}
    }
    
    private boolean usingTyping = false;
    private void SetUseType(boolean value, int type){
    	if(!value)
    	{
    		usingTyping = false;
    		myVM.UseNFC(false);
    		return;
    	}
    	
    	switch(type)
    	{
    		case 0:
		    	usingTyping = true;
	    		myVM.UseNFC(false);
		    	//addressBox.ViewReference().setFocusable(value);
		    	break;
    		case 1:
    			usingTyping = false;
        		myVM.UseNFC(true);
    			break;
			default:
				break;
    	}
    }
    private void SetUseType(boolean value){
    	SetUseType(value, 0);
    }
    
    private void focusAddressField(){
    	try{
			//addressBox.ViewReference().requestFocus();
			//InputMethodManager imm = (InputMethodManager) MainActivity.instance.getSystemService(Context.INPUT_METHOD_SERVICE);
			//imm.showSoftInput(addressBox.ViewReference(), InputMethodManager.SHOW_IMPLICIT);
			//MainActivity.instance.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			  
			//TODO: Make this work.
			//InputMethodManager imm = (InputMethodManager)getSystemService(
			//      Context.INPUT_METHOD_SERVICE);
			//imm.hideSoftInputFromWindow(myEditText.getWindowToken(), 0);
    	}catch(Exception ex){
    		MessageManager.Instance().AddMessage(ex.toString(), true);
    	}
    }
    
    private Dialog dialog = null;
    private void showAddressDialog(){
    	boolean createdNow = false;
    	//set up dialog
    	if(dialog == null)
    	{
    		createdNow = true;
	        dialog = new Dialog(MainActivity.instance); //TODO: Clean that a bit...
	        dialog.setContentView(R.layout.addressloaddialog);
	        dialog.setTitle("Choose input type:");
	        dialog.setCancelable(true);
	        //there are a lot of settings, for dialog, check them all out!

	        //Set up type button:
	        Button typeButton = (Button) dialog.findViewById(R.id.TypeChoiceButton);
	        typeButton.setOnClickListener(new OnClickListener() {
	        	@Override
	            public void onClick(View v) {
		        	dialog.hide();

		        	SetUseType(true);
		        	focusAddressField();
	            }
	        });
	        
	        //Set up QRcode button:
	        Button qrCodeButton = (Button) dialog.findViewById(R.id.TypeQRCodeButton);
	        qrCodeButton.setOnClickListener(new OnClickListener() {
	        	@Override
	            public void onClick(View v) {
		        	dialog.hide();
		        	
		        	SetUseType(false);
		        	showQrDialog();
		        	//setupQRCodeScanner();
	            }
	        });
	        Button nfcButton = (Button) dialog.findViewById(R.id.NFCChoiceButton);
	        nfcButton.setOnClickListener(new OnClickListener() {
	        	@Override
	            public void onClick(View v) {
		        	dialog.hide();
	        		SetUseType(true, 1);
	            }
	        });
    	}
        
        //now that the dialog is set up, it's time to show it
    	try{
    		dialog.show();
    	}catch(Exception ex){
    		dialog = null;
    		if(!createdNow)
    			showAddressDialog();
    	}
    } 
   
    
	private void selectButton(Currency currency){
    	btcButton.setEnabled(currency != Currency.MicroBitcoins);
    	appleButton.setEnabled(currency != Currency.Apples);
    	dollarButton.setEnabled(currency != Currency.Dollars);
    	yuanButton.setEnabled(currency != Currency.Yuans);
    	euroButton.setEnabled(currency != Currency.Euros);
    	
    	//euroButton.setPressed(pressed);
    }
    
    /*private class ButtonPresser implements Runnable{ //TODO: Internalize in "EasyButton". NOT WORKING
    	Button btn;
    	boolean pressed;
    	
    	public ButtonPresser(Button b, boolean press){
    		btn = b;
    		pressed = press;
    	}
    	public void run() {
        	btn.setPressed(pressed);
        }
    }*/
    
    private class ButtonListener implements OnClickListener {
		@Override
		public void onClick(View v) { //Select logic by btn id:
			try{
				SetUseType(false);
				switch(v.getId())
				{
					case R.id.courtesyCheckBox:
						myVM.CourtesyOK(courtesyOK.isChecked());
						break;
					case R.id.btcButton:
						myVM.ChosenCurrency(Currency.MicroBitcoins);
						break;
					case R.id.appleButton:
						myVM.ChosenCurrency(Currency.Apples);
						break;
					case R.id.dollarButton:
						myVM.ChosenCurrency(Currency.Dollars);
						break;
					case R.id.yuanButton:
						myVM.ChosenCurrency(Currency.Yuans);
						break;
					case R.id.euroButton:
						myVM.ChosenCurrency(Currency.Euros);
						break;
					case R.id.okSettingsButton:
						NavigationManager.Instance().setPage(PageTags.CHARGE_PAGE);
						break;
					default:
						MessageManager.Instance().AddMessage("Unknown button clicked: " + v.getId(), true);
						break;
				}
			}catch (Exception ex){
	    		MessageManager.Instance().AddMessage(ex.toString(), true);
	    	}
		}
    }
    
    private class TextListener extends EventListener<Integer>{
		@Override
		public void onEvent(Integer event) {
			try{
				switch(event){
					case R.id.addressText:
						myVM.Address(addressBox.getText().toString());
						break;
					case R.id.feeText:
						myVM.Fee(feeBox.getText().toString());
						SetUseType(false);
						break;
					default:
						MessageManager.Instance().AddMessage("Text change event Id not found.", true);
						break;
				}
			}catch (Exception ex){
	    		MessageManager.Instance().AddMessage(ex.toString(), true);
	    	}
		}
    }
    
    private class TextClickListener extends EventListener<Boolean>{
		@Override
		public void onEvent(Boolean event) {
			if(!usingTyping)
				showAddressDialog();
		}
    }
    
    private class UpdateListener extends EventListener<Object> {
		@Override
		public void onEvent(Object event) {
			update();
		}
    }
}
