package org.directtruststandards.timplus.client.config;

import java.awt.Window;
import java.io.File;
import java.io.IOException;

import org.apache.commons.configuration2.YAMLConfiguration;
import org.apache.commons.configuration2.builder.ReloadingFileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.lang3.StringUtils;

public class ConfigurationManager
{
	static protected ConfigurationManager INSTANCE;
	
	protected final ReloadingFileBasedConfigurationBuilder<YAMLConfiguration> configBuilder;
	
	protected org.apache.commons.configuration2.Configuration config;
	
	public static synchronized ConfigurationManager getInstance()
	{
		if (INSTANCE == null)
			INSTANCE = new ConfigurationManager();
		
		return INSTANCE;
	}
	
	public ConfigurationManager()
	{
		final File configFile = new File("timplusClient.yml");
		
		try
		{
			if (!configFile.exists())
				configFile.createNewFile();
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Unable to create configuration file.", e);
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
	
	public boolean isCompleteConfiguration()
	{
		final Configuration config = getConfiguration();
		
		return (!StringUtils.isEmpty(config.getDomain()) && !StringUtils.isEmpty(config.getUsername()) && !StringUtils.isEmpty(config.getPassword()));
	}
	
	public Configuration getConfiguration()
	{		
		final Configuration retVal = new Configuration();
		
		Object setting = config.getProperty("timplus.im.domain");
		retVal.setDomain(setting == null ? "" : setting.toString());

		setting = config.getProperty("timplus.im.username");
		retVal.setUsername(setting == null ? "" : setting.toString());
		
		setting = config.getProperty("timplus.im.password");
		retVal.setPassword(setting == null ? "" : setting.toString());
		
		setting = config.getProperty("timplus.im.server");
		retVal.setServer(setting == null ? "" : setting.toString());
		
		return retVal;
	}
	
	public void doConfigure(Window w)
	{
		final ConfigurationDialog configDialog = new ConfigurationDialog(w, config);
		
		configDialog.setVisible(true);
	}
}
