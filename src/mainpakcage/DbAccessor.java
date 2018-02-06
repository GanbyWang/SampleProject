package mainpakcage;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/** Used to access database */
public class DbAccessor {
    public Connection connection;

    /**
     * Constructor method
     * Would create a new connection to the table
     * */
    public DbAccessor() {
        connection = getConnected();
    }

    /** Connect to the table */
    public Connection getConnected() {
        // Relation information
        String driver = "org.postgresql.Driver";
        String url = "jdbc:postgresql://localhost:5432/projectdb";
        String username = "alex";
        String password = "PakChoi3421";
        try {
            Class.forName(driver);
            connection = (Connection) DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    /** Disconnect from the table */
    public void disconnected() {
        // Close the connection after getting the result
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Extract a single tuple specified by a SQL command
     * Return a double array
     * */
    public List<Double> extractSingleTuple(String SQL) {
        List<Double> tuple = new ArrayList<Double>();

        PreparedStatement preparedStmt;
        try {
            preparedStmt = (PreparedStatement)connection.prepareStatement(SQL);
            ResultSet resultSet = preparedStmt.executeQuery();

            resultSet.next();

            for(int i = 1; i <= 8; i++) {
                tuple.add(resultSet.getDouble(i));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tuple;
    }

    /**
     * Extract a single tuple specified by its id
     * Return a double array
     * */
    public List<Double> getTupleById(int id) {
        String SQL = "SELECT * FROM hr_comma_sep WHERE id=" + id + ";";

        return extractSingleTuple(SQL);
    }

    /**
     * Get the number of tuples of the table
     * Use a SQL command to realize
     * */
    public int getTupleNum() {
        int tupleNum = -1;

        // The SQL to query
        String SQL = "SELECT COUNT(*) FROM hr_comma_sep;";

        PreparedStatement preparedStmt;
        try {
            preparedStmt = (PreparedStatement)connection.prepareStatement(SQL);
            ResultSet resultSet = preparedStmt.executeQuery();

            resultSet.next();
            tupleNum = resultSet.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return tupleNum;
    }

    /** Generate a sample specified by the fraction */
    public List<List<Double>> getSample(double fraction) {
        List<List<Double>> sample = new ArrayList<List<Double>>();

        String SQL = "SELECT * FROM hr_comma_sep TABLESAMPLE bernoulli(" + fraction + ");";

        PreparedStatement preparedStmt;
        try {
            preparedStmt = (PreparedStatement)connection.prepareStatement(SQL);
            ResultSet resultSet = preparedStmt.executeQuery();

            while(resultSet.next()) {
                List<Double> tuple = new ArrayList<Double>();

                for(int i = 1; i <= 8; i++) {
                    tuple.add(resultSet.getDouble(i));
//                    System.out.printf("%f ", resultSet.getDouble(i));
                }

//                System.out.printf("%s ", resultSet.getString(9));
//                System.out.printf("%s ", resultSet.getString(10));
//                System.out.printf("%f\n", resultSet.getDouble(11));

                sample.add(tuple);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sample;
    }

    /** Generate a sample together with a constraint query */
    public List<List<Double>> getSample(double fraction, String constraint) {
        List<List<Double>> sample = new ArrayList<List<Double>>();

        /*
         * Reconstruct the SQL query
         * Can only put TABLESAMPLE in the inner query
         * */
        String sampleSQL = "(SELECT * FROM hr_comma_sep TABLESAMPLE bernoulli(" + fraction + ")) AS sample";
        String preSQL = constraint.substring(0, constraint.indexOf("FROM")) + "FROM ";
        String postSQL = "";

        if(constraint.indexOf("WHERE") > 0) {
            postSQL += " ";
            postSQL += constraint.substring(constraint.indexOf("WHERE"));
        } else {
            postSQL += ";";
        }

        String SQL = preSQL + sampleSQL + postSQL;

        PreparedStatement preparedStmt;
        try {
            preparedStmt = (PreparedStatement)connection.prepareStatement(SQL);
            ResultSet resultSet = preparedStmt.executeQuery();

            while(resultSet.next()) {
                List<Double> tuple = new ArrayList<Double>();

                for(int i = 1; i <= 8; i++) {
                    tuple.add(resultSet.getDouble(i));
                }

                sample.add(tuple);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return sample;
    }

    /** Generate a sample with only one column */
    public List<Double> getSingleColSample(int fraction, String targetCol) {
        List<Double> result = new ArrayList<Double>();
        String SQL = "SELECT " + targetCol + " FROM hr_comma_sep TABLESAMPLE bernoulli(" + fraction + ");";

        PreparedStatement preparedStmt;
        try {
            preparedStmt = (PreparedStatement)connection.prepareStatement(SQL);
            ResultSet resultSet = preparedStmt.executeQuery();

            while(resultSet.next()) {
                result.add(resultSet.getDouble(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return result;
    }

    /** Use SQL query to generate the mean and variance of a sample */
    public String colInfo(String targetCol, int sampleFraction, String colName) {
        String columnInfo = "";

        String SQL = "SELECT AVG(sample." + targetCol + "), ";
        SQL += "VARIANCE(sample." + targetCol + ") FROM ";
        SQL += "(SELECT " + targetCol + " FROM hr_comma_sep TABLESAMPLE bernoulli(" + sampleFraction + ")) AS sample;";
//        System.out.println(SQL);

        PreparedStatement preparedStmt;
        try {
            preparedStmt = (PreparedStatement)connection.prepareStatement(SQL);
            ResultSet resultSet = preparedStmt.executeQuery();

            resultSet.next();

            double average = resultSet.getDouble(1);
            double variance = resultSet.getDouble(2);

            // Round up average and variance
            BigDecimal bdAVG = new BigDecimal(average);
            double roundedAVG = bdAVG.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();

            BigDecimal bdVAR = new BigDecimal(variance);
            double roundedVAR = bdVAR.setScale(5, BigDecimal.ROUND_HALF_UP).doubleValue();

            columnInfo += "The average of column " + colName + " is " + roundedAVG + ". ";
            columnInfo += "The variance of column " + colName + " is " + roundedVAR + ". ";

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return columnInfo;
    }
}