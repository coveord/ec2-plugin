package hudson.plugins.ec2;

import java.net.URI;
import java.net.URL;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class EucalyptusTest {
    @Rule
    public JenkinsRule r = new JenkinsRule();

    @Test
    public void configRoundTrip() throws Exception {
        Eucalyptus cloud = new Eucalyptus(
                "test",
                URI.create("https://ec2"),
                URI.create("https://s3"),
                false,
                null,
                "test",
                null,
                "0",
                null,
                null,
                null);
        r.jenkins.clouds.add(cloud);
        r.jenkins.save();
        JenkinsRule.WebClient wc = r.createWebClient();
        HtmlPage p = wc.goTo(cloud.getUrl() + "configure");
        HtmlForm f = p.getFormByName("config");
        r.submit(f);
        r.assertEqualBeans(
                cloud,
                r.jenkins.getCloud("test"),
                "name,ec2EndpointUrl,s3EndpointUrl,useInstanceProfileForCredentials,roleArn,roleSessionName,credentialsId,sshKeysCredentialsId,instanceCap,templates");
    }
}
