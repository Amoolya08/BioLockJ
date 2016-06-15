/** 
 * Author:  anthony.fodor@gmail.com    
 * This code is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version,
* provided that any use properly credits the author.
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details at http://www.gnu.org * * */


package utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.Properties;

public class ConfigReader
{
	private final Properties props;
	private final File propertiesFile;
	
	public File getPropertiesFile()
	{
		return propertiesFile;
	}
	
	public static final String TRUE = "TRUE";
	public static final String YES = "YES";

	// by convention, directories should contain the string "DIR"
	// by convention, paths should contain the string "PATH"
	public static final String BIOLOCKJ_BASE_DIR = "JAVA_BASE_DIR";
	public static final String SCRIPT_BASE_DIR = "SCRIPT_BASE_DIR";
	public static final String BLAST_PRELIMINARY_STRING = "BLAST_PRELIMINARY_STRING";
	public static final String BLAST_BINARY_DIR= "BLAST_BINARY_DIR";
	public static final String QUERY_SEQUENCE_PATH = "QUERY_SEQUENCE_PATH";
	public static final String CARDS_DATABASE_FASTA_PATH = "CARDS_DATABASE_FASTA";
	public static final String FASTA_TO_CHUNK_PATH = "FASTA_TO_CHUNK_PATH";
	public static final String BASE_OUTPUT_DIR = "BASE_OUTPUT_DIR";
	public static final String CLUSTER_NAME = "CLUSTER_NAME";
	
	public boolean isSetToTrue(String namedProperty)
	{
		Object obj = props.get(namedProperty);

		if (obj == null)
			return false;

		if (obj.toString().equalsIgnoreCase(TRUE)
				|| obj.toString().equalsIgnoreCase(YES))
			return true;

		return false;
	}
	
	public String getAProperty(String namedProperty) throws Exception
	{
		Object obj = props.get(namedProperty);

		if (obj == null)
			return null;
			
		return obj.toString();
	}
	
	
	public ConfigReader(File propertiesFile) throws Exception
	{
		this.propertiesFile = propertiesFile;
		InputStream in = new FileInputStream(propertiesFile);
		props = new Properties();
		props.load(in);

	}
}
