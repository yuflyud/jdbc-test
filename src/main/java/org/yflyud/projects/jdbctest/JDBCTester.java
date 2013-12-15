package org.yflyud.projects.jdbctest;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Class for testing JDBC connection. It is responsible for all steps of
 * connection testing, trying to log as much details as possible to easily
 * troubleshoot and error.
 * 
 * @author Yuriy Flyud
 * 
 */
public class JDBCTester {
	private static final Logger LOGGER = Logger.getLogger(JDBCTester.class);

	private final String driverJarLoc;
	private final String driverName;
	private final String dbUrl;
	private final String user;
	private final String password;

	/**
	 * The only constructor that gets all configuration at once. There is no
	 * need for getters/setters.
	 * 
	 * @param driverJarLoc
	 *            Path to JAR file with driver.
	 * @param driverName
	 *            Name of the Driver. E.g.: com.mysql.jdbc.Driver
	 * @param dbUrl
	 *            Database URL. E.g.: jdbc:mysql://hostname:port/dbname
	 * @param user
	 *            User name
	 * @param password
	 *            Password
	 */
	public JDBCTester(String driverJarLoc, String driverName, String dbUrl,
			String user, String password) {
		super();
		this.driverJarLoc = driverJarLoc;
		this.driverName = driverName;
		this.dbUrl = dbUrl;
		this.user = user;
		this.password = password;
	}

	/**
	 * Tests connection using current configuration. Handles all exceptions(at
	 * least checked and common), logging the information about progress and
	 * errors.
	 * 
	 * @return True if connection succeeded.
	 */
	public boolean test() {

		LOGGER.info(String
				.format("Starting to test connection using configuration:\n Driver JAR location: %s\n Driver name: %s\n Url: %s\n User name: %s\n Password: %s",
						driverJarLoc, driverName, dbUrl, user, password));
		LOGGER.debug("Checking location of JAR archive with JDBC driver");
		File driverJar;
		try {
			driverJar = checkDriverLocation();
		} catch (FileNotFoundException e) {
			LOGGER.error(invalidJarLocationMessage(), e);
			return false;
		} catch (InvalidJarLocationException e) {
			LOGGER.error(invalidJarLocationMessage(), e);
			return false;
		}

		LOGGER.debug(String.format("Loading JAR file from %s",
				driverJarLoc.toString()));
		URLClassLoader child;
		try {

			child = new URLClassLoader(new URL[] { driverJar.toURI().toURL() },
					JDBCTester.class.getClassLoader());
		} catch (MalformedURLException e) {
			LOGGER.error(String.format(
					"Unable to parse URL '%s'. See log for more details.",
					driverJarLoc), e);
			return false;
		}

		LOGGER.debug(String.format("Loading driver '%s'", driverName));
		Driver driver;
		try {
			driver = loadDriver(child);
		} catch (LoadDriverException e) {
			LOGGER.error(String.format(
					"Unable to load driver '%s'. See log for more details.",
					driverName), e);
			return false;
		}
		LOGGER.debug("Registering driver to DriverManager");
		try {
			DriverManager.registerDriver(new DriverShim(driver));
		} catch (SQLException e) {
			LOGGER.error(
					String.format(
							"An SQL Exception occured while registering driver '%s'. See log for more details.",
							driverName), e);
			return false;
		}

		LOGGER.debug("Driver registered");

		LOGGER.debug("Trying to connect...");
		try {
			tryConnection();
		} catch (SQLException e) {
			LOGGER.error("Connection failed. See log for more details.", e);
			return false;
		}

		LOGGER.info("Connection successfull!");
		return true;
	}

	private String invalidJarLocationMessage() {
		return String
				.format("Invalid driver JAR archive location '%s'. See log for more details.",
						driverJarLoc);
	}

	private File checkDriverLocation() throws FileNotFoundException,
			InvalidJarLocationException {
		File driverJar = new File(driverJarLoc);
		if (!driverJar.exists()) {
			throw new FileNotFoundException("File not found for location: "
					+ driverJarLoc);
		}
		if (!driverJar.isFile()
				|| !driverJar.getName().toLowerCase().endsWith(".jar")) {
			throw new InvalidJarLocationException(
					"Given location exists, but does not point to a JAR file.");
		}
		return driverJar;
	}

	private boolean tryConnection() throws SQLException {
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(dbUrl, user, password);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (SQLException e) {
					// I don't care
				}
			}
		}
		return connection != null;
	}

	private Driver loadDriver(ClassLoader classLoader)
			throws LoadDriverException {
		Driver driver;

		try {
			driver = (Driver) Class.forName(driverName, true, classLoader)
					.newInstance();
		} catch (InstantiationException e) {
			throw new LoadDriverException(e);
		} catch (IllegalAccessException e) {
			throw new LoadDriverException(e);
		} catch (ClassNotFoundException e) {
			throw new LoadDriverException(e);
		}

		return driver;
	}
}

// Driver wrapper. Thanks to Nick Sayer and his detailed instructions about how
// to load JDBC driver in runtime: http://www.kfu.com/~nsayer/Java/dyn-jdbc.html
class DriverShim implements Driver {
	private Driver driver;

	DriverShim(Driver d) {
		this.driver = d;
	}

	@Override
	public boolean acceptsURL(String u) throws SQLException {
		return this.driver.acceptsURL(u);
	}

	@Override
	public Connection connect(String u, Properties p) throws SQLException {
		return this.driver.connect(u, p);
	}

	@Override
	public int getMajorVersion() {
		return this.driver.getMajorVersion();
	}

	@Override
	public int getMinorVersion() {
		return this.driver.getMinorVersion();
	}

	@Override
	public DriverPropertyInfo[] getPropertyInfo(String u, Properties p)
			throws SQLException {
		return this.driver.getPropertyInfo(u, p);
	}

	@Override
	public boolean jdbcCompliant() {
		return this.driver.jdbcCompliant();
	}

	// Java 1.7
	public java.util.logging.Logger getParentLogger()
			throws SQLFeatureNotSupportedException {
		throw new SQLFeatureNotSupportedException();
	}

}

// Custom Exceptions
@SuppressWarnings("serial")
class LoadDriverException extends Exception {

	public LoadDriverException(Throwable cause) {
		super(cause);
	}

}

@SuppressWarnings("serial")
class InvalidJarLocationException extends Exception {

	public InvalidJarLocationException(String message) {
		super(message);
	}

}
