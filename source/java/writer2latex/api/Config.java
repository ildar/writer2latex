/************************************************************************
 *
 *  Config.java
 *
 *  Copyright: 2002-2018 by Henrik Just
 *
 *  This file is part of Writer2LaTeX.
 *  
 *  Writer2LaTeX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Writer2LaTeX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Writer2LaTeX.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Version 2.0 (2018-03-11)
 *
 */

package writer2latex.api;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.IllegalArgumentException;
import java.util.List;
import java.util.Map;

/** This is an interface for configuration of a {@link Converter}.
 *  A configuration always supports simple name/value options.
 *  In addition, you can read and write configurations using streams
 *  or abstract file names. The format depends on the {@link Converter}
 *  implementation, cf. the user's manual.
 */
public interface Config {
    
    /** Read a default configuration: The available configurations depend on the
     *  {@link Converter} implementation
     *
     * @param sName the name of the configuration
     * @throws IllegalArgumentException if the configuration does not exist
     */
	public void readDefaultConfig(String sName) throws IllegalArgumentException;
	 
    /** Read a configuration (stream based version) 
     * 
     * @param is the <code>InputStream</code> to read from
     * @throws IOException if an error occurs reading the stream, or the data
     * is not in the right format
     */
	public void read(InputStream is) throws IOException;
	
	/** Read a configuration (file based version) 
	 * 
	 * @param file the <code>File</code> to read from
	 * @throws IOException if the file does not exist, an error occurs reading
	 * the file, or the data is not in the right format
	 */
	public void read(File file) throws IOException;
    
	/** Write the configuration (stream based version)
	 * 
	 * @param os the <code>OutputStream</code> to write to
	 * @throws IOException if an error occurs writing to the stream
	 */
	public void write(OutputStream os) throws IOException;
    
	/** Write the configuration (file based version)
	 * 
	 * @param file the <code>File</code> to write to
	 * @throws IOException if an error occurs writing to the file
	 */
	public void write(File file) throws IOException;
	
	/** Get the definitions of all defined parameters. Parameters are defined
	 *  in configuration files. A parameter has a name and a list of associated
	 *  values, the first value being the default value. The actual values are
	 *  accessed as ordinary options. The current value of parameters are
	 *  automatically applied to option values by the the get method.
	 *  
	 * @return map from parameter names to list of possible parameter values
	 */
	public Map<String,List<String>> getParameters();
	
	/** Set a name/value option. Options that are not defined by the
	 * {@link Converter} implementation as well as null values are
	 * silently ignored
	 * 
	 * @param sName the name of the option
	 * @param sValue the value of the option
	 */
	public void setOption(String sName, String sValue);
	
	/** Get a named option
	 * 
	 * @param sName the name of the option
	 * @return the value of the option, or <code>null</code> if the option does
	 * not exist or the given name is null
	 */
	public String getOption(String sName);
	
	/** Get a complex option
	 * 
	 * @param sName the name of the complex option
	 * @return the option
	 */
	public ComplexOption getComplexOption(String sName);
	
}

