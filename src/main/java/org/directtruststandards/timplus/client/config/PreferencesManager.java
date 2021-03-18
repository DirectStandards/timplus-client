package org.directtruststandards.timplus.client.config;

import java.awt.Window;
import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;

public class PreferencesManager 
{
	static protected PreferencesManager INSTANCE;
	
	protected final ReloadingFileBasedConfigurationBuilder<YAMLConfiguration> configBuilder;
	
	protected org.apache.commons.configuration2.Configuration config;
	
	public static synchronized PreferencesManager getInstance()
	{
		if (INSTANCE == null)
			INSTANCE = new PreferencesManager();
		
		return INSTANCE;
	}	
	
	private PreferencesManager()
	{
		final File configFile = new File("timplusClient.yml");
		
		try
		{
			if (!configFile.exists())
				configFile.createNewFile();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Unable to create preferences file.", e);
		}
		
		final Parameters params = new Parameters();
		configBuilder = new ReloadingFileBasedConfigurationBuilder<>(YAMLConfiguration.class)
				.configure(params.fileBased().setFile(configFile));
		
		configBuilder.setAutoSave(true);
		
		config = null;
		try
		{
			config = configBuilder.getConfiguration();
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Can't get configuration from timplusClient.yml file.", e);
		}
	}	
	
	public Preferences getPreferences()
	{		
		final Preferences retVal = new Preferences();
		
		Object setting = config.getProperty("timplus.preferences.groupchat.defaultNickName");
		retVal.setGroupChatNickName(setting == null ? "" : setting.toString());
		
		return retVal;
	}	
	
	public void doMaintainPreferences(Window w)
	{
		final PreferencesDialog configDialog = new PreferencesDialog(w, config);
		
		configDialog.setVisible(true);
	}	
}
