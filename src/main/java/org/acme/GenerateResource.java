package org.acme;

import io.vertx.core.json.JsonObject;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.security.SecureRandom;

@ApplicationScoped
@Path("/v1/api")
public class GenerateResource {

    private final Logger log = LoggerFactory.getLogger(GenerateResource.class);

    @ConfigProperty(name = "git.username")
    public String gitUser;

    @ConfigProperty(name = "git.password")
    public String gitPassword;

    @ConfigProperty(name = "git.url")
    public String gitRepo;

    protected Git git;

    @POST
    @Path("generate")
    @Produces(MediaType.TEXT_PLAIN)
    @Consumes(MediaType.APPLICATION_JSON)
    public String generate(JsonObject specification) {
        log.info("{}", specification);
        try {
            createGitRepo(specification);
            createGitDevBranch(specification);
        } catch (Exception e) {
            log.warn(e.getMessage());
            return "failed";
        }
        return "done";
    }

    protected void createGitRepo(JsonObject specification) throws IOException, GitAPIException, URISyntaxException {
        File localPath = File.createTempFile("tempRepo", "");
        Files.delete(localPath.toPath());

        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
        try {
            SystemReader.getInstance().getUserConfig().clear();
        } catch (ConfigInvalidException e) {
            log.warn(e.getMessage());
        }

        try {
            git = Git.cloneRepository()
                    .setURI(gitRepo)
                    .setDirectory(localPath)
                    .setCredentialsProvider(cp)
                    .call();

            log.info("Cloned master repo: {}", git.getRepository().getDirectory());

            final String branchName = "api-spec-" + getAlphaNumericString(4);
            log.info("Git Branch URL: {}", gitRepo.replaceAll(".git", "") + "/src/" + branchName);

            git.checkout()
                    .setCreateBranch(true)
                    .setName(branchName)
                    .call();

            final StoredConfig config = git.getRepository().getConfig();
            RemoteConfig remoteConfig = new RemoteConfig(config, "gogs");
            URIish uri = new URIish(git.getRepository().getDirectory().toURI()
                    .toURL());
            remoteConfig.addURI(uri);
            remoteConfig.addPushRefSpec(new RefSpec("HEAD:refs/heads/" + branchName));
            remoteConfig.update(config);
            config.save();

            File myFile = new File(git.getRepository().getDirectory().getParent(), "openapi-spec.json");
            try (FileWriter fileWriter = new FileWriter(myFile)) {
                PrintWriter printWriter = new PrintWriter(fileWriter);
                printWriter.print(specification);
                printWriter.close();
            }

            git.add().addFilepattern("openapi-spec.json").call();
            RevCommit commit = git.commit().setMessage("\uD83E\uDDA9 Initial commit \uD83E\uDDA9").call();
            log.info("Committed: {}", commit.getId());
            git.push().setCredentialsProvider(cp).setRemote("origin").call();
            log.info("Pushed: {}", commit.getId());

        } catch (Exception ex) {
            log.warn(ex.getMessage());
        }
        FileUtils.deleteDirectory(localPath);
    }

    protected String getAlphaNumericString(int n) {
        String alpha = "0123456789"
                + "abcdefghijklmnopqrstuvxyz";
        StringBuilder sb = new StringBuilder(n);
        SecureRandom r = new SecureRandom();
        for (int i = 0; i < n; i++) {
            // generate a random number between
            // 0 to alpha variable length
            int index
                    = (int) (alpha.length()
                    * r.nextDouble());
            sb.append(alpha
                    .charAt(index));
        }
        return sb.toString();
    }

    protected void createGitDevBranch(JsonObject specification) throws IOException, GitAPIException, URISyntaxException {
        File localPath = File.createTempFile("dev", "");
        Files.delete(localPath.toPath());

        CredentialsProvider cp = new UsernamePasswordCredentialsProvider(gitUser, gitPassword);
        try {
            SystemReader.getInstance().getUserConfig().clear();
        } catch (ConfigInvalidException e) {
            log.warn(e.getMessage());
        }

        final String branchName = "dev";

        try {
            boolean isDevExist = Git
                    .lsRemoteRepository()
                    .setRemote(gitRepo)
                    .setCredentialsProvider(cp)
                    .callAsMap().containsKey("refs/heads/" + branchName);

            CloneCommand cm = Git.cloneRepository()
                    .setURI(gitRepo)
                    .setDirectory(localPath)
                    .setCredentialsProvider(cp);
            git = isDevExist? cm.setBranch(branchName).call(): cm.call();

            log.info("Cloned repo: {}", git.getRepository().getDirectory());
            log.info("Git Branch URL: {}", gitRepo.replaceAll(".git", "") + "/src/" + branchName);

            git.checkout()
                    .setCreateBranch(!isDevExist)
                    .setName(branchName)
                    .call();

            final StoredConfig config = git.getRepository().getConfig();
            RemoteConfig remoteConfig = new RemoteConfig(config, "gogs");
            URIish uri = new URIish(git.getRepository().getDirectory().toURI()
                    .toURL());
            remoteConfig.addURI(uri);
            remoteConfig.addPushRefSpec(new RefSpec("HEAD:refs/heads/" + branchName));
            remoteConfig.update(config);
            config.save();

            File myFile = new File(git.getRepository().getDirectory().getParent(), "openapi-spec.json");
            try (FileWriter fileWriter = new FileWriter(myFile)) {
                PrintWriter printWriter = new PrintWriter(fileWriter);
                printWriter.print(specification);
                printWriter.close();
            }

            git.add().addFilepattern("openapi-spec.json").call();
            RevCommit commit = git.commit().setMessage("\uD83E\uDDA9 Initial commit \uD83E\uDDA9").call();
            log.info("Committed: {}", commit.getId());
            git.push().setCredentialsProvider(cp).setRemote("origin").call();
            log.info("Pushed: {}", commit.getId());

        } catch (Exception ex) {
            log.warn(ex.getMessage());
        }
        FileUtils.deleteDirectory(localPath);
    }
}
