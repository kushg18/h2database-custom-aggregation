package org.h2.api;
/**
 * Created by Kushal Gevaria and Akshay Pudage
 * 
 */
import java.sql.Connection;
import java.sql.SQLException;

public class PIVOT implements AggregateFunction{
	Object result;

	@Override
	public void init(Connection conn) throws SQLException {
	}

	@Override
	public int getType(int[] inputTypes) throws SQLException {
		System.out.println("In Get Type");
		if (inputTypes.length < 1) {
	           throw new java.sql.SQLException("PIVOT aggregate function requires an input type.");
	    }
		return inputTypes[0];
	}

	@Override
	public void add(Object value) throws SQLException {
		Object[] listOfValues = (Object[]) value;
		int lengthOfList = listOfValues.length;
		result = listOfValues[lengthOfList / 2];
		System.out.println("Length:" + lengthOfList);

	}

	@Override
	public Object getResult() throws SQLException {
		System.out.println(result);
		return result;
	}

}
