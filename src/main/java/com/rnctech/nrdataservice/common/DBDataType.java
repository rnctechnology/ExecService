package com.rnctech.nrdataservice.common;

import java.sql.Types;

public class DBDataType {
	
	public enum SourceTableType {TABLE, VIEW}
	
	public enum KeyType {PRIMARY, FOREIGN, CANDIDATE}
	
	public enum DataType {
	    STRING(Types.VARCHAR),
	    TEXT(Types.CLOB),
	    CHAR(Types.CHAR),
	    DATETIME(Types.TIMESTAMP),
	    DATE(Types.DATE),
	    TIME(Types.TIME),
	    SHORT(Types.SMALLINT),
	    INTEGER(Types.INTEGER),
	    LONG(Types.BIGINT),
	    BIGINT(Types.BIGINT),
	    FLOAT(Types.FLOAT),
	    DOUBLE(Types.DOUBLE),
	    DECIMAL(Types.DECIMAL),
	    BOOLEAN(Types.BIT);

	    private int jdbcType;
	    DataType(int jdbcType) {
	        this.jdbcType = jdbcType;
	    }

	    public int jdbcType() {
	        return jdbcType;
	    }
	}

	 public class FieldData {
		    public String name;
		    public int type;
		    public boolean nullable = true;
		    public int precision = 0;
		    public int scale = 0;

		    public FieldData(String name, int type) {
		        this.name = name;
		        this.type = type;
		    }

		    public FieldData(String name, int type, boolean nullable, int precision, int scale) {
		        this.name = name;
		        this.type = type;
		        this.nullable = nullable;
		        this.precision = precision;
		        this.scale = scale;
		    }
		}



}
