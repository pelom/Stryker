package stryker.database;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;

import javax.activation.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ReplacementDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSet;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.operation.TransactionOperation;

import stryker.exception.StrykerException;

/**
 * Tool for DBUnit.
 */
public class DBUnitHelper {

	/**
	 * Reset the database to dataset content.
	 * @param resourcePath Path for dbunit dataset.
	 * @param connection {@link Connection}.
	 * @deprecated By {@link #cleanInsert(String, Connection)}.
	 */
	@Deprecated
	public static void init(String resourcePath, Connection connection) {
		new DBUnitHelper().cleanInsert(resourcePath, connection);
	}

	/**
	 * Reset the database to dataset content performing a {@link TransactionOperation#CLEAN_INSERT} from DBUnit.
	 * @param resourcePath Path for dbunit dataset.
	 * @param connection {@link Connection}.
	 */
	public void cleanInsert(String resourcePath, Connection connection) {
		execute(resourcePath, connection, TransactionOperation.CLEAN_INSERT);
	}

	/**
	 * Reset the database to dataset content.
	 * @param resourcePath Path for dbunit dataset.
	 * @deprecated By {@link #cleanInsert(String)}
	 */
	@Deprecated
	public static void init(String resourcePath) {
		new DBUnitHelper().cleanInsert(resourcePath);
	}

	/**
	 * Reset the database to dataset content performing a {@link TransactionOperation#CLEAN_INSERT} from DBUnit.
	 * @param resourcePath Path for dbunit dataset.
	 */
	public void cleanInsert(String resourcePath) {
		Connection connection = ConnectionHelper.getConnection();
		try {
			new DBUnitHelper().cleanInsert(resourcePath, connection);
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new StrykerException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Clean the database with a {@link TransactionOperation#DELETE_ALL} from DBUnit.
	 * @param resourcePath Path for dbunit dataset.
	 */
	public void deleteAll(String resourcePath) {
		Connection connection = ConnectionHelper.getConnection();
		try {
			execute(resourcePath, connection, TransactionOperation.DELETE_ALL);
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new StrykerException(e.getMessage(), e);
			}
		}
	}

	public void truncate(String resourcePath, Connection connection) {
		execute(resourcePath, connection, DatabaseOperation.TRUNCATE_TABLE);
	}

	public void truncate(String resourcePath) {
		Connection connection = ConnectionHelper.getConnection();
		try {
			truncate(resourcePath, connection);
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new StrykerException(e.getMessage(), e);
			}
		}
	}

	/**
	 * Clean the database.
	 * @param resourcePath Path for dbunit dataset.
	 * @deprecated By {@link #deleteAll(String)}
	 */
	public static void clean(String resourcePath) {
		new DBUnitHelper().deleteAll(resourcePath);
	}

	/**
	 * Generate a DBUnit dataSet file from {@link DataSource}.
	 * @param path Place where dataset will be created. If path does not exist, it will be created.
	 * @param connection {@link Connection} to {@link DataSource}.
	 */
	// TODO: TEST NOT COVERED EXCEPTIONS.
	public static void generateDataSet(String path, Connection connection) {
		try {
			IDatabaseConnection dbUnitConnection = new DatabaseConnection(connection);
			IDataSet dataSet = dbUnitConnection.createDataSet();
			File file = new File(path);
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			FlatXmlDataSet.write(dataSet, new FileOutputStream(file));
		} catch (SQLException e) {
			throw new StrykerException(e.getMessage(), e);
		} catch (DataSetException e) {
			throw new StrykerException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			throw new StrykerException(e.getMessage(), e);
		} catch (IOException e) {
			throw new StrykerException(e.getMessage(), e);
		} catch (DatabaseUnitException e) {
			throw new StrykerException(e.getMessage(), e);
		}
	}
	
	/**
	 * Execute dbunit operations in datasource.
	 * @param resourcePath Path for dbunit dataset.
	 * @param connection connection {@link Connection}.
	 * @param operations {@link DatabaseOperation} to be executed.
	 */
	void execute(String resourcePath, Connection connection, DatabaseOperation... operations) {
		try {
			InputStream resourceAsStream = DBUnitHelper.class.getResourceAsStream(resourcePath);
			
			FlatXmlDataSetBuilder builder = new FlatXmlDataSetBuilder();
			builder.setCaseSensitiveTableNames(true);
			IDataSet dataSet = builder.build(resourceAsStream);
			
			ReplacementDataSet replacementDataSet = new ReplacementDataSet(dataSet);
			replacementDataSet.addReplacementObject("[null]", null);
			IDatabaseConnection iConnection = new DatabaseConnection(connection);
			
			for(DatabaseOperation operation : operations) {
				operation.execute(iConnection, replacementDataSet);
			}
			resourceAsStream.close();
		} catch (DatabaseUnitException e) {
			throw new StrykerException(e.getMessage(), e);
		} catch (SQLException e) {
			throw new StrykerException(e.getMessage(), e);
		} catch (IOException e) {
			throw new StrykerException(e.getMessage(), e);
		}
	}

	public void truncateAndInsert(String resourcePath, Connection connection) {
		execute(resourcePath, connection, TransactionOperation.TRUNCATE_TABLE);
		execute(resourcePath, connection, TransactionOperation.INSERT);
	}

	public void truncateAndInsert(String resourcePath) {
		Connection connection = ConnectionHelper.getConnection();
		try {
			execute(resourcePath, connection, TransactionOperation.TRUNCATE_TABLE);
			execute(resourcePath, connection, TransactionOperation.INSERT);
		} finally {
			try {
				connection.close();
			} catch (SQLException e) {
				throw new StrykerException(e.getMessage(), e);
			}
		}
	}
}