package org.directtruststandards.timplus.client;

import java.awt.EventQueue;

import org.directtruststandards.timplus.client.packets.CredRequest;
import org.directtruststandards.timplus.client.packets.CredRequestProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class TIMPlusClientApplication
{
    public static void main(String[] args) 
    {

        ApplicationContext ctx = new SpringApplicationBuilder(TIMPlusClientApplication.class)
                .headless(false).run(args);

        EventQueue.invokeLater(() -> 
        {

        	final RosterFrame ex = ctx.getBean(RosterFrame.class);
            ex.setVisible(true);
            
            ProviderManager.addIQProvider(CredRequest.ELEMENT, CredRequest.NAMESPACE, new CredRequestProvider());
        });
    }
}
