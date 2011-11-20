package edu.sjsu.cs.davsync;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

import com.android.keepass.R;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.content.Context;

public class davsync extends Activity {

    private Context context;
    private File conffile;
    private File kdbfile;
    protected boolean changed;
    //private DSDatabase db;
    private EditText[] field = new EditText[4]; // hostname, resource, username, password
    private Button[] button = new Button[4]; // save, clear, test, sync

    private enum ButtonType {
        SAVE, CLEAR, TEST, SYNC
    }

    private class ButtonListener implements OnClickListener {
        ButtonType type;

        public ButtonListener(ButtonType type) {
            this.type = type;
        }
        public void onClick(View v) {
            switch( type ) {
            case SAVE:
            	save();
                break;
            case CLEAR:
            	clear();
                break;
            case TEST:
                test();
                break;
            case SYNC:
            	sync();
            	break;
            }
        }
    }

    private void clear() {
    	for(int i = 0; i < field.length; i++)
    		field[i].setText("");    
    	if(conffile.exists())
    		conffile.delete();
    }
    
    private void save() {
    	
    	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Saving...")
		       .setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
    	
    	Properties props = new Properties();
    	props.put("hostname",field[0].getText().toString());
    	props.put("resource",field[1].getText().toString()); 
    	props.put("username",field[2].getText().toString()); 
    	props.put("password",field[3].getText().toString()); 
    	try
    	{
    		if(!conffile.exists())
    			conffile.createNewFile();
    		
    	FileOutputStream fos = new FileOutputStream(conffile);
    	props.save(fos, "");
    	fos.close();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    	finish();
    }
    
    // TODO: add a dialog during the test
    // http://developer.android.com/guide/topics/ui/dialogs.html
    private void test() {
    	Toast toast = Toast.makeText(context, "Unspecified test failure", Toast.LENGTH_SHORT);
    	try {
			DAVNetwork net = new DAVNetwork(getCurrentProfile(), kdbfile);
			if( net.testRemote() ) {
				toast = Toast.makeText(context, "Test succeeded", Toast.LENGTH_SHORT);
			} else {
				toast = Toast.makeText(context, "Test failed", Toast.LENGTH_SHORT);
			}
		} catch (ConfigurationException ce) {
			toast = Toast.makeText(context, ce.toString(), Toast.LENGTH_SHORT);
		} finally {
			toast.show();
		}
    }
    
    private void sync() {
    	Toast toast = Toast.makeText(context, "Unspecified test failure", Toast.LENGTH_SHORT);
    	try {
    		DAVNetwork net = new DAVNetwork(getCurrentProfile(), kdbfile);
    		if( net.sync() == true ) {
    			toast = Toast.makeText(context, "Sync succeeded", Toast.LENGTH_SHORT);
    		} else {
    			toast = Toast.makeText(context, "Sync failed", Toast.LENGTH_SHORT);
    		}
    	} catch( Exception e ) {
    		// TODO
    	} finally {
    		toast.show();
    	}
    }

    // read the state of all fields from memory and return a Profile object
    private Profile getCurrentProfile() throws ConfigurationException {
        String host = field[0].getText().toString();
        if( ! host.matches("[a-zA-Z0-9.]+") ) {
        	throw new ConfigurationException("please input a valid hostname");
        }
        String rsrc = field[1].getText().toString();
        if( ! rsrc.matches("[a-zA-Z0-9./]+") ) {
        	throw new ConfigurationException("please input a valid resource");
        }
        String user = field[2].getText().toString();
        if( user.length() == 0 ) {
        	throw new ConfigurationException("please input a valid username");
        }
        String pass = field[3].getText().toString();
        if( pass.length() == 0 ) {
        	throw new ConfigurationException("please input a valid password");
        }
        
        return new Profile("", host, rsrc, user, pass);
    }

    // removes any text from all fields
    private void clearTextFields() {
        for(int i = 0; i < 4; i++)
            field[i].setText("");
    }

    /** Called when the activity is first created. */
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        changed = false;
        setContentView(R.layout.info);
        context = getApplicationContext();
        //db = new DSDatabase(context);
        String filepath = getIntent().getStringExtra("wdcfile");
        String kdbfilepath = getIntent().getStringExtra("kdbfile");
        conffile = new File(filepath);
        kdbfile = new File(kdbfilepath);
                
        /*
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(filepath)
		       .setCancelable(true);
		AlertDialog alert = builder.create();
		alert.show();
		*/
		
        
        // handle button events
        button[0] = (Button)this.findViewById(R.id.wdc_save);
        button[1] = (Button)this.findViewById(R.id.wdc_clear);
        button[2] = (Button)this.findViewById(R.id.wdc_test);

        button[0].setOnClickListener(new ButtonListener(ButtonType.SAVE));
        button[1].setOnClickListener(new ButtonListener(ButtonType.CLEAR));
        button[2].setOnClickListener(new ButtonListener(ButtonType.TEST));

        /*field[0].setOnEditorActionListener(new EditActionListener());
        field[1].setOnEditorActionListener(new EditActionListener());
        field[2].setOnEditorActionListener(new EditActionListener());
        field[3].setOnEditorActionListener(new EditActionListener());
        */
        
        // access to text fields
        field[0] = (EditText)findViewById(R.id.hostname);
        field[1] = (EditText)findViewById(R.id.resource);
        field[2] = (EditText)findViewById(R.id.username);
        field[3] = (EditText)findViewById(R.id.password);

        if(conffile.exists())
        {
        	try
        	{
        	FileInputStream fin = new FileInputStream(conffile);
        	Properties props = new Properties();
        	props.load(fin);
        	fin.close();
        	
        	
        	field[0].setText(getString(props,"hostname"));
            field[1].setText(getString(props,"resource"));
            field[2].setText(getString(props,"username"));
            field[3].setText(getString(props,"password"));
        	//read the file...
        	}
        	catch(Exception e) {
        		e.printStackTrace();
        	}
        }
        /*
        Profile p = db.getProfile("");
        field[0].setText(p.getHostname());
        field[1].setText(p.getResource());
        field[2].setText(p.getUsername());
        field[3].setText(p.getPassword());
        */
    }
    
    public String getString(Properties props, String key)
    {
    	String temp = (String)props.get(key);
    	if(temp == null)
    		temp = "";
    	
    	return temp;
    }
    
    class EditActionListener implements OnEditorActionListener
    {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			// TODO Auto-generated method stub
			changed = true;
			return false;
		}
    	
    }
}
