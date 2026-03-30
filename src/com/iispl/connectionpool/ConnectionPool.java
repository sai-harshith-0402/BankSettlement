package com.iispl.connectionpool;


import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import com.mchange.v2.c3p0.ComboPooledDataSource;

public class ConnectionPool {
	private static ComboPooledDataSource dataSource;
	static {
		try {
			dataSource = new ComboPooledDataSource();
			Properties properties = new Properties();
			//reading data from db.properties
			InputStream inputStream  = new FileInputStream("resources/db.properties");
			//Loading inputstream to properties object
			properties.load(inputStream);
			
			//setting values to dataSource object by fetching values from db.properties through properties.getProperty()
			dataSource.setDriverClass(properties.getProperty("DRIVER_CLASS"));
			dataSource.setJdbcUrl(properties.getProperty("CONNECTION_STRING"));
			dataSource.setUser(properties.getProperty("USERNAME"));
			dataSource.setPassword(properties.getProperty("PASSWORD"));
			
			//Optional
			//c3p0 can set default values
			dataSource.setInitialPoolSize(5);
			dataSource.setMinPoolSize(5);
			dataSource.setAcquireIncrement(5);
			dataSource.setMaxPoolSize(20);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static javax.sql.DataSource getDataSource(){
		return dataSource;
	}
}
