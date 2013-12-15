package org.yflyud.projects.jdbctest;

public class App {
	public static void main(String[] args) {
		if (args.length != 5) {
			throw new IllegalArgumentException(
					"Invalid argument list. Expected arguments: Driver JAR location, JDBC driver name, Database URL, User name, Password");
		}
		String driverJarLoc = args[0];
		String driverName = args[1];
		String dbUrl = args[2];
		String user = args[3];
		String password = args[4];

		JDBCTester tester = new JDBCTester(driverJarLoc, driverName, dbUrl,
				user, password);
		tester.test();
	}
}
