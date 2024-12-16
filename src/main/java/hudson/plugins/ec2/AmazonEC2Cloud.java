/*
 * The MIT License
 *
 * Copyright (c) 2004-, Kohsuke Kawaguchi, Sun Microsystems, Inc., and a number of other of contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.ec2;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeRegionsResponse;
import software.amazon.awssdk.services.ec2.model.Region;
import edu.umd.cs.findbugs.annotations.Nullable;
import hudson.Extension;
import hudson.Util;
import hudson.model.Failure;
import hudson.model.ItemGroup;
import hudson.plugins.ec2.util.AmazonEC2Factory;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import org.kohsuke.stapler.verb.POST;

/**
 * The original implementation of {@link EC2Cloud}.
 *
 * @author Kohsuke Kawaguchi
 */
public class AmazonEC2Cloud extends EC2Cloud {
    private static final Logger LOGGER = Logger.getLogger(AmazonEC2Cloud.class.getName());

    /**
     * Represents the region. Can be null for backward compatibility reasons.
     */
    private String region;

    private String altEC2Endpoint;

    private boolean noDelayProvisioning;

    @DataBoundConstructor
    public AmazonEC2Cloud(
            String name,
            boolean useInstanceProfileForCredentials,
            String credentialsId,
            String region,
            String privateKey,
            String sshKeysCredentialsId,
            String instanceCapStr,
            List<? extends SlaveTemplate> templates,
            String roleArn,
            String roleSessionName) {
        super(
                name,
                useInstanceProfileForCredentials,
                credentialsId,
                privateKey,
                sshKeysCredentialsId,
                instanceCapStr,
                templates,
                roleArn,
                roleSessionName);
        this.region = region;
    }

    @Deprecated
    public AmazonEC2Cloud(
            String name,
            boolean useInstanceProfileForCredentials,
            String credentialsId,
            String region,
            String privateKey,
            String instanceCapStr,
            List<? extends SlaveTemplate> templates,
            String roleArn,
            String roleSessionName) {
        super(
                name,
                useInstanceProfileForCredentials,
                credentialsId,
                privateKey,
                instanceCapStr,
                templates,
                roleArn,
                roleSessionName);
        this.region = region;
    }

    /**
     * @deprecated Use public field "name" instead.
     */
    @Deprecated
    public String getCloudName() {
        return name;
    }

    public String getRegion() {
        if (region == null) {
            region = DEFAULT_EC2_HOST; // Backward compatibility
        }
        // Handles pre 1.14 region names that used the old AwsRegion enum, note we don't change
        // the region here to keep the meta-data compatible in the case of a downgrade (is that right?)
        if (region.indexOf('_') > 0) {
            return region.replace('_', '-').toLowerCase(Locale.ENGLISH);
        }
        return region;
    }

    public static URI getEc2EndpointUri(String region) {
        return URI.create("https://" + getAwsPartitionHostForService(region, "ec2"));
    }

    @Override
    public URI getEc2EndpointUri() {
        return getEc2EndpointUri(getRegion());
    }

    @Override
    public URI getS3EndpointUri() {
        return URI.create("https://" + getAwsPartitionHostForService(getRegion(), "s3") + "/");
    }

    public boolean isNoDelayProvisioning() {
        return noDelayProvisioning;
    }

    @DataBoundSetter
    public void setNoDelayProvisioning(boolean noDelayProvisioning) {
        this.noDelayProvisioning = noDelayProvisioning;
    }

    public String getAltEC2Endpoint() {
        return altEC2Endpoint;
    }

    @DataBoundSetter
    public void setAltEC2Endpoint(String altEC2Endpoint) {
        this.altEC2Endpoint = altEC2Endpoint;
    }

    @Override
    protected AwsCredentialsProvider createCredentialsProvider() {
        return createCredentialsProvider(
                isUseInstanceProfileForCredentials(),
                getCredentialsId(),
                getRoleArn(),
                getRoleSessionName(),
                getRegion());
    }

    @Extension
    public static class DescriptorImpl extends EC2Cloud.DescriptorImpl {

        @Override
        public String getDisplayName() {
            return "Amazon EC2";
        }

        @POST
        public FormValidation doCheckCloudName(@QueryParameter String value) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
            try {
                Jenkins.checkGoodName(value);
            } catch (Failure e) {
                return FormValidation.error(e.getMessage());
            }
            return FormValidation.ok();
        }

        @POST
        public FormValidation doCheckAltEC2Endpoint(@QueryParameter String value) {
            if (Util.fixEmpty(value) != null) {
                try {
                    new URL(value);
                } catch (MalformedURLException ignored) {
                    return FormValidation.error(Messages.AmazonEC2Cloud_MalformedUrl());
                }
            }
            return FormValidation.ok();
        }

        @RequirePOST
        public ListBoxModel doFillRegionItems(
                @QueryParameter String altEC2Endpoint,
                @QueryParameter boolean useInstanceProfileForCredentials,
                @QueryParameter String credentialsId)
                throws IOException, ServletException {
            ListBoxModel model = new ListBoxModel();
            if (Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                try {
                    AwsCredentialsProvider credentialsProvider =
                            createCredentialsProvider(useInstanceProfileForCredentials, credentialsId);
                    Ec2Client client = AmazonEC2Factory.getInstance()
                            .connect(credentialsProvider, determineEC2EndpointURI(altEC2Endpoint));
                    DescribeRegionsResponse regions = client.describeRegions();
                    List<Region> regionList = regions.regions();
                    for (Region r : regionList) {
                        String name = r.regionName();
                        model.add(name, name);
                    }
                } catch (SdkClientException ex) {
                    // Ignore, as this may happen before the credentials are specified
                }
            }
            return model;
        }

        // Will use the alternate EC2 endpoint if provided by the UI (via a @QueryParameter field), or use the default
        // value if not specified.
        // VisibleForTesting
        URI determineEC2EndpointURI(@Nullable String altEC2Endpoint) throws MalformedURLException {
            URI defaultEndpointURI = URI.create(DEFAULT_EC2_ENDPOINT);
            if (Util.fixEmpty(altEC2Endpoint) == null) {
                return defaultEndpointURI;
            }
            try {
                URL url = new URL(altEC2Endpoint);
                return url.toURI();
            } catch (MalformedURLException e) {
                LOGGER.log(
                        Level.WARNING,
                        "The alternate EC2 endpoint is malformed ({0}). Using the default endpoint ({1})",
                        new Object[] {altEC2Endpoint, DEFAULT_EC2_ENDPOINT});
                return defaultEndpointURI;
            } catch (URISyntaxException ex) {
                throw new MalformedURLException(ex.getMessage());
            }
        }

        @RequirePOST
        public FormValidation doTestConnection(
                @AncestorInPath ItemGroup context,
                @QueryParameter String region,
                @QueryParameter boolean useInstanceProfileForCredentials,
                @QueryParameter String credentialsId,
                @QueryParameter String sshKeysCredentialsId,
                @QueryParameter String roleArn,
                @QueryParameter String roleSessionName)
                throws IOException, ServletException {

            if (Util.fixEmpty(region) == null) {
                region = DEFAULT_EC2_HOST;
            }

            return super.doTestConnection(
                    context,
                    getEc2EndpointUri(region),
                    useInstanceProfileForCredentials,
                    credentialsId,
                    sshKeysCredentialsId,
                    roleArn,
                    roleSessionName,
                    region);
        }
    }
}
