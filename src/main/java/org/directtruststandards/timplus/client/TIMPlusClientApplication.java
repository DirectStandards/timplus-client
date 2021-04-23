package org.directtruststandards.timplus.client;

import org.directtruststandards.timplus.client.packets.CredRequest;
import org.directtruststandards.timplus.client.packets.CredRequestProvider;
import org.directtruststandards.timplus.client.roster.RosterFrame;
import org.jivesoftware.smack.provider.ProviderManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class TIMPlusClientApplication
{
    public static void main(String[] args) 
    {

        new SpringApplicationBuilder(TIMPlusClientApplication.class)
                .headless(false).run(args);

    	final RosterFrame ex = new RosterFrame();
    	ex.setVisible(true);
    	ProviderManager.addIQProvider(CredRequest.ELEMENT, CredRequest.NAMESPACE, new CredRequestProvider());
    }
}
