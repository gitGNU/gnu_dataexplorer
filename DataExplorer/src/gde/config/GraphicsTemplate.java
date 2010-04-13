/**************************************************************************************
  	This file is part of GNU DataExplorer.

    GNU DataExplorer is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    DataExplorer is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with GNU DataExplorer.  If not, see <http://www.gnu.org/licenses/>.
    
    Copyright (c) 2008 - 2010 Winfried Bruegmann
****************************************************************************************/
package gde.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import gde.log.Level;
import java.util.logging.Logger;

import gde.GDE;

/**
 * CSlass managing graphics visualization, store, restore
 * @author Winfried Brügmann 
 */
public class GraphicsTemplate extends Properties {
	static final long			serialVersionUID	= 260357;
	static final Logger		log								= Logger.getLogger(GraphicsTemplate.class.getName());

	private boolean				isAvailable				= false;
	private boolean				isSaved						= false;																				// indicates if template is saved to file
	private String				defaultFileName;
	private String				selectedFileName;
	private String				currentFileFilePath;
	private String				templatePath;
	private String				templateFilePath;

	/**
	 * constructor using the application home path and the device signature as initialization parameter
	 * @param deviceSignature - device signature as String (Picolario_K1)
	 */
	public GraphicsTemplate(String deviceSignature) {
		this.templatePath = Settings.getInstance().getGraphicsTemplatePath();
		this.defaultFileName = deviceSignature + Settings.GRAPHICS_TEMPLATES_EXTENSION.substring(Settings.GRAPHICS_TEMPLATES_EXTENSION.length() - 4);
		this.templateFilePath = this.defaultFileName;
		log.log(Level.FINE, "graphics template file is " + this.templateFilePath); //$NON-NLS-1$
	}

	/**
	 * @return the isAvailable
	 */
	public boolean isAvailable() {
		return this.isAvailable;
	}

	/**
	 * @return the isSaved
	 */
	public boolean isSaved() {
		return this.isSaved;
	}

	/**
	 * load the properties from file
	 */
	public void load() {
		try {
			this.currentFileFilePath = this.templatePath + GDE.FILE_SEPARATOR_UNIX + ((this.selectedFileName == null) ? this.defaultFileName : this.selectedFileName);
			log.log(Level.FINE, "opening template file " + this.currentFileFilePath); //$NON-NLS-1$
			this.loadFromXML(new FileInputStream(new File(this.currentFileFilePath)));
			this.isAvailable = true;
			log.log(Level.FINE, "template file successful loaded " + this.currentFileFilePath); //$NON-NLS-1$
		}
		catch (InvalidPropertiesFormatException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage());
		}
	}

	/**
	 * store the properties to file
	 */
	public void store() {
		try {
			// check if templatePath exist, else create directory
			if (!(new File(this.templatePath).exists())) {
				File tmpPath = new File(this.templatePath);
				tmpPath.mkdir();
			}

			this.currentFileFilePath = this.templatePath + GDE.FILE_SEPARATOR_UNIX + ((this.selectedFileName == null) ? this.defaultFileName : this.selectedFileName);
			this.storeToXML(new FileOutputStream(new File(this.currentFileFilePath)), "-- DataExplorer GraphicsTemplate --"); //$NON-NLS-1$
			this.isSaved = true;
			this.selectedFileName = null;
		}
		catch (InvalidPropertiesFormatException e) {
			log.log(Level.SEVERE, e.getMessage(), e);
		}
		catch (Exception e) {
			log.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * @param newValue the isSaved to set
	 */
	public void setSaved(boolean newValue) {
		this.isSaved = newValue;
	}

	/**
	 * @return the newFileName
	 */
	public String getNewFileName() {
		return this.selectedFileName;
	}

	/**
	 * @param newFileName the newFileName to set
	 */
	public void setNewFileName(String newFileName) {
		this.selectedFileName = newFileName;
	}

	/**
	 * @return the usedFileName
	 */
	public String getCurrentFilePath() {
		return this.currentFileFilePath;
	}

	/**
	 * @return the default filename for the current device and channel configuration number
	 */
	public String getDefaultFileName() {
		return this.defaultFileName;
	}

}
