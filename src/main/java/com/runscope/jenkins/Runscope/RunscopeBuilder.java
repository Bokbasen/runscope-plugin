package com.runscope.jenkins.Runscope;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.*;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.concurrent.*;

/**
 * RunscopeBuilder {@link Builder}.
 *
 * email:  help@runscope.com
 */
public class RunscopeBuilder implements SimpleBuildStep {

    private static final String DISPLAY_NAME = "Runscope Test Configuration";
    private static final String TEST_RESULTS_PASS = "pass";
 	
    private String triggerEndPoint;
    private String accessToken;
    private int timeout = 60;

    @DataBoundConstructor
    public RunscopeBuilder(String triggerEndPoint, String accessToken, int timeout) {
		this.triggerEndPoint = triggerEndPoint;
		this.accessToken = accessToken;
		if(timeout >= 0 )
		    this.timeout = timeout;
	}

    @DataBoundSetter
    public void setTriggerEndPoint(String triggerEndPoint) {
        this.triggerEndPoint = triggerEndPoint;
    }

    /**
	 * @return the triggerEndPoint
	 */
	public String getTriggerEndPoint() {
		return triggerEndPoint;
	}

	@DataBoundSetter
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
	 * @return the accessToken
	 */
	public String getAccessToken() {
		return accessToken;
	}

	@DataBoundSetter
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
	 * @return the timeout
	 */
	public Integer getTimeout() {
		return timeout;
	}

    @Override
    public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher, @Nonnull TaskListener taskListener) throws InterruptedException, IOException {
        PrintStream logger = taskListener.getLogger();
        EnvVars envVars = run.getEnvironment(taskListener);
        String expandedTriggerEndPoint = envVars.expand(triggerEndPoint);

        logger.println("Build Trigger Configuration:");
        logger.println("Trigger End Point:" + expandedTriggerEndPoint);
        logger.println("Access Token:" + accessToken);
        logger.println("Timeout:" + timeout);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<String> future = executorService.submit(new RunscopeTrigger(logger, expandedTriggerEndPoint, accessToken /*triggerEndPoint, */));

        try {
            String result = future.get(timeout, TimeUnit.SECONDS);
            if (!TEST_RESULTS_PASS.equalsIgnoreCase(result)) {
                run.setResult(Result.FAILURE);
            }
        } catch (TimeoutException e) {
            logger.println("Timeout Exception:" + e.toString());

            run.setResult(Result.FAILURE);
            e.printStackTrace();
        } catch (Exception e) {
            logger.println("Exception:" + e.toString());
            run.setResult(Result.FAILURE);
            e.printStackTrace();
        }
        executorService.shutdownNow();
    }

    @Override
    public boolean prebuild(AbstractBuild<?, ?> abstractBuild, BuildListener buildListener) {
        return false;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> abstractBuild, Launcher launcher, BuildListener buildListener) throws InterruptedException, IOException {
        return false;
    }

    @Override
    public Action getProjectAction(AbstractProject<?, ?> abstractProject) {
        return null;
    }

    @Override
    public Collection<? extends Action> getProjectActions(AbstractProject<?, ?> abstractProject) {
        return null;
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    /**
     * Descriptor for {@link RunscopeBuilder}. Used as a singleton.
     * The class is marked as public so that it can be accessed from views.
     */
    @Extension 
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * In order to load the persisted global configuration, you have to 
         * call load() in the constructor.
         */
        public DescriptorImpl() {
            load();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * This name is used in the configuration screen.
         */
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }
}

