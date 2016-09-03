package org.zstack.test;

import org.apache.catalina.LifecycleState;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;
import org.apache.commons.io.FileUtils;
import org.apache.coyote.http11.Http11NioProtocol;
import org.eclipse.jetty.server.ServerConnector;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.appliancevm.ApplianceVmGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.kvm.KVMGlobalProperty;
import org.zstack.network.service.virtualrouter.VirtualRouterGlobalProperty;
import org.zstack.storage.backup.sftp.SftpBackupStorageGlobalProperty;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;

public class WebBeanConstructor extends BeanConstructor {
    private Server jetty;
    private Tomcat tomcat;
    private static final String BASE_DIR = "target/test-classes/tomcat";
    private static final String APP_NAME = "zstack";
    private int port = 8989;
    private UriComponentsBuilder ub;
    private String siteUrl;

    public WebBeanConstructor() {
        // initialize static block in Platform
        Platform.getUuid();
        KVMGlobalProperty.AGENT_PORT = port;
        VirtualRouterGlobalProperty.AGENT_PORT = port;
        SftpBackupStorageGlobalProperty.AGENT_PORT = port;
        ApplianceVmGlobalProperty.AGENT_PORT = port;
    }
    
    private void generateWarFile() {
        WarBuilder wbuilder = new WarBuilder();
        wbuilder.setSpringConfigPath(springConfigPath);
        wbuilder.setWarExportedToPath(BASE_DIR);
        wbuilder.build();
    }

    private void prepareTomcat() throws IOException {
        File dir = new File(BASE_DIR);
        FileUtils.deleteDirectory(dir);
        FileUtils.forceMkdir(dir);

        generateWarFile();

        jetty = new Server();
        ServerConnector http = new ServerConnector(jetty);
        http.setHost("localhost");
        http.setPort(port);
        http.setDefaultProtocol("HTTP/1.1");
        jetty.addConnector(http);
        final WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(new File(BASE_DIR, APP_NAME + ".war").getAbsolutePath());
        jetty.setHandler(webapp);
    }

    public void startTomcat() {
        try {
            prepareTomcat();
            jetty.start();
         //   jetty.join();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    public void stopTomcat() {
        try {
            if(jetty != null)
                jetty.stop();
        } catch (Exception e){
            throw new CloudRuntimeException(e);
        }
    }
    
    @Override
    public ComponentLoader build() {
        generateSpringConfig();
        startTomcat();
        return Platform.getComponentLoader();
    }
    
    public String getSiteUrl() {
        if (siteUrl == null) {
            ub = UriComponentsBuilder.fromHttpUrl("http://localhost");
            ub.port(getPort());
            siteUrl = ub.build().toUriString();
        }
        return siteUrl;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public String buildUrl(String...path) {
        UriComponentsBuilder ubb = UriComponentsBuilder.fromHttpUrl(getSiteUrl());
        for (String p : path) {
            ubb.path(p);
        }
        return ubb.build().toUriString();
    }
}
