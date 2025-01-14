package edu.sjsu.cs.davsync;

import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.lang.IllegalArgumentException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PropPatchMethod;
import org.apache.jackrabbit.webdav.property.DavProperty;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.apache.jackrabbit.webdav.property.DavPropertySet;
import org.apache.jackrabbit.webdav.property.DefaultDavProperty;
import org.apache.jackrabbit.webdav.property.PropEntry;
// import org.apache.jackrabbit.webdav.util.HttpDateFormat;
import org.apache.jackrabbit.webdav.DavException;

public class DAVNetwork {
	File path;
	private String url;
	private Credentials creds;
	HttpClient client;
	
	public DAVNetwork(DAVProfile profile, File kpfile) {
		url = "https://" + profile.getHostname() + profile.getResource();
		creds = new UsernamePasswordCredentials(profile.getUsername(), profile.getPassword());
		//File sdcard = Environment.getExternalStorageDirectory();
		path = kpfile;
		//no need to create dirs since the file MUST already exist...
		//path.getParentFile().mkdirs();
		client = new HttpClient();
		client.getState().setCredentials(AuthScope.ANY, creds);
		client.getHttpConnectionManager().getParams().setConnectionTimeout(5000);
	}

	public boolean testRemote() {
		final String TAG = "DAVNetwork::testRemote";
		try {
			PropFindMethod pfm = new PropFindMethod(url);
            int ret = client.executeMethod(pfm);

            Log.d(TAG, "PropFindMethod returned with " + ret);
            if( (ret == HttpStatus.SC_MULTI_STATUS && pfm.succeeded()) || ret == HttpStatus.SC_NOT_FOUND ) {
            	// not found (404) isn't an error in this sense since it only means
            	// that the remote file doesn't exist - BUT, this means we can log in
            	pfm.releaseConnection();
            	return true;
            } else {
            	pfm.releaseConnection();
            	return false;
            }
		} catch( IOException ioe ) {
			// also handles HttpException from the client
			return false;
		} catch( IllegalArgumentException iae ) {
			// from client, in case something is wrong with 'url'
			return false;
		}
	}
	
	public boolean sync() throws HttpException, IOException, IllegalArgumentException, DavException {
		final String TAG = "DAVNetwork::sync";
		Date date_remote, date_local;
		boolean has_remote, has_local;
		
		// get local & remote info
		try {
			date_remote = getRemoteTimestamp();
			has_remote = true;
		} catch( Exception e ) {
			date_remote = new Date(1900, 1, 1);
			has_remote = false;
		}
        if( path.exists() ) {
        	date_local = new Date(path.lastModified());
        	has_local = true;
        } else {
        	date_local = new Date(1900, 1, 1);
        	has_local = false;
        }
        
        // do the sync
        if( has_local == true && has_remote == false ) {
        	Log.d(TAG, "Uploading local file");
        	return upload(date_local);
        } else if( has_local == false && has_remote == true ) {
        	Log.d(TAG, "Downloading remote file");
        	return download(date_remote);
        } else if( has_local == false && has_remote == false ) {
        	// this should never happen
        	Log.d(TAG, "New KDB file creation unimplemented");
        	return false;
        } else {        	
        	Log.d(TAG, date_remote.toString() + " <=> " + date_local.toString() );
        	
        	int comparator = date_local.compareTo(date_remote);
        	if( comparator < 0 ) {
        		Log.d(TAG, "Final sync decision: download");
        		return download(date_remote);
        	} else if( comparator > 0 ) {
        		Log.d(TAG, "Final sync decision: upload");
        		return upload(date_local);
        	} else {
        		// the files are already synced, we do nothing
        		Log.d(TAG, "Final sync decision: the files are equal");
        		return true;
        	}
        }
	}
	
	private Date getRemoteTimestamp() throws DavException, IOException {
        PropFindMethod pfm = new PropFindMethod(url);
        int ret = client.executeMethod(pfm);
        if( ret == HttpStatus.SC_NOT_FOUND || ret != HttpStatus.SC_MULTI_STATUS || !pfm.succeeded() ) {
        	throw new IOException();
        }
        
        MultiStatusResponse[] msr;
		msr = pfm.getResponseBodyAsMultiStatus().getResponses();
        if( msr.length != 1 ) {
    		// FIXME: how do we handle this?
    		Log.d("DAVNetwork::sync", "Got " + msr.length + " MultiStatusResponse objects");
    		throw new FileNotFoundException();
    	}
        
        String dateString = null;
        Iterator<? extends PropEntry> iter = msr[0].getProperties(HttpStatus.SC_OK).getContent().iterator();
        while( iter.hasNext() ) {
    		DefaultDavProperty tmp = (DefaultDavProperty)iter.next();
    		if( tmp.getName().toString().equals("{DAV:}getlastmodified") ) {
    			dateString = tmp.getValue().toString();
    			break;
    		}
    	}
        if( dateString == null ) {
        	// FIXME: is there a better exception class?
        	throw new IOException();
        }
        
        DateFormat fmt = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
    	Date date_remote;
    	try {
    		date_remote = (Date)fmt.parse(dateString);
    	} catch( ParseException pe ) {
    		// FIXME: is there a better exception class?
    		Log.d("DAVNetwork::sync", "Unable to parse remote timestamp: " + dateString);
    		throw new IOException();
    	}
    	
    	pfm.releaseConnection();
        
        return date_remote;
	}
	
	// not working - for now, just set local file's timestamp after sync
	// http://wiki.apache.org/jackrabbit/WebDAV
	// http://jackrabbit.apache.org/api/2.2/org/apache/jackrabbit/webdav/client/methods/PropPatchMethod.html
	private void setRemoteTimestamp() {
        DavPropertySet newProps = new DavPropertySet();   
        DavPropertyNameSet removeProperties=new DavPropertyNameSet();
        
        DavProperty testProp = new DefaultDavProperty("TheAnswer", "42", DavConstants.NAMESPACE);
        newProps.add(testProp);
        PropPatchMethod ppm;
		try {
			ppm = new PropPatchMethod("http://www.somehost.com/duff/test4.txt", newProps, removeProperties);
			client.executeMethod(ppm);
		} catch (IOException e1) {
			// TODO
			return;
		}
        
        Log.d("setRemoteTimestamp", ppm.getStatusCode() + " " + ppm.getStatusText());
	}
	
	// FIXME: set the date on the uploaded file
	private boolean upload(Date modified) {
		int ret = -1;
		final String TAG = "DAVNetwork::upload";
        
        PutMethod pm = new PutMethod(url);
		pm.setRequestEntity(new FileRequestEntity(path, "binary/octet-stream"));
		
		/*
		String lastModStr = HttpDateFormat.modificationDateFormat().format(modified);
		pm.setRequestHeader(new Header("Date", lastModStr));
		Log.d(TAG, "Set Date header to: " + lastModStr);
		*/
		
		try {
			ret = client.executeMethod(pm);
		} catch (HttpException he) {
			Log.w(TAG, "Caught HttpException: " + he.getMessage());
		} catch (IOException ioe) {
			Log.w(TAG, "Caught IOException: " + ioe.getMessage());
		} finally {
			pm.releaseConnection();
		}
		
		// common: HttpStatus.SC_NO_CONTENT HttpStatus.SC_CREATED HttpStatus.SC_OK
		if ( ret < 200 || ret > 226 ) {
			Log.d(TAG, "Failed to execute Put method: " + ret);
			return false;
		} else {
			Log.d(TAG, "Put method successfully completed");
			/* hack, hack, hack: set the local file's modification date equal to the remote's */
			try {
				Date d = getRemoteTimestamp();
				if( ! path.setLastModified( d.getTime() ) )
						Log.d(TAG, "Could not set local timestamp [1]");
			} catch( Exception e ) {
				Log.d(TAG, "Could not set local timestamp [2]");
			}
			return true;
		}
	}
	
	private boolean download(Date modified) {
		int ret = -1;
		final String TAG = "DAVNetwork::upload";
		boolean fail = false;
		GetMethod gm = new GetMethod(url);
		gm.setFollowRedirects(true);
		try {
			ret = client.executeMethod(gm);
			// http://www.eboga.org/java/open-source/httpclient-demo.html
			InputStream input = gm.getResponseBodyAsStream();
			FileOutputStream output = new FileOutputStream(path, false);
			int count = -1;
			byte[] buffer = new byte[8192];
			while( (count = input.read(buffer)) != -1 ) {
				output.write(buffer, 0, count);
			}
			output.flush();
			output.close();
			if( ! path.setLastModified(modified.getTime()) ) {
				Log.w(TAG, "Failed to set local last-modified time equal to remote");
			}
		}catch (HttpException he) {
			Log.w(TAG, "Caught HttpException: " + he.getMessage());
			fail = true;
		} catch (IOException ioe) {
			Log.w(TAG, "Caught IOException: " + ioe.getMessage());
			fail = true;
		} finally {
			gm.releaseConnection();
		}
		
		if ( ret != HttpStatus.SC_OK || fail == true ) {
			Log.d(TAG, "Failed to execute Get method: " + ret);
			return false;
		} else {
			Log.d(TAG, "Get method successfully completed");
			return true;
		}
	}
}
