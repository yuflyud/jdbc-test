package org.yflyud.projects.jdbctest.ui;

import javax.swing.JFrame;

import org.apache.log4j.Appender;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.yflyud.projects.jdbctest.App;

public class JDBCTestWindow extends JFrame {
	
	private void configureLogging() {
		PropertyConfigurator.configure(App.class
				.getResourceAsStream("/log4j-ui.properties"));
		Appender appender = Logger.getLogger("ui").getAppender("ui");
		Logger.getRootLogger().addAppender(appender);
	}
	

}
