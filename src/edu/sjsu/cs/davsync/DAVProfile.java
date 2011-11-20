package edu.sjsu.cs.davsync;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class DAVProfile {
	private String filename, username, password, hostname, resource;

	public DAVProfile(File conf)
	{
		try
		{
			FileInputStream fin = new FileInputStream(conf);
			Properties props = new Properties();
			props.load(fin);
			fin.close();
			
			filename = "";
			username = getString(props,"username");
			password = getString(props,"password");
			hostname = getString(props,"hostname");
			resource = getString(props,"resource");
			
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	 public String getString(Properties props, String key)
	    {
	    	String temp = (String)props.get(key);
	    	if(temp == null)
	    		temp = "";
	    	
	    	return temp;
	    }
	
	public DAVProfile(String filename, String hostname, String resource, String username, String password) {
		this.filename = new String(filename);
		this.username = new String(username);
		this.password = new String(password);
		this.hostname = new String(hostname);
		this.resource = new String(resource);
	}

	public String getFilename() {
		return new String(filename);
	}
	
	public String getHostname() {
		return new String(hostname);
	}

	public String getResource() {
		return new String(resource);
	}

	public String getUsername() {
		return new String(username);
	}

	public String getPassword() {
		return new String(password);
	}
}

